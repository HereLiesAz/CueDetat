#!/usr/bin/env python3
"""
Fetch reuse-licensed pool / billiards photos from Openverse (https://openverse.org), which
indexes Creative Commons and public-domain images from Flickr, Wikimedia Commons and others.

Only licenses that allow reuse *and* modification (training and cropping are modification)
are kept: CC0, Public Domain Mark, CC BY, CC BY-SA. Every image is recorded in
data/manifest.csv with its creator, license and source page, so attribution survives into
whatever is built from it. CC BY-SA images carry share-alike: derived *datasets* must keep
that license; trained weights are generally not considered a derivative, but check before
redistributing.

Usage:
    python3 fetch_openverse.py [--out data] [--max-per-query 240]

Anonymous Openverse access is rate limited and capped at a few hundred results per query,
so the script spreads over several queries and sleeps between requests.
"""
import argparse
import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request

API = "https://api.openverse.org/v1/images/"
LICENSES = "cc0,pdm,by,by-sa"
UA = "CueDetatDatasetBuilder/0.1 (+https://github.com/HereLiesAz/CueDetat)"

# Player's-eye phrasings first: the app never sees the table from above.
QUERIES = [
    "pool table", "billiards", "playing pool", "pool game", "shooting pool",
    "billiard table", "pool hall", "8 ball pool", "9 ball", "pool player",
    "billiards player", "pool cue", "billiard balls table", "bar pool table",
    "pool break", "pool shot",
]

FIELDS = [
    "id", "file", "query", "title", "creator", "creator_url", "license", "license_version",
    "license_url", "source", "provider", "foreign_landing_url", "url", "width", "height",
    "attribution", "thumbnail",
]


def get_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def download(url, path):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r, open(path, "wb") as f:
        f.write(r.read())


def fetch_image(r, path):
    """Downloads the image; falls back to Openverse's thumbnail when the source refuses."""
    try:
        download(r["url"], path)
        return True
    except Exception as e:
        # Wikimedia refuses bulk full-size fetches (429) and asks for thumbnails.
        thumb = r.get("thumbnail")
        try:
            if not thumb:
                raise e
            download(thumb, path)
            return True
        except Exception as e2:
            print(f"  skip {r['id']}: {e2}", file=sys.stderr)
            return False


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="data")
    ap.add_argument("--max-per-query", type=int, default=240)
    ap.add_argument("--sleep", type=float, default=1.2)
    ap.add_argument("--links-only", action="store_true",
                    help="record links and attribution without downloading images")
    args = ap.parse_args()

    img_dir = os.path.join(args.out, "images")
    os.makedirs(img_dir if not args.links_only else args.out, exist_ok=True)
    manifest = os.path.join(args.out, "manifest.csv")

    seen = set()
    rows = []
    if os.path.exists(manifest):
        with open(manifest, newline="") as f:
            for row in csv.DictReader(f):
                seen.add(row["id"])
                rows.append(row)

    for q in QUERIES:
        page, got = 1, 0
        while got < args.max_per_query:
            params = urllib.parse.urlencode({
                "q": q, "license": LICENSES, "page_size": 20, "page": page, "mature": "false",
            })
            try:
                data = get_json(f"{API}?{params}")
            except Exception as e:  # rate limit or end of the anonymous window
                print(f"[{q}] page {page}: {e}", file=sys.stderr)
                break
            results = data.get("results", [])
            if not results:
                break
            for r in results:
                got += 1
                if r["id"] in seen:
                    continue
                ext = os.path.splitext(urllib.parse.urlparse(r["url"]).path)[1].lower() or ".jpg"
                if ext not in (".jpg", ".jpeg", ".png", ".webp"):
                    continue
                path = "" if args.links_only else os.path.join(img_dir, r["id"] + ext)
                if path and not fetch_image(r, path):
                    continue
                seen.add(r["id"])
                rows.append({
                    "id": r["id"], "file": os.path.relpath(path, args.out) if path else "", "query": q,
                    "title": r.get("title") or "", "creator": r.get("creator") or "",
                    "creator_url": r.get("creator_url") or "", "license": r["license"],
                    "license_version": r.get("license_version") or "",
                    "license_url": r.get("license_url") or "", "source": r.get("source") or "",
                    "provider": r.get("provider") or "",
                    "foreign_landing_url": r.get("foreign_landing_url") or "", "url": r["url"],
                    "width": r.get("width") or "", "height": r.get("height") or "",
                    "attribution": r.get("attribution") or "",
                    "thumbnail": r.get("thumbnail") or "",
                })
                if path:
                    time.sleep(0.2)
            print(f"[{q}] page {page}: {len(rows)} images total")
            if page >= data.get("page_count", page):
                break
            page += 1
            time.sleep(args.sleep)

        with open(manifest, "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=FIELDS)
            w.writeheader()
            w.writerows(rows)

    print(f"done: {len(rows)} images, manifest {manifest}")


if __name__ == "__main__":
    main()
