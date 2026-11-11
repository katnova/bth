#!/usr/bin/env bash

set -euo pipefail

push_release=false
while [[ $# -gt 0 ]]; do
    case "$1" in
        --push)
            push_release=true
            ;;
        *)
            printf 'Usage: %s [--push]\n' "$0" >&2
            exit 2
            ;;
    esac
    shift
done

root=$(git rev-parse --show-toplevel)
cd "$root"

current_branch=$(git symbolic-ref --short -q HEAD || true)
case "$current_branch" in
    master) dev_release=false ;;
    dev)    dev_release=true ;;
    *)
        echo "release: releases can only be cut from master (stable) or dev (dev); current: ${current_branch:-detached HEAD}" >&2
        exit 1
        ;;
esac

if [[ -n $(git status --porcelain) ]]; then
    echo "release: working tree must be clean" >&2
    exit 1
fi

version=$(sed -nE 's/^version[[:space:]]*=[[:space:]]*"([0-9]+\.[0-9]+\.[0-9]+)".*/\1/p' build.gradle.kts)
if [[ -z "$version" ]]; then
    echo "release: no semver version found in build.gradle.kts" >&2
    exit 1
fi

release_tag="v$version"
release_channel=stable
if [[ "$dev_release" == true ]]; then
    release_tag+="-dev"
    release_channel=dev
fi
echo "release: $release_channel release $release_tag from $current_branch"
if git rev-parse -q --verify "refs/tags/$release_tag" >/dev/null; then
    echo "release: tag $release_tag already exists" >&2
    exit 1
fi

git tag -s "$release_tag" -m "bth $release_tag"
if [[ "$dev_release" == true ]]; then
    ./gradlew --no-daemon "-PreleaseTag=$release_tag" generatePkgbuild
    git add PKGBUILD
else
    ./gradlew --no-daemon generatePkgbuild generateFormula
    git add PKGBUILD Formula/bth.rb
fi
git commit -m "Generate release artifacts for $release_tag"

if [[ "$push_release" == true ]]; then
    git push origin HEAD "$release_tag"
fi