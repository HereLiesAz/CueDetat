#!/usr/bin/env python3
"""
List YouTube videos their creators licensed Creative Commons Attribution (CC BY), via the
official YouTube Data API v3 (`videoLicense=creativeCommon`). Writes data/youtube_cc.csv with
id, title, channel and URL: the attribution record.

This script only searches. The CC BY license permits reuse, but YouTube's Terms of Service
restrict downloading to means YouTube provides, so fetching the videos is left to the person
running the pipeline. Once videos are on disk, extract_frames.py samples them.

Needs an API key: export YOUTUBE_API_KEY=...  (Google Cloud console, YouTube Data API v3).

Usage:
    python3 youtube_cc_search.py [--out data] [--max 200]
"""
import argparse
import csv
import json
import os
import sys
import urllib.parse
import urllib.request

QUERIES = [
    "pool shot", "billiards shot pov", "8 ball pool game", "9 ball pool match",
    "pool practice drill", "bar pool", "pool trick shot", "billiards tutorial aiming",
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="data")
    ap.add_argument("--max", type=int, default=200)
    args = ap.parse_args()
    key = os.environ.get("YOUTUBE_API_KEY")
    if not key:
        sys.exit("Set YOUTUBE_API_KEY (YouTube Data API v3).")
    os.makedirs(args.out, exist_ok=True)
    rows, seen = [], set()
    for q in QUERIES:
        token = ""
        while len(rows) < args.max:
            params = {
                "part": "snippet", "q": q, "type": "video", "videoLicense": "creativeCommon",
                "maxResults": 50, "key": key,
            }
            if token:
                params["pageToken"] = token
            url = "https://www.googleapis.com/youtube/v3/search?" + urllib.parse.urlencode(params)
            with urllib.request.urlopen(url, timeout=30) as r:
                data = json.load(r)
            for item in data.get("items", []):
                vid = item["id"]["videoId"]
                if vid in seen:
                    continue
                seen.add(vid)
                s = item["snippet"]
                rows.append({
                    "id": vid, "query": q, "title": s["title"], "channel": s["channelTitle"],
                    "channel_id": s["channelId"], "url": f"https://www.youtube.com/watch?v={vid}",
                    "license": "CC BY 3.0 (YouTube Creative Commons)",
                })
            token = data.get("nextPageToken", "")
            if not token:
                break
    path = os.path.join(args.out, "youtube_cc.csv")
    with open(path, "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["id", "query", "title", "channel", "channel_id", "url", "license"])
        w.writeheader()
        w.writerows(rows)
    print(f"{len(rows)} CC BY videos -> {path}")


if __name__ == "__main__":
    main()
