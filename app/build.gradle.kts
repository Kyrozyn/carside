import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "me.kyrozyn.carside"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    val appVersionCode = (project.findProperty("VERSION_CODE") as? String
        ?: System.getenv("VERSION_CODE"))?.toIntOrNull() ?: 22
    val appVersionName = (project.findProperty("VERSION_NAME") as? String
        ?: System.getenv("VERSION_NAME")) ?: "1.0.$appVersionCode"

    defaultConfig {
        applicationId = "me.kyrozyn.carside"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreEnv = project.findProperty("KEYSTORE_FILE") as? String ?: System.getenv("KEYSTORE_FILE")
            val storeFileObj = if (!keystoreEnv.isNullOrBlank()) {
                val f = file(keystoreEnv)
                if (f.exists()) f else rootProject.file(keystoreEnv)
            } else {
                rootProject.file("carside.jks")
            }
            val storePasswordProp = project.findProperty("KEYSTORE_PASSWORD") as? String ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAliasProp = project.findProperty("KEY_ALIAS") as? String ?: System.getenv("KEY_ALIAS")
            val keyPasswordProp = project.findProperty("KEY_PASSWORD") as? String ?: System.getenv("KEY_PASSWORD")

            if (!storePasswordProp.isNullOrBlank() && !keyAliasProp.isNullOrBlank() && !keyPasswordProp.isNullOrBlank() && storeFileObj.exists()) {
                storeFile = storeFileObj
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            } else {
                // Pastikan file debug.keystore ada secara fisik sebelum release signing divalidasi oleh Gradle
                val debugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")
                if (!debugKeystore.exists()) {
                    debugKeystore.parentFile?.mkdirs()
                    try {
                        ProcessBuilder(
                            "keytool", "-genkey", "-v",
                            "-keystore", debugKeystore.absolutePath,
                            "-alias", "androiddebugkey",
                            "-storepass", "android",
                            "-keypass", "android",
                            "-keyalg", "RSA",
                            "-keysize", "2048",
                            "-validity", "10000",
                            "-dname", "CN=Android Debug,O=Android,C=US"
                        ).inheritIO().start().waitFor()
                    } catch (_: Exception) {}
                }

                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                } else {
                    initWith(getByName("debug"))
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    packaging {
        jniLibs {
            // Menyimpan simbol debug untuk library native
            keepDebugSymbols += listOf("**/*.so")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)
    implementation(libs.maplibre.android)

    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.okhttp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}
