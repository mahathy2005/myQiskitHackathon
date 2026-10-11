plugins {
    alias(libs.plugins.android.application)

    // Firebase Google Services Plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.sheshield.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sheshield.app"
        minSdk = 26
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Enable code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enable automatic unused resource removal
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")
    implementation("org.mapsforge:mapsforge-map-android:0.21.0")
    implementation("org.mapsforge:mapsforge-map:0.21.0")
    implementation("org.mapsforge:mapsforge-themes:0.21.0")
    implementation("com.graphhopper:graphhopper-core:1.0-pre33.4")

    // =========================================================
    // ANDROIDX
    // =========================================================

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.preference)


    // =========================================================
    // GOOGLE LOCATION SERVICES
    // =========================================================

    implementation(libs.play.services.location)


    // =========================================================
    // GOOGLE PLACES SDK
    // =========================================================

    implementation(
        "com.google.android.libraries.places:places:5.0.0"
    )


    // =========================================================
    // VOSK OFFLINE SPEECH RECOGNITION
    // =========================================================

    implementation(
        "com.alphacephei:vosk-android:0.3.47"
    )


    // =========================================================
    // FIREBASE BOM
    // =========================================================

    implementation(
        platform(
            "com.google.firebase:firebase-bom:32.8.0"
        )
    )


    // =========================================================
    // FIREBASE AUTHENTICATION & FIRESTORE
    // =========================================================

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")


    // =========================================================
    // GOOGLE ML KIT - ON DEVICE TRANSLATION
    // =========================================================

    implementation(
        "com.google.mlkit:translate:17.0.2"
    )


    // =========================================================
    // UNIT & ANDROID TESTING
    // =========================================================

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
