#!/usr/bin/env python3
"""
Sample frames from local videos for the dataset.

Takes one frame every --every seconds, skipping frames nearly identical to the last kept one
(a static shot yields one frame, not hundreds) and blurry frames (motion blur ruins ball
edges). Output: data/frames/<video-stem>_<ms>.jpg plus data/frames.csv linking each frame to
its source video, so attribution follows the frame.

Usage:
    python3 extract_frames.py VIDEO_DIR [--out data] [--every 1.0]
"""
import argparse
import csv
import os

import cv2
import numpy as np


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("videos")
    ap.add_argument("--out", default="data")
    ap.add_argument("--every", type=float, default=1.0, help="seconds between samples")
    ap.add_argument("--min-change", type=float, default=12.0, help="mean abs pixel change to keep")
    ap.add_argument("--min-sharpness", type=float, default=60.0, help="Laplacian variance floor")
    args = ap.parse_args()
    frame_dir = os.path.join(args.out, "frames")
    os.makedirs(frame_dir, exist_ok=True)
    rows = []
    for name in sorted(os.listdir(args.videos)):
        if not name.lower().endswith((".mp4", ".webm", ".mkv", ".mov")):
            continue
        stem = os.path.splitext(name)[0]
        cap = cv2.VideoCapture(os.path.join(args.videos, name))
        fps = cap.get(cv2.CAP_PROP_FPS) or 30
        step = max(1, int(round(fps * args.every)))
        last, i = None, 0
        while True:
            ok = cap.grab()
            if not ok:
                break
            if i % step == 0:
                ok, frame = cap.retrieve()
                if ok:
                    gray = cv2.cvtColor(cv2.resize(frame, (320, 180)), cv2.COLOR_BGR2GRAY)
                    sharp = cv2.Laplacian(gray, cv2.CV_64F).var()
                    changed = last is None or np.abs(gray.astype(int) - last).mean() > args.min_change
                    if sharp >= args.min_sharpness and changed:
                        ms = int(i / fps * 1000)
                        path = os.path.join(frame_dir, f"{stem}_{ms}.jpg")
                        cv2.imwrite(path, frame, [cv2.IMWRITE_JPEG_QUALITY, 92])
                        rows.append({"file": os.path.relpath(path, args.out), "video": name, "ms": ms})
                        last = gray.astype(int)
            i += 1
        cap.release()
        print(f"{name}: {sum(1 for r in rows if r['video'] == name)} frames")
    with open(os.path.join(args.out, "frames.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["file", "video", "ms"])
        w.writeheader()
        w.writerows(rows)


if __name__ == "__main__":
    main()
