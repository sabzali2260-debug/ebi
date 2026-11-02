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
        targetSdk = 36
        versionCode = 7
        versionName = "0.4.3"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.maplibre.gl:android-sdk:13.6.1")
    testImplementation("junit:junit:4.13.2")
}
