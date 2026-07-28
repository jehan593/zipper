package com.zipper.app.ui.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zipper.app.R
import com.zipper.app.archive.ArchiveFormat
import com.zipper.app.archive.saf.PickedSource
import com.zipper.app.ui.components.OperationStatus
import com.zipper.app.ui.components.PasswordField
import com.zipper.app.ui.components.ProgressDialog
import com.zipper.app.ui.components.ResultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateArchiveScreen(onBack: () -> Unit, viewModel: CreateArchiveViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val addFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.addFiles(context, uris)
    }
    val addFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.addFolder(context, uri)
    }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destination ->
        if (destination != null) viewModel.createArchive(context, destination)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Archive") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // A single LazyColumn for the whole form (not a Column with a separately-scrolling
        // LazyColumn nested inside it for just the source list) plus imePadding(): targeting
        // SDK 35 means the system no longer physically shrinks the window for the keyboard the
        // way windowSoftInputMode="adjustResize" used to (edge-to-edge is enforced by default at
        // that target level) — Compose has to reserve the keyboard's own space itself, and having
        // every field share one scrollable list is what guarantees a focused field can always be
        // scrolled back into view above the keyboard, regardless of screen size.
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { addFilesLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) {
                        Icon(painterResource(R.drawable.ic_note_add), contentDescription = null)
                        Text(" Add files")
                    }
                    OutlinedButton(onClick = { addFolderLauncher.launch(null) }, modifier = Modifier.weight(1f)) {
                        Icon(painterResource(R.drawable.ic_create_new_folder), contentDescription = null)
                        Text(" Add folder")
                    }
                }
            }

            if (uiState.sources.isEmpty()) {
                item {
                    Text(
                        "Nothing selected yet — add files or a folder to archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.sources, key = { it.uri.toString() }) { source ->
                    SourceRow(source = source, onRemove = { viewModel.removeSource(source) })
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.archiveName,
                    onValueChange = viewModel::setArchiveName,
                    label = { Text("Archive name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                var formatMenuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = formatMenuExpanded,
                    onExpandedChange = { formatMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.format.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = formatMenuExpanded,
                        onDismissRequest = { formatMenuExpanded = false }
                    ) {
                        ArchiveFormat.creatable.forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.label) },
                                onClick = {
                                    viewModel.setFormat(format)
                                    formatMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                PasswordField(
                    password = uiState.password,
                    onPasswordChange = viewModel::setPassword,
                    enabled = uiState.format.supportsPassword,
                    modifier = Modifier.fillMaxWidth(),
                    label = if (uiState.format.supportsPassword) "Password (optional)" else "Password (not supported for ${uiState.format.label})"
                )
            }

            item {
                Button(
                    onClick = { createDocumentLauncher.launch(uiState.suggestedFileName) },
                    enabled = uiState.sources.isNotEmpty() && uiState.archiveName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Archive")
                }
            }
        }
    }

    val status = uiState.status
    when (status) {
        is OperationStatus.InProgress -> ProgressDialog(status, "Creating archive")
        is OperationStatus.Success, is OperationStatus.Failure -> ResultDialog(
            status = status,
            successMessage = "Archive created successfully.",
            onDismiss = viewModel::dismissResult
        )
        OperationStatus.Idle -> Unit
    }
}

@Composable
private fun SourceRow(source: PickedSource, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painterResource(if (source.isTree) R.drawable.ic_folder else R.drawable.ic_file),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(source.displayName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove")
            }
        }
    }
}
