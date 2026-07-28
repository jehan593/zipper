package com.zipper.app.archive

/**
 * RAR can only ever be [canCreate] = false: there is no free/open-source library that writes the
 * proprietary RAR format (see app/build.gradle.kts' comment on junrar, which only reads it).
 */
enum class ArchiveFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val canCreate: Boolean,
    val supportsPassword: Boolean
) {
    ZIP("ZIP", "zip", "application/zip", canCreate = true, supportsPassword = true),
    SEVEN_Z("7-Zip", "7z", "application/x-7z-compressed", canCreate = true, supportsPassword = true),
    TAR("TAR", "tar", "application/x-tar", canCreate = true, supportsPassword = false),
    TAR_GZ("TAR.GZ", "tar.gz", "application/gzip", canCreate = true, supportsPassword = false),
    RAR("RAR", "rar", "application/x-rar-compressed", canCreate = false, supportsPassword = true);

    companion object {
        val creatable: List<ArchiveFormat> = entries.filter { it.canCreate }

        fun detect(fileName: String): ArchiveFormat? {
            // Some sources append a duplicate-file suffix *after* the extension (e.g.
            // "archive.7z (2)") rather than before it (Chrome/most download managers instead
            // produce "archive (2).7z", which already matches fine without this) — stripped here
            // so a plain endsWith(".7z") check below isn't fooled into reporting no extension at
            // all just because of a copy/duplicate marker.
            val lower = fileName.lowercase().replace(Regex("""\s\(\d+\)$"""), "")
            return when {
                lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> TAR_GZ
                lower.endsWith(".tar") -> TAR
                lower.endsWith(".zip") -> ZIP
                lower.endsWith(".7z") -> SEVEN_Z
                lower.endsWith(".rar") -> RAR
                else -> null
            }
        }
    }
}
