package com.zipper.app.archive.readers

import com.zipper.app.archive.ArchiveEntryInfo
import com.zipper.app.archive.ArchiveOpenResult
import com.zipper.app.archive.ArchiveReader
import com.zipper.app.archive.saf.SafExtractionTarget
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.FileHeader
import java.io.ByteArrayOutputStream
import java.io.File

class ZipReader private constructor(private val zipFile: ZipFile) : ArchiveReader {

    override fun listEntries(): List<ArchiveEntryInfo> =
        zipFile.fileHeaders.map { it.toEntryInfo() }

    override fun extractAll(target: SafExtractionTarget, onProgress: (Int, Int) -> Unit) {
        val headers = zipFile.fileHeaders
        val total = headers.size
        headers.forEachIndexed { index, header ->
            if (header.isDirectory) {
                target.ensureDirectory(header.fileName.trimEnd('/'))
            } else {
                zipFile.getInputStream(header).use { input ->
                    target.openOutput(header.fileName).use { output -> input.copyTo(output) }
                }
            }
            onProgress(index + 1, total)
        }
    }

    // zip4j opens its own RandomAccessFile per call rather than holding one open across calls, so
    // there's no handle here that needs releasing.
    override fun close() = Unit

    private fun FileHeader.toEntryInfo() = ArchiveEntryInfo(
        path = fileName.trimEnd('/'),
        isDirectory = isDirectory,
        size = uncompressedSize
    )

    companion object {
        fun open(file: File, password: CharArray?): ArchiveOpenResult {
            return try {
                val zipFile = if (password != null) ZipFile(file, password) else ZipFile(file)
                val headers = zipFile.fileHeaders // forces header parsing; throws if not a valid zip

                if (zipFile.isEncrypted && password == null) {
                    return ArchiveOpenResult.PasswordRequired
                }
                if (zipFile.isEncrypted) {
                    // zip4j doesn't validate the password until an entry is actually decompressed —
                    // probe-decode the smallest entry so a wrong password surfaces now instead of
                    // partway through extractAll().
                    val probe = headers.filterNot { it.isDirectory }.minByOrNull { it.uncompressedSize }
                    if (probe != null) {
                        try {
                            zipFile.getInputStream(probe).use { it.copyTo(ByteArrayOutputStream()) }
                        } catch (e: ZipException) {
                            return ArchiveOpenResult.WrongPassword
                        }
                    }
                }
                ArchiveOpenResult.Success(ZipReader(zipFile))
            } catch (e: ZipException) {
                ArchiveOpenResult.Error(e.message ?: "Couldn't open ZIP file")
            }
        }
    }
}
