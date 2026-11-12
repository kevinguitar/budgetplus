#!/usr/bin/env python3
"""Reuse an editable iOS version, or create the live version's next patch draft.

Uses the same ASC_KEY_ID / ASC_ISSUER_ID / ASC_KEY_PATH credentials as creative
asset uploads. Writes the selected version to GITHUB_OUTPUT when run in CI.
Does not upload a build, change app source versions, or submit/release anything.
"""

import os
from pathlib import Path
import re
import sys

from upload_creative_assets import ASC, BUNDLE_ID, EDITABLE, errors_text, q


RELEASED = {"READY_FOR_DISTRIBUTION", "READY_FOR_SALE"}


def version_state(version):
    attributes = version["attributes"]
    return attributes.get("appVersionState") or attributes.get("appStoreState")


def version_number(text):
    if not re.fullmatch(r"[0-9]+(?:\.[0-9]+){0,2}", text):
        raise ValueError(f"Cannot determine a numeric patch version from {text!r}")
    parts = tuple(map(int, text.split(".")))
    return parts + (0,) * (3 - len(parts))


def next_patch(text):
    major, minor, patch = version_number(text)
    return f"{major}.{minor}.{patch + 1}"


def all_versions(api, app_id):
    path = f"/v1/apps/{app_id}/appStoreVersions" + q({"filter[platform]": "IOS", "limit": 50})
    versions = []
    while path:
        payload = api.must("GET", path)
        versions.extend(payload["data"])
        path = payload.get("links", {}).get("next")
    return versions


def ensure_editable_version(api, app_id):
    versions = all_versions(api, app_id)
    editable = [v for v in versions if version_state(v) in EDITABLE]
    if editable:
        selected = max(editable, key=lambda v: version_number(v["attributes"]["versionString"]))
        return selected, False

    released = [v for v in versions if version_state(v) in RELEASED]
    if not released:
        raise ValueError("No editable or released iOS version found; cannot safely choose a patch version")
    live = max(released, key=lambda v: version_number(v["attributes"]["versionString"]))
    wanted = next_patch(live["attributes"]["versionString"])
    existing = next((v for v in versions if v["attributes"]["versionString"] == wanted), None)
    if existing is not None:
        raise ValueError(f"iOS {wanted} already exists in {version_state(existing)} and is not editable; "
                         "resolve its App Store Connect state before uploading")

    status, payload = api.call("POST", "/v1/appStoreVersions", {"data": {
        "type": "appStoreVersions",
        "attributes": {"platform": "IOS", "versionString": wanted, "releaseType": "MANUAL"},
        "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
    }})
    if status == 201:
        return payload["data"], True
    if status == 409:
        # Another run or a Console user may have created the same draft after
        # the GET. Reuse only that exact editable version, never bump again.
        for version in all_versions(api, app_id):
            if version["attributes"]["versionString"] == wanted and version_state(version) in EDITABLE:
                return version, False
    raise ValueError(f"Cannot create iOS {wanted} ({status}): {errors_text(payload)}")


def main():
    api = ASC(os.environ["ASC_KEY_ID"], os.environ["ASC_ISSUER_ID"], os.environ["ASC_KEY_PATH"])
    apps = api.must("GET", "/v1/apps" + q({"filter[bundleId]": BUNDLE_ID}))["data"]
    if len(apps) != 1:
        sys.exit(f"Expected one app with bundle id {BUNDLE_ID}, found {len(apps)}")
    try:
        version, created = ensure_editable_version(api, apps[0]["id"])
    except ValueError as error:
        sys.exit(str(error))
    number = version["attributes"]["versionString"]
    print(f"{'Created patch draft' if created else 'Using editable version'}: iOS {number} ({version['id']})")
    if os.environ.get("GITHUB_OUTPUT"):
        with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
            output.write(f"version={number}\n")


if __name__ == "__main__":
    main()
