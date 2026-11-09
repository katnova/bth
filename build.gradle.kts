import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "xyz.akat.bth"
version = "0.0.187"

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
//                freeCompilerArgs += "-Xbinary=objcDisposeOnMain=false"
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
