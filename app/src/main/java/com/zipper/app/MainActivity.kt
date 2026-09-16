package com.zipper.app

import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZipperTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
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
                            Screen.Create -> CreateArchiveScreen(onBack = { screen = Screen.Home })
                        }
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
