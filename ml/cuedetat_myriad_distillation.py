# cuedetat_myriad_distillation.py
#
# Generates a (image, poke, cue_trajectory) supervised dataset by running the
# Python `billiards` physics library directly. No neural teacher — physics IS
# the ground truth.
#
# Designed to run as a Kaggle notebook on a CPU instance (no GPU required).
# Cells delimited with `# %%` for jupytext-style conversion. Convert with:
#   `python ml/_scripts/py_to_ipynb.py ml/cuedetat_myriad_distillation.py`
#
# Output: Parquet table + image PNGs uploaded as a Kaggle dataset:
#   hereliesaz/cuedetat-trajectories

# %% [markdown]
# # CueDetat trajectory dataset — physics-direct generation
#
# Ground truth: `billiards` Python lib (Chipmunk2D-style 2D physics).
# Used by: cuedetat_myriad_student notebook to train an on-device CNN+MLP.
#
# This notebook produces ~50,000 (image, poke, trajectory) tuples.

# %% [markdown]
# ## Cell 1 — Install dependencies

# %%
!pip install -q --upgrade pip
!pip install -q \
    "billiards" \
    "opencv-python-headless" \
    "Pillow" \
    "numpy" \
    "pandas" \
    "pyarrow" \
    "tqdm" \
    "matplotlib" \
    "kaggle"

# %% [markdown]
# ## Cell 2 — Inline physics + rendering helpers
#
# Copied (and trimmed) from CompVis `flow-poke-transformer/myriad/data_billiards.py`
# — MIT license at the repo root applies. We avoid cloning the whole repo to
# keep this notebook standalone.

# %%
import math
import random
import numpy as np
import cv2
import billiards
from typing import Optional

COLORS = {
    "BACKGROUND": (255, 255, 255),
    "BOUNDS":     (128, 128, 128),
    "OUR_BALL":   (255,   0,   0),  # cue ball (red)
    "DEFAULT":    (  0,   0,   0),  # other balls (black)
}

def setup_billiard_game(
    frame_size: int,
    min_border_offset: int,
    max_border_offset: int,
    nr_balls: int,
    ball_radius: int,
    p_moving: float = 0.0,
    rng: Optional[random.Random] = None,
):
    rng = rng or random
    border_offsets = [rng.randrange(min_border_offset, max_border_offset)
                      for _ in range(4)]
    bounds = [
        billiards.InfiniteWall((0, border_offsets[0]),
                               (frame_size - border_offsets[1], border_offsets[0])),
        billiards.InfiniteWall((frame_size - border_offsets[1], border_offsets[0]),
                               (frame_size - border_offsets[1], frame_size - border_offsets[2])),
        billiards.InfiniteWall((frame_size - border_offsets[1], frame_size - border_offsets[2]),
                               (border_offsets[3], frame_size - border_offsets[2])),
        billiards.InfiniteWall((border_offsets[3], frame_size - border_offsets[2]),
                               (border_offsets[3], border_offsets[0])),
    ]
    bld = billiards.Billiard(obstacles=bounds)
    border_margin = 0.1 * ball_radius
    added = 0
    while added < nr_balls:
        x = rng.uniform(border_offsets[3] + ball_radius + border_margin,
                        frame_size - border_offsets[1] - ball_radius - border_margin)
        y = rng.uniform(border_offsets[0] + ball_radius + border_margin,
                        frame_size - border_offsets[2] - ball_radius - border_margin)
        existing = getattr(bld, "balls_position",
                           getattr(bld, "balls_initial_position", []))
        if any((x - bx) ** 2 + (y - by) ** 2 < (2 * ball_radius) ** 2
               for bx, by in existing):
            continue
        if rng.random() <= p_moving and added > 0:
            bld.add_ball((x, y),
                         (rng.gauss(0, frame_size / 2),
                          rng.gauss(0, frame_size / 2)),
                         ball_radius)
        else:
            bld.add_ball((x, y), (0, 0), ball_radius)
        added += 1
    return bld, border_offsets


def simulate(bld: billiards.Billiard, duration: float, dt: float):
    start = bld.time
    frames = int(duration / dt) + 1
    pos = []
    for i in range(frames):
        bld.evolve(start + i * dt)
        pos.append(bld.balls_position.copy())
    return pos


def render_frame(ball_pos, ball_rad, frame_size, border_offsets,
                 antialiasing: bool = True):
    f = np.zeros((frame_size, frame_size, 3), dtype=np.uint8)
    f[:, :] = COLORS["BOUNDS"]
    cv2.rectangle(
        f,
        (border_offsets[3], border_offsets[0]),
        (frame_size - border_offsets[1], frame_size - border_offsets[2]),
        COLORS["BACKGROUND"], -1,
    )
    lt = cv2.LINE_AA if antialiasing else cv2.LINE_8
    for i, (p, r) in enumerate(zip(ball_pos, ball_rad)):
        cv2.circle(f, (int(p[0]), int(p[1])), int(r),
                   COLORS["OUR_BALL"] if i == 0 else COLORS["DEFAULT"],
                   -1, lineType=lt)
    return f

# %% [markdown]
# ## Cell 3 — Smoke test: 5 scenes plotted with their physics trajectories

# %%
import matplotlib.pyplot as plt

SMOKE_FRAME = 128       # mobile-target resolution (matches student input)
SMOKE_BALLS = 16
SMOKE_RAD   = int(0.0333 * SMOKE_FRAME)
SMOKE_DT    = 0.05
SMOKE_T     = 30        # 30 trajectory points per shot
SMOKE_DUR   = SMOKE_T * SMOKE_DT
SMOKE_BMIN  = int(0.05 * SMOKE_FRAME)
SMOKE_BMAX  = int(0.10 * SMOKE_FRAME)

rng = random.Random(42)

fig, axes = plt.subplots(1, 5, figsize=(20, 4))
for i in range(5):
    bld, border_offsets = setup_billiard_game(
        SMOKE_FRAME, SMOKE_BMIN, SMOKE_BMAX, SMOKE_BALLS, SMOKE_RAD,
        p_moving=0.0, rng=rng,
    )
    # Inject a poke on ball 0 (cue ball)
    angle = rng.uniform(0, 2 * math.pi)
    speed = rng.uniform(0.4, 1.5) * SMOKE_FRAME  # px/sec
    bld.balls_velocity[0] = np.array([speed * math.cos(angle),
                                      speed * math.sin(angle)])
    pos_per_step = simulate(bld, SMOKE_DUR, SMOKE_DT)  # list of [N, 2]
    cue = np.array([p[0] for p in pos_per_step])  # [T+1, 2]

    initial = render_frame(pos_per_step[0],
                           [SMOKE_RAD] * SMOKE_BALLS,
                           SMOKE_FRAME, border_offsets)
    axes[i].imshow(initial)
    axes[i].plot(cue[:, 0], cue[:, 1], "y-", linewidth=2, label="trajectory")
    axes[i].plot(cue[0, 0], cue[0, 1], "go", markersize=8, label="start")
    axes[i].plot(cue[-1, 0], cue[-1, 1], "rs", markersize=8, label="end")
    axes[i].set_title(f"scene {i} | speed={speed/SMOKE_FRAME:.2f}")
    axes[i].axis("off")

plt.tight_layout()
plt.savefig("/kaggle/working/smoke_test.png", dpi=100)
plt.show()
print("Smoke test: trajectories should stay in white area, bounce off bounds,")
print("and curve away from other balls on collision. Halt if not.")

# %% [markdown]
# ## Cell 4 — Bulk generation: 50,000 samples
#
# Output schema (Parquet rows):
#   id           : int64
#   image_path   : string         — relative path to PNG
#   poke_x       : float32        — normalized [0, 1], cue start position
#   poke_y       : float32
#   poke_dx      : float32        — normalized poke velocity per dt
#   poke_dy      : float32
#   trajectory   : list<float32>[60]  — 30 (x, y) cue points, normalized [0, 1]

# %%
import os, time, gc, json
import pandas as pd
from PIL import Image
from tqdm import tqdm

OUT_DIR   = "/kaggle/working/dataset"
IMG_DIR   = os.path.join(OUT_DIR, "images")
PARQUET   = os.path.join(OUT_DIR, "samples.parquet")
N_SAMPLES = 50_000
T_STEPS   = 30
RES       = 128         # student input resolution
N_BALLS   = 16
RAD       = int(0.0333 * RES)
DT        = 0.05
DUR       = T_STEPS * DT
BMIN      = int(0.05 * RES)
BMAX      = int(0.10 * RES)

os.makedirs(IMG_DIR, exist_ok=True)
rng = random.Random(20260512)
start = time.time()
rows = []

for i in tqdm(range(N_SAMPLES), desc="generating"):
    bld, border_offsets = setup_billiard_game(
        RES, BMIN, BMAX, N_BALLS, RAD, p_moving=0.0, rng=rng,
    )
    cue_start = bld.balls_position[0].copy()  # [2]
    angle = rng.uniform(0, 2 * math.pi)
    speed = rng.uniform(0.4, 1.5) * RES  # px/sec
    poke_v = np.array([speed * math.cos(angle), speed * math.sin(angle)])
    bld.balls_velocity[0] = poke_v.copy()

    pos_per_step = simulate(bld, DUR, DT)
    cue = np.array([p[0] for p in pos_per_step])[:T_STEPS]  # [T, 2]

    img = render_frame(pos_per_step[0],
                       [RAD] * N_BALLS,
                       RES, border_offsets)
    img_name = f"{i:06d}.png"
    Image.fromarray(img).save(os.path.join(IMG_DIR, img_name), optimize=True)

    rows.append({
        "id": i,
        "image_path": f"images/{img_name}",
        "poke_x": float(cue_start[0] / RES),
        "poke_y": float(cue_start[1] / RES),
        "poke_dx": float(poke_v[0] * DT / RES),  # normalized displacement per dt
        "poke_dy": float(poke_v[1] * DT / RES),
        "trajectory": (cue / RES).astype(np.float32).flatten().tolist(),
    })

    if (i + 1) % 2000 == 0:
        elapsed = time.time() - start
        rate = (i + 1) / elapsed
        eta = (N_SAMPLES - i - 1) / rate / 60
        print(f"  {i+1}/{N_SAMPLES}  rate={rate:.0f}/s  ETA={eta:.1f} min")
        gc.collect()

df = pd.DataFrame(rows)
df.to_parquet(PARQUET, index=False)
print(f"Saved {len(df)} rows to {PARQUET}")

# %% [markdown]
# ## Cell 5 — Sanity stats

# %%
df = pd.read_parquet(PARQUET)
print(df[["poke_x", "poke_y", "poke_dx", "poke_dy"]].describe())
print(f"\nTrajectory length consistent: "
      f"{(df['trajectory'].apply(len) == 60).all()}")
print(f"Parquet size: {os.path.getsize(PARQUET) / 1e6:.1f} MB")
print(f"Image count: {len(os.listdir(IMG_DIR))}")
print(f"Total dataset size: "
      f"{sum(os.path.getsize(os.path.join(IMG_DIR, f)) for f in os.listdir(IMG_DIR)) / 1e6:.1f} MB images")

# %% [markdown]
# ## Cell 6 — Upload as a Kaggle dataset
#
# Configure your Kaggle API key first via "Add-ons → Secrets" or by uploading
# a `kaggle.json` to `~/.kaggle/`. First push uses `create`; subsequent runs
# use `version`.

# %%
import json, subprocess
META = {
    "title":   "CueDetat trajectory dataset",
    "id":      "hereliesaz/cuedetat-trajectories",
    "licenses": [{"name": "CC0-1.0"}],
    "keywords": ["billiards", "trajectory", "physics", "cuedetat"],
}
with open(os.path.join(OUT_DIR, "dataset-metadata.json"), "w") as f:
    json.dump(META, f, indent=2)

result = subprocess.run(
    ["kaggle", "datasets", "create", "-p", OUT_DIR, "--dir-mode", "zip"],
    capture_output=True, text=True,
)
print("STDOUT:", result.stdout)
print("STDERR:", result.stderr)
print("\nIf dataset already exists, run instead:")
print(f"  kaggle datasets version -p {OUT_DIR} -m 'regen'")
