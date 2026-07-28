package com.zipper.app.archive

import com.zipper.app.archive.readers.RarReader
import com.zipper.app.archive.readers.SevenZReader
import com.zipper.app.archive.readers.TarReader
import com.zipper.app.archive.readers.ZipReader
import com.zipper.app.archive.writers.SevenZWriter
import com.zipper.app.archive.writers.TarWriter
import com.zipper.app.archive.writers.ZipWriter
import java.io.File

object ArchiveReaders {
    fun open(format: ArchiveFormat, file: File, password: CharArray?): ArchiveOpenResult = when (format) {
        ArchiveFormat.ZIP -> ZipReader.open(file, password)
        ArchiveFormat.SEVEN_Z -> SevenZReader.open(file, password)
        ArchiveFormat.TAR -> TarReader.open(file, gzip = false)
        ArchiveFormat.TAR_GZ -> TarReader.open(file, gzip = true)
        ArchiveFormat.RAR -> RarReader.open(file, password)
    }
}

object ArchiveWriters {
    fun forFormat(format: ArchiveFormat): ArchiveWriter = when (format) {
        ArchiveFormat.ZIP -> ZipWriter
        ArchiveFormat.SEVEN_Z -> SevenZWriter
        ArchiveFormat.TAR -> TarWriter(gzip = false)
        ArchiveFormat.TAR_GZ -> TarWriter(gzip = true)
        ArchiveFormat.RAR -> error("RAR creation isn't supported — no free library can write it")
    }
}
