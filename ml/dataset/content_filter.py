#!/usr/bin/env python3
"""
Zero-shot content and viewpoint check with CLIP (open_clip, ViT-B-32, LAION-2B weights).

Keyword search for "pool" returns swimming pools, football pitches and card tables; felt
geometry (perspective_score.py) can't tell those apart. CLIP can. Each image gets:

  is_table   probability it shows a pool/billiards/snooker table (vs. the listed decoys)
  eye_level  probability the table is seen from a standing player's angle rather than from
             directly above or as a diagram

and a verdict: keep (is_table >= 0.6 and eye_level >= 0.5), overhead, off_topic.

Usage:
    pip install open_clip_torch torch torchvision   # CPU builds are fine
    python3 content_filter.py [--data data]
Writes data/content.csv.
"""
import argparse
import csv
import os

import torch
import open_clip
from PIL import Image

TABLE = [
    "a photo of a pool table", "a photo of people playing pool", "a photo of billiard balls on a table",
    "a photo of a snooker table",
]
DECOY = [
    "a photo of a swimming pool", "a photo of a sports field", "a portrait of a person",
    "a photo of a room", "a photo of a card table", "a diagram", "a photo of text",
    "a photo of a street", "a photo of food",
]
EYE = [
    "a photo of a pool table taken from a player's standing position at an angle",
    "a close-up photo of billiard balls on the felt from the side",
]
TOP = [
    "a photo of a pool table taken from directly above, top-down view",
    "a diagram of a pool table seen from above",
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="data")
    args = ap.parse_args()
    model, _, preprocess = open_clip.create_model_and_transforms("ViT-B-32", pretrained="laion2b_s34b_b79k")
    tok = open_clip.get_tokenizer("ViT-B-32")
    model.eval()
    with torch.no_grad():
        def enc(texts):
            t = model.encode_text(tok(texts))
            return t / t.norm(dim=-1, keepdim=True)
        topic_t, view_t = enc(TABLE + DECOY), enc(EYE + TOP)

    rows = list(csv.DictReader(open(os.path.join(args.data, "manifest.csv"), newline="")))
    out, counts = [], {}
    batch = []

    def flush():
        imgs = torch.stack([b[1] for b in batch])
        with torch.no_grad():
            f = model.encode_image(imgs)
            f = f / f.norm(dim=-1, keepdim=True)
            topic = (100 * f @ topic_t.T).softmax(dim=-1)
            view = (100 * f @ view_t.T).softmax(dim=-1)
        for (r, _), tp, vp in zip(batch, topic, view):
            is_table = float(tp[: len(TABLE)].sum())
            eye = float(vp[: len(EYE)].sum())
            verdict = "off_topic" if is_table < 0.6 else ("keep" if eye >= 0.5 else "overhead")
            counts[verdict] = counts.get(verdict, 0) + 1
            out.append({"id": r["id"], "file": r["file"], "is_table": f"{is_table:.3f}",
                        "eye_level": f"{eye:.3f}", "verdict": verdict})
        batch.clear()

    for r in rows:
        try:
            img = preprocess(Image.open(os.path.join(args.data, r["file"])).convert("RGB"))
        except Exception:
            continue
        batch.append((r, img))
        if len(batch) == 32:
            flush()
    if batch:
        flush()
    with open(os.path.join(args.data, "content.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["id", "file", "is_table", "eye_level", "verdict"])
        w.writeheader()
        w.writerows(out)
    print(counts)


if __name__ == "__main__":
    main()
