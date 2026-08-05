rootProject.name = "stockcut"

// Pure JVM modules. No Android SDK required to build or test these.
// :app and :data are added in Phase 4/5 and will bring the Android plugin with them.
include(":units")
include(":optimizer")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
    }
}
