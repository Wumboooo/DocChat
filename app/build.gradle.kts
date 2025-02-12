import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}
android {
    namespace = "com.example.docchat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.docchat"
        minSdk = 25
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    val localProperties = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localProperties.load(FileInputStream(localFile))
    }

    defaultConfig {
        manifestPlaceholders["GEO_API_KEY"] = localProperties.getProperty("GEO_API_KEY")
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.base)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.5")

    // Untuk HTTP request ke Firebase Cloud Messaging (FCM)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Untuk memproses JSON
    implementation("org.json:json:20210307")

    // layout resources
    implementation("com.airbnb.android:lottie:6.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.0")

    // preferences datastore
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // room
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-common:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // oauth2
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // splashscreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0") // KTX version of Firebase Auth
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:23.1.0")
    implementation("com.google.firebase:firebase-firestore:25.1.1")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")

    //pdf
    implementation("com.github.mhiew:android-pdf-viewer:3.2.0-beta.3")

    //google gson
    implementation("com.google.code.gson:gson:2.10.1")

    //location
    implementation("com.google.android.libraries.places:places:4.1.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    //maps
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    //navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    //paging3
    implementation("androidx.paging:paging-runtime:3.3.5")
    implementation("androidx.paging:paging-runtime-ktx:3.3.5")

    implementation("com.google.dagger:hilt-android:2.49")
    kapt("com.google.dagger:hilt-android-compiler:2.49")
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // material
    implementation("com.google.android.material:material:1.12.0")

    // photo messaging
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    //notification messaging
    implementation("com.google.firebase:firebase-messaging-ktx:24.1.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.16.0")
    implementation("com.onesignal:OneSignal:4.8.6")

}