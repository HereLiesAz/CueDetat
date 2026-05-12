"""Convert a .py file with `# %%` cell markers to a Kaggle-ready .ipynb.

Markers:
  `# %% [markdown]` → markdown cell (lines after this until next marker)
  `# %%`            → code cell
First content before any marker becomes the leading code cell.

Usage: python py_to_ipynb.py SRC.py [DST.ipynb]
"""
import json, sys, os, re, uuid


def parse_cells(src: str):
    cells = []
    cur_type = "code"
    cur_lines: list[str] = []
    for raw in src.splitlines():
        line = raw.rstrip("\n")
        m = re.match(r"#\s*%%\s*(\[markdown\])?\s*$", line)
        if m:
            if cur_lines:
                cells.append((cur_type, cur_lines))
            cur_type = "markdown" if m.group(1) else "code"
            cur_lines = []
            continue
        cur_lines.append(line)
    if cur_lines:
        cells.append((cur_type, cur_lines))
    return cells


def to_ipynb(cells):
    nb_cells = []
    for ctype, lines in cells:
        # strip trailing blank lines
        while lines and not lines[-1].strip():
            lines.pop()
        if not lines:
            continue
        if ctype == "markdown":
            # strip leading "# " from markdown comment lines
            md = []
            for ln in lines:
                if ln.startswith("# "):
                    md.append(ln[2:])
                elif ln.startswith("#"):
                    md.append(ln[1:])
                else:
                    md.append(ln)
            nb_cells.append({
                "cell_type": "markdown",
                "metadata": {},
                "source": [l + "\n" for l in md[:-1]] + [md[-1]],
            })
        else:
            nb_cells.append({
                "cell_type": "code",
                "execution_count": None,
                "metadata": {},
                "outputs": [],
                "source": [l + "\n" for l in lines[:-1]] + [lines[-1]],
            })
    return {
        "cells": nb_cells,
        "metadata": {
            "kernelspec": {
                "display_name": "Python 3",
                "language": "python",
                "name": "python3",
            },
            "language_info": {"name": "python", "version": "3.11"},
        },
        "nbformat": 4,
        "nbformat_minor": 5,
    }


if __name__ == "__main__":
    src = sys.argv[1]
    dst = sys.argv[2] if len(sys.argv) > 2 else os.path.splitext(src)[0] + ".ipynb"
    with open(src) as f:
        text = f.read()
    nb = to_ipynb(parse_cells(text))
    with open(dst, "w") as f:
        json.dump(nb, f, indent=1)
    print(f"wrote {dst} ({len(nb['cells'])} cells)")
