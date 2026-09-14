#!/usr/bin/env bash
# Installs git hooks from shell/hooks/ into .git/hooks/
set -euo pipefail

SCRIPT_DIR="$(dirname "$(realpath "$0")")"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
HOOKS_SRC="$SCRIPT_DIR/hooks"
HOOKS_DST="$PROJECT_ROOT/.git/hooks"

if [[ ! -d "$PROJECT_ROOT/.git" ]]; then
    echo "install-hooks: not a git repository: $PROJECT_ROOT" >&2
    exit 1
fi

for hook in "$HOOKS_SRC"/*; do
    name="$(basename "$hook")"
    dest="$HOOKS_DST/$name"
    cp "$hook" "$dest"
    chmod +x "$dest"
    echo "installed: .git/hooks/$name"
done
