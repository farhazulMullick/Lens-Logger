plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.composeHotReload)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.farhazulmullick",
        artifactId = "lens-logger",
        version = libs.versions.lensLogger.get()
    )

    pom {
        name.set("Lens Logger")
        description.set(
            "LensLogger is a Kotlin Multiplatform (KMP) library for Android and iOS " +
                "that makes debugging network requests effortless. " +
                "It automatically logs all Ktor network requests and responses, " +
                "and provides a built-in UI to inspect these logs directly in your app."
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
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "lensktorKit"
        }
    }
    sourceSets {
        jvm("desktop")
        val desktopMain by getting
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serializer)
            implementation(libs.ktor.json.serializer)
            implementation(libs.napier)
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        androidMain.dependencies{}
        iosMain.dependencies {}
        desktopMain.dependencies {}

    }

}

android {
    namespace = "io.github.farhazulmullick.lenslogger"
    buildFeatures {
        compose = true
        dataBinding = true
    }

    compileSdk = 35
    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        androidTarget()
        jvmToolchain(17)
    }
}