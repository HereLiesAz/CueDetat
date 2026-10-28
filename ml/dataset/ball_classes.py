"""
The ball classes of the labelling pipeline, and how they collapse to what the app uses.

Label Studio (`labelstudio/config.xml`) and `ls_to_yolo.py` label every ball with a number
(`cue`, `1`..`15`) or `ball` (round and on the table, number unknown). The app's `BallType`
(app/.../data/VisionData.kt) is coarser: cue, eight, solid, stripe. 1-7 are solids, 9-15 are
stripes.

Why train on the coarse set: the labelled data is small (about 1,700 boxes at the last count),
so fifteen number classes get roughly a hundred boxes each, and what the app needs, solids against
stripes, is decided by how much white a ball shows, which does not depend on its number. The
number scheme stays available (`SCHEME_NUMBER`) for when there is more data.

Pure Python, no dependencies: `prelabel.py`, `ls_to_yolo.py`, `make_ball_dataset.py` and the tests
all import it, and it must stay importable without OpenCV.
"""

# Index in this list is the YOLO class id that ls_to_yolo.py writes.
BALL_LABELS = ["cue"] + [str(n) for n in range(1, 16)] + ["ball"]

SCHEME_NUMBER = "number"
SCHEME_TYPE = "type"
SCHEMES = (SCHEME_NUMBER, SCHEME_TYPE)

# Same order as BallType.CUE / EIGHT / SOLID / STRIPE.
TYPE_NAMES = ["cue", "eight", "solid", "stripe"]

UNKNOWN_LABEL = "ball"


def type_name(label):
    """The coarse class of a pipeline label, or None for `ball` (number unknown)."""
    if label == "cue":
        return "cue"
    if label == UNKNOWN_LABEL:
        return None
    n = int(label)
    if n == 8:
        return "eight"
    return "solid" if n < 8 else "stripe"


def class_names(scheme):
    if scheme == SCHEME_NUMBER:
        return list(BALL_LABELS)
    if scheme == SCHEME_TYPE:
        return list(TYPE_NAMES)
    raise ValueError(f"unknown scheme {scheme!r}; expected one of {SCHEMES}")


def remap_lines(lines, scheme):
    """
    Rewrite the lines of one YOLO label file (class cx cy w h) from the pipeline's ids to the ids
    of [scheme].

    Returns (new_lines, unknown): [unknown] counts boxes whose number is unknown (`ball`). In the
    number scheme they keep their id (`ball` is a class there). In the type scheme they have no
    class, and dropping the box would leave a ball on the image labelled as background, which
    teaches the detector to ignore balls: the caller decides what to do with the image (see
    `make_ball_dataset.py`). They are omitted from [new_lines].
    """
    if scheme not in SCHEMES:
        raise ValueError(f"unknown scheme {scheme!r}; expected one of {SCHEMES}")
    out = []
    unknown = 0
    for line in lines:
        parts = line.split()
        if not parts:
            continue
        label = BALL_LABELS[int(parts[0])]
        if scheme == SCHEME_NUMBER:
            out.append(" ".join(parts))
            if label == UNKNOWN_LABEL:
                unknown += 1
            continue
        name = type_name(label)
        if name is None:
            unknown += 1
            continue
        out.append(" ".join([str(TYPE_NAMES.index(name))] + parts[1:]))
    return out, unknown
