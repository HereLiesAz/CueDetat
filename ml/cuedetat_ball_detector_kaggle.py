# cuedetat_ball_detector_kaggle.py
#
# Kaggle-native training of a YOLOv8n BALL detector for CueDetat, from the labelling pipeline's
# output (ml/dataset: Label Studio -> ls_to_yolo.py -> make_ball_dataset.py). The pocket/table
# detector is trained separately (cuedetat_pocket_detector_kaggle.py).
#
# Cells delimited with `# %%` for jupytext-style conversion. Convert with:
#   `python ml/_scripts/py_to_ipynb.py ml/cuedetat_ball_detector_kaggle.py`
#
# Output: /kaggle/working/exports/ball_detector_fp16.tflite and ball_detector_final.onnx.
# Nothing here is wired into the app: the app finds balls with CvBallDetector today. A model
# that passes the gate in Cell 6 is the start of that work, not the end of it.

# %% [markdown]
# # CueDetat ball detector — Kaggle train
#
# YOLOv8n on player's-eye frames labelled in Label Studio. Four classes by default
# (cue, eight, solid, stripe: what the app's `BallType` needs); the fifteen-number scheme is
# available when there is enough data (set `SCHEME = "number"` and build the dataset with
# `make_ball_dataset.py --scheme number`).
#
# Before running: build the dataset (ml/dataset/README.md, "Ball detector") and upload the output
# folder as a Kaggle dataset, then set `BALLS_DATASET` below.

# %% [markdown]
# ## Cell 1 — Install (skip torch: Kaggle's preinstalled torch matches the GPU)

# %%
!pip install -q ultralytics kagglehub pyyaml

# %% [markdown]
# ## Cell 2 — Fetch the dataset

# %%
import os, glob, yaml, kagglehub

# The Kaggle dataset you uploaded from `make_ball_dataset.py --out ...`. Change this to its slug.
BALLS_DATASET = "hereliesaz/cue-detat-balls"
SCHEME = "type"          # "type" (cue/eight/solid/stripe) or "number" (cue, 1..15, ball)

BALLS_PATH = kagglehub.dataset_download(BALLS_DATASET)
print("dataset:", BALLS_PATH)
print("contents:", os.listdir(BALLS_PATH)[:10])

yamls = glob.glob(os.path.join(BALLS_PATH, "**", "data.yaml"), recursive=True)
assert yamls, f"no data.yaml under {BALLS_PATH}: upload the folder make_ball_dataset.py wrote"
SRC_YAML = yamls[0]
with open(SRC_YAML) as f:
    cfg = yaml.safe_load(f)
NAMES = cfg["names"]
NAMES = [NAMES[i] for i in sorted(NAMES)] if isinstance(NAMES, dict) else list(NAMES)
print("classes:", NAMES)

EXPECTED = {
    "type": ["cue", "eight", "solid", "stripe"],
    "number": ["cue"] + [str(n) for n in range(1, 16)] + ["ball"],
}[SCHEME]
assert NAMES == EXPECTED, f"data.yaml classes {NAMES} are not the {SCHEME!r} scheme {EXPECTED}"

# %% [markdown]
# ## Cell 3 — Rewrite data.yaml with absolute paths (the uploaded one points at the uploader's disk)

# %%
SRC_ROOT = os.path.dirname(SRC_YAML)
DATA_YAML = "/kaggle/working/balls_data.yaml"
with open(DATA_YAML, "w") as f:
    yaml.safe_dump({
        "path": SRC_ROOT,
        "train": "images/train",
        "val": "images/val",
        "nc": len(NAMES),
        "names": NAMES,
    }, f)

counts = {}
for split in ("train", "val"):
    n = len(os.listdir(os.path.join(SRC_ROOT, "images", split)))
    boxes = [0] * len(NAMES)
    for p in glob.glob(os.path.join(SRC_ROOT, "labels", split, "*.txt")):
        for line in open(p):
            if line.strip():
                boxes[int(line.split()[0])] += 1
    counts[split] = (n, dict(zip(NAMES, boxes)))
    print(f"{split}: {n} images, boxes per class {counts[split][1]}")
thin = [n for n, c in counts["train"][1].items() if c < 100]
if thin:
    print(f"WARNING: fewer than 100 training boxes for {thin}; those classes will be weak.")

# %% [markdown]
# ## Cell 4 — Train YOLOv8n
#
# Longer than the pocket detector, and with augmentation, on purpose: a published run of the same
# kind (YOLOv5, four ball classes, own data) went from 30% precision / 45% recall at 30 epochs to
# 92% / 93.5% at 350 epochs with rotation augmentation. Those numbers are from another dataset and
# do not transfer; the lesson is that ball detectors are slow to converge on small data.
#
# Augmentation: the view is always a player's-eye one, so no vertical flips and only small
# rotations. Hue shift is small in the type scheme and off in the number scheme, where the hue is
# what tells 3 from 11 from 5.

# %%
import torch
from ultralytics import YOLO

device = 0 if torch.cuda.is_available() else "cpu"
print("Device:", device, "| CUDA available:", torch.cuda.is_available())

PROJECT = "/kaggle/working/runs"
NAME = "ball_detector"

model = YOLO("yolov8n.pt")
results = model.train(
    data=DATA_YAML,
    epochs=300,
    patience=40,
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
    degrees=10.0,
    flipud=0.0,
    fliplr=0.5,
    hsv_h=0.015 if SCHEME == "type" else 0.0,
    hsv_s=0.6,
    hsv_v=0.4,
    close_mosaic=15,
)
print("Training done.")

# %% [markdown]
# ## Cell 5 — Validate, per class

# %%
metrics = model.val()
print(f"mAP50:    {metrics.box.map50:.4f}")
print(f"mAP50-95: {metrics.box.map:.4f}")
print(f"Precision: {metrics.box.mp:.4f}")
print(f"Recall:    {metrics.box.mr:.4f}")
per_class = {}
for i, name in enumerate(NAMES):
    if i < len(metrics.box.maps):
        per_class[name] = float(metrics.box.maps[i])
        print(f"  {name:>8}: mAP50-95 {per_class[name]:.3f}")

# %% [markdown]
# ## Cell 6 — Gate
#
# mAP50 over the whole set can hide a class that does not work. Solid against stripe is the one
# the app needs, so each class is held to a floor as well.

# %%
GATE_MAP50 = 0.80
GATE_CLASS_MAP = 0.40
weak = {n: round(v, 3) for n, v in per_class.items() if v < GATE_CLASS_MAP}
ok = metrics.box.map50 >= GATE_MAP50 and not weak
print(f"Gate: mAP50 >= {GATE_MAP50} and every class mAP50-95 >= {GATE_CLASS_MAP}: "
      f"{'PASS' if ok else 'FAIL'}")
if weak:
    print("Weak classes:", weak, "-> label more of them, or merge them, before deploying.")

# %% [markdown]
# ## Cell 7 — Export (TFLite FP16 with NMS, and ONNX)

# %%
import shutil

EXPORT_DIR = "/kaggle/working/exports"
os.makedirs(EXPORT_DIR, exist_ok=True)

exported = model.export(format="tflite", imgsz=640, half=True, nms=True)
print("Export returned:", exported)
tflite_src = None
if isinstance(exported, str):
    if exported.endswith(".tflite") and os.path.isfile(exported):
        tflite_src = exported
    elif os.path.isdir(exported):
        cands = [f for f in os.listdir(exported) if f.endswith(".tflite")]
        if cands:
            tflite_src = os.path.join(exported, cands[0])
if tflite_src is None:
    for root, _, files in os.walk(PROJECT):
        for f in files:
            if f.endswith(".tflite"):
                tflite_src = os.path.join(root, f)
                break
assert tflite_src and os.path.exists(tflite_src), f"could not find the exported .tflite ({exported!r})"

target = os.path.join(EXPORT_DIR, "ball_detector_fp16.tflite")
shutil.copy2(tflite_src, target)
print(f"TFLite: {target} ({os.path.getsize(target) / 1e6:.2f} MB)")

onnx_export = model.export(format="onnx", imgsz=640, opset=17, simplify=True)
onnx_target = os.path.join(EXPORT_DIR, "ball_detector_final.onnx")
if isinstance(onnx_export, str) and os.path.exists(onnx_export):
    shutil.copy2(onnx_export, onnx_target)
    print(f"ONNX: {onnx_target} ({os.path.getsize(onnx_target) / 1e6:.2f} MB)")

# %% [markdown]
# ## Cell 8 — Report

# %%
from datetime import datetime
report = f"""# Ball Detector Training Report

Generated: {datetime.now()}

## Hyperparameters
- Model: YOLOv8n, scheme `{SCHEME}`
- Epochs: up to 300 (early stop patience 40), batch 32, image size 640, AMP
- Augmentation: rotate 10 deg, fliplr 0.5, no flipud, hsv_h {0.015 if SCHEME == "type" else 0.0}, hsv_s 0.6, hsv_v 0.4

## Dataset
- {BALLS_DATASET}
- train {counts['train'][0]} images {counts['train'][1]}
- val   {counts['val'][0]} images {counts['val'][1]}

## Classes
{NAMES}

## Validation metrics
- mAP50:    {metrics.box.map50:.4f}
- mAP50-95: {metrics.box.map:.4f}
- Precision: {metrics.box.mp:.4f}
- Recall:    {metrics.box.mr:.4f}
- Per class mAP50-95: {per_class}
- Gate (mAP50 >= {GATE_MAP50}, every class >= {GATE_CLASS_MAP}): {'PASS' if ok else 'FAIL'}

## Exports
- {target}
- {onnx_target}

## Not done
Not wired into the app: CvBallDetector finds balls today. Validation is on the held-out split of
the same labelled set (hash-split by image id), so it says nothing yet about frames from other
tables, lighting, or phones; add in-app captures (ml/capture-relay) to the val split before
trusting it.
"""
with open("/kaggle/working/training_report.md", "w") as f:
    f.write(report)
print(report)
