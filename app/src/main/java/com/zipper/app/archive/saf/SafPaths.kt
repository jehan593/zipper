package com.zipper.app.archive.saf

import android.content.Context
import java.io.File

/**
 * Every archive library here (zip4j, commons-compress, junrar) needs real java.io.File access —
 * none of them can read/write directly against a content:// Uri — so this is where picked sources,
 * the archive being read, and the archive being built all get staged as plain files before being
 * handed to those libraries. Wiped on every cold start by ZipperApplication, and by each caller
 * when its own operation finishes (success or failure).
 */
fun stagingDir(context: Context): File = File(context.cacheDir, "zipper_staging")

fun newStagingRun(context: Context): File {
    val dir = File(stagingDir(context), System.currentTimeMillis().toString())
    dir.mkdirs()
    return dir
}
