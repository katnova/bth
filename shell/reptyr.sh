#!/usr/bin/env bash
set -euo pipefail

clear

SYMLINK=/tmp/bth_reptyr_pty_symlinx
PIDFILE=/tmp/bth_reptyr_pty_symlinx.pid

if ! command -v reptyr >/dev/null 2>&1; then
  echo "reptyr not found on PATH (install it, e.g. 'sudo apt install reptyr')" >&2
  exit 1
fi

cleanup() { rm -f "$SYMLINK" "$PIDFILE"; }
trap cleanup EXIT INT TERM

printf "Waiting for bth...\n"

HOLDER='
  ln -sf "$REPTYR_PTY" "'"$SYMLINK"'"
  printf "%s\n" "$PPID" > "'"$PIDFILE"'"
  exec sleep infinity
'
reptyr -l bash -c "$HOLDER"
