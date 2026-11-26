#!/bin/sh
# Installs the tracked hooks into .git/hooks.
set -e
repo_root=$(git rev-parse --show-toplevel)
cp "$repo_root/scripts/hooks/pre-commit" "$repo_root/.git/hooks/pre-commit"
chmod +x "$repo_root/.git/hooks/pre-commit"
echo "installed .git/hooks/pre-commit"
