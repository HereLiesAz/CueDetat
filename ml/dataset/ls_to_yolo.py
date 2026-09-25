#!/usr/bin/env python3
"""
Convert a Label Studio JSON export (made with labelstudio/config.xml) into two YOLO datasets.

  yolo/balls/   detection: one box per ball, classes cue, 1..15, ball (unknown number)
  yolo/table/   pose: one box around the felt plus its four corners as keypoints,
                clockwise from top-left

Only tasks with a human annotation are used; bare predictions are skipped, because an
unreviewed pre-label is not a label. Tasks are split into train/val by a stable hash of the
image id so re-exports keep the same split.

Images are copied from data/images when present (matched by id), from --local-root for tasks
served by Label Studio's local-file storage (in-app captures), otherwise downloaded from the
task's image URL.

Usage:
    python3 ls_to_yolo.py export.json [--data data] [--out yolo] [--val 0.15] [--local-root DIR]
"""
import argparse
import glob
import hashlib
import json
import os
import shutil
import sys

import numpy as np
import requests

from prelabel import BALL_LABELS, order_clockwise


def split_of(image_id, val):
    h = int(hashlib.sha1(image_id.encode()).hexdigest()[:8], 16) / 0xFFFFFFFF
    return "val" if h < val else "train"


LOCAL_PREFIX = "/data/local-files/?d="


def fetch(task, data_dir, dest_no_ext, local_root=""):
    iid = task["data"].get("id", str(task.get("id")))
    local = glob.glob(os.path.join(data_dir, "images", iid + ".*"))
    if local:
        dest = dest_no_ext + os.path.splitext(local[0])[1]
        shutil.copyfile(local[0], dest)
        return dest
    url = task["data"]["image"]
    if url.startswith(LOCAL_PREFIX) and local_root:
        src = os.path.join(local_root, url[len(LOCAL_PREFIX):])
        if not os.path.exists(src):
            return None
        dest = dest_no_ext + os.path.splitext(src)[1]
        shutil.copyfile(src, dest)
        return dest
    if not url.startswith("http"):
        return None
    r = requests.get(url, timeout=30, headers={"User-Agent": "CueDetat-dataset/1.0"})
    r.raise_for_status()
    dest = dest_no_ext + ".jpg"
    with open(dest, "wb") as f:
        f.write(r.content)
    return dest


def clamp01(v):
    return min(1.0, max(0.0, v))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("export")
    ap.add_argument("--data", default="data")
    ap.add_argument("--out", default="yolo")
    ap.add_argument("--val", type=float, default=0.15)
    ap.add_argument("--local-root", default="", help="Label Studio local-files document root")
    args = ap.parse_args()

    with open(args.export) as f:
        tasks = json.load(f)

    for kind in ("balls", "table"):
        for split in ("train", "val"):
            os.makedirs(os.path.join(args.out, kind, "images", split), exist_ok=True)
            os.makedirs(os.path.join(args.out, kind, "labels", split), exist_ok=True)

    counts = {"tasks": 0, "skipped": 0, "balls": 0, "tables": 0}
    for t in tasks:
        anns = [a for a in t.get("annotations", []) if not a.get("was_cancelled")]
        if not anns:
            counts["skipped"] += 1
            continue
        result = anns[-1]["result"]
        iid = t["data"].get("id", str(t.get("id")))
        split = split_of(iid, args.val)

        ball_lines, table_lines = [], []
        for r in result:
            v = r["value"]
            if r["type"] == "rectanglelabels" and r["from_name"] == "balls":
                cls = BALL_LABELS.index(v["rectanglelabels"][0])
                cx = (v["x"] + v["width"] / 2) / 100
                cy = (v["y"] + v["height"] / 2) / 100
                ball_lines.append(f"{cls} {clamp01(cx):.6f} {clamp01(cy):.6f} {v['width'] / 100:.6f} {v['height'] / 100:.6f}")
            elif r["type"] == "polygonlabels" and r["from_name"] == "table":
                pts = np.array(v["points"], float) / 100
                if len(pts) != 4:
                    print(f"  {iid}: table has {len(pts)} points, need 4; skipped", file=sys.stderr)
                    continue
                q = order_clockwise(pts)
                x0, y0 = q.min(axis=0).clip(0, 1)
                x1, y1 = q.max(axis=0).clip(0, 1)
                # Corners may lie off-image (table runs out of frame): keep them, mark visible=1.
                kp = " ".join(f"{x:.6f} {y:.6f} {2 if 0 <= x <= 1 and 0 <= y <= 1 else 1}" for x, y in q)
                table_lines.append(f"0 {(x0 + x1) / 2:.6f} {(y0 + y1) / 2:.6f} {x1 - x0:.6f} {y1 - y0:.6f} {kp}")

        try:
            img = None
            for kind, lines in (("balls", ball_lines), ("table", table_lines)):
                if kind == "table" and not lines:
                    continue
                dest = os.path.join(args.out, kind, "images", split, iid)
                if img is None:
                    img = fetch(t, args.data, dest, args.local_root)
                    if img is None:
                        raise RuntimeError("no image")
                else:
                    shutil.copyfile(img, dest + os.path.splitext(img)[1])
                with open(os.path.join(args.out, kind, "labels", split, iid + ".txt"), "w") as f:
                    f.write("\n".join(lines) + ("\n" if lines else ""))
        except Exception as e:
            print(f"  {iid}: {e}", file=sys.stderr)
            counts["skipped"] += 1
            continue
        counts["tasks"] += 1
        counts["balls"] += len(ball_lines)
        counts["tables"] += len(table_lines)

    root = os.path.abspath(args.out)
    with open(os.path.join(args.out, "balls", "data.yaml"), "w") as f:
        f.write(f"path: {root}/balls\ntrain: images/train\nval: images/val\nnames:\n")
        f.writelines(f"  {i}: '{n}'\n" for i, n in enumerate(BALL_LABELS))
    with open(os.path.join(args.out, "table", "data.yaml"), "w") as f:
        f.write(f"path: {root}/table\ntrain: images/train\nval: images/val\n"
                "kpt_shape: [4, 3]\nflip_idx: [1, 0, 3, 2]\nnames:\n  0: table\n")
    print(counts)


if __name__ == "__main__":
    main()
