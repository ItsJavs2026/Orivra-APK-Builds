plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build391trial"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 391
        versionName = "1.0.55-build391-v4-union-idle-swipes"
        manifestPlaceholders["appLabel"] = "Orivra Candidate 391"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Candidate 391"
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
