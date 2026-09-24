#!/usr/bin/env python3
"""
Build links.csv: a database of links to reuse-licensed pool photos, each scored for content
and camera angle. No images are stored.

Input: a manifest from `fetch_openverse.py --links-only`. Each link's Openverse thumbnail is
fetched into memory and scored with CLIP (see content_filter.py for the prompts): is it a pool
table, and is it seen from a player's position or from above.

Columns: id, url, foreign_landing_url, title, creator, creator_url, license, license_version,
license_url, source, attribution, is_table, eye_level, verdict (keep / overhead / off_topic /
unscored).

Usage:
    python3 fetch_openverse.py --links-only --out links_run
    python3 build_links_db.py --manifest links_run/manifest.csv --out links.csv
"""
import argparse
import csv
import io
import urllib.request
from concurrent.futures import ThreadPoolExecutor

import open_clip
import torch
from PIL import Image

from content_filter import DECOY, EYE, TABLE, TOP

UA = "CueDetatDatasetBuilder/0.1 (+https://github.com/HereLiesAz/CueDetat)"
COLUMNS = [
    "id", "url", "foreign_landing_url", "title", "creator", "creator_url", "license",
    "license_version", "license_url", "source", "attribution", "is_table", "eye_level", "verdict",
]


def fetch(url):
    try:
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=30) as r:
            return Image.open(io.BytesIO(r.read())).convert("RGB")
    except Exception:
        return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", default="links_run/manifest.csv")
    ap.add_argument("--out", default="links.csv")
    ap.add_argument("--workers", type=int, default=8)
    args = ap.parse_args()

    model, _, preprocess = open_clip.create_model_and_transforms("ViT-B-32", pretrained="laion2b_s34b_b79k")
    tok = open_clip.get_tokenizer("ViT-B-32")
    model.eval()
    with torch.no_grad():
        def enc(texts):
            t = model.encode_text(tok(texts))
            return t / t.norm(dim=-1, keepdim=True)
        topic_t, view_t = enc(TABLE + DECOY), enc(EYE + TOP)

    rows = list(csv.DictReader(open(args.manifest, newline="")))
    out, counts = [], {}
    for start in range(0, len(rows), 64):
        chunk = rows[start:start + 64]
        with ThreadPoolExecutor(args.workers) as ex:
            imgs = list(ex.map(lambda r: fetch(r.get("thumbnail") or r["url"]), chunk))
        scored = [(r, preprocess(im)) for r, im in zip(chunk, imgs) if im is not None]
        scores = {}
        if scored:
            with torch.no_grad():
                f = model.encode_image(torch.stack([t for _, t in scored]))
                f = f / f.norm(dim=-1, keepdim=True)
                topic = (100 * f @ topic_t.T).softmax(dim=-1)
                view = (100 * f @ view_t.T).softmax(dim=-1)
            for (r, _), tp, vp in zip(scored, topic, view):
                scores[r["id"]] = (float(tp[: len(TABLE)].sum()), float(vp[: len(EYE)].sum()))
        for r in chunk:
            is_table, eye = scores.get(r["id"], (None, None))
            if is_table is None:
                verdict = "unscored"
            else:
                verdict = "off_topic" if is_table < 0.6 else ("keep" if eye >= 0.5 else "overhead")
            counts[verdict] = counts.get(verdict, 0) + 1
            out.append({
                **{k: r.get(k, "") for k in COLUMNS if k in r},
                "is_table": "" if is_table is None else f"{is_table:.3f}",
                "eye_level": "" if eye is None else f"{eye:.3f}",
                "verdict": verdict,
            })
        print(f"{start + len(chunk)}/{len(rows)} {counts}", flush=True)

    out.sort(key=lambda r: ({"keep": 0, "overhead": 1, "unscored": 2, "off_topic": 3}[r["verdict"]], r["id"]))
    with open(args.out, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLUMNS)
        w.writeheader()
        w.writerows(out)
    print("done", counts)


if __name__ == "__main__":
    main()
