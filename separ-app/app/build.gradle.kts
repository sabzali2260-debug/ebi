plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.separ.alert"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.separ.alert"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
