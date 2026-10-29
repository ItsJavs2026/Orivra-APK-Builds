plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build289trial"
        minSdk = 26
        targetSdk = 35
        versionCode = 289
        versionName = "1.0.15-build289-swipedelete"
        manifestPlaceholders["appLabel"] = "Orivra Build 289 Trial"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Build 289 Trial"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
