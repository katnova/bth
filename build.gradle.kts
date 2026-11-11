import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}


group = "xyz.akat.bth"
version = "0.0.197"
description = "Better Terminal History"


val brewPrefix = providers.gradleProperty("brewPrefix")
    .orElse(providers.environmentVariable("HOMEBREW_PREFIX"))
    .getOrElse("/opt/homebrew")

val generatedVersionFile = layout.projectDirectory.file("src/nativeMain/kotlin/buildinfo/BuildInfo.kt")
val generateVersionInfo = tasks.register("generateVersionInfo") {
    description = ""
    outputs.file(generatedVersionFile)
    doLast {
        val outputFile = generatedVersionFile.asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package buildinfo

            object BuildInfo {
                const val VERSION = "$version"
            }
            """.trimIndent()
        )
    }
}

repositories {
    mavenCentral()
}

kotlin {
    if (HostManager.hostIsLinux) linuxX64()
    if (HostManager.hostIsMac) macosArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main") {
            cinterops {
                create("sqlite3") { //Shouldn't need extra compiler arg for sqlite on MacOS.
                    defFile("src/nativeInterop/cinterop/sqlite3.def")
                    packageName("sqlite3")
                }

                create("notcurses") {
                    defFile("src/nativeInterop/cinterop/notcurses.def")
                    packageName("notcurses")
                    if(HostManager.hostIsMac) {
                        compilerOpts("-I$brewPrefix/include")
                    }
                }

                create("confuse") {
                    defFile("src/nativeInterop/cinterop/confuse.def")
                    packageName("confuse")
                    if(HostManager.hostIsMac) {
                        compilerOpts("-I$brewPrefix/include")
                    }
                }
            }
        }

        binaries {
            executable {
                entryPoint = "main"
//            freeCompilerArgs += "-Xbinary=objcDisposeOnMain=false"
//            freeCompilerArgs.add("-Xbinary=disableMmap=false")
//            freeCompilerArgs.add("-Xbinary=stackProtector=all")
//            freeCompilerArgs.add("-Xbinary=latin1Strings=false") //kills perf, experimental
//            freeCompilerArgs.add("-Xbinary=pagedAllocator=false") // experimental
//            freeCompilerArgs.add("-Xbinary=gc=pmcs") // experimental?
//            freeCompilerArgs.add("-Xbinary=preCodegenInlineThreshold=40") // experimental
//            freeCompilerArgs.add("-Xklib-ir-inliner=full") // experimental
//            freeCompilerArgs.add("-Xpartial-linkage-loglevel=INFO")
//            freeCompilerArgs.add("-Xbinary=smallBinary=false") //kills perf, experimental
                when (konanTarget.family) {
                    Family.LINUX -> {
                        linkerOpts = mutableListOf(
                            "-Wl,-z,relro",
                            "-Wl,-z,now",
                            // "-static"
                        )
                        freeCompilerArgs += "-Xoverride-konan-properties=" +
                                "targetSysRoot.linux_x64=/;" +
                                "libGcc.linux_x64=/usr/lib/gcc/x86_64-pc-linux-gnu/16;" +
                                "linker.linux_x64=/usr/bin/ld.lld;" +
                                "crtFilesLocation.linux_x64=usr/lib"
                        freeCompilerArgs += "-Xbinary=sourceInfoType=libbacktrace"
                    }
                    Family.OSX -> {
                        linkerOpts("-L$brewPrefix/lib")
                    }
                    else -> {}
                }
            }
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(libs.kotlinxCoroutinesCore)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateVersionInfo)
}

fun gitArchiveSha256(ref: String, dir: File): String {
    val process = ProcessBuilder("git", "-c", "core.abbrev=no", "archive", "--format", "tar", ref)
        .directory(dir)
        .start()
    val messageDigest = MessageDigest.getInstance("SHA-256")
    process.inputStream.use { input ->
        val buffer = ByteArray(1 shl 16)
        var n: Int
        while (input.read(buffer).also { n = it } > 0) messageDigest.update(buffer, 0, n)
    }
    val error = process.errorStream.bufferedReader().readText()
    if (process.waitFor() != 0) throw GradleException("git archive failed for $ref: $error")
    return messageDigest.digest().joinToString("") { "%02x".format(it) }
}

val generatedPkgbuildFile = layout.projectDirectory.file("PKGBUILD")

val generatePkgbuild = tasks.register("generatePkgbuild") {
    description = "Generate PKGBUILD"
    doLast {
        val outputFile = generatedPkgbuildFile.asFile
        outputFile.writeText(
            $$"""
            # Maintainer: akat <akat@akat.xyz>
            pkgname=bth
            pkgver=$$version
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
            sha256sums=('$${gitArchiveSha256("HEAD", rootDir)}')
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
        """.trimIndent())
    }
}