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

    // `implementation`, not `api`: Room is an implementation detail of this
    // module. Consumers talk to ProjectRepository / CutListRepository and get
    // domain types back, so nothing outside :data needs Room on its classpath.
    // If this ever has to become `api` again, something has started leaking
    // entities and the repository layer is being bypassed.
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
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
