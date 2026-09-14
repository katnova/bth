#!/bin/bash

mkdir /tmp/bth
curl https://git.akat.xyz/bth/~raw/master/PKGBUILD -o PKGBUILD
makepkg -si < /dev/tty
rm -rf /tmp/bth
