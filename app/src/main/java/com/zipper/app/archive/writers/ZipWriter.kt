package com.zipper.app.archive.writers

import com.zipper.app.archive.ArchiveWriter
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File

object ZipWriter : ArchiveWriter {
    override fun create(sourceRoot: File, output: File, password: CharArray?, onProgress: (Int, Int) -> Unit) {
        val zipFile = if (password != null) ZipFile(output, password) else ZipFile(output)
        val parameters = ZipParameters()
        if (password != null) {
            parameters.isEncryptFiles = true
            parameters.encryptionMethod = EncryptionMethod.AES
            parameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        }

        // Progress is per top-level pick, not per file inside it — zip4j's addFolder is a single
        // blocking call with no per-file callback short of its separate thread+ProgressMonitor
        // API, which isn't worth the extra complexity for what's a short-lived operation anyway.
        val topLevel = sourceRoot.listFiles()?.toList().orEmpty()
        val total = topLevel.size
        topLevel.forEachIndexed { index, item ->
            if (item.isDirectory) {
                zipFile.addFolder(item, parameters)
            } else {
                zipFile.addFile(item, parameters)
            }
            onProgress(index + 1, total)
        }
    }
}
