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
    python3 build_links_db.py --rescore-unscored --out links.csv   # retry the unscored rows

--rescore-unscored retries only rows whose thumbnail couldn't be fetched last time (mostly
rate limits), slowly and with backoff, and rewrites --out in place.
"""
import argparse
import csv
import io
import time
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


def fetch_patiently(row, tries=4):
    """Openverse's thumbnail, then the source; backs off on failure (429s are the usual cause)."""
    urls = [f"https://api.openverse.org/v1/images/{row['id']}/thumb/", row["url"]]
    for attempt in range(tries):
        for url in urls:
            im = fetch(url)
            if im is not None:
                return im
        time.sleep(2 ** (attempt + 1))
    return None


def verdict_of(is_table, eye):
    if is_table is None:
        return "unscored"
    return "off_topic" if is_table < 0.6 else ("keep" if eye >= 0.5 else "overhead")


ORDER = {"keep": 0, "overhead": 1, "unscored": 2, "off_topic": 3}


def rescore_unscored(path, score):
    rows = list(csv.DictReader(open(path, newline="")))
    todo = [r for r in rows if r["verdict"] == "unscored"]
    print(f"{len(todo)} unscored", flush=True)
    for i, r in enumerate(todo):
        im = fetch_patiently(r)
        if im is not None:
            is_table, eye = score([im])[0]
            r["is_table"], r["eye_level"] = f"{is_table:.3f}", f"{eye:.3f}"
            r["verdict"] = verdict_of(is_table, eye)
        time.sleep(0.5)
        if (i + 1) % 25 == 0:
            print(f"{i + 1}/{len(todo)}", flush=True)
    rows.sort(key=lambda r: (ORDER[r["verdict"]], r["id"]))
    with open(path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLUMNS)
        w.writeheader()
        w.writerows(rows)
    counts = {}
    for r in rows:
        counts[r["verdict"]] = counts.get(r["verdict"], 0) + 1
    print("done", counts)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--manifest", default="links_run/manifest.csv")
    ap.add_argument("--out", default="links.csv")
    ap.add_argument("--workers", type=int, default=8)
    ap.add_argument("--rescore-unscored", action="store_true")
    args = ap.parse_args()

    model, _, preprocess = open_clip.create_model_and_transforms("ViT-B-32", pretrained="laion2b_s34b_b79k")
    tok = open_clip.get_tokenizer("ViT-B-32")
    model.eval()
    with torch.no_grad():
        def enc(texts):
            t = model.encode_text(tok(texts))
            return t / t.norm(dim=-1, keepdim=True)
        topic_t, view_t = enc(TABLE + DECOY), enc(EYE + TOP)

    def score(images):
        """(is_table, eye_level) per image."""
        with torch.no_grad():
            f = model.encode_image(torch.stack([preprocess(im) for im in images]))
            f = f / f.norm(dim=-1, keepdim=True)
            topic = (100 * f @ topic_t.T).softmax(dim=-1)
            view = (100 * f @ view_t.T).softmax(dim=-1)
        return [(float(tp[: len(TABLE)].sum()), float(vp[: len(EYE)].sum())) for tp, vp in zip(topic, view)]

    if args.rescore_unscored:
        rescore_unscored(args.out, score)
        return

    rows = list(csv.DictReader(open(args.manifest, newline="")))
    out, counts = [], {}
    for start in range(0, len(rows), 64):
        chunk = rows[start:start + 64]
        with ThreadPoolExecutor(args.workers) as ex:
            imgs = list(ex.map(lambda r: fetch(r.get("thumbnail") or r["url"]), chunk))
        got = [(r, im) for r, im in zip(chunk, imgs) if im is not None]
        scores = dict(zip([r["id"] for r, _ in got], score([im for _, im in got]))) if got else {}
        for r in chunk:
            is_table, eye = scores.get(r["id"], (None, None))
            verdict = verdict_of(is_table, eye)
            counts[verdict] = counts.get(verdict, 0) + 1
            out.append({
                **{k: r.get(k, "") for k in COLUMNS if k in r},
                "is_table": "" if is_table is None else f"{is_table:.3f}",
                "eye_level": "" if eye is None else f"{eye:.3f}",
                "verdict": verdict,
            })
        print(f"{start + len(chunk)}/{len(rows)} {counts}", flush=True)

    out.sort(key=lambda r: (ORDER[r["verdict"]], r["id"]))
    with open(args.out, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=COLUMNS)
        w.writeheader()
        w.writerows(out)
    print("done", counts)


if __name__ == "__main__":
    main()
