#!/usr/bin/env python3
"""
Pre-label the kept links for correction in Label Studio: table outline, ball boxes, ball class.

Nothing here is ground truth. Every label is a guess made from colour alone, meant to be
accepted or fixed by a person, which is faster than drawing from scratch.

  table   the felt: largest region of the dominant non-orange saturated hue (same method as
          perspective_score.py), reduced to four corners, ordered clockwise from top-left.
  balls   islands inside the felt hull that are not felt, roughly round, and ball-sized for
          the table (a ball is ~2.25% of the table's long side; perspective widens the range).
  class   from the ball's pixels: mostly white -> cue, mostly black -> 8, otherwise the
          dominant hue picks the number pair (yellow 1/9, blue 2/10, red 3/11, purple 4/12,
          orange 5/13, green 6/14, maroon 7/15) and a white share picks stripe over solid.
          No hue -> "ball" (unknown). Expect this to be wrong often; it is only a head start.

Images come from data/images when fetch_openverse.py already has them, otherwise they are
downloaded (thumbnail fallback) into data/images. data/ is git-ignored.

Output:
  labelstudio/tasks.json      import into a Label Studio project (predictions included)
  labelstudio/config.xml      the project's labelling interface

Usage:
    python3 prelabel.py [--links links.csv] [--data data] [--out labelstudio]
                        [--local-root /abs/path/to/data]   # serve images from disk
                        [--limit N]

Without --local-root, tasks point at each image's source URL (all are reuse-licensed).
With it, they point at Label Studio local-file serving; start Label Studio with
LABEL_STUDIO_LOCAL_FILES_SERVING_ENABLED=true and
LABEL_STUDIO_LOCAL_FILES_DOCUMENT_ROOT=<that path>.
"""
import argparse
import csv
import json
import os
import sys
import time

import cv2
import numpy as np
import requests

from perspective_score import felt_quad

BALL_LABELS = ["cue"] + [str(n) for n in range(1, 16)] + ["ball"]

# OpenCV hue (0-180) bands for the solid colours, and the number each names.
HUE_BANDS = [
    ((0, 7), 3),      # red
    ((170, 180), 3),  # red
    ((8, 19), 5),     # orange
    ((20, 34), 1),    # yellow
    ((35, 90), 6),    # green
    ((91, 129), 2),   # blue
    ((130, 169), 4),  # purple
]

WORK_WIDTH = 640
MAX_BALLS = 18
MIN_FELT_IN_TABLE = 0.55


def load_image(row, img_dir, manifest_files):
    path = manifest_files.get(row["id"])
    if path and os.path.exists(path):
        return cv2.imread(path), path
    ext = os.path.splitext(row["url"].split("?")[0])[1].lower()
    path = os.path.join(img_dir, row["id"] + (ext if ext in (".jpg", ".jpeg", ".png") else ".jpg"))
    if not os.path.exists(path):
        # Openverse serves a thumbnail for every image; Wikimedia refuses bulk full-size fetches.
        thumb = f"https://api.openverse.org/v1/images/{row['id']}/thumb/"
        for url in (row["url"], thumb):
            if not url:
                continue
            try:
                r = requests.get(url, timeout=30, headers={"User-Agent": "CueDetat-dataset/1.0"})
                r.raise_for_status()
                with open(path, "wb") as f:
                    f.write(r.content)
                break
            except Exception as e:
                print(f"  {row['id']}: {e}", file=sys.stderr)
                time.sleep(1)
        else:
            return None, None
    return cv2.imread(path), path


def order_clockwise(quad):
    c = quad.mean(axis=0)
    ang = np.arctan2(quad[:, 1] - c[1], quad[:, 0] - c[0])
    q = quad[np.argsort(ang)]  # clockwise in image coordinates (y down)
    start = int(np.argmin(q[:, 0] + q[:, 1]))  # top-left: smallest x + y
    return np.roll(q, -start, axis=0)


def felt_mask(hsv, contour_mask):
    """Pixels inside the table hull that match its felt colour."""
    sat = (hsv[..., 1] > 70) & (hsv[..., 2] > 40)
    inside = contour_mask > 0
    h = hsv[..., 0][inside & sat]
    if h.size == 0:
        return None
    hue = int(np.argmax(np.bincount(h, minlength=180)))
    lo, hi = (hue - 12) % 180, (hue + 12) % 180
    hh = hsv[..., 0].astype(int)
    in_hue = ((hh >= lo) & (hh <= hi)) if lo < hi else ((hh >= lo) | (hh <= hi))
    return (in_hue & sat).astype(np.uint8) * 255, hue


def classify(hsv_patch, mask):
    px = hsv_patch[mask > 0]
    if len(px) < 10:
        return "ball", 0.0
    s, v, h = px[:, 1].astype(int), px[:, 2].astype(int), px[:, 0].astype(int)
    white = np.mean((s < 60) & (v > 170))
    black = np.mean(v < 60)
    if white > 0.6:
        return "cue", float(white)
    if black > 0.5:
        return "8", float(black)
    colour = (s > 90) & (v > 60)
    if colour.mean() < 0.15:
        return "ball", 0.0
    hist = np.bincount(h[colour], minlength=180)
    hue = int(np.argmax(hist))
    number = next(n for (lo, hi), n in HUE_BANDS if lo <= hue <= hi)
    # Dark red is the 7 (maroon); bright red the 3.
    if number == 3 and np.median(v[colour]) < 110:
        number = 7
    if white > 0.2:
        number += 8
    return str(number), float(hist[hue] / colour.sum())


def find_balls(small, hsv, felt, hull):
    """Circles resting on the felt that are mostly not felt-coloured."""
    gray = cv2.GaussianBlur(cv2.cvtColor(small, cv2.COLOR_BGR2GRAY), (5, 5), 1.5)
    w = small.shape[1]
    r_min, r_max = max(5, int(0.008 * w)), int(0.2 * w)
    circles = cv2.HoughCircles(gray, cv2.HOUGH_GRADIENT_ALT, dp=1.5, minDist=r_min * 2,
                               param1=200, param2=0.7, minRadius=r_min, maxRadius=r_max)
    if circles is None:
        return []
    out, kept = [], []
    for cx, cy, r in circles[0]:  # strongest first
        cx, cy, r = int(cx), int(cy), int(r)
        # A ball on the felt can stick out past the felt's hull, but not by more than half.
        if cv2.pointPolygonTest(hull, (float(cx), float(cy)), True) < -0.5 * r:
            continue
        # One ball, one circle: skip circles overlapping a stronger one (inner rings, stripes).
        if any((cx - kx) ** 2 + (cy - ky) ** 2 < (0.8 * (r + kr)) ** 2 for kx, ky, kr in kept):
            continue
        disk = np.zeros(felt.shape, np.uint8)
        cv2.circle(disk, (cx, cy), max(1, int(r * 0.85)), 255, -1)
        inside = disk > 0
        if np.mean(felt[inside] > 0) > 0.35:
            continue
        # Felt should be right below it: the ball rests on the table.
        # Felt should be near it: the ball rests on the table. Look beside and below, past
        # the contact shadow, which is too dark to read as felt.
        ring = np.zeros(felt.shape, np.uint8)
        cv2.circle(ring, (cx, cy), int(r * 2.2), 255, -1)
        cv2.circle(ring, (cx, cy), int(r * 1.3), 0, -1)
        ring[: max(0, cy - r)] = 0
        if ring.any() and np.mean(felt[ring > 0] > 0) < 0.1:
            continue
        x0, y0 = max(0, cx - r), max(0, cy - r)
        x1, y1 = min(small.shape[1], cx + r), min(small.shape[0], cy + r)
        label, conf = classify(hsv[y0:y1, x0:x1], disk[y0:y1, x0:x1])
        out.append(((x0, y0, x1 - x0, y1 - y0), label, conf))
        kept.append((cx, cy, r))
    return out


def detect(img):
    scale = WORK_WIDTH / img.shape[1]
    small = cv2.resize(img, (WORK_WIDTH, int(img.shape[0] * scale)))
    hsv = cv2.cvtColor(small, cv2.COLOR_BGR2HSV)

    quad, contour, qshape = felt_quad(img)
    if quad is None:
        return None, []
    # felt_quad works at 320 px wide; bring it up to this working size.
    k = WORK_WIDTH / qshape[1]
    quad = order_clockwise(quad * k)
    contour = (contour.astype(float) * k).astype(np.int32)

    hull = cv2.convexHull(contour)
    hull_mask = np.zeros(small.shape[:2], np.uint8)
    cv2.fillPoly(hull_mask, [hull], 255)
    fm = felt_mask(hsv, hull_mask)
    if fm is None:
        return quad / scale, []
    felt, _ = fm
    quad_mask = np.zeros(small.shape[:2], np.uint8)
    cv2.fillPoly(quad_mask, [quad.astype(np.int32)], 255)
    if np.mean(felt[quad_mask > 0] > 0) < MIN_FELT_IN_TABLE:
        # The "table" is mostly something else (a red wall, a shirt): no guess beats a bad one.
        return None, []

    balls = find_balls(small, hsv, felt, hull)
    balls = [((x / scale, y / scale, w / scale, h / scale), label, conf) for (x, y, w, h), label, conf in balls]
    if len(balls) > MAX_BALLS:
        # More "balls" than a rack holds means the detector is reading texture; a blank
        # canvas is quicker to label than 40 wrong boxes.
        balls = []
    return quad / scale, balls


def to_task(row, image_ref, width, height, quad, balls):
    results = []
    if quad is not None:
        results.append({
            "from_name": "table", "to_name": "image", "type": "polygonlabels",
            "original_width": width, "original_height": height,
            "value": {
                "points": [[float(x) / width * 100, float(y) / height * 100] for x, y in quad],
                "polygonlabels": ["table"],
            },
        })
    for (x, y, w, h), label, _ in balls:
        results.append({
            "from_name": "balls", "to_name": "image", "type": "rectanglelabels",
            "original_width": width, "original_height": height,
            "value": {
                "x": x / width * 100, "y": y / height * 100,
                "width": w / width * 100, "height": h / height * 100,
                "rotation": 0, "rectanglelabels": [label],
            },
        })
    return {
        "data": {
            "image": image_ref,
            "id": row["id"],
            "attribution": row["attribution"],
            "license_url": row["license_url"],
            "landing": row["foreign_landing_url"],
        },
        "predictions": [{"model_version": "prelabel-colour-v1", "result": results}],
    }


CONFIG = """<View>
  <Image name="image" value="$image" zoom="true" zoomControl="true"/>
  <PolygonLabels name="table" toName="image" strokeWidth="2" pointSize="small" opacity="0.2">
    <Label value="table" background="#ffffff"/>
  </PolygonLabels>
  <RectangleLabels name="balls" toName="image" strokeWidth="1">
{labels}
  </RectangleLabels>
  <Header value="$attribution" size="6"/>
</View>
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--links", default="links.csv")
    ap.add_argument("--data", default="data")
    ap.add_argument("--out", default="labelstudio")
    ap.add_argument("--local-root", default="", help="absolute path Label Studio serves local files from")
    ap.add_argument("--limit", type=int, default=0)
    args = ap.parse_args()

    img_dir = os.path.join(args.data, "images")
    os.makedirs(img_dir, exist_ok=True)
    os.makedirs(args.out, exist_ok=True)

    manifest_files = {}
    mpath = os.path.join(args.data, "manifest.csv")
    if os.path.exists(mpath):
        with open(mpath, newline="") as f:
            for r in csv.DictReader(f):
                manifest_files[r["id"]] = os.path.join(args.data, r["file"])

    with open(args.links, newline="") as f:
        rows = [r for r in csv.DictReader(f) if r["verdict"] == "keep"]
    if args.limit:
        rows = rows[:args.limit]

    tasks, stats = [], {"images": 0, "tables": 0, "balls": 0, "missing": 0}
    for i, row in enumerate(rows):
        img, path = load_image(row, img_dir, manifest_files)
        if img is None:
            stats["missing"] += 1
            continue
        quad, balls = detect(img)
        h, w = img.shape[:2]
        if args.local_root:
            rel = os.path.relpath(os.path.abspath(path), args.local_root)
            ref = "/data/local-files/?d=" + rel
        else:
            ref = row["url"]
        tasks.append(to_task(row, ref, w, h, quad, balls))
        stats["images"] += 1
        stats["tables"] += quad is not None
        stats["balls"] += len(balls)
        if (i + 1) % 50 == 0:
            print(f"{i + 1}/{len(rows)} {stats}", flush=True)

    with open(os.path.join(args.out, "tasks.json"), "w") as f:
        json.dump(tasks, f)
    labels = "\n".join(f'    <Label value="{l}"/>' for l in BALL_LABELS)
    with open(os.path.join(args.out, "config.xml"), "w") as f:
        f.write(CONFIG.format(labels=labels))
    print(stats)


if __name__ == "__main__":
    main()
