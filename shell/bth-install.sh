#!/bin/bash

UNAME="$(uname -r)"



if [[ "$UNAME" == *"cachyos"* ]]; then
    curl https://git.akat.xyz/bth/~raw/master/shell/bth-install-arch.sh | bash
elif [[ "$UNAME" == *"arch"* ]]; then
    curl https://git.akat.xyz/bth/~raw/master/shell/bth-install-arch.sh | bash
elif [[ "$UNAME" == *"manjaro"* ]]; then
    curl https://git.akat.xyz/bth/~raw/master/shell/bth-install-arch.sh | bash
else
    echo "Unknown or potentially supported OS: $UNAME"
fi