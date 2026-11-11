# Maintainer: akat <akat@akat.xyz>
pkgname=bth
pkgver=0.0.198
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
source=("git+https://git.akat.xyz/$pkgname.git#tag=v0.0.198")
sha256sums=('e0a520e695e23d9ed2364b113b2e761420ff9b7c63d6676ff216bdf49432e98f')
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