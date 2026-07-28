package com.zipper.app.archive

sealed class ArchiveNode {
    abstract val name: String
    abstract val path: String

    data class Folder(override val name: String, override val path: String) : ArchiveNode()
    data class FileEntry(override val name: String, override val path: String, val size: Long) : ArchiveNode()
}

/**
 * Builds the folder-drill-down listing for [currentPath] ("" for the archive root) from a flat
 * entry list. Many archives (most zips made by non-Android tools included) never emit an explicit
 * directory entry for every folder — only leaf files with slash-separated paths — so non-empty
 * folders are synthesized from path segments rather than relying on [ArchiveEntryInfo.isDirectory].
 * An *empty* folder has no descendant entry to synthesize it from, so its own explicit directory
 * entry (which every writer in this app does emit, even for empty folders) is what surfaces it —
 * dropping that case here previously made empty folders vanish from the popup entirely.
 */
fun childrenAt(entries: List<ArchiveEntryInfo>, currentPath: String): List<ArchiveNode> {
    val prefix = if (currentPath.isEmpty()) "" else "$currentPath/"
    val folders = LinkedHashMap<String, ArchiveNode.Folder>()
    val files = LinkedHashMap<String, ArchiveNode.FileEntry>()

    for (entry in entries) {
        if (!entry.path.startsWith(prefix)) continue
        val remainder = entry.path.removePrefix(prefix)
        if (remainder.isEmpty()) continue // the prefix's own directory entry, if one exists

        val slashIndex = remainder.indexOf('/')
        if (slashIndex >= 0) {
            val folderName = remainder.substring(0, slashIndex)
            val folderPath = prefix + folderName
            folders.getOrPut(folderName) { ArchiveNode.Folder(folderName, folderPath) }
        } else if (entry.isDirectory) {
            // An empty folder has no descendant entries to synthesize it from via the branch
            // above — its own explicit directory entry (present since the writers do add one for
            // every folder, even empty ones) is the only signal it exists at all.
            folders.getOrPut(remainder) { ArchiveNode.Folder(remainder, entry.path) }
        } else {
            files[remainder] = ArchiveNode.FileEntry(remainder, entry.path, entry.size)
        }
    }

    return folders.values.sortedBy { it.name.lowercase() } +
        files.values.sortedBy { it.name.lowercase() }
}
