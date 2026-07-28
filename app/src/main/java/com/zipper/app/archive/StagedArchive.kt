package com.zipper.app.archive

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.zipper.app.archive.saf.newStagingRun
import java.io.File

/**
 * Shared by ExtractArchiveViewModel and ArchivePreviewViewModel: stages a picked/tapped archive
 * Uri into a real file once, detects its format, and lets the caller retry ArchiveReaders.open()
 * as the user supplies passwords without re-copying the staged file on every attempt.
 */
class StagedArchive private constructor(
    val archiveName: String,
    val format: ArchiveFormat,
    private val file: File,
    private val runDir: File
) {
    private var reader: ArchiveReader? = null

    fun tryOpen(password: CharArray?): ArchiveOpenResult {
        val result = ArchiveReaders.open(format, file, password)
        if (result is ArchiveOpenResult.Success) reader = result.reader
        return result
    }

    fun requireReader(): ArchiveReader = requireNotNull(reader) { "Archive isn't open yet" }

    fun dispose() {
        reader?.close()
        runDir.deleteRecursively()
    }

    companion object {
        /** Returns null if [uri]'s display name doesn't match any supported extension. */
        fun stage(context: Context, uri: Uri): StagedArchive? {
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: return null
            val format = ArchiveFormat.detect(name) ?: return null
            val dir = newStagingRun(context)
            val file = File(dir, "archive.${format.extension}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            return StagedArchive(name, format, file, dir)
        }
    }
}
