import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * AdMob identifiers.
 *
 * 🔴 Real IDs are OPT-IN, never the default. Building with no flag — which is
 * what every debug build, every CI run and every closed-test build does — uses
 * Google's public test IDs. Production ads require passing
 * `-Pstockcut.productionAds=true` deliberately.
 *
 * The reason is CLAUDE.md rule 8: clicking a live ad in your own app terminates
 * the AdMob account permanently and forfeits earnings. A default that can be
 * reached by accident is how that happens, so there isn't one. docs/02 §7 also
 * puts test IDs in the closed-test build, not just debug.
 */
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val useProductionAds = (project.findProperty("stockcut.productionAds") as? String) == "true"

// Google's documented public test IDs. Safe to commit — they are the same for
// everyone and serve test ads only.
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/6300978111"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"

fun adId(key: String, fallback: String): String =
    if (useProductionAds) localProps.getProperty(key) ?: fallback else fallback

android {
    namespace = "com.stockcut"
    compileSdk = 36

    defaultConfig {
        // 🔴 PERMANENT once published to Play. Verified free on 2026-08-07
        // (the Play listing for it returns 404). Store title is
        // "StockCut — Cut List Optimizer".
        //
        // Note this differs from `namespace` above, which is the Kotlin package
        // and stays com.stockcut — the two are independent, and changing the
        // source package would be churn for no benefit.
        applicationId = "com.measure.stockcut"

        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The App ID goes in the manifest; the unit IDs go in BuildConfig.
        manifestPlaceholders["admobAppId"] = adId("admob.appId", testAppId)
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${adId("admob.bannerId", testBannerId)}\"")
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_ID",
            "\"${adId("admob.interstitialId", testInterstitialId)}\"",
        )
        buildConfigField("boolean", "PRODUCTION_ADS", useProductionAds.toString())
    }

    buildTypes {
        debug {
            // Keeps a debug build installable alongside a release one.
            applicationIdSuffix = ".debug"
        }
        release {
            // R8 per TRD §13.1 — cheap and reasonable, not anti-piracy theatre.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // For BuildConfig.DEBUG, which gates StrictMode.
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":optimizer"))
    implementation(project(":units"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // MeasurementField's logic lives in a plain state holder, so most of it is
    // tested here on the JVM in milliseconds rather than on a device.
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    androidTestImplementation(kotlin("test"))
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Pulled in transitively by compose-ui-test, but pinned explicitly: the
    // version that arrives on its own is too old for Android 16 and every UI
    // test fails with NoSuchMethodException on InputManager.getInstance.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
