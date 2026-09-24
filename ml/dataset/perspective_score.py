#!/usr/bin/env python3
"""
Sort a folder of pool photos by camera angle: player's-eye (oblique) versus overhead.

The app's camera is a phone held by a player, never above the table, and most public pool
datasets are the opposite (shot from above for projector trainers). So every image is
scored and labelled:

  oblique   the felt outline is a trapezoid (far edge shorter than the near edge, corners
            well off 90 degrees): what the app sees. Keep.
  overhead  the felt outline is close to a rectangle: shot from above. Drop.
  partial   the felt runs off the frame, so its shape can't be judged. Review by eye.
  none      no dominant felt colour found (close-ups, people, rooms, black and white).
            Review by eye.

Felt is looked for outside the orange hues (skin, wood, lamplight).

How: the felt is the largest region of the dominant saturated hue; its outline is reduced
to four corners; the score is the larger of (a) how unequal opposite edges are and (b) how
far the corners are from right angles.

Usage:
    python3 perspective_score.py [--data data]
Writes data/perspective.csv and adds an `angle` column to nothing else; the manifest is
left untouched.
"""
import argparse
import csv
import math
import os

import cv2
import numpy as np


def felt_quad(img):
    small = cv2.resize(img, (320, int(320 * img.shape[0] / img.shape[1])))
    hsv = cv2.cvtColor(small, cv2.COLOR_BGR2HSV)
    sat = (hsv[..., 1] > 70) & (hsv[..., 2] > 40)
    if sat.mean() < 0.1:
        return None, None, small.shape
    hist = np.bincount(hsv[..., 0][sat].ravel(), minlength=180).astype(float)
    # Skin, wood and warm lamplight sit at orange hues (OpenCV 8-30); felt never does. Without
    # this, portraits and wooden rails pass for felt.
    hist[8:31] = 0
    if hist.sum() < 0.08 * sat.size:
        return None, None, small.shape
    hue = int(np.argmax(np.convolve(hist, np.ones(9), mode="same")))
    lo, hi = (hue - 12) % 180, (hue + 12) % 180
    h = hsv[..., 0].astype(int)
    in_hue = ((h >= lo) & (h <= hi)) if lo < hi else ((h >= lo) | (h <= hi))
    mask = (in_hue & sat).astype(np.uint8) * 255
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, np.ones((9, 9), np.uint8))
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return None, None, small.shape
    c = max(contours, key=cv2.contourArea)
    if cv2.contourArea(c) < 0.12 * mask.size:
        return None, None, small.shape
    hull = cv2.convexHull(c)
    peri = cv2.arcLength(hull, True)
    quad = None
    for eps in np.linspace(0.01, 0.08, 15):
        approx = cv2.approxPolyDP(hull, eps * peri, True)
        if len(approx) == 4:
            quad = approx.reshape(4, 2).astype(float)
            break
    return quad, c, small.shape


def score(quad):
    sides = [np.linalg.norm(quad[(i + 1) % 4] - quad[i]) for i in range(4)]
    uneven = max(abs(1 - sides[0] / sides[2]), abs(1 - sides[1] / sides[3]))
    angles = []
    for i in range(4):
        a, b, c = quad[i - 1], quad[i], quad[(i + 1) % 4]
        v1, v2 = a - b, c - b
        cos = np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2) + 1e-9)
        angles.append(abs(math.degrees(math.acos(np.clip(cos, -1, 1))) - 90))
    return uneven, max(angles)


def touches_border(quad, shape, margin=3):
    h, w = shape[:2]
    return any(x < margin or y < margin or x > w - margin or y > h - margin for x, y in quad)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    args = ap.parse_args()
    with open(os.path.join(args.data, "manifest.csv"), newline="") as f:
        rows = list(csv.DictReader(f))
    out = []
    counts = {}
    for r in rows:
        img = cv2.imread(os.path.join(args.data, r["file"]))
        label, uneven, corner = "none", "", ""
        if img is not None:
            quad, _, shape = felt_quad(img)
            if quad is not None:
                u, a = score(quad)
                uneven, corner = f"{u:.3f}", f"{a:.1f}"
                if touches_border(quad, shape):
                    label = "partial"
                elif u > 0.2 or a > 12:
                    label = "oblique"
                else:
                    label = "overhead"
        counts[label] = counts.get(label, 0) + 1
        out.append({"id": r["id"], "file": r["file"], "angle": label, "uneven": uneven, "max_corner_dev": corner})
    with open(os.path.join(args.data, "perspective.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["id", "file", "angle", "uneven", "max_corner_dev"])
        w.writeheader()
        w.writerows(out)
    print(counts)


if __name__ == "__main__":
    main()
