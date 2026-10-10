package com.zipper.app.archive.readers

import com.zipper.app.archive.ArchiveOpenResult
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream

class ZipReaderTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun largeEncryptedEntryIsVerifiedWithBoundedMemory() {
        val file = temp.newFile("large.zip")
        file.delete()
        val parameters = ZipParameters().apply {
            fileNameInZip = "large.bin"
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
        }
        // The decompressed entry exceeds the test worker's 128 MiB heap.
        val source = object : InputStream() {
            var remaining = 300L * 1024 * 1024
            override fun read(): Int = if (remaining-- > 0) 0 else -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val count = minOf(remaining, len.toLong()).toInt()
                b.fill(0, off, off + count)
                remaining -= count
                return count
            }
        }
        ZipFile(file, "secret".toCharArray()).use { it.addStream(source, parameters) }
        assertEquals(ArchiveOpenResult.PasswordRequired, ZipReader.open(file, null))
        assertEquals(ArchiveOpenResult.WrongPassword, ZipReader.open(file, "wrong".toCharArray()))
        val result = ZipReader.open(file, "secret".toCharArray())
        assertTrue(result is ArchiveOpenResult.Success)
        (result as ArchiveOpenResult.Success).reader.use {
            assertEquals(300L * 1024 * 1024, it.listEntries().single().size)
        }
    }

    @Test fun invalidZipReportsAnError() {
        val file = temp.newFile("invalid.zip")
        file.writeText("not a zip")
        assertTrue(ZipReader.open(file, null) is ArchiveOpenResult.Error)
    }
}
