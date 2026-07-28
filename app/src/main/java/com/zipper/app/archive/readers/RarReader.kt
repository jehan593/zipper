package com.zipper.app.archive.readers

import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import com.zipper.app.archive.ArchiveEntryInfo
import com.zipper.app.archive.ArchiveOpenResult
import com.zipper.app.archive.ArchiveReader
import com.zipper.app.archive.saf.SafExtractionTarget
import java.io.ByteArrayOutputStream
import java.io.File

/** Extraction only — see app/build.gradle.kts' comment on junrar for why RAR can never be a
 *  create option. */
class RarReader private constructor(private val archive: Archive) : ArchiveReader {

    override fun listEntries(): List<ArchiveEntryInfo> =
        archive.fileHeaders.map { it.toEntryInfo() }

    override fun extractAll(target: SafExtractionTarget, onProgress: (Int, Int) -> Unit) {
        val headers = archive.fileHeaders
        val total = headers.size
        headers.forEachIndexed { index, header ->
            val path = header.fileName.replace('\\', '/').trimEnd('/')
            if (header.isDirectory) {
                target.ensureDirectory(path)
            } else {
                target.openOutput(path).use { output -> archive.extractFile(header, output) }
            }
            onProgress(index + 1, total)
        }
    }

    override fun close() {
        archive.close()
    }

    private fun com.github.junrar.rarfile.FileHeader.toEntryInfo() = ArchiveEntryInfo(
        path = fileName.replace('\\', '/').trimEnd('/'),
        isDirectory = isDirectory,
        size = fullUnpackSize
    )

    companion object {
        fun open(file: File, password: CharArray?): ArchiveOpenResult {
            return try {
                val archive = if (password != null) Archive(file, String(password)) else Archive(file)

                if (archive.isPasswordProtected && password == null) {
                    archive.close()
                    return ArchiveOpenResult.PasswordRequired
                }
                if (archive.isPasswordProtected) {
                    // junrar doesn't validate the password until content is actually decoded —
                    // probe-extract the smallest file so a wrong password surfaces now instead of
                    // partway through extractAll().
                    val probeHeader = archive.fileHeaders.filterNot { it.isDirectory }
                        .minByOrNull { it.fullUnpackSize }
                    if (probeHeader != null) {
                        try {
                            archive.extractFile(probeHeader, ByteArrayOutputStream())
                        } catch (e: RarException) {
                            archive.close()
                            return ArchiveOpenResult.WrongPassword
                        }
                    }
                }
                ArchiveOpenResult.Success(RarReader(archive))
            } catch (e: RarException) {
                ArchiveOpenResult.Error(e.message ?: "Couldn't open RAR file")
            }
        }
    }
}
