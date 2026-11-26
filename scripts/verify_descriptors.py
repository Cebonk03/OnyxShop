#!/usr/bin/env python3
"""Descriptor parity gate for OnyxShop.

Two plugin descriptors ship in one jar: plugin.yml (legacy Bukkit/Spigot) and
paper-plugin.yml (Paper). They are maintained by hand, so this script fails the
build when their identity, permission nodes or soft dependencies drift apart.

Stdlib only; the build is fully offline.
"""

import re
import sys

BUILD_GRADLE = "build.gradle"
LEGACY = "src/main/resources/plugin.yml"
PAPER = "src/main/resources/paper-plugin.yml"

TOP_LEVEL_KEY = re.compile(r"^([A-Za-z][A-Za-z0-9_-]*):")
VERSION_IN_GRADLE = re.compile(r"^version\s*=\s*'([^']+)'", re.MULTILINE)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def scalar(text, key):
    match = re.search(r"(?m)^%s:\s*'?([^'\s#]+)'?" % re.escape(key), text)
    return match.group(1) if match else None


def block_lines(text, key):
    """Return the indented lines that belong to a top-level key."""
    lines = text.splitlines()
    out = []
    inside = False
    for line in lines:
        if TOP_LEVEL_KEY.match(line):
            inside = TOP_LEVEL_KEY.match(line).group(1) == key
            continue
        if inside:
            out.append(line)
    return out


def listed_values(lines):
    values = []
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if stripped.startswith("- "):
            values.append(stripped[2:].strip().strip("'\""))
    return values


def mapping_keys(lines):
    keys = []
    for line in lines:
        match = re.match(r"^ {4}([A-Za-z0-9_.-]+):\s*$", line)
        if match:
            keys.append(match.group(1))
    return keys


def duplicates(values):
    seen = set()
    dupes = []
    for value in values:
        if value in seen:
            dupes.append(value)
        seen.add(value)
    return dupes


def main():
    errors = []
    try:
        legacy = read(LEGACY)
        paper = read(PAPER)
        gradle = read(BUILD_GRADLE)
    except OSError as exc:
        print("verify_descriptors: cannot read descriptor: %s" % exc)
        return 1

    for key in ("name", "main", "version"):
        legacy_value = scalar(legacy, key)
        paper_value = scalar(paper, key)
        if legacy_value is None or paper_value is None:
            errors.append("'%s' missing in %s" % (key, LEGACY if legacy_value is None else PAPER))
        elif legacy_value != paper_value:
            errors.append("'%s' differs: %s=%s vs %s=%s" % (key, LEGACY, legacy_value, PAPER, paper_value))

    gradle_version = VERSION_IN_GRADLE.search(gradle)
    if not gradle_version:
        errors.append("cannot read 'version = ...' from %s" % BUILD_GRADLE)
    elif scalar(legacy, "version") != gradle_version.group(1):
        errors.append("version in %s (%s) does not match %s (%s)"
                      % (BUILD_GRADLE, gradle_version.group(1), LEGACY, scalar(legacy, "version")))

    for path, text in ((LEGACY, legacy), (PAPER, paper)):
        if scalar(text, "api-version") is None:
            errors.append("api-version missing in %s" % path)

    legacy_perms = set(re.findall(r"(?m)^  (OnyxShop[^:\s]*):", legacy))
    paper_perms = set(re.findall(r"(?m)^  (OnyxShop[^:\s]*):", paper))
    if legacy_perms != paper_perms:
        only_legacy = sorted(legacy_perms - paper_perms)
        only_paper = sorted(paper_perms - legacy_perms)
        errors.append("permission sets differ: only in %s %s, only in %s %s"
                      % (LEGACY, only_legacy, PAPER, only_paper))

    legacy_deps = set(listed_values(block_lines(legacy, "softdepend")))
    legacy_deps |= set(listed_values(block_lines(legacy, "loadbefore")))
    paper_deps = set(mapping_keys(block_lines(paper, "dependencies")))
    if legacy_deps != paper_deps:
        errors.append("dependency sets differ: only in %s %s, only in %s %s"
                      % (LEGACY, sorted(legacy_deps - paper_deps), PAPER, sorted(paper_deps - legacy_deps)))

    for path, text in ((LEGACY, legacy), (PAPER, paper)):
        keys = TOP_LEVEL_KEY.findall(text)
        for dupe in duplicates(keys):
            errors.append("duplicate top-level key '%s' in %s" % (dupe, path))
        for key in ("softdepend", "loadbefore"):
            for dupe in duplicates(listed_values(block_lines(text, key))):
                errors.append("duplicate %s entry '%s' in %s" % (key, dupe, path))

    if errors:
        print("verify_descriptors: FAILED")
        for error in errors:
            print("  - %s" % error)
        return 1

    print("verify_descriptors: OK (%d permission nodes, %d soft dependencies)" % (len(legacy_perms), len(legacy_deps)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
