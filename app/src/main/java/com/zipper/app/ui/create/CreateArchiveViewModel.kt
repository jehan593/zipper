package com.zipper.app.ui.create

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zipper.app.archive.ArchiveFormat
import com.zipper.app.archive.ArchiveWriters
import com.zipper.app.archive.saf.PickedSource
import com.zipper.app.archive.saf.SafFileCollector
import com.zipper.app.archive.saf.newStagingRun
import com.zipper.app.ui.components.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class CreateArchiveUiState(
    val sources: List<PickedSource> = emptyList(),
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val password: String = "",
    val archiveName: String = "Archive",
    val status: OperationStatus = OperationStatus.Idle
) {
    val suggestedFileName: String get() = "$archiveName.${format.extension}"
}

/** No repository/DI here — every field is one-shot UI state for a single run of "pick sources,
 *  pick a destination, compress, done", nothing persists past leaving this screen. */
class CreateArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateArchiveUiState())
    val uiState: StateFlow<CreateArchiveUiState> = _uiState

    fun addFiles(context: Context, uris: List<Uri>) {
        val added = uris.mapNotNull { uri ->
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: return@mapNotNull null
            PickedSource(uri, name, isTree = false)
        }
        _uiState.update { it.copy(sources = it.sources + added) }
    }

    fun addFolder(context: Context, treeUri: Uri) {
        val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: return
        _uiState.update { it.copy(sources = it.sources + PickedSource(treeUri, name, isTree = true)) }
    }

    fun removeSource(source: PickedSource) {
        _uiState.update { it.copy(sources = it.sources - source) }
    }

    fun setFormat(format: ArchiveFormat) {
        _uiState.update { it.copy(format = format, password = if (format.supportsPassword) it.password else "") }
    }

    fun setPassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun setArchiveName(name: String) {
        _uiState.update { it.copy(archiveName = name) }
    }

    fun dismissResult() {
        _uiState.update { it.copy(status = OperationStatus.Idle) }
    }

    fun createArchive(context: Context, destination: Uri) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(status = OperationStatus.InProgress(0, state.sources.size)) }
            val runDir = newStagingRun(context)
            try {
                val stageDir = File(runDir, "source").apply { mkdirs() }
                SafFileCollector.collect(context, state.sources, stageDir) { copied ->
                    _uiState.update { it.copy(status = OperationStatus.InProgress(copied, state.sources.size)) }
                }

                val outputFile = File(runDir, "output.${state.format.extension}")
                val password = state.password
                    .takeIf { it.isNotEmpty() && state.format.supportsPassword }
                    ?.toCharArray()
                ArchiveWriters.forFormat(state.format).create(stageDir, outputFile, password) { done, total ->
                    _uiState.update { it.copy(status = OperationStatus.InProgress(done, total)) }
                }

                context.contentResolver.openOutputStream(destination)?.use { out ->
                    outputFile.inputStream().use { input -> input.copyTo(out) }
                } ?: error("Couldn't open the chosen destination")

                _uiState.update { it.copy(status = OperationStatus.Success) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = OperationStatus.Failure(e.message ?: "Couldn't create the archive")) }
            } finally {
                runDir.deleteRecursively()
            }
        }
    }
}
