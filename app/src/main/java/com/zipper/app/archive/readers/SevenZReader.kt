package com.zipper.app.archive.readers

import com.zipper.app.archive.ArchiveEntryInfo
import com.zipper.app.archive.ArchiveOpenResult
import com.zipper.app.archive.ArchiveReader
import com.zipper.app.archive.saf.SafExtractionTarget
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.File
import java.io.IOException

class SevenZReader private constructor(private val file: File, private val password: CharArray?) : ArchiveReader {

    override fun listEntries(): List<ArchiveEntryInfo> =
        openFile().use { sevenZFile ->
            sevenZFile.entries.map {
                ArchiveEntryInfo(path = it.name.replace('\\', '/').trimEnd('/'), isDirectory = it.isDirectory, size = it.size)
            }
        }

    override fun extractAll(target: SafExtractionTarget, onProgress: (Int, Int) -> Unit) {
        openFile().use { sevenZFile ->
            val total = sevenZFile.entries.count()
            val buffer = ByteArray(64 * 1024)
            var index = 0
            var entry = sevenZFile.nextEntry
            while (entry != null) {
                val path = entry.name.replace('\\', '/').trimEnd('/')
                if (entry.isDirectory) {
                    target.ensureDirectory(path)
                } else {
                    target.openOutput(path).use { output ->
                        while (true) {
                            val read = sevenZFile.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                        }
                    }
                }
                index++
                onProgress(index, total)
                entry = sevenZFile.nextEntry
            }
        }
    }

    override fun close() = Unit

    private fun openFile(): SevenZFile {
        val builder = SevenZFile.builder().setFile(file)
        if (password != null) builder.setPassword(password)
        return builder.get()
    }

    companion object {
        fun open(file: File, password: CharArray?): ArchiveOpenResult {
            return try {
                val builder = SevenZFile.builder().setFile(file)
                if (password != null) builder.setPassword(password)
                builder.get().use { probe ->
                    probe.entries.toList() // forces full header parse

                    // Headers can parse fine even when only *content* is encrypted (7-Zip's
                    // "encrypt filenames" is a separate, optional checkbox) — probe-decode one
                    // entry's content so a missing/wrong password surfaces now instead of partway
                    // through extractAll().
                    var entry = probe.nextEntry
                    while (entry != null && entry.isDirectory) entry = probe.nextEntry
                    if (entry != null) probe.read(ByteArray(8192))
                }
                ArchiveOpenResult.Success(SevenZReader(file, password))
            } catch (e: PasswordRequiredException) {
                ArchiveOpenResult.PasswordRequired
            } catch (e: IOException) {
                if (password != null) ArchiveOpenResult.WrongPassword else ArchiveOpenResult.PasswordRequired
            }
        }
    }
}
