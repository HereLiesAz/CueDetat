#!/usr/bin/env python3
"""Publish one AAB to several Google Play tracks in a single edit.

Why this exists rather than r0adkll/upload-google-play
------------------------------------------------------
The release policy is: internal and alpha go live, open testing (``beta``) and
production land as drafts a human promotes. That is four tracks with two
different statuses, off ONE artifact.

The action cannot express it. Its ``tracks`` input is comma-separated but
``status`` is a single value applied to all of them, so two statuses means two
invocations -- and each invocation opens its own Play edit and re-uploads the
bundle, which fails the second time because that version code already exists.
Its ``existingEditId`` input looks like an escape hatch, but the action exposes
no edit id as an output, so there is nothing to thread from the first call to
the second.

The Play Developer API models this natively. An edit is a transaction: upload
the bundle once, point as many tracks at the resulting version code as you
like, commit once. That is what this script does, and it is the shape the
release policy actually has.

Environment
-----------
PLAY_SERVICE_ACCOUNT_JSON  Service-account key, as JSON text.
PLAY_PACKAGE_NAME          Application id.
PLAY_AAB_PATH              Path to the .aab to upload.
PLAY_MAPPING_PATH          Optional R8 mapping.txt, uploaded for deobfuscation.
PLAY_RELEASE_NAME          Optional human-readable release name.
"""

from __future__ import annotations

import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaFileUpload

SCOPE = "https://www.googleapis.com/auth/androidpublisher"

# The release policy, in one place. Order matters only for readability -- every
# entry in a single edit refers to the same uploaded bundle.
#
# Play's track ids do not match the console's labels: "closed testing" is
# `alpha` and "open testing" is `beta`.
TRACK_PLAN: list[tuple[str, str]] = [
    ("internal", "completed"),   # live to internal testers
    ("alpha", "completed"),      # live to closed testing
    ("beta", "draft"),           # open testing, awaiting a human
    ("production", "draft"),     # production, awaiting a human
]


def require(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        sys.exit(f"error: {name} is unset or empty")
    return value


def main() -> int:
    package_name = require("PLAY_PACKAGE_NAME")
    aab_path = require("PLAY_AAB_PATH")
    mapping_path = os.environ.get("PLAY_MAPPING_PATH", "").strip()
    release_name = os.environ.get("PLAY_RELEASE_NAME", "").strip()

    if not os.path.isfile(aab_path):
        sys.exit(f"error: no AAB at {aab_path}")

    credentials = service_account.Credentials.from_service_account_info(
        json.loads(require("PLAY_SERVICE_ACCOUNT_JSON")),
        scopes=[SCOPE],
    )
    service = build(
        "androidpublisher", "v3", credentials=credentials, cache_discovery=False
    )
    edits = service.edits()

    edit_id = edits.insert(packageName=package_name, body={}).execute()["id"]
    print(f"Opened edit {edit_id}")

    try:
        # Upload once. Every track below reuses the version code this returns.
        bundle = edits.bundles().upload(
            packageName=package_name,
            editId=edit_id,
            media_body=MediaFileUpload(
                aab_path, mimetype="application/octet-stream", resumable=True
            ),
        ).execute()
        version_code = bundle["versionCode"]
        print(f"Uploaded {aab_path} as versionCode {version_code}")

        if mapping_path and os.path.isfile(mapping_path):
            edits.deobfuscationfiles().upload(
                packageName=package_name,
                editId=edit_id,
                apkVersionCode=version_code,
                deobfuscationFileType="proguard",
                media_body=MediaFileUpload(
                    mapping_path, mimetype="application/octet-stream"
                ),
            ).execute()
            print(f"Uploaded mapping from {mapping_path}")
        elif mapping_path:
            print(f"No mapping file at {mapping_path}; skipping deobfuscation upload")

        for track, status in TRACK_PLAN:
            release: dict[str, object] = {
                "versionCodes": [str(version_code)],
                "status": status,
            }
            if release_name:
                release["name"] = release_name
            edits.tracks().update(
                packageName=package_name,
                editId=edit_id,
                track=track,
                body={"releases": [release]},
            ).execute()
            print(f"Assigned versionCode {version_code} to {track} ({status})")

        edits.commit(packageName=package_name, editId=edit_id).execute()
        print(f"Committed edit {edit_id}")

    except Exception:
        # Leave no half-built edit behind to collide with the next run. Delete is
        # best-effort: the original failure is what matters, so never let a
        # cleanup error replace it.
        try:
            edits.delete(packageName=package_name, editId=edit_id).execute()
            print(f"Discarded edit {edit_id}", file=sys.stderr)
        except HttpError as cleanup_error:
            print(
                f"warning: could not discard edit {edit_id}: {cleanup_error}",
                file=sys.stderr,
            )
        raise

    tracks = ", ".join(f"{t} ({s})" for t, s in TRACK_PLAN)
    print(f"Published versionCode {version_code} to: {tracks}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
