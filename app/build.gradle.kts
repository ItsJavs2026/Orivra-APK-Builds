plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build292trial"
        minSdk = 26
        targetSdk = 35
        versionCode = 292
        versionName = "1.0.18-build292-enter-swipe"
        manifestPlaceholders["appLabel"] = "Orivra Build 292 Trial"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Build 292 Trial"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
