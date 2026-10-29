plugins { id("com.android.application") }

android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        // This separate trial package cannot replace any existing Orivra install.
        applicationId = "com.chroomareborn.keyboard.publictrial"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-publictrial"
        manifestPlaceholders["appLabel"] = "Orivra Public Trial"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Public Trial"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
