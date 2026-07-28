package com.zipper.app

import android.app.Application
import com.zipper.app.archive.saf.stagingDir

/**
 * No DI container: unlike noter/linker there's no database or repository to own — every screen
 * is a self-contained one-shot operation (pick sources, pick a destination, run, done) with
 * nothing to share across them.
 */
class ZipperApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Create/extract stage files under a temp cache dir (see archive/saf/SafFileCollector.kt
        // and SafExtractionWriter.kt) since the archive libraries need real java.io.File access
        // that content:// Uris can't provide directly. A crash or force-stop mid-run can leave
        // that staging dir behind, so it's wiped once here on every cold start rather than trying
        // to clean it up from every possible exit path of every screen.
        stagingDir(this).deleteRecursively()
    }
}
