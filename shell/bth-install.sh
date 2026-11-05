#!/bin/bash

set -o pipefail

UNAMER="$(uname -r)"
UNAME="$(uname)"

TARGETREVISION="${REVISION:-master}"

echo "Target revision: $TARGETREVISION"

if [[ "$UNAMER" == *"cachyos"* ]]; then
    echo "Installing bth vis PKGBUILD..."
    curl --fail --location --silent --show-error "https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT&revision=$TARGETREVISION&file=shell/bth-install-arch.sh" | bash
elif [[ "$UNAMER" == *"arch"* ]]; then
    echo "Installing bth vis PKGBUILD..."
    curl --fail --location --silent --show-error "https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT&revision=$TARGETREVISION&file=shell/bth-install-arch.sh" | bash
elif [[ "$UNAMER" == *"manjaro"* ]]; then
    echo "Installing bth vis PKGBUILD..."
    curl --fail --location --silent --show-error "https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT&revision=$TARGETREVISION&file=shell/bth-install-arch.sh" | bash
elif [[ "$UNAME" == "Darwin" ]]; then
    echo "MacOS detected."
else
    echo "Unknown or potentially supported OS: $UNAMER"
fi
