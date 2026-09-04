import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
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
val testPublisher = "ca-app-pub-3940256099942544"
val testAppId = "$testPublisher~3347511713"
val testBannerId = "$testPublisher/6300978111"
val testInterstitialId = "$testPublisher/1033173712"

/**
 * 🔴 Asking for production ads and not getting them must FAIL THE BUILD.
 *
 * This used to be `localProps.getProperty(key) ?: fallback`, which meant a
 * missing entry in local.properties silently produced a build carrying Google's
 * test IDs. That build compiles, signs, uploads and installs, and then earns
 * nothing — forever, from every user, with no error in Gradle, in Play, in
 * logcat or in AdMob. The only symptom is a revenue line that stays at zero,
 * and by the time it is noticed the release is already public.
 *
 * local.properties is git-ignored, so it is exactly the file that goes missing
 * on a new machine, a fresh clone, or a rebuild after a disk wipe — the moments
 * when a release is most likely to be cut in a hurry.
 *
 * Throwing here makes the failure loud and immediate. The manual AAB check in
 * docs/17 Step 7 is still worth running, but it is now a backstop rather than
 * the only thing standing between a typo and a year of unpaid impressions.
 */
fun adId(key: String, fallback: String): String {
    if (!useProductionAds) return fallback

    val id = localProps.getProperty(key)?.trim()
    if (id.isNullOrEmpty()) {
        throw GradleException(
            "Production ads were requested (-Pstockcut.productionAds=true) but " +
                "'$key' is missing from local.properties.\n" +
                "Refusing to fall back to the test ID: that build would look correct, " +
                "upload correctly, and earn nothing with no error anywhere.\n" +
                "Add the real ID from AdMob, or drop the flag to build with test ads.",
        )
    }
    if (id.startsWith(testPublisher)) {
        throw GradleException(
            "'$key' in local.properties is Google's TEST publisher ($testPublisher).\n" +
                "That serves test ads to real users and earns nothing. Copy the real " +
                "ad unit ID from AdMob instead.",
        )
    }
    return id
}

/**
 * Release signing.
 *
 * Reads keystore.properties, which is git-ignored and does not exist on a fresh
 * clone or in CI. When it is absent the release build still runs and produces an
 * UNSIGNED artifact rather than failing — CI has no business holding the upload
 * key, and a build that breaks without it would mean the only way to check R8
 * is on the one machine that can sign.
 *
 * 🔴 An unsigned AAB cannot be uploaded to Play. That is the intended failure
 * mode: better to notice at upload than to ship something signed by a debug key.
 */
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

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
        // 🔴 versionCode may only ever go UP, and Play remembers the highest one
        // it has EVER seen — including from a build you uploaded and discarded.
        // There is no way to reuse or lower a number. Bump it for every upload,
        // even a re-upload of the same code after a rejected review.
        versionCode = 6

        // 1.0.0 was the first release, published to closed testing on
        // 2026-08-13. Before that it was 0.1.0, which is honest for a
        // pre-release but wrong on a store listing: users read a leading 0 as
        // "unfinished", and a tradesman deciding whether to trust a measurement
        // tool is the last person to give the benefit of the doubt.
        //
        // MAJOR.MINOR.PATCH from here: PATCH for fixes, MINOR for features,
        // MAJOR for a change that alters how a saved job behaves.
        // 1.0.2 (code 3): the first build with the edge-to-edge and keyboard
        // fixes, plus the "Save trim" data-loss fix. PATCH, not MINOR — every
        // change in it repairs behaviour rather than adding any.
        //
        // 1.0.3 (code 4): THE FIRST PRODUCTION BUILD. Identical source to
        // 1.0.2 — the only difference is that it carries the real AdMob IDs
        // instead of Google's test IDs, which is a build flag, not a code
        // change.
        //
        // The version is bumped anyway rather than shipping a second artifact
        // also called 1.0.2. Two builds sharing a user-facing version but
        // behaving differently (test ads vs real ads) is exactly the ambiguity
        // that makes a support conversation useless a year from now.
        // 1.0.4 (code 5): first update after launch. No feature changes and
        // no behaviour changes for the user beyond one they will only notice
        // if they run dark mode — the launch window no longer flashes white.
        // The rest is build hygiene Play asked for: fragment forced off a 2019
        // release, and optimised resource shrinking.
        // 1.0.5 (code 6): drops the com.android.vending.BILLING permission
        // from the merged manifest, which is what made Play stamp "In-app
        // purchases" on the listing of an app that sells nothing. The billing
        // library and the whole dormant paywall stay in place — see the
        // manifest for why removing the permission is safe while
        // PAYWALL_ENABLED is false.
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔴 Clears app data between EVERY instrumented test.
        //
        // These tests drive the real app against the real database, so without
        // this they leak state into each other: one adds a 7000 mm part that no
        // stock can hold, and every later test that tries to optimize the same
        // job is blocked by the infeasible banner — correctly, which is what
        // makes the failure so confusing.
        //
        // Deleting the files by hand does NOT work: Room and DataStore hold
        // process-wide instances, and pulling the files out from under them
        // makes the next read hang. The orchestrator restarts the process per
        // test, so there is nothing live to corrupt.
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

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

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Keeps a debug build installable alongside a release one.
            applicationIdSuffix = ".debug"
        }
        // NOTE: the .debug suffix means the debug applicationId is
        // com.measure.stockcut.debug, which is NOT in google-services.json.
        // That is fine and deliberate — see the task-disabling block below.
        release {
            // R8 per TRD §13.1 — cheap and reasonable, not anti-piracy theatre.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // 🔴 NEVER falls back to the debug signing config. An app signed
            // with the debug key and uploaded once can never be updated with
            // the real key — Play binds the app to the certificate it first
            // saw. Unsigned is recoverable; wrongly signed is not.
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
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

    // 🔴 Force androidx.fragment forward. NOT a dependency this app wants.
    //
    // Nothing here asks for fragment — StockCut is single-Activity Compose and
    // does not use one. It arrives transitively from Google's own SDKs (ads,
    // billing, play-review), which still declare fragment 1.0.0/1.1.0, and
    // Gradle was resolving it to 1.1.0 — a 2019 release. Play flags it on the
    // production dashboard as "an outdated SDK version of
    // androidx.fragment:fragment".
    //
    // A constraint rather than an `implementation` line, because the point is
    // to raise the floor for a transitive nobody declared, not to take on a
    // dependency this app has no use for. If a future SDK stops pulling
    // fragment entirely, this quietly does nothing rather than adding it back.
    constraints {
        implementation(libs.androidx.fragment) {
            because("Play flags fragment 1.1.0 (2019), pulled in by Google's own SDKs")
        }
    }

    // Phase 6. Every one of these is on the allowed list in docs/02 §9.
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.play.review.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

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
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestUtil(libs.androidx.test.services)
    debugImplementation(libs.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}

/**
 * Firebase is a RELEASE-ONLY concern here.
 *
 * docs/02 §7 sets Crashlytics to Off in debug and On in release, so a debug
 * build has no reason to carry Firebase config. It also cannot: the .debug
 * applicationId suffix means debug is com.measure.stockcut.debug, which has no
 * client entry in google-services.json, and the plugin fails the build over it.
 *
 * The alternatives were worse. Registering a second Firebase app just to satisfy
 * a build step that should not run is busywork, and dropping the .debug suffix
 * would cost the ability to keep a debug and a release build side by side on one
 * device — which is exactly what you want while testing a purchase flow.
 *
 * So the Firebase steps are switched off for debug and left on for release,
 * which is what the spec asked for in the first place.
 */
tasks.matching {
    it.name == "processDebugGoogleServices" ||
        it.name.startsWith("uploadCrashlyticsMappingFileDebug") ||
        it.name.startsWith("injectCrashlyticsMappingFileIdDebug")
}.configureEach {
    enabled = false
}
