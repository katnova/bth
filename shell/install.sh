#!/bin/bash
SCRIPT_DIR="$(dirname "$(realpath "$0")")"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
"$PROJECT_ROOT/gradlew" compileKotlinLinuxX64 assemble linuxX64Binaries --build-cache
sudo cp "$PROJECT_ROOT/build/bin/linuxX64/releaseExecutable/bth.kexe" /usr/local/bin/bth
