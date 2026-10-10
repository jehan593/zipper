package com.zipper.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zipper.app.archive.ArchiveFormat
import com.zipper.app.ui.components.OperationStatus
import com.zipper.app.ui.components.ResultDialog
import com.zipper.app.ui.create.CreateArchiveScreen
import com.zipper.app.ui.home.HomeScreen
import com.zipper.app.ui.preview.ArchivePreviewHost
import com.zipper.app.ui.theme.ZipperTheme

/** Two screens share this activity; a fresh launch starts at Home. */
private sealed class Screen {
    data object Home : Screen()
    data object Create : Screen()
}

class MainActivity : ComponentActivity() {
    private fun savedArchivePath(uri: Uri): String {
        if (uri.scheme == "file") return uri.path ?: uri.toString()
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        if (uri.authority == "com.android.externalstorage.documents" && documentId != null) {
            val volume = documentId.substringBefore(':')
            val relativePath = documentId.substringAfter(':', "")
            val storage = if (volume == "primary") "Internal storage" else "Storage ($volume)"
            return "$storage/$relativePath"
        }
        if (uri.authority == "com.android.providers.downloads.documents" &&
            documentId?.startsWith("raw:") == true) {
            return documentId.removePrefix("raw:")
        }
        // Some storage providers expose only a URI.
        return uri.toString()
    }

    private fun shareArchive(uri: Uri, name: String) {
        val mime = ArchiveFormat.detect(name)?.mimeType ?: "application/octet-stream"
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(send, "Share archive"))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No app available to share this archive", Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Couldn't access the archive to share it", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZipperTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                var showCreationSuccess by rememberSaveable { mutableStateOf(false) }
                var createdArchiveUri by rememberSaveable { mutableStateOf<String?>(null) }
                var createdArchiveName by rememberSaveable { mutableStateOf("") }
                // Reuse the preview popup for archives opened here or from another app.
                var previewUri by remember { mutableStateOf<Uri?>(null) }

                BackHandler(enabled = screen == Screen.Create && previewUri == null) {
                    screen = Screen.Home
                }

                val pickArchiveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) previewUri = uri
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = screen,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val direction = if (targetState == Screen.Create) 1 else -1
                            (slideInHorizontally(tween(300)) { direction * it / 4 } +
                                fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { -direction * it / 4 } +
                                    fadeOut(tween(200)))
                        },
                        label = "Archive navigation"
                    ) { currentScreen ->
                        when (currentScreen) {
                            Screen.Home -> HomeScreen(
                                onCreateArchive = { screen = Screen.Create },
                                onExtractArchive = { pickArchiveLauncher.launch(arrayOf("*/*")) }
                            )
                            Screen.Create -> CreateArchiveScreen(
                                onBack = { screen = Screen.Home },
                                onCreated = { uri, name ->
                                    createdArchiveUri = uri.toString()
                                    createdArchiveName = name
                                    screen = Screen.Home
                                    showCreationSuccess = true
                                }
                            )
                        }
                    }
                }

                if (showCreationSuccess && previewUri == null) {
                    ResultDialog(
                        status = OperationStatus.Success,
                        successMessage = "Archive created.",
                        savedPath = createdArchiveUri?.let { savedArchivePath(Uri.parse(it)) },
                        onDismiss = { showCreationSuccess = false },
                        onOpen = createdArchiveUri?.let { savedUri ->
                            {
                                previewUri = Uri.parse(savedUri)
                            }
                        },
                        onShare = createdArchiveUri?.let { savedUri ->
                            { shareArchive(Uri.parse(savedUri), createdArchiveName) }
                        }
                    )
                }

                val uri = previewUri
                if (uri != null) {
                    ArchivePreviewHost(uri = uri, onDismiss = { previewUri = null })
                }
            }
        }
    }
}
