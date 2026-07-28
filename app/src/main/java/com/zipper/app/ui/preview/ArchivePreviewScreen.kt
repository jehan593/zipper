package com.zipper.app.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zipper.app.R
import com.zipper.app.archive.ArchiveNode
import com.zipper.app.ui.components.OperationStatus
import com.zipper.app.ui.components.PasswordPromptDialog
import com.zipper.app.ui.components.ProgressDialog
import com.zipper.app.ui.components.ResultDialog
import kotlin.math.ln
import kotlin.math.pow

/**
 * A Dialog rather than plain full-screen content — this is reused two ways: as the entire content
 * of the translucent ArchivePreviewActivity (tapping an archive elsewhere on the device) and as an
 * in-app overlay on top of Home (the Extract Archive action). A Dialog gives both call sites the
 * same scrim/dismiss/back behavior for free instead of each needing its own.
 */
@Composable
fun ArchivePreviewScreen(
    viewModel: ArchivePreviewViewModel,
    onExtractRequested: () -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, usePlatformDefaultWidth = false)
    ) {
        // Back navigates up a folder first, only closing the popup once already at the root —
        // dismissOnBackPress is off above specifically so this can intercept it first.
        BackHandler { if (!viewModel.navigateUp()) onDismiss() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.currentPath.isNotEmpty()) {
                            IconButton(onClick = { viewModel.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up a folder")
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiState.archiveName ?: "Archive",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.currentPath.isNotEmpty()) {
                                Text(
                                    "/${uiState.currentPath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (uiState.children.isEmpty() && uiState.readyToExtract) {
                        Text(
                            "This folder is empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                            items(uiState.children, key = { it.path }) { node -> EntryRow(node, onOpenFolder = viewModel::openFolder) }
                        }
                    }

                    Button(
                        onClick = onExtractRequested,
                        enabled = uiState.readyToExtract,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text("Extract")
                    }
                }
            }
        }
    }

    if (uiState.passwordPromptVisible) {
        PasswordPromptDialog(
            isError = uiState.passwordError,
            onSubmit = viewModel::submitPassword,
            onDismiss = onDismiss
        )
    }

    val status = uiState.status
    when (status) {
        is OperationStatus.InProgress -> ProgressDialog(status, uiState.progressLabel)
        is OperationStatus.Success -> ResultDialog(status, "Archive extracted successfully.", onDismiss = onDismiss)
        is OperationStatus.Failure -> ResultDialog(
            status, "",
            // A failure before the archive was ever successfully opened (e.g. an unrecognized
            // file type) leaves nothing browsable behind it — OK should close the whole popup
            // rather than reveal an empty, dead card. A failure *during* extraction of an
            // already-open archive just clears the error so the user can retry.
            onDismiss = { if (uiState.readyToExtract) viewModel.dismissResult() else onDismiss() }
        )
        OperationStatus.Idle -> Unit
    }
}

@Composable
private fun EntryRow(node: ArchiveNode, onOpenFolder: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = node is ArchiveNode.Folder) {
                if (node is ArchiveNode.Folder) onOpenFolder(node.path)
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painterResource(if (node is ArchiveNode.Folder) R.drawable.ic_folder else R.drawable.ic_file),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(node.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (node is ArchiveNode.FileEntry) {
            Text(formatSize(node.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.size)
    val value = bytes / 1024.0.pow(exponent)
    return "%.1f %s".format(value, units[exponent - 1])
}
