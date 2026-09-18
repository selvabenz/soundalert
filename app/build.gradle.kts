plugins {
    id("com.android.application")
}

android {
    namespace = "com.bridgeconn.soundalert"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bridgeconn.soundalert.v2beta"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("soundAlertVersionCode").get().toInt()
        versionName = providers.gradleProperty("soundAlertVersionName").get()

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("beta") {
            storeFile = rootProject.file("signing/soundalert-v2-beta.keystore")
            storePassword = "soundalert-v2-beta"
            keyAlias = "soundalert-v2-beta"
            keyPassword = "soundalert-v2-beta"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("beta")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        noCompress += "tflite"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*"
        )
    }
}

dependencies {
    implementation("com.google.mediapipe:tasks-audio:1.0.0")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    testImplementation("junit:junit:4.13.2")
}
