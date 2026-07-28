package com.zipper.app.archive.writers

import com.zipper.app.archive.ArchiveWriter
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.File

object SevenZWriter : ArchiveWriter {
    override fun create(sourceRoot: File, output: File, password: CharArray?, onProgress: (Int, Int) -> Unit) {
        val allEntries = sourceRoot.walkTopDown().filter { it != sourceRoot }.toList()
        val total = allEntries.count { it.isFile }
        var completed = 0

        val sevenZOutput = if (password != null) SevenZOutputFile(output, password) else SevenZOutputFile(output)
        sevenZOutput.use { out ->
            for (file in allEntries) {
                val entryName = sourceRoot.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                val entry = out.createArchiveEntry(file, entryName) as SevenZArchiveEntry
                out.putArchiveEntry(entry)
                if (file.isFile) {
                    file.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                        }
                    }
                    completed++
                    onProgress(completed, total)
                }
                out.closeArchiveEntry()
            }
        }
    }
}
