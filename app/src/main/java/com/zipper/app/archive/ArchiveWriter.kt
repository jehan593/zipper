package com.zipper.app.archive

import java.io.File

interface ArchiveWriter {
    /** [sourceRoot]'s immediate children become the archive's top-level entries. */
    fun create(sourceRoot: File, output: File, password: CharArray?, onProgress: (done: Int, total: Int) -> Unit)
}
