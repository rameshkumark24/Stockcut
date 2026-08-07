package com.stockcut.ui.about

/**
 * Open-source attribution.
 *
 * Every third-party library in this app ships under a licence that requires
 * attribution — Apache-2.0, MIT and BSD all do. `docs/14` audited all 199
 * artifacts and found no copyleft, so a list of notices satisfies the whole
 * obligation.
 *
 * Written out by hand rather than pulled from a generator library. The generator
 * would be another dependency, in an app whose dependency policy needs a written
 * reason for each one, to render a list that changes about twice a year.
 *
 * 🔴 Add an entry here whenever a dependency is added. `docs/14` has the command
 * that lists the full graph.
 */
data class Licence(
    val name: String,
    val copyright: String,
    val licence: String,
)

val LICENCES: List<Licence> = listOf(
    Licence(
        name = "AndroidX — Core, Compose, Room, DataStore, Navigation, Lifecycle, Activity",
        copyright = "Copyright The Android Open Source Project",
        licence = "Apache License 2.0",
    ),
    Licence(
        name = "Kotlin standard library and kotlinx.coroutines",
        copyright = "Copyright JetBrains s.r.o. and Kotlin contributors",
        licence = "Apache License 2.0",
    ),
    Licence(
        name = "Google Play Billing Library",
        copyright = "Copyright Google LLC",
        licence = "Android Software Development Kit License",
    ),
    Licence(
        name = "Google Mobile Ads SDK (AdMob) and User Messaging Platform",
        copyright = "Copyright Google LLC",
        licence = "Android Software Development Kit License",
    ),
    Licence(
        name = "Google Play In-App Review",
        copyright = "Copyright Google LLC",
        licence = "Android Software Development Kit License",
    ),
    Licence(
        name = "Firebase Crashlytics",
        copyright = "Copyright Google LLC",
        licence = "Apache License 2.0",
    ),
    Licence(
        name = "Guava, Gson, Error Prone annotations, j2objc annotations",
        copyright = "Copyright Google LLC",
        licence = "Apache License 2.0",
    ),
    Licence(
        name = "Protocol Buffers",
        copyright = "Copyright Google LLC",
        licence = "BSD 3-Clause License",
    ),
    Licence(
        name = "Okio",
        copyright = "Copyright Square, Inc.",
        licence = "Apache License 2.0",
    ),
    Licence(
        name = "Checker Framework annotations",
        copyright = "Copyright the Checker Framework developers",
        licence = "MIT License",
    ),
    Licence(
        name = "JSR 305 annotations",
        copyright = "Copyright FindBugs project",
        licence = "BSD 3-Clause License",
    ),
)

/** The notice Apache-2.0 §4 asks to be reproduced. Kept short and accurate. */
const val APACHE_NOTICE: String =
    "Licensed under the Apache License, Version 2.0. You may obtain a copy at " +
        "http://www.apache.org/licenses/LICENSE-2.0 — distributed on an \"AS IS\" " +
        "BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND."
