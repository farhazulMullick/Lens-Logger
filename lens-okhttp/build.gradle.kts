plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.vanniktech.publish)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.farhazulmullick",
        artifactId = "lens-okhttp",
        version = "1.2.0-SNAPSHOT",
    )
    pom {
        name.set("Lens OkHttp")
        description.set("OkHttp interceptor for Lens network logging and mocking (Android and JVM desktop).")
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
    androidTarget()
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            api(project(":lens-core"))
            implementation(libs.okhttp)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {}
        desktopMain.dependencies {}
    }
}

android {
    namespace = "io.github.farhazulmullick.lenslogger.okhttp"
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
