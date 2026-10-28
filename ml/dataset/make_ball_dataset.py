#!/usr/bin/env python3
"""
Turn the labelling pipeline's ball export (`ls_to_yolo.py` -> yolo/balls) into the dataset the
ball-detector notebook trains on.

  python3 make_ball_dataset.py yolo/balls --out yolo/balls_type            # cue / eight / solid / stripe
  python3 make_ball_dataset.py yolo/balls --out yolo/balls_number --scheme number

The input is `images/{train,val}` + `labels/{train,val}` as ls_to_yolo.py writes them. The
output has the same layout, remapped labels (see ball_classes.py) and a data.yaml. Upload the
output folder as a Kaggle dataset and point `BALLS_DATASET` in cuedetat_ball_detector_kaggle.py at
it.

Images with a ball of unknown number (`ball`) cannot go in the type scheme: the box has no class,
and leaving it out would train the detector to treat that ball as background. By default the whole
image is left out (`--unknown drop-image`); `--unknown keep` keeps the image and drops only the
box, which is wrong in the way described and only worth it while data is scarcer than precision.
Empty label files (an image with no balls) are kept: they are real negatives.
"""
import argparse
import os
import shutil
import sys

from ball_classes import SCHEME_NUMBER, SCHEME_TYPE, SCHEMES, class_names, remap_lines

IMAGE_EXTS = (".jpg", ".jpeg", ".png", ".webp", ".bmp")


def convert(src, out, scheme, unknown_policy):
    counts = {"images": 0, "boxes": 0, "dropped_images": 0, "dropped_boxes": 0}
    per_class = {n: 0 for n in class_names(scheme)}
    names = class_names(scheme)
    for split in ("train", "val"):
        img_dir = os.path.join(src, "images", split)
        lbl_dir = os.path.join(src, "labels", split)
        if not os.path.isdir(img_dir):
            continue
        os.makedirs(os.path.join(out, "images", split), exist_ok=True)
        os.makedirs(os.path.join(out, "labels", split), exist_ok=True)
        for fname in sorted(os.listdir(img_dir)):
            stem, ext = os.path.splitext(fname)
            if ext.lower() not in IMAGE_EXTS:
                continue
            lbl_path = os.path.join(lbl_dir, stem + ".txt")
            lines = []
            if os.path.exists(lbl_path):
                with open(lbl_path) as f:
                    lines = f.read().splitlines()
            new_lines, unknown = remap_lines(lines, scheme)
            if unknown and scheme == SCHEME_TYPE:
                if unknown_policy == "drop-image":
                    counts["dropped_images"] += 1
                    counts["dropped_boxes"] += unknown
                    continue
                counts["dropped_boxes"] += unknown
            shutil.copyfile(os.path.join(img_dir, fname), os.path.join(out, "images", split, fname))
            with open(os.path.join(out, "labels", split, stem + ".txt"), "w") as f:
                f.write("\n".join(new_lines) + ("\n" if new_lines else ""))
            counts["images"] += 1
            counts["boxes"] += len(new_lines)
            for line in new_lines:
                per_class[names[int(line.split()[0])]] += 1
    root = os.path.abspath(out)
    with open(os.path.join(out, "data.yaml"), "w") as f:
        f.write(f"path: {root}\ntrain: images/train\nval: images/val\nnames:\n")
        f.writelines(f"  {i}: '{n}'\n" for i, n in enumerate(names))
    return counts, per_class


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src", help="ls_to_yolo.py output for balls, e.g. yolo/balls")
    ap.add_argument("--out", required=True)
    ap.add_argument("--scheme", choices=SCHEMES, default=SCHEME_TYPE)
    ap.add_argument("--unknown", choices=("drop-image", "keep"), default="drop-image",
                    help="what to do with an image that has a ball of unknown number (type scheme)")
    args = ap.parse_args()
    if os.path.abspath(args.src) == os.path.abspath(args.out):
        sys.exit("--out must differ from the source")
    counts, per_class = convert(args.src, args.out, args.scheme, args.unknown)
    print(counts)
    print("boxes per class:", per_class)
    thin = [n for n, c in per_class.items() if c < 100]
    if thin:
        print(f"warning: fewer than 100 boxes for {thin}; expect those classes to be weak")


if __name__ == "__main__":
    main()
