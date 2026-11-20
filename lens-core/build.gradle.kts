plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.publish)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.farhazulmullick",
        artifactId = "lens-core",
        version = "1.2.0-SNAPSHOT",
    )
    pom {
        name.set("Lens Core")
        description.set(
            "Transport-neutral network log store, HTTP snapshots, mock rules, and DataStore helpers for Lens."
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
    )

    jvm("desktop")
    androidTarget()

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(compose.runtime)
            implementation(libs.jetbrains.material3)
            implementation(libs.napier)
            implementation(libs.datastore.core)
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {}
        desktopMain.dependencies {}

        val iosMain by creating { dependsOn(commonMain.get()) }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosX64Main by getting
        listOf(iosArm64Main, iosSimulatorArm64Main, iosX64Main).forEach { it.dependsOn(iosMain) }
        iosMain.dependencies {}
    }
}

android {
    namespace = "io.github.farhazulmullick.lenslogger.core"
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
