# Maintainer: akat <akat@akat.xyz>
pkgname=bth
pkgver=0.0.194
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
source=("git+https://git.akat.xyz/$pkgname.git")
noextract=()
sha256sums=('8c19b4fb9cfda7cc0acd8e266c46d1c27806cb063efd4afd13bad4602ca05e7c')
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