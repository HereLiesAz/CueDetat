# cuedetat_pocket_detector_kaggle.py
#
# Kaggle-native re-train of the YOLOv8n pocket/pool detector for CueDetat.
# Mirrors the same hyperparameters and class set as the original Colab notebook,
# but stripped of Drive mounts and `/content/` paths so it runs unmodified on
# Kaggle GPU.
#
# Cells delimited with `# %%` for jupytext-style conversion. Convert with:
#   `python ml/_scripts/py_to_ipynb.py ml/cuedetat_pocket_detector_kaggle.py`
#
# Output: /kaggle/working/pocket_detector_fp16.tflite (deploy as
# app/src/main/assets/ml/merged_pocket_detector_final_float16.tflite).

# %% [markdown]
# # CueDetat pocket detector — Kaggle re-train
#
# YOLOv8n. 100 epochs, batch 32, imgsz 640, FP16 TFLite export with NMS.
# Datasets: `hereliesaz/cue-detat` + `diveshcrazy/pool-table-balls-classification`.

# %% [markdown]
# ## Cell 1 — Install (skip torch — Kaggle's preinstalled torch matches the GPU)

# %%
!pip install -q ultralytics kagglehub pyyaml

# %% [markdown]
# ## Cell 2 — Attach Kaggle datasets via kagglehub
#
# These also work via the Kaggle UI ("Add Data" → search), but kagglehub is
# explicit and survives notebook re-runs.

# %%
import os, kagglehub

CUE_PATH   = kagglehub.dataset_download("hereliesaz/cue-detat")
BALLS_PATH = kagglehub.dataset_download("diveshcrazy/pool-table-balls-classification")
print("cue-detat:", CUE_PATH)
print("balls    :", BALLS_PATH)
print("contents (cue-detat):", os.listdir(CUE_PATH)[:10])
print("contents (balls):    ", os.listdir(BALLS_PATH)[:10])

# %% [markdown]
# ## Cell 3 — Merge into a unified YOLO dataset
#
# Master class list (matches the deployed model's expectations):
#   0: pool-table
#   1: pool-table-hole  (pockets — what the AR overlay actually consumes)
#   2: pool-table-side
#
# Each source is scanned for `train/`, `valid/`, `test/` splits with
# `images/` + `labels/` subdirs. Class IDs are remapped per source via its
# `data.yaml` -> master class names mapping.

# %%
import os, shutil, yaml, glob

MASTER_CLASSES = ["pool-table", "pool-table-hole", "pool-table-side"]
COMBINED = "/kaggle/working/combined_dataset"

for split in ["train", "valid", "test"]:
    os.makedirs(os.path.join(COMBINED, split, "images"), exist_ok=True)
    os.makedirs(os.path.join(COMBINED, split, "labels"), exist_ok=True)

def find_data_yaml(root):
    for p in glob.iglob(os.path.join(root, "**", "data.yaml"), recursive=True):
        return p
    return None

def remap_label_file(src_path, dst_path, id_map):
    """Rewrite a YOLO label file remapping class IDs; drop classes not in id_map."""
    out_lines = []
    with open(src_path) as f:
        for line in f:
            parts = line.strip().split()
            if not parts:
                continue
            old_id = int(parts[0])
            if old_id not in id_map:
                continue
            new_id = id_map[old_id]
            out_lines.append(" ".join([str(new_id)] + parts[1:]))
    if out_lines:
        with open(dst_path, "w") as f:
            f.write("\n".join(out_lines) + "\n")

def merge_source(src_root, tag):
    yaml_path = find_data_yaml(src_root)
    if not yaml_path:
        print(f"  [{tag}] no data.yaml found, skipping")
        return 0
    with open(yaml_path) as f:
        cfg = yaml.safe_load(f)
    src_names = cfg.get("names") or []
    # Build remap: src class id -> master id (if name matches), else drop
    id_map = {i: MASTER_CLASSES.index(n)
              for i, n in enumerate(src_names)
              if n in MASTER_CLASSES}
    if not id_map:
        print(f"  [{tag}] no overlap with master classes "
              f"(src had {src_names}); skipping")
        return 0
    print(f"  [{tag}] class remap: "
          f"{ {src_names[i]: MASTER_CLASSES[id_map[i]] for i in id_map} }")

    src_base = os.path.dirname(yaml_path)
    copied = 0
    for split_in, split_out in [("train", "train"), ("valid", "valid"),
                                ("val", "valid"), ("test", "test")]:
        img_src = os.path.join(src_base, split_in, "images")
        lbl_src = os.path.join(src_base, split_in, "labels")
        if not os.path.isdir(img_src):
            continue
        img_dst = os.path.join(COMBINED, split_out, "images")
        lbl_dst = os.path.join(COMBINED, split_out, "labels")
        for img in os.listdir(img_src):
            stem, ext = os.path.splitext(img)
            new_name = f"{tag}__{stem}"
            shutil.copy2(os.path.join(img_src, img),
                         os.path.join(img_dst, new_name + ext))
            lbl_in = os.path.join(lbl_src, stem + ".txt")
            if os.path.exists(lbl_in):
                remap_label_file(
                    lbl_in,
                    os.path.join(lbl_dst, new_name + ".txt"),
                    id_map,
                )
            copied += 1
    return copied

print("Merging cue-detat...")
n1 = merge_source(CUE_PATH, "cue")
print(f"  copied {n1} images")
print("Merging pool-table-balls...")
n2 = merge_source(BALLS_PATH, "balls")
print(f"  copied {n2} images")

# Write the combined data.yaml
data_yaml = os.path.join(COMBINED, "data.yaml")
with open(data_yaml, "w") as f:
    yaml.safe_dump({
        "path":  COMBINED,
        "train": "train/images",
        "val":   "valid/images",
        "test":  "test/images",
        "nc":    len(MASTER_CLASSES),
        "names": MASTER_CLASSES,
    }, f)

# Quick sanity counts
for split in ["train", "valid", "test"]:
    n = len(os.listdir(os.path.join(COMBINED, split, "images")))
    print(f"  {split}: {n} images")
print(f"data.yaml written: {data_yaml}")

# %% [markdown]
# ## Cell 4 — Train YOLOv8n
#
# Hyperparameters mirror the original training: 100 epochs, batch 32, imgsz 640,
# AMP, periodic checkpointing.

# %%
import os, torch
from ultralytics import YOLO

device = 0 if torch.cuda.is_available() else "cpu"
print("Device:", device, "| CUDA available:", torch.cuda.is_available())
if torch.cuda.is_available():
    print("GPU:", torch.cuda.get_device_name(0))

PROJECT = "/kaggle/working/runs"
NAME    = "pocket_detector"

model = YOLO("yolov8n.pt")
results = model.train(
    data="/kaggle/working/combined_dataset/data.yaml",
    epochs=100,
    imgsz=640,
    batch=32,
    device=device,
    workers=4,
    cache=True,
    amp=True,
    save_period=10,
    project=PROJECT,
    name=NAME,
    exist_ok=True,
    patience=30,    # early stopping if no improvement
)
print("Training done.")

# %% [markdown]
# ## Cell 5 — Validate

# %%
metrics = model.val()
print(f"mAP50:    {metrics.box.map50:.4f}")
print(f"mAP50-95: {metrics.box.map:.4f}")
print(f"Precision: {metrics.box.mp:.4f}")
print(f"Recall:    {metrics.box.mr:.4f}")
print(f"Acceptance gate: mAP50 >= 0.80. "
      f"{'PASS' if metrics.box.map50 >= 0.80 else 'FAIL — investigate before deploying'}")

# %% [markdown]
# ## Cell 6 — Export to TFLite FP16 with NMS

# %%
import os, shutil

EXPORT_DIR = "/kaggle/working/exports"
os.makedirs(EXPORT_DIR, exist_ok=True)

print("Exporting TFLite FP16 with NMS...")
exported = model.export(format="tflite", imgsz=640, half=True, nms=True)
print("Export returned:", exported)

# Locate the actual .tflite file (export may return a dir or a file path)
tflite_src = None
if isinstance(exported, str):
    if exported.endswith(".tflite") and os.path.isfile(exported):
        tflite_src = exported
    elif os.path.isdir(exported):
        cands = [f for f in os.listdir(exported) if f.endswith(".tflite")]
        if cands:
            tflite_src = os.path.join(exported, cands[0])
if tflite_src is None:
    # fallback: scan the runs dir
    for root, _, files in os.walk(PROJECT):
        for f in files:
            if f.endswith(".tflite"):
                tflite_src = os.path.join(root, f)
                break

assert tflite_src and os.path.exists(tflite_src), \
    f"Could not locate exported .tflite (got {exported!r})"

target = os.path.join(EXPORT_DIR, "pocket_detector_fp16.tflite")
shutil.copy2(tflite_src, target)
print(f"\nFinal TFLite: {target}")
print(f"  size: {os.path.getsize(target) / 1e6:.2f} MB")

# Also export ONNX for parity with the deployed merged_pocket_detector_final.onnx
print("\nExporting ONNX...")
onnx_export = model.export(format="onnx", imgsz=640, opset=17, simplify=True)
onnx_target = os.path.join(EXPORT_DIR, "pocket_detector_final.onnx")
if isinstance(onnx_export, str) and os.path.exists(onnx_export):
    shutil.copy2(onnx_export, onnx_target)
    print(f"ONNX: {onnx_target}  ({os.path.getsize(onnx_target) / 1e6:.2f} MB)")

# %% [markdown]
# ## Cell 7 — Smoke-test the exported TFLite on a held-out image

# %%
import os, numpy as np, cv2
import tensorflow as tf  # comes preinstalled on Kaggle

interp = tf.lite.Interpreter(
    model_path="/kaggle/working/exports/pocket_detector_fp16.tflite"
)
interp.allocate_tensors()
inp_d, out_d = interp.get_input_details(), interp.get_output_details()
print("Input  :", inp_d[0]["shape"], inp_d[0]["dtype"])
print("Output :", [(o["shape"].tolist(), o["dtype"].__name__)
                   for o in out_d])

# pick a test image
test_imgs = sorted(
    os.path.join(r, f)
    for r, _, files in os.walk("/kaggle/working/combined_dataset/test/images")
    for f in files if f.lower().endswith((".jpg", ".png"))
)
if not test_imgs:
    test_imgs = sorted(
        os.path.join(r, f)
        for r, _, files in os.walk("/kaggle/working/combined_dataset/valid/images")
        for f in files if f.lower().endswith((".jpg", ".png"))
    )
print(f"Smoke-testing on {test_imgs[0]}")

img = cv2.cvtColor(cv2.imread(test_imgs[0]), cv2.COLOR_BGR2RGB)
SZ = int(inp_d[0]["shape"][1])
inp = np.expand_dims((cv2.resize(img, (SZ, SZ)) / 255.0)
                     .astype(inp_d[0]["dtype"]), 0)
interp.set_tensor(inp_d[0]["index"], inp)
interp.invoke()
out = interp.get_tensor(out_d[0]["index"])
print(f"Output tensor shape: {out.shape}")
print(f"Top detections (first 3 rows): {out[0, :3]}")

# %% [markdown]
# ## Cell 8 — Generate training_report.md

# %%
from datetime import datetime
report = f"""# Pocket Detector Training Report

Generated: {datetime.now()}

## Hyperparameters
- Model: YOLOv8n
- Epochs: 100 (early stop patience 30)
- Batch: 32
- Image size: 640
- AMP: True

## Datasets
- hereliesaz/cue-detat
- diveshcrazy/pool-table-balls-classification

## Classes
{MASTER_CLASSES}

## Validation metrics
- mAP50:    {metrics.box.map50:.4f}
- mAP50-95: {metrics.box.map:.4f}
- Precision: {metrics.box.mp:.4f}
- Recall:    {metrics.box.mr:.4f}

## Exports
- /kaggle/working/exports/pocket_detector_fp16.tflite
- /kaggle/working/exports/pocket_detector_final.onnx

## Deploy locations (in CueDetat repo)
- app/src/main/assets/ml/merged_pocket_detector_final_float16.tflite
- app/src/main/assets/ml/merged_pocket_detector_final.onnx
"""
with open("/kaggle/working/exports/training_report.md", "w") as f:
    f.write(report)
print(report)
