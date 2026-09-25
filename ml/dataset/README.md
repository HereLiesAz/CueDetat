# Player's-eye pool dataset

Images and video frames of pool tables seen the way the app sees them: from a player standing
at the table, never from above. Most public pool datasets are overhead (shot for projector
trainers), which is why this one exists.

Only reuse-licensed material. Nothing here is scraped from platforms that give creators no
reuse setting (Facebook, Instagram and TikTok have none).

## The links database

**`links.csv`** is the dataset: links to reuse-licensed photos, no images stored. One row per
photo: image URL, source page, title, creator, licence (with URL and version), a ready-made
attribution line, and CLIP scores:

- `is_table`: probability the photo shows a pool / billiards / snooker table.
- `eye_level`: probability it is seen from a player's position rather than from above.
- `verdict`: `keep` (player's-eye pool table), `overhead`, `off_topic`, or `unscored` (its
  thumbnail couldn't be fetched).

Rows are sorted `keep` first. Rebuild it with:

~~~
python3 fetch_openverse.py --links-only --out links_run   # metadata only, no downloads
python3 build_links_db.py --manifest links_run/manifest.csv --out links.csv
~~~

`build_links_db.py` reads each Openverse thumbnail into memory to score it and keeps nothing.

## Sources

| Source | How | Licence filter |
|---|---|---|
| Openverse (Flickr, Wikimedia Commons, …) | `fetch_openverse.py`, no key | CC0, Public Domain Mark, CC BY, CC BY-SA |
| YouTube | `youtube_cc_search.py` lists videos; you download them | CC BY (YouTube's Creative Commons setting) |
| Your own footage | `extract_frames.py` | yours |

YouTube's licence allows reuse, but its Terms of Service restrict downloading to means
YouTube provides, so the search script only produces the list (`data/youtube_cc.csv`);
fetching is up to whoever runs it.

## Pipeline

~~~
python3 fetch_openverse.py            # data/images/, data/manifest.csv (attribution per image)
python3 content_filter.py             # data/content.csv: keep / overhead / off_topic (CLIP)
python3 perspective_score.py          # data/perspective.csv: oblique / overhead / partial / none
export YOUTUBE_API_KEY=...
python3 youtube_cc_search.py          # data/youtube_cc.csv
# download the listed videos into some folder yourself, then:
python3 extract_frames.py VIDEO_DIR   # data/frames/, data/frames.csv
~~~

`content_filter.py` is the main filter: CLIP (ViT-B-32, zero-shot) scores each image for
"is a pool table" against decoys (swimming pools, fields, portraits, diagrams…) and for
player's-eye versus overhead. A keyword search for "pool" is mostly other things; geometry
alone can't tell. On a hand-checked sample, its `keep` set was all player's-eye pool tables;
its `overhead` set is loose (pocket close-ups land there) and worth a look by eye.

`perspective_score.py` is a geometric second opinion: it fits the felt outline to four corners: a trapezoid (unequal opposite
edges, corners far from 90°) is **oblique** and kept; a near-rectangle is **overhead** and
dropped; felt running off the frame is **partial** and a missing felt colour is **none**,
both for review by eye.

## Labelling

Pre-labels are colour guesses for a person to accept or fix in
[Label Studio](https://labelstud.io), not ground truth.

~~~
python3 prelabel.py                   # labelstudio/tasks.json + config.xml (kept links only)
pip install label-studio && label-studio start
# new project -> Labeling Setup -> Code: paste labelstudio/config.xml
# Import: labelstudio/tasks.json (pre-labels arrive as predictions)
# fix each task, Submit; then Export -> JSON
python3 ls_to_yolo.py export.json     # yolo/balls (detect), yolo/table (pose, 4 corners)
~~~

What is guessed:

- **table**: the felt outline as four corners, clockwise from top-left (same method as
  `perspective_score.py`). Skipped when under 55% of the quad is felt colour.
- **balls**: circles (Hough) centred on or near the felt, mostly not felt-coloured, with felt
  beside or below them. Skipped when more than 18 are found (texture, not balls).
- **number**: white -> `cue`, black -> `8`, else hue picks the pair (yellow 1/9, blue 2/10,
  red 3/11, purple 4/12, orange 5/13, green 6/14, maroon 7/15) and a white share picks the
  stripe. `ball` means round and on the table, number unknown. Often wrong: check every one.

On the 1,084 kept links: a table on 557, 1,678 ball boxes. Recall is the weak side; expect
to draw more than you delete.

`tasks.json` points at source URLs, so it imports without downloading anything. To label
from disk instead, run `prelabel.py --local-root $PWD/data` and start Label Studio with
`LABEL_STUDIO_LOCAL_FILES_SERVING_ENABLED=true` and
`LABEL_STUDIO_LOCAL_FILES_DOCUMENT_ROOT=$PWD/data`.

`ls_to_yolo.py` uses only submitted annotations (unreviewed predictions are skipped) and
splits train/val by a hash of the image id, so re-exports keep the split. Table corners that
fall outside the frame are kept, flagged as not visible.

### In-app captures

With the user's consent the app sends its own player's-eye frames, with sensor readings, the
table pose and its detections, through `ml/capture-relay` into a private repo. To label them:

~~~
git clone git@github.com:HereLiesAz/cuedetat-captures.git
python3 captures_to_labelstudio.py --captures cuedetat-captures --root $PWD
# import labelstudio/captures_tasks.json into the same project (local-file serving on, root $PWD)
python3 ls_to_yolo.py export.json --local-root $PWD
~~~

The table outline is pre-labelled only where the user locked the table or the snap fit agreed
with the felt; ball boxes are the app's own detections.

## Licences and attribution

`data/manifest.csv` keeps creator, licence, licence URL and source page for every image, and
`data/frames.csv` ties each frame to its video. Keep them with anything built from the data.
CC BY-SA images carry share-alike: a redistributed *dataset* containing them must stay
CC BY-SA.

`data/` is git-ignored: the images are not committed.
