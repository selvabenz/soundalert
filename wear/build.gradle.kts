plugins {
    id("com.android.application")
}

android {
    namespace = "com.bridgeconn.soundalert"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bridgeconn.soundalert.v2beta"
        minSdk = 30
        targetSdk = 37
        versionCode = providers.gradleProperty("soundAlertVersionCode").get().toInt()
        versionName = providers.gradleProperty("soundAlertVersionName").get()
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
}

dependencies {
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
}
