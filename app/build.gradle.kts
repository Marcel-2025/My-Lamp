import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ---------------------------------------------------------------------------------------------
// Zentrale Monetarisierungs-Konfiguration
// Werte werden aus local.properties (nicht in Git) oder gradle.properties gelesen.
//   mylamp.admobBannerId   = Produktive AdMob-Banner-ID (ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy)
//   mylamp.playLicenseKey  = Base64-RSA-Public-Key aus der Play Console (optional, für Signaturprüfung)
// Fehlt die Banner-ID, fordert der Release-Build KEINE Anzeigen an.
// ---------------------------------------------------------------------------------------------
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun configValue(key: String): String =
    (localProps.getProperty(key) ?: providers.gradleProperty(key).orNull ?: "").trim()

val admobAppId = "ca-app-pub-6130152704553606~5683863711"
val admobTestBannerId = "ca-app-pub-3940256099942544/9214589741" // Offizielle Google-Test-ID (adaptiver Banner)
val admobReleaseBannerId = configValue("mylamp.admobBannerId")
val playLicenseKey = configValue("mylamp.playLicenseKey")

android {
    namespace = "dev.flondy.mylamp"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.flondy.mylamp"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "PLAY_LICENSE_KEY", "\"$playLicenseKey\"")
        buildConfigField("String", "PLAY_STORE_URL", "\"https://play.google.com/store/apps/details?id=dev.flondy.mylamp\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"flondy.dev@gmail.com\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${configValue("mylamp.privacyPolicyUrl")}\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobTestBannerId\"")
        }
        release {
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobReleaseBannerId\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    bundle {
        language {
            // Alle Sprachen ins Gerät ausliefern, damit die Sprachwahl in der App sofort funktioniert
            enableSplit = false
        }
    }
    androidResources {
        // Erzeugt locales_config.xml → App-Sprache auch in den Android-Systemeinstellungen wählbar (Android 13+)
        generateLocaleConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    // Google Mobile Ads (AdMob) + User Messaging Platform (Consent EWR/UK)
    implementation(libs.play.services.ads)
    implementation(libs.ump)

    // Google Play Billing Library
    implementation(libs.billing.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
