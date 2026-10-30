plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build310trial"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 310
        versionName = "1.0.27-build310-search-editor-gestures"
        manifestPlaceholders["appLabel"] = "Orivra Candidate 310"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Candidate 310"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
