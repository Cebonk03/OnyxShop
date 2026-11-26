#!/usr/bin/env python3
"""Check vendored Maven coordinates in libs/VERSIONS.tsv against their repositories.

usage: check_updates.py [--all]

Network only: the offline build never calls this. Rows detected via embedded
pom.properties are looked up on Maven Central and the PaperMC repository; rows
whose coordinate came from a manifest, or that have none, are reported as
"manual" and must be checked at the plugin's own download page.
"""
import concurrent.futures
import json
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

REPOS = [
    'https://repo1.maven.org/maven2',
    'https://repo.papermc.io/repository/maven-public',
]
TIMEOUT = 12


def metadata_url(repo, group, artifact):
    return '%s/%s/%s/maven-metadata.xml' % (repo, group.replace('.', '/'), artifact)


def latest_version(group, artifact):
    for repo in REPOS:
        try:
            with urllib.request.urlopen(metadata_url(repo, group, artifact), timeout=TIMEOUT) as response:
                text = response.read().decode('utf-8', 'replace')
        except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError):
            continue
        release = re.search(r'<release>([^<]+)</release>', text)
        if release:
            return release.group(1)
        versions = re.findall(r'<version>([^<]+)</version>', text)
        if versions:
            return versions[-1]
    return None


def paper_builds(line):
    url = 'https://fill.papermc.io/v3/projects/paper/versions/%s/builds' % line
    try:
        with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
            builds = json.loads(response.read().decode('utf-8', 'replace'))
    except (urllib.error.URLError, urllib.error.HTTPError, TimeoutError, ValueError):
        return None
    if not builds:
        return None
    return builds[0].get('id'), builds[0].get('channel', '?')


def check_paper(path, coord):
    version = coord.rsplit(':', 1)[1]
    line = version.split('-')[0]
    builds = paper_builds(line)
    if builds is None:
        return (path, coord, 'not-found', '-')
    latest, channel = builds
    match = re.search(r'\.build\.(\d+)-', Path(path).name)
    if match is None:
        return (path, coord, 'info', 'build %s (%s)' % (latest, channel))
    current = int(match.group(1))
    if latest == current:
        return (path, coord, 'current', 'build %s (%s)' % (latest, channel))
    return (path, coord, 'newer' if latest > current else 'ahead',
            'build %s (%s)' % (latest, channel))


def read_rows():
    path = Path('libs/VERSIONS.tsv')
    if not path.exists():
        sys.exit('libs/VERSIONS.tsv not found - run scripts/gen_dependency_manifest.py first')
    rows = []
    with open(path, encoding='utf-8') as handle:
        next(handle, None)
        for line in handle:
            parts = line.rstrip('\n').split('\t')
            if len(parts) == 4:
                rows.append(parts)
    return rows


def main():
    show_all = '--all' in sys.argv
    rows = read_rows()
    results = []

    def work(row):
        path, kind, coord, _ = row
        if kind == 'paper':
            return check_paper(path, coord)
        if kind != 'pom' or coord.count(':') != 2:
            return (path, coord, 'manual', '-')
        group, artifact, current = coord.split(':')
        if current.endswith('-SNAPSHOT'):
            return (path, coord, 'snapshot', '-')
        latest = latest_version(group, artifact)
        if latest is None:
            return (path, coord, 'not-found', '-')
        return (path, coord, 'current' if latest == current else 'newer', latest)

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        for result in pool.map(work, rows):
            results.append(result)

    interesting = [item for item in results if show_all or item[2] not in ('current', 'snapshot', 'manual')]
    for path, coord, status, latest in sorted(interesting, key=lambda item: (item[2], item[0])):
        print('%-10s %-55s %s -> %s' % (status, coord, path, latest))

    counts = {}
    for _, _, status, _ in results:
        counts[status] = counts.get(status, 0) + 1
    print('\n' + ', '.join('%s=%d' % item for item in sorted(counts.items())))
    print('manual rows have no Spring/Maven metadata; check their vendor page.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
