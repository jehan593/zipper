package com.zipper.app.archive.saf

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * One entry the user added on the Create Archive screen: [isTree] distinguishes a folder picked
 * via ACTION_OPEN_DOCUMENT_TREE (walked recursively, preserving its internal structure) from a
 * single file picked via ACTION_OPEN_DOCUMENT (copied as-is).
 */
data class PickedSource(
    val uri: Uri,
    val displayName: String,
    val isTree: Boolean
)

/**
 * Stages every picked file/folder under [destinationRoot] as plain java.io.File content, since
 * none of the archive-writing libraries can read directly from a content:// Uri. Each top-level
 * pick becomes its own top-level entry in destinationRoot — a picked folder is recreated with its
 * full subtree, a picked file is copied by itself — which is exactly what then gets fed into the
 * chosen ArchiveWriter as "the contents to archive".
 */
object SafFileCollector {

    fun collect(
        context: Context,
        sources: List<PickedSource>,
        destinationRoot: File,
        onFileCopied: (Int) -> Unit = {}
    ) {
        val resolver = context.contentResolver
        val usedNames = mutableSetOf<String>()
        var copiedCount = 0

        for (source in sources) {
            val name = uniqueName(usedNames, source.displayName)
            if (source.isTree) {
                val treeDoc = DocumentFile.fromTreeUri(context, source.uri) ?: continue
                val targetDir = File(destinationRoot, name)
                targetDir.mkdirs()
                copiedCount = copyTree(resolver, treeDoc, targetDir, onFileCopied, copiedCount)
            } else {
                copyDocument(resolver, source.uri, File(destinationRoot, name))
                copiedCount++
                onFileCopied(copiedCount)
            }
        }
    }

    private fun copyTree(
        resolver: ContentResolver,
        doc: DocumentFile,
        targetDir: File,
        onFileCopied: (Int) -> Unit,
        startCount: Int
    ): Int {
        var count = startCount
        for (child in doc.listFiles()) {
            val name = child.name ?: continue
            if (child.isDirectory) {
                val childDir = File(targetDir, name)
                childDir.mkdirs()
                count = copyTree(resolver, child, childDir, onFileCopied, count)
            } else {
                copyDocument(resolver, child.uri, File(targetDir, name))
                count++
                onFileCopied(count)
            }
        }
        return count
    }

    private fun copyDocument(resolver: ContentResolver, uri: Uri, targetFile: File) {
        resolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        }
    }

    /** Two picks with the same display name (e.g. two folders both named "Photos") can't share a
     *  top-level slot in destinationRoot, so the later one gets " (1)", " (2)", etc. appended. */
    private fun uniqueName(used: MutableSet<String>, name: String): String {
        if (used.add(name)) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var counter = 1
        var candidate: String
        do {
            candidate = "$base ($counter)$ext"
            counter++
        } while (!used.add(candidate))
        return candidate
    }
}
