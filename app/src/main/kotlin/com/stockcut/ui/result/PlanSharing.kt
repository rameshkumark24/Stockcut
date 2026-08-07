package com.stockcut.ui.result

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Sharing a cut plan as an image (US-09: "share the plan on WhatsApp so my
 * apprentice can cut it").
 *
 * 🔴 NO STORAGE PERMISSION. The PNG is written into the app's own cache
 * directory and handed out as a content:// URI through FileProvider, with
 * FLAG_GRANT_READ_URI_PERMISSION giving the receiving app temporary read
 * access to that one file. CLAUDE.md rule 7 allows three permissions and none
 * of them is storage; if sharing ever seems to need one, this is the wrong
 * implementation.
 *
 * Sharing as an image is free at both tiers (docs/02 §6) — it is how the app
 * spreads.
 */
object PlanSharing {

    private const val DIR = "shared"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Writes [bitmap] to the cache and returns a share Intent.
     *
     * Old files are cleared first. The cache is not a document store: a
     * tradesman's plans accumulating there would be data we never told them we
     * kept, and Android may delete it at any time anyway.
     */
    fun shareIntent(context: Context, bitmap: Bitmap, jobName: String): Intent {
        val dir = File(context.cacheDir, DIR)
        dir.deleteRecursively()
        dir.mkdirs()

        val file = File(dir, "${safeName(jobName)}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file,
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, jobName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.let { Intent.createChooser(it, "Share cut plan") }
    }

    /**
     * A job name is free text — it can hold emoji, slashes, or a path traversal
     * attempt. Anything that is not a safe filename character is dropped rather
     * than escaped, and an empty result falls back to a fixed name.
     */
    private fun safeName(jobName: String): String {
        val cleaned = jobName.trim()
            .map { char -> if (char.isLetterOrDigit() || char == '_') char else '-' }
            .joinToString("")
            // Collapse runs, so "Example: gate frame" is not "Example--gate-frame".
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(40)
        return cleaned.ifBlank { "cut-plan" }
    }
}
