# Maintainer: akat <akat@akat.xyz>
pkgname=bth
pkgver=0.0.200
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
source=("git+https://git.akat.xyz/$pkgname.git#tag=v0.0.200")
sha256sums=('65e06a99539fac2b3934b36daa423d63d081b7dbf3e028389d8c27d37b8474f3')
noextract=()

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