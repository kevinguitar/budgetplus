"""Reject a Release archive if dead stripping removed the Meta mediation adapter."""

import argparse
from collections import Counter
from pathlib import Path
import plistlib
import re
import subprocess
import sys


ADAPTER_CLASS = "GADMediationAdapterFacebook"


def class_definitions(lines):
    """Read actual class definitions, not string references or metaclasses, from otool."""
    definitions = Counter()
    in_class_list = False
    flags = None
    for line in lines:
        if line.startswith("Contents of "):
            in_class_list = ",__objc_classlist) section" in line
            flags = None
        if not in_class_list:
            continue
        match = re.match(r"\s+flags\s+(0x[0-9a-fA-F]+)\b", line)
        if match:
            flags = int(match.group(1), 16)
        match = re.match(r"\s+name\s+0x[0-9a-fA-F]+\s+(\S+)\s*$", line)
        if match and flags is not None:
            # RO_META (bit 0) describes the metaclass, not another adapter instance.
            if not (flags & 1):
                definitions[match.group(1)] += 1
            flags = None
    return definitions


def verify_archive(archive):
    apps = list((archive / "Products/Applications").glob("*.app"))
    if len(apps) != 1:
        raise ValueError(f"Expected one archived application in {archive}, found {len(apps)}")
    app = apps[0]
    with (app / "Info.plist").open("rb") as info:
        executable = app / plistlib.load(info)["CFBundleExecutable"]
    # Device archives are stripped: nm/strings alone cannot establish that the
    # Objective-C runtime can discover the class. Inspect its arm64 class metadata.
    result = subprocess.run(
        ["xcrun", "otool", "-arch", "arm64", "-ov", str(executable)],
        check=True,
        capture_output=True,
        text=True,
    )
    count = class_definitions(result.stdout.splitlines())[ADAPTER_CLASS]
    if count != 1:
        raise ValueError(
            f"Expected exactly one {ADAPTER_CLASS} definition in {executable}, found {count}. "
            "Check MetaAdapterTarget linkage and the -ObjC linker flag before publishing."
        )
    print(f"PASS: {ADAPTER_CLASS} has one arm64 Objective-C class definition in {executable}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="Path to the Release .xcarchive")
    args = parser.parse_args()
    try:
        verify_archive(args.archive)
    except (OSError, KeyError, ValueError, subprocess.CalledProcessError) as error:
        print(f"Meta adapter verification failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
