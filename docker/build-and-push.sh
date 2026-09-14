#!/usr/bin/env bash
set -euo pipefail

# Build and push script for Arch Linux BTH CI Docker image

REGISTRY="git.akat.xyz/bth"
IMAGE_NAME="bth-arch-ci"
DEFAULT_VERSION="0.0.85"

VERSION="${1:-${BTH_VERSION:-$DEFAULT_VERSION}}"
TAG_SPEC="${REGISTRY}/${IMAGE_NAME}:${VERSION}"
TAG_LATEST="${REGISTRY}/${IMAGE_NAME}:latest"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKERFILE="${SCRIPT_DIR}/ARCH_DOCKERFILE"

echo "==> Building image ${TAG_SPEC}..."
docker build \
  --build-arg BTH_VERSION="${VERSION}" \
  -t "${TAG_SPEC}" \
  -t "${TAG_LATEST}" \
  -f "${DOCKERFILE}" \
  "${SCRIPT_DIR}"

echo "==> Pushing ${TAG_SPEC}..."
docker push "${TAG_SPEC}"

echo "==> Pushing ${TAG_LATEST}..."
docker push "${TAG_LATEST}"

echo "==> Build and push completed successfully."
