package com.zipper.app.preview

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zipper.app.ui.preview.ArchivePreviewHost
import com.zipper.app.ui.theme.ZipperTheme

/**
 * Fires when an archive file is tapped elsewhere on the device (see the manifest's intent
 * filters). Transient and translucent, like Linker's LinkInterceptorActivity: every exit path
 * (Close, cancelling the password prompt, acknowledging a successful extraction) calls finish().
 */
class ArchivePreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val archiveUri: Uri? = intent?.data
        if (archiveUri == null) {
            finish()
            return
        }

        setContent {
            ZipperTheme {
                ArchivePreviewHost(uri = archiveUri, onDismiss = { finish() })
            }
        }
    }
}
