package com.stockcut

import android.content.Context
import com.stockcut.data.db.StockCutDatabase
import com.stockcut.data.settings.SettingsStore

/**
 * Manual dependency injection. The whole of it.
 *
 * TRD §2: "DI — Manual (a single AppContainer). Hilt is overhead for four
 * screens. Do not add it." CLAUDE.md repeats the ban. Four screens and three
 * collaborators do not need a graph, an annotation processor, or a second build
 * step; they need one object created once and passed down.
 *
 * Everything is `by lazy` so nothing touches the disk during Application.onCreate
 * — cold start has a 1.5 s budget on a low-end device (NFR-2), and opening Room
 * eagerly would spend part of it before the first frame.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: StockCutDatabase by lazy { StockCutDatabase.build(appContext) }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val projectDao by lazy { database.projectDao() }
    val stockDao by lazy { database.stockDao() }
    val partDao by lazy { database.partDao() }
    val stockProfileDao by lazy { database.stockProfileDao() }
}
