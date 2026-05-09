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
        artifactId = "lens-ui",
        version = "1.2.0-SNAPSHOT",
    )
    pom {
        name.set("Lens UI")
        description.set("Compose Multiplatform UI for inspecting Lens network logs, mocks, and DataStores.")
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
            api(project(":lens-core"))
            implementation(libs.kotlin.stdlib)
            implementation(libs.jetbrains.material3)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation)
            implementation(libs.napier)
            implementation(libs.datastore.core)
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.core)
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
    namespace = "io.github.farhazulmullick.lenslogger.ui"
    compileSdk = 35
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    kotlin {
        jvmToolchain(17)
    }
}
