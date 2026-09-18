plugins {
    id("com.android.application")
}

android {
    namespace = "com.bridgeconn.soundalert"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bridgeconn.soundalert"
        minSdk = 30
        targetSdk = 37
        versionCode = 20
        versionName = "0.2.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
