plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

// Modules configure themselves. :optimizer and :units apply ONLY the Kotlin JVM
// plugin — never the Android plugin — which makes CLAUDE.md hard rule 1
// (no android.* imports in those modules) structurally impossible to violate.
