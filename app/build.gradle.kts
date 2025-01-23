import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}
android {

    compileSdk = 34

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    defaultConfig {
        applicationId = "net.opendasharchive.openarchive"
        minSdk = 29
        targetSdk = 34
        versionCode = 30006
        versionName = "0.7.8"
        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    base {
        archivesName.set("save-${project.version}")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {

        getByName("release") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".release"
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }

        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    signingConfigs {
        getByName("debug") {
            val props = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { props.load(it) }
            }

            storeFile = file(props["storeFile"] as? String ?: "")
            storePassword = props["storePassword"] as? String ?: ""
            keyAlias = props["keyAlias"] as? String ?: ""
            keyPassword = props["keyPassword"] as? String ?: ""
        }
    }

    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/LICENSE.txt", "META-INF/NOTICE.txt", "META-INF/LICENSE",
                    "META-INF/NOTICE", "META-INF/DEPENDENCIES", "LICENSE.txt"
                )
            )
        }
    }

    lint {
        abortOnError = false
    }

    namespace = "net.opendasharchive.openarchive"

    configurations.all {
        resolutionStrategy {
            force("org.bouncycastle:bcprov-jdk15to18:1.72")
            exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        }
    }
}

dependencies {

    // Core Kotlin and Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")


    // AndroidX Libraries
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // Compose Preferences
    implementation("me.zhanghai.compose.preference:library:1.1.1")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // AndroidX SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")

    // Compose Libraries
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Dependency Injection
    implementation("io.insert-koin:koin-core:4.1.0-Beta5")
    implementation("io.insert-koin:koin-android:4.1.0-Beta5")
    implementation("io.insert-koin:koin-androidx-compose:4.1.0-Beta5")

    // Image Libraries
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.esafirm:android-image-picker:3.0.0")
    implementation("com.squareup.picasso:picasso:2.5.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")

    // Networking and Data
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.github.guardianproject:sardine-android:89f7eae512")

    // Utility Libraries
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("com.orhanobut:logger:2.2.0")
    implementation("com.github.abdularis:circularimageview:1.4")
    implementation("com.tbuonomo:dotsindicator:5.1.0")
    implementation("com.guolindev.permissionx:permissionx:1.6.4")

    // Barcode Scanning
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.2.0")

    // Security and Encryption
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.72")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
    api("org.bouncycastle:bcpg-jdk15to18:1.71")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.3.0")
//    implementation("com.google.android.play:core-ktx:1.8.1")
//    implementation("com.google.android.play:asset-delivery-ktx:2.3.0")
//    implementation("com.google.android.play:feature-delivery-ktx:2.1.0")
//    implementation("com.google.android.play:review-ktx:2.0.2")
//    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Google Drive API
    implementation("com.google.http-client:google-http-client-gson:1.42.3")
    implementation("com.google.api-client:google-api-client-android:1.26.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")

    // Tor Libraries
    implementation("info.guardianproject:tor-android:0.4.7.14")
    implementation("info.guardianproject:jtorctl:0.4.5.7")

    implementation("org.bitcoinj:bitcoinj-core:0.16.2")
    implementation("com.eclipsesource.j2v8:j2v8:6.2.1@aar")

    // ProofMode //from here: https://github.com/guardianproject/proofmode
    implementation("org.proofmode:android-libproofmode:1.0.26") {
        //transitive = false
        exclude(group = "org.bitcoinj")
        exclude(group = "com.google.protobuf")
        exclude(group = "org.slf4j")
        exclude(group = "net.jcip")
        exclude(group = "commons-cli")
        exclude(group = "org.json")
        exclude(group = "com.google.guava")
        exclude(group = "com.google.guava", module = "guava-jdk5")
        exclude(group = "com.google.code.findbugs", module = "annotations")
        exclude(group = "com.squareup.okio", module = "okio")
    }

    // Guava Conflicts
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")


    implementation("com.github.satyan:sugar:1.5")


    // adding web dav support: https://github.com/thegrizzlylabs/sardine-android'
    implementation("com.github.guardianproject:sardine-android:89f7eae512")


    implementation("com.github.derlio:audio-waveform:v1.0.1")


    implementation("org.cleaninsights.sdk:clean-insights-sdk:2.8.0")
    implementation("info.guardianproject.netcipher:netcipher:2.2.0-alpha")


    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.10.3")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    testImplementation("androidx.work:work-testing:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}

/**
testdroid {username '$bbusername'
password '$bbpassword'
deviceGroup 'gpdevices'
mode "FULL_RUN"
projectName "OASave"}**/

