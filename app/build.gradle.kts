plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}



android {
    namespace = "com.example.vouchersharingapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.vouchersharingapplication"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }


    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module", //Bu satır özellikle Kotlin ile ilgili çakışmaları gidermek için eklendi
                "META-INF/gradle/incremental.annotation.processors" // Çakışan dosyayı hariç tutuyoruz
            )
        }
}


dependencies {
    implementation("com.google.dagger:hilt-android:2.50")
    implementation("com.google.dagger:hilt-android-compiler:2.50")
    implementation("io.coil-kt:coil:2.6.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.okhttp)
    implementation(libs.picasso)
    implementation(libs.firebase.analytics)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.android.gms:play-services-auth:20.0.0")
    implementation("com.google.android.gms:play-services-base:18.0.1")
    implementation("com.google.android.gms:play-services-vision:20.1.3")
    implementation("com.google.android.gms:play-services-vision-common:19.1.3")
    implementation("com.google.android.gms:play-services-clearcut:17.0.0")
    implementation(libs.play.services.base)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.vision.common)
    implementation(libs.htmlunit)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
   }
}
