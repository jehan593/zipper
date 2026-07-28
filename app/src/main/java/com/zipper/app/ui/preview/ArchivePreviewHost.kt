package com.zipper.app.ui.preview

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Wires up an ArchivePreviewViewModel + its destination-tree launcher around
 * ArchivePreviewScreen — shared by both places that show the popup: ArchivePreviewActivity (an
 * archive tapped elsewhere on the device) and MainActivity's Extract Archive action (pick a file,
 * land in the same popup instead of a separate screen).
 */
@Composable
fun ArchivePreviewHost(uri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ArchivePreviewViewModel = viewModel(key = uri.toString())

    val destinationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { destination ->
        if (destination != null) viewModel.extractTo(context, destination)
    }

    LaunchedEffect(uri) { viewModel.open(context, uri) }

    ArchivePreviewScreen(
        viewModel = viewModel,
        onExtractRequested = { destinationLauncher.launch(null) },
        onDismiss = onDismiss
    )
}
