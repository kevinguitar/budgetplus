"""Guard against Calf components that crash on iOS when calf-ui is not exported.

:shared deliberately does not export() calf-ui into the iOS framework: the export keeps all of
Calf's public API, its SFSymbol catalog and every Skiko type it mentions alive (~6.7 MB of
release binary). That is only safe while the app avoids the Calf components whose Kotlin objects
receive UIKit callbacks, through @ObjCAction target-action methods or delegate protocols. Without
the export those callbacks are missing at runtime, and the app crashes with "unrecognized
selector sent to instance" (Calf issues #189 and #330).

test_calf_export.py runs these checks in iOS CI. To re-audit after a Calf upgrade:

    python3 iosApp/scripts/calf_export_check.py audit \\
        ~/.gradle/caches/modules-2/files-2.1/com.mohamedrejeb.calf/calf-ui-iosarm64/<version>/*/calf-ui-iosarm64-<version>-sources.jar

then copy the printed packages into CALLBACK_PACKAGES and the version into AUDITED_CALF_VERSION.
"""

import argparse
import os
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]

# Result of `audit` on the calf-ui iosarm64 sources jar of this version.
AUDITED_CALF_VERSION = "0.14.0"
CALLBACK_PACKAGES = frozenset({"datepicker", "dialog", "fab", "navigation", "sheet", "timepicker"})

CALF_PACKAGE = re.compile(r"\bcom\.mohamedrejeb\.calf\.ui\.([a-z0-9_]+)\b")
CALF_EXPORT = re.compile(r"^\s*(?:if\s*\(.*\)\s*)?export\(\s*libs\.calf\.ui\s*\)")
CALF_VERSION = re.compile(r'^\s*calf\s*=\s*"([^"]+)"', re.MULTILINE)
# A Kotlin object UIKit calls back into: a target-action method or a delegate protocol.
UIKIT_CALLBACK = re.compile(r"@ObjCAction\b|NSObject\(\)\s*,\s*[\w.]+Protocol\b")
# Source sets that never reach the iOS binary.
NON_IOS_SOURCE_SETS = re.compile(r"/(?:android\w*|jvm\w*|desktop\w*|js\w*|wasm\w*|\w*Test)/")


def strip_comments(kotlin):
    kotlin = re.sub(r"/\*.*?\*/", "", kotlin, flags=re.DOTALL)
    return re.sub(r"//[^\n]*", "", kotlin)


def exports_calf(build_script):
    """Whether a Gradle Kotlin script calls export(libs.calf.ui) outside comments."""
    return any(CALF_EXPORT.match(line) for line in strip_comments(build_script).splitlines())


def calf_version(version_catalog):
    match = CALF_VERSION.search(version_catalog)
    if not match:
        raise ValueError("No calf version in the version catalog")
    return match.group(1)


def callback_packages_used(kotlin):
    """Calf packages from CALLBACK_PACKAGES that a Kotlin source references."""
    return sorted(set(CALF_PACKAGE.findall(strip_comments(kotlin))) & CALLBACK_PACKAGES)


SKIPPED_DIRS = {"build", ".git", ".gradle", ".idea", ".kotlin", "node_modules"}


def ios_kotlin_sources(root=ROOT):
    for directory, subdirectories, files in os.walk(root):
        subdirectories[:] = sorted(d for d in subdirectories if d not in SKIPPED_DIRS)
        for name in sorted(files):
            path = Path(directory) / name
            relative = "/" + path.relative_to(root).as_posix()
            if name.endswith(".kt") and "/src/" in relative and not NON_IOS_SOURCE_SETS.search(relative):
                yield path


def violations(root=ROOT):
    """(file, packages) for every iOS source using a callback package while calf-ui isn't exported."""
    if exports_calf((root / "shared/build.gradle.kts").read_text()):
        return []
    found = []
    for path in ios_kotlin_sources(root):
        packages = callback_packages_used(path.read_text())
        if packages:
            found.append((path.relative_to(root).as_posix(), packages))
    return found


def callback_packages_in_sources(files):
    """Audit: Calf iOS packages with Kotlin objects that receive UIKit callbacks.

    `files` yields (path inside the sources jar, Kotlin text) pairs.
    """
    packages = set()
    for name, text in files:
        match = re.search(r"iosMain/com/mohamedrejeb/calf/ui/([a-z0-9_]+)/", name)
        if match and UIKIT_CALLBACK.search(strip_comments(text)):
            packages.add(match.group(1))
    return sorted(packages)


def read_sources(path):
    path = Path(path)
    if path.is_dir():
        for file in sorted(path.rglob("*.kt")):
            yield file.relative_to(path).as_posix(), file.read_text()
    else:
        with zipfile.ZipFile(path) as jar:
            for name in jar.namelist():
                if name.endswith(".kt"):
                    yield name, jar.read(name).decode()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("check", help="Fail if iOS code uses a Calf callback package without the export")
    audit = commands.add_parser("audit", help="List Calf iOS packages that receive UIKit callbacks")
    audit.add_argument("sources", type=Path, help="calf-ui iosarm64 sources jar, or an unpacked source tree")
    args = parser.parse_args()

    if args.command == "audit":
        print(" ".join(callback_packages_in_sources(read_sources(args.sources))))
        return 0

    found = violations()
    for file, packages in found:
        print(f"error: {file} uses calf-ui {', '.join(packages)} without export(libs.calf.ui)", file=sys.stderr)
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
