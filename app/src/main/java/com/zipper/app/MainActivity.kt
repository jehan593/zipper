package com.zipper.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zipper.app.ui.create.CreateArchiveScreen
import com.zipper.app.ui.home.HomeScreen
import com.zipper.app.ui.preview.ArchivePreviewHost
import com.zipper.app.ui.theme.ZipperTheme

/** No navigation library: two flat screens and nothing to deep-link into, so a plain sealed
 *  in-memory state (reset on process death, same as a fresh launch) is simpler than wiring one up. */
private sealed class Screen {
    data object Home : Screen()
    data object Create : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZipperTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                // Extract Archive has no screen of its own: picking a file goes straight into the
                // same folder-drill-down popup used when an archive is tapped elsewhere on the
                // device (ArchivePreviewActivity) — one look-inside-and-extract UI instead of two.
                var previewUri by remember { mutableStateOf<Uri?>(null) }

                val pickArchiveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) previewUri = uri
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            onCreateArchive = { screen = Screen.Create },
                            onExtractArchive = { pickArchiveLauncher.launch(arrayOf("*/*")) }
                        )
                        Screen.Create -> CreateArchiveScreen(onBack = { screen = Screen.Home })
                    }
                }

                val uri = previewUri
                if (uri != null) {
                    ArchivePreviewHost(uri = uri, onDismiss = { previewUri = null })
                }
            }
        }
    }
}
