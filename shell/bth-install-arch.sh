#!/bin/bash

mkdir /tmp/bth
curl https://git.akat.xyz/bth/~raw?disposition=ATTACHMENT\&revision=master\&file=PKGBUILD -o PKGBUILD
makepkg -si < /dev/tty
rm -rf /tmp/bth
