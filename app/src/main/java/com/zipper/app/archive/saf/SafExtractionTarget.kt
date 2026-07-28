package com.zipper.app.archive.saf

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.OutputStream

/**
 * Writes extracted archive entries under [root], creating the nested folder structure via
 * DocumentFile as needed — this is the only place in the app that translates an archive's internal
 * "/"-separated paths into real SAF documents.
 *
 * Takes a [DocumentFile] rather than a plain tree Uri: `DocumentFile.fromTreeUri()` always
 * resolves to a tree's *root* document, even when given the Uri of some document nested inside it
 * (it re-derives a document Uri from the tree ID component alone) — so a caller that wants entries
 * written into an already-created subfolder (the "Extract to '<archive name>'" action) has to pass
 * that subfolder's own DocumentFile directly, not round-trip it through a Uri.
 */
class SafExtractionTarget(private val context: Context, private val root: DocumentFile) {

    private val dirCache = HashMap<String, DocumentFile>()

    /** [entryPath] is "/"-separated as it appears in the archive, e.g. "photos/2020/img.jpg". */
    fun openOutput(entryPath: String): OutputStream {
        val segments = entryPath.split('/').filter { it.isNotEmpty() }
        require(segments.isNotEmpty()) { "Empty entry path" }
        val fileName = segments.last()
        val parent = if (segments.size > 1) directoryFor(segments.dropLast(1)) else root

        // Overwrite semantics: re-extracting the same archive to the same folder replaces files
        // rather than leaving a stale copy behind under a SAF-generated " (1)" name.
        parent.findFile(fileName)?.delete()
        val created = parent.createFile("application/octet-stream", fileName)
            ?: error("Couldn't create $fileName in destination")
        return context.contentResolver.openOutputStream(created.uri)
            ?: error("Couldn't open output stream for $fileName")
    }

    /** Called for explicit empty-directory entries so they still show up post-extraction even
     *  though no file write will ever touch them. */
    fun ensureDirectory(entryPath: String) {
        val segments = entryPath.split('/').filter { it.isNotEmpty() }
        if (segments.isNotEmpty()) directoryFor(segments)
    }

    private fun directoryFor(segments: List<String>): DocumentFile {
        val key = segments.joinToString("/")
        dirCache[key]?.let { return it }

        val parent = if (segments.size == 1) root else directoryFor(segments.dropLast(1))
        val name = segments.last()
        val dir = parent.findFile(name)?.takeIf { it.isDirectory }
            ?: parent.createDirectory(name)
            ?: error("Couldn't create directory $name")
        dirCache[key] = dir
        return dir
    }
}
