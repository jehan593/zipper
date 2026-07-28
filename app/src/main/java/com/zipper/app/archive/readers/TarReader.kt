package com.zipper.app.archive.readers

import com.zipper.app.archive.ArchiveEntryInfo
import com.zipper.app.archive.ArchiveOpenResult
import com.zipper.app.archive.ArchiveReader
import com.zipper.app.archive.saf.SafExtractionTarget
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

private fun openTarStream(file: File, gzip: Boolean): TarArchiveInputStream {
    val fileIn = BufferedInputStream(FileInputStream(file))
    return TarArchiveInputStream(if (gzip) GzipCompressorInputStream(fileIn) else fileIn)
}

/** Handles both plain TAR and TAR.GZ — [gzip] picks which. Neither has an encryption concept, so
 *  unlike the other three formats this never takes or checks a password. */
class TarReader private constructor(private val file: File, private val gzip: Boolean) : ArchiveReader {

    override fun listEntries(): List<ArchiveEntryInfo> {
        val entries = mutableListOf<ArchiveEntryInfo>()
        openTarStream(file, gzip).use { tarIn ->
            var entry = tarIn.nextEntry
            while (entry != null) {
                entries += ArchiveEntryInfo(path = entry.name.trimEnd('/'), isDirectory = entry.isDirectory, size = entry.size)
                entry = tarIn.nextEntry
            }
        }
        return entries
    }

    override fun extractAll(target: SafExtractionTarget, onProgress: (Int, Int) -> Unit) {
        val total = listEntries().size
        openTarStream(file, gzip).use { tarIn ->
            var index = 0
            var entry = tarIn.nextEntry
            while (entry != null) {
                val path = entry.name.trimEnd('/')
                if (entry.isDirectory) {
                    target.ensureDirectory(path)
                } else {
                    target.openOutput(path).use { output -> tarIn.copyTo(output) }
                }
                index++
                onProgress(index, total)
                entry = tarIn.nextEntry
            }
        }
    }

    override fun close() = Unit

    companion object {
        fun open(file: File, gzip: Boolean): ArchiveOpenResult {
            return try {
                openTarStream(file, gzip).use { it.nextEntry }
                ArchiveOpenResult.Success(TarReader(file, gzip))
            } catch (e: IOException) {
                ArchiveOpenResult.Error(e.message ?: "Couldn't open ${if (gzip) "TAR.GZ" else "TAR"} file")
            }
        }
    }
}
