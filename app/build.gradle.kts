plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build287trial"
        minSdk = 26
        targetSdk = 35
        versionCode = 287
        versionName = "1.0.13-build287-visualtrial"
        manifestPlaceholders["appLabel"] = "Orivra Build 287 Trial"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Build 287 Trial"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
