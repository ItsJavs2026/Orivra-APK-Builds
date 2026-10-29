plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.publictrial"
        minSdk = 26
        targetSdk = 35
        versionCode = 286
        versionName = "1.0.12-build286-publictrial"
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
