#!/bin/bash

set -euo pipefail

work_dir=$(mktemp -d "${TMPDIR:-/tmp}/bth-install.XXXXXX")
cleanup() {
    rm -rf -- "$work_dir"
}
trap cleanup EXIT

makepkg_config=${MAKEPKG_CONF:-/etc/makepkg.conf}
if [[ "$makepkg_config" != /* ]]; then
    makepkg_config="$PWD/$makepkg_config"
fi

mkdir -p "$work_dir/src" "$work_dir/build"
target_revision=${TARGETREVISION:-master}
curl --fail --location --silent --show-error \
    --get \
    --data-urlencode 'disposition=ATTACHMENT' \
    --data-urlencode "revision=$target_revision" \
    --data-urlencode 'file=PKGBUILD' \
    'https://git.akat.xyz/bth/~raw' \
    --output "$work_dir/PKGBUILD"

printf -v quoted_makepkg_config '%q' "$makepkg_config"
printf -v quoted_srcdest '%q' "$work_dir/src"
printf -v quoted_builddir '%q' "$work_dir/build"
makepkg_config_file="$work_dir/makepkg.conf"
{
    printf 'source %s\n' "$quoted_makepkg_config"
    printf 'SRCDEST=%s\n' "$quoted_srcdest"
    printf 'BUILDDIR=%s\n' "$quoted_builddir"
} > "$makepkg_config_file"

cd "$work_dir"
makepkg --config "$makepkg_config_file" -si < /dev/tty
