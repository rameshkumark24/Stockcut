plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.stockcut.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    // Room's exported schemas are the only record of what shipped to users.
    // They are committed to git; a migration cannot be written without them.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":units"))
    implementation(project(":optimizer"))

    implementation(libs.androidx.core.ktx)

    // `api`, not `implementation`: this module's public surface returns Room
    // types — StockCutDatabase extends RoomDatabase — so a consumer cannot call
    // database.projectDao() without Room on its compile classpath.
    //
    // This is a symptom worth naming rather than hiding. docs/07 W2 lists
    // "Repositories — Room ↔ domain mapping" as a deliverable and they were
    // never built, so :app currently reaches for DAOs directly. When the
    // repositories land, :data should expose only domain types and this can go
    // back to `implementation`.
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // JVM unit tests — pure logic only (entitlement rules, seed data).
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Instrumented tests — DAO behaviour and migrations. Need a device/emulator.
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
