plugins { id("com.android.application") }
android {
    namespace = "com.chroomareborn.keyboard"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chroomareborn.keyboard.build300trial"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 300
        versionName = "1.0.21-build300-keep-compatibility"
        manifestPlaceholders["appLabel"] = "Orivra Candidate 300"
    }
    buildTypes {
        getByName("debug") {
            manifestPlaceholders["appLabel"] = "Orivra Candidate 300"
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
