# Maintainer: akat <akat@akat.xyz>
pkgname=bth
pkgver=0.0.196
pkgrel=1
epoch=
pkgdesc="Better Terminal History"
arch=('x86_64')
url="https://bth.sh"
license=('BSD-3-Clause')
groups=()
depends=('notcurses' 'sqlite' 'confuse' 'glibc' 'gcc-libs' 'libxcrypt')
makedepends=('jdk25-openjdk' 'lld' 'git')
checkdepends=('jdk25-openjdk' 'lld')
optdepends=()
provides=('bth')
conflicts=()
replaces=()
backup=()
options=()
install=
changelog=
source=("git+https://git.akat.xyz/$pkgname.git#branch=${TARGETREVISION:-master}")
noextract=()
sha256sums=('329e33ff0cc8415581a500246aad53505e9afe318bda5e1c4de9bf9480e43e5c')
validpgpkeys=('3F0A3AA96A8EFD6CFD5F67423902BCEB8BFD5E8F')

prepare() {
	GRADLE_USER_HOME="$srcdir/.gradle"
}

build() {
	cd "$srcdir/$pkgname" # todo: This seems wrong.
	./gradlew --no-daemon compileKotlinLinuxX64 assemble linuxX64Binaries
}

check() {
	cd "$srcdir/$pkgname"
	./gradlew --no-daemon linuxX64Test
}

package() {
	cd "$srcdir/$pkgname"
	install -Dm755 "build/bin/linuxX64/releaseExecutable/$pkgname.kexe" "$pkgdir/usr/bin/$pkgname"
}