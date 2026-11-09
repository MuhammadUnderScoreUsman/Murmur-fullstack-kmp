import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }
    android {
       namespace = "com.example.chatappkross.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    sourceSets {

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)

        }

        commonMain.dependencies {
            api(project(":core"))

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)

            implementation(libs.ktor.serialization.kotlinx.json)

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")

        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }



        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}