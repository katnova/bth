#!/bin/bash

set -o pipefail

UNAMER="$(uname -r)"
UNAME="$(uname)"

if [[ "$UNAMER" == *"cachyos"* ]]; then
    curl --fail --location --silent --show-error 'https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT&revision=master&file=shell/bth-install-arch.sh' | bash
elif [[ "$UNAMER" == *"arch"* ]]; then
    curl --fail --location --silent --show-error 'https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT&revision=master&file=shell/bth-install-arch.sh' | bash
elif [[ "$UNAMER" == *"manjaro"* ]]; then
    curl --fail --location --silent --show-error 'https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT&revision=master&file=shell/bth-install-arch.sh' | bash
elif [[ "$UNAME" == "Darwin" ]]; then
    echo "MacOS detected."
else
    echo "Unknown or potentially supported OS: $UNAMER"
fi
