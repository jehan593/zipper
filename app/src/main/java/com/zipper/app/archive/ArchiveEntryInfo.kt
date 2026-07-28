package com.zipper.app.archive

/**
 * [path] is always "/"-separated with no leading slash, regardless of the host OS the archive was
 * created on (both zip4j and commons-compress already normalize to this; junrar's FileHeader is
 * normalized to it explicitly in RarReader).
 */
data class ArchiveEntryInfo(
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)
