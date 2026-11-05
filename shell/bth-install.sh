#!/bin/bash

UNAMER="$(uname -r)"
UNAME="$(uname)"

if [[ "$UNAMER" == *"cachyos"* ]]; then
    curl https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT\&revision=master\&file=shell/bth-install-arch.sh | bash
elif [[ "$UNAMER" == *"arch"* ]]; then
    curl https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT\&revision=master\&file=shell/bth-install-arch.sh | bash
elif [[ "$UNAMER" == *"manjaro"* ]]; then
    curl https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT\&revision=master\&file=shell/bth-install-arch.sh | bash
elif [[ "$UNAME" == "Darwin" ]]; then
    echo "MacOS detected."
else
    echo "Unknown or potentially supported OS: $UNAMER"
fi
