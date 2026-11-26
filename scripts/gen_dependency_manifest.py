#!/usr/bin/env python3
"""Regenerate libs/VERSIONS.tsv from the vendored jars.

usage: gen_dependency_manifest.py [repo-root]

Each row records the jar path, how its coordinate was detected
(pom / manifest / unknown), the coordinate itself and its SHA-256.
Run this after adding, removing or replacing anything under libs/.
"""
import hashlib
import re
import sys
import zipfile
from pathlib import Path

ROOTS = ['libs/cp', 'libs/api', 'libs/nms', 'libs/vendored']


def filename_coordinate(path):
    name = path.name
    if name.startswith('paper-api-') and name.endswith('.jar'):
        rest = name[len('paper-api-'):-len('.jar')]
        line = rest.split('.build.')[0]
        return 'paper', 'io.papermc.paper:paper-api:%s' % line
    if name.startswith('paper-dev-') and name.endswith('.jar'):
        return 'paper-dev', 'io.papermc.paper:dev-bundle:%s' % name[len('paper-dev-'):-len('.jar')]
    if name.startswith('spigot-') and name.endswith('.jar'):
        return 'nms', 'org.spigotmc:spigot:%s' % name[len('spigot-'):-len('.jar')]
    return None


def coordinate(path):
    fallback = filename_coordinate(path)
    if fallback:
        return fallback
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        for name in names:
            if name.endswith('pom.properties'):
                text = jar.read(name).decode('utf-8', 'replace')
                group = re.search(r'groupId=(.+)', text)
                artifact = re.search(r'artifactId=(.+)', text)
                version = re.search(r'version=(.+)', text)
                if group and artifact and version:
                    return 'pom', '%s:%s:%s' % (group.group(1).strip(),
                                                artifact.group(1).strip(),
                                                version.group(1).strip())
        manifest = ''
        if 'META-INF/MANIFEST.MF' in names:
            manifest = jar.read('META-INF/MANIFEST.MF').decode('utf-8', 'replace')
        title = re.search(r'(?:Implementation-Title|Bundle-SymbolicName):\s*(\S+)', manifest)
        version = re.search(r'(?:Implementation-Version|Bundle-Version):\s*(\S+)', manifest)
        if title and version:
            return 'manifest', '%s:%s' % (title.group(1), version.group(1))
    return 'unknown', '-'


def sha256(path):
    digest = hashlib.sha256()
    with open(path, 'rb') as handle:
        for block in iter(lambda: handle.read(1 << 20), b''):
            digest.update(block)
    return digest.hexdigest()


def main():
    root = Path(sys.argv[1] if len(sys.argv) > 1 else '.').resolve()
    rows = []
    for relative in ROOTS:
        for path in sorted((root / relative).rglob('*.jar')):
            kind, coord = coordinate(path)
            rows.append((path.relative_to(root).as_posix(), kind, coord, sha256(path)))

    target = root / 'libs' / 'VERSIONS.tsv'
    with open(target, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('path\tkind\tcoordinate\tsha256\n')
        for row in rows:
            handle.write('\t'.join(row) + '\n')

    counts = {}
    for _, kind, _, _ in rows:
        counts[kind] = counts.get(kind, 0) + 1
    print('wrote %s (%d jars: %s)' % (target.relative_to(root),
                                      len(rows),
                                      ', '.join('%s=%d' % item for item in sorted(counts.items()))))


if __name__ == '__main__':
    main()
