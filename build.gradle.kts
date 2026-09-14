import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "xyz.akat.bth"
version = "0.0.173"

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
    linuxX64()

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName("main") {
            cinterops {
                create("sqlite3") {
                    defFile("src/nativeInterop/cinterop/sqlite3.def")
                    packageName("sqlite3")
                }

                create("notcurses") {
                    defFile("src/nativeInterop/cinterop/notcurses.def")
                    packageName("notcurses")
                }

                create("confuse") {
                    defFile("src/nativeInterop/cinterop/confuse.def")
                    packageName("confuse")
                }
            }
        }

        binaries {
            executable {
                entryPoint = "main"
                linkerOpts = mutableListOf(
                    "-Wl,-z,relro",
                    "-Wl,-z,now",
                    // "-static"
                )
            }
        }
    }

    tasks.withType<KotlinNativeLink>().configureEach {
        toolOptions {
            //todo: While this works, will probably need to be reworked.
            freeCompilerArgs.add("-Xoverride-konan-properties=" +
                    "targetSysRoot.linux_x64=/;" +
                    "libGcc.linux_x64=/usr/lib/gcc/x86_64-pc-linux-gnu/16;" +
                    "linker.linux_x64=/usr/bin/ld.lld;" +
                    "crtFilesLocation.linux_x64=usr/lib"
            )
//            freeCompilerArgs.add("-Xbinary=disableMmap=false")
//            freeCompilerArgs.add("-Xbinary=stackProtector=all")
//            freeCompilerArgs.add("-Xbinary=latin1Strings=false") //kills perf, experimental
//            freeCompilerArgs.add("-Xbinary=pagedAllocator=false") // experimental
//            freeCompilerArgs.add("-Xbinary=gc=pmcs") // experimental?
//            freeCompilerArgs.add("-Xbinary=preCodegenInlineThreshold=40") // experimental
            freeCompilerArgs.add("-Xbinary=objcDisposeOnMain=false")
//            freeCompilerArgs.add("-Xklib-ir-inliner=full") // experimental
//            freeCompilerArgs.add("-Xpartial-linkage-loglevel=INFO")
            freeCompilerArgs.add("-Xbinary=sourceInfoType=libbacktrace")
//            freeCompilerArgs.add("-Xbinary=smallBinary=false") //kills perf, experimental
        }
    }

    sourceSets {
        nativeMain.dependencies {
            implementation(libs.kotlinxCoroutinesLinuxx64)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateVersionInfo)
}
