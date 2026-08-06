plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.stockcut"
    compileSdk = 36

    defaultConfig {
        // 🔴 PLACEHOLDER — the package name is PERMANENT once published to Play
        // and has not been cleared yet (W0 gate, docs/00-phase-0 §10). Change it
        // HERE and nowhere else before the first upload. "CutList" collides with
        // several existing apps; docs/00-phase-0 §10 proposes
        // "StockCut — Cut List Optimizer" as title, which sets this.
        applicationId = "com.stockcut"

        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
