package com.zipper.app.archive.writers

import com.zipper.app.archive.ArchiveWriter
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/** Handles both plain TAR and TAR.GZ — [gzip] picks which. Neither has an encryption concept, so
 *  [password] is always null here (enforced by ArchiveFormat.supportsPassword up in the UI, not
 *  re-checked here). */
class TarWriter(private val gzip: Boolean) : ArchiveWriter {
    override fun create(sourceRoot: File, output: File, password: CharArray?, onProgress: (Int, Int) -> Unit) {
        val allEntries = sourceRoot.walkTopDown().filter { it != sourceRoot }.toList()
        val total = allEntries.count { it.isFile }
        var completed = 0

        val fileOut: OutputStream = BufferedOutputStream(FileOutputStream(output))
        TarArchiveOutputStream(if (gzip) GzipCompressorOutputStream(fileOut) else fileOut).use { tarOut ->
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            for (file in allEntries) {
                val relativePath = sourceRoot.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                val entryName = if (file.isDirectory) "$relativePath/" else relativePath
                val entry = TarArchiveEntry(file, entryName)
                tarOut.putArchiveEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { input -> input.copyTo(tarOut) }
                    completed++
                    onProgress(completed, total)
                }
                tarOut.closeArchiveEntry()
            }
        }
    }
}
