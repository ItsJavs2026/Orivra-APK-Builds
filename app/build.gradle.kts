plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build291trial"
        minSdk = 26
        targetSdk = 35
        versionCode = 291
        versionName = "1.0.17-build291-safe-enter"
        manifestPlaceholders["appLabel"] = "Orivra Build 291 Safe Enter Trial"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Build 291 Safe Enter Trial"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
