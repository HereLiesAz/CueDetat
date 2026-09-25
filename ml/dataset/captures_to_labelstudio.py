#!/usr/bin/env python3
"""
Turn in-app training captures (CaptureRecorder, stored by ml/capture-relay) into Label Studio
tasks, with what the app detected as pre-labels. Same project config as prelabel.py
(labelstudio/config.xml), so captures and web images label side by side and export through
ls_to_yolo.py.

Input: a checkout of the private captures repo (or any folder of cap_*.jpg / cap_*.json pairs,
searched recursively).

Pre-labels:
  table   the virtual table's four corners, only when the user had locked it or the snap fit
          agreed with the felt (IoU >= --min-iou); otherwise the pose is a guess and is left out.
  balls   the app's detections: cue -> "cue", eight -> "8", anything else -> "ball" (the app
          doesn't read numbers).

Images are served by Label Studio's local-file storage, so start it with
LABEL_STUDIO_LOCAL_FILES_SERVING_ENABLED=true and
LABEL_STUDIO_LOCAL_FILES_DOCUMENT_ROOT=<--root>.

Usage:
    git clone git@github.com:HereLiesAz/cuedetat-captures.git
    python3 captures_to_labelstudio.py --captures cuedetat-captures --root $PWD \
        [--out labelstudio/captures_tasks.json] [--min-iou 0.7]
"""
import argparse
import glob
import json
import os

TYPE_TO_LABEL = {"CUE": "cue", "EIGHT": "8"}


def task_for(meta, image_path, root, min_iou):
    w, h = meta["width"], meta["height"]
    results = []
    table = meta.get("table") or {}
    corners = table.get("cornersImage")
    trusted = table.get("locked") or (table.get("fitIou") or 0) >= min_iou
    if corners and len(corners) == 4 and trusted:
        results.append({
            "from_name": "table", "to_name": "image", "type": "polygonlabels",
            "original_width": w, "original_height": h,
            "value": {
                "points": [[x / w * 100, y / h * 100] for x, y in corners],
                "polygonlabels": ["table"],
            },
        })
    for b in meta.get("balls") or []:
        results.append({
            "from_name": "balls", "to_name": "image", "type": "rectanglelabels",
            "original_width": w, "original_height": h,
            "value": {
                "x": b["x"] / w * 100, "y": b["y"] / h * 100,
                "width": b["w"] / w * 100, "height": b["h"] / h * 100,
                "rotation": 0, "rectanglelabels": [TYPE_TO_LABEL.get(b.get("type"), "ball")],
            },
        })
    rel = os.path.relpath(os.path.abspath(image_path), root)
    return {
        "data": {
            "image": "/data/local-files/?d=" + rel,
            "id": meta["id"],
            "attribution": f"in-app capture, install {meta.get('installId', '?')[:8]}",
            "source": "capture",
        },
        "predictions": [{"model_version": "app-" + str(meta.get("appVersion", "")), "result": results}],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--captures", required=True)
    ap.add_argument("--root", required=True, help="Label Studio local-files document root")
    ap.add_argument("--out", default="labelstudio/captures_tasks.json")
    ap.add_argument("--min-iou", type=float, default=0.7)
    args = ap.parse_args()

    tasks, tables = [], 0
    for jpath in sorted(glob.glob(os.path.join(args.captures, "**", "cap_*.json"), recursive=True)):
        img = jpath[:-5] + ".jpg"
        if not os.path.exists(img):
            continue
        with open(jpath) as f:
            meta = json.load(f)
        t = task_for(meta, img, os.path.abspath(args.root), args.min_iou)
        tables += any(r["type"] == "polygonlabels" for r in t["predictions"][0]["result"])
        tasks.append(t)

    os.makedirs(os.path.dirname(args.out) or ".", exist_ok=True)
    with open(args.out, "w") as f:
        json.dump(tasks, f)
    print(f"{len(tasks)} tasks, {tables} with a trusted table outline -> {args.out}")


if __name__ == "__main__":
    main()
