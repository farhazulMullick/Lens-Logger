plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.vanniktech.publish)
}

/**
 * Convenience aggregate: pulls in [lens-core], [lens-ui], [lens-ktor], and (Android/desktop only)
 * [lens-okhttp]. For smaller binaries or clearer boundaries, depend on the sub-artifacts directly.
 */
mavenPublishing {
    coordinates(
        groupId = "io.github.farhazulmullick",
        artifactId = "lens-logger",
        version = providers.gradleProperty("LIBRARY_VERSION").get(),
    )
    pom {
        name.set("Lens Logger")
        description.set(
            "Kotlin Multiplatform library for debugging network requests with optional Ktor, OkHttp, and Compose UI modules."
        )
        inceptionYear.set("2025")
        url.set("https://github.com/farhazulMullick/Lens/")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("farhazulMullick")
                name.set("Farhazul Mullick")
                url.set("https://github.com/farhazulMullick/")
            }
        }
        scm {
            url.set("https://github.com/farhazulMullick/Lens-Logger/")
            connection.set("scm:git:git://github.com/farhazulMullick/Lens-Logger.git")
            developerConnection.set("scm:git:ssh://git@github.com/farhazulMullick/Lens-Logger.git")
        }
    }
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "lensktorKit"
        }
    }

    jvm("desktop")
    androidTarget()

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            api(project(":lens-core"))
            api(project(":lens-ui"))
            api(project(":lens-ktor"))
        }
        androidMain.dependencies {
            api(project(":lens-okhttp"))
        }
        desktopMain.dependencies {
            api(project(":lens-okhttp"))
        }
    }
}

android {
    namespace = "io.github.farhazulmullick.lenslogger"
    compileSdk = 35
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
