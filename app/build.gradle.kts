plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build290trial"
        minSdk = 26
        targetSdk = 35
        versionCode = 290
        versionName = "1.0.16-build290-swipesafety"
        manifestPlaceholders["appLabel"] = "Orivra Build 290 Trial"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Build 290 Trial"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
