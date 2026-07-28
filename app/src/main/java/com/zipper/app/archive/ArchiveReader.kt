package com.zipper.app.archive

import com.zipper.app.archive.saf.SafExtractionTarget
import java.io.Closeable

interface ArchiveReader : Closeable {
    fun listEntries(): List<ArchiveEntryInfo>
    fun extractAll(target: SafExtractionTarget, onProgress: (done: Int, total: Int) -> Unit)
}

/**
 * [PasswordRequired] and [WrongPassword] are surfaced separately from [Error] so the UI can react
 * to them with a password prompt instead of a plain failure message.
 */
sealed class ArchiveOpenResult {
    data class Success(val reader: ArchiveReader) : ArchiveOpenResult()
    data object PasswordRequired : ArchiveOpenResult()
    data object WrongPassword : ArchiveOpenResult()
    data class Error(val message: String) : ArchiveOpenResult()
}
