package com.zipper.app.ui.preview

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zipper.app.archive.ArchiveEntryInfo
import com.zipper.app.archive.ArchiveFormat
import com.zipper.app.archive.ArchiveNode
import com.zipper.app.archive.ArchiveOpenResult
import com.zipper.app.archive.StagedArchive
import com.zipper.app.archive.childrenAt
import com.zipper.app.archive.saf.SafExtractionTarget
import com.zipper.app.ui.components.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ArchivePreviewUiState(
    val archiveName: String? = null,
    val format: ArchiveFormat? = null,
    val entries: List<ArchiveEntryInfo> = emptyList(),
    val currentPath: String = "",
    val readyToExtract: Boolean = false,
    val passwordPromptVisible: Boolean = false,
    val passwordError: Boolean = false,
    val progressLabel: String = "",
    val status: OperationStatus = OperationStatus.Idle
) {
    val children: List<ArchiveNode> get() = childrenAt(entries, currentPath)
}

class ArchivePreviewViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArchivePreviewUiState())
    val uiState: StateFlow<ArchivePreviewUiState> = _uiState

    private var staged: StagedArchive? = null

    fun open(context: Context, uri: Uri) {
        cleanup()
        val archive = StagedArchive.stage(context, uri)
        if (archive == null) {
            _uiState.value = ArchivePreviewUiState(status = OperationStatus.Failure("Unrecognized archive type"))
            return
        }
        staged = archive
        _uiState.value = ArchivePreviewUiState(
            archiveName = archive.archiveName,
            format = archive.format,
            progressLabel = "Reading archive",
            status = OperationStatus.InProgress(0, 0)
        )
        viewModelScope.launch(Dispatchers.IO) { applyOpenResult(archive.tryOpen(null)) }
    }

    fun submitPassword(password: String) {
        val archive = staged ?: return
        _uiState.update { it.copy(progressLabel = "Checking password", status = OperationStatus.InProgress(0, 0)) }
        viewModelScope.launch(Dispatchers.IO) { applyOpenResult(archive.tryOpen(password.toCharArray())) }
    }

    private fun applyOpenResult(result: ArchiveOpenResult) {
        when (result) {
            is ArchiveOpenResult.Success -> {
                val entries = result.reader.listEntries()
                _uiState.update {
                    it.copy(
                        entries = entries,
                        readyToExtract = true,
                        passwordPromptVisible = false,
                        passwordError = false,
                        status = OperationStatus.Idle
                    )
                }
            }
            ArchiveOpenResult.PasswordRequired -> _uiState.update {
                it.copy(passwordPromptVisible = true, passwordError = false, status = OperationStatus.Idle)
            }
            ArchiveOpenResult.WrongPassword -> _uiState.update {
                it.copy(passwordPromptVisible = true, passwordError = true, status = OperationStatus.Idle)
            }
            is ArchiveOpenResult.Error -> _uiState.update {
                it.copy(status = OperationStatus.Failure(result.message))
            }
        }
    }

    fun openFolder(path: String) {
        _uiState.update { it.copy(currentPath = path) }
    }

    /** Returns false when already at the root, so the caller (the popup's back button / system
     *  back gesture) knows to close the whole popup instead. */
    fun navigateUp(): Boolean {
        val current = _uiState.value.currentPath
        if (current.isEmpty()) return false
        val parent = current.substringBeforeLast('/', "")
        _uiState.update { it.copy(currentPath = if (parent == current) "" else parent) }
        return true
    }

    fun extractTo(context: Context, destination: Uri) {
        val archive = staged ?: return
        val root = DocumentFile.fromTreeUri(context, destination)
        if (root == null) {
            _uiState.update { it.copy(status = OperationStatus.Failure("Couldn't access the chosen folder")) }
            return
        }
        _uiState.update { it.copy(progressLabel = "Extracting", status = OperationStatus.InProgress(0, 0)) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val target = SafExtractionTarget(context, root)
                archive.requireReader().extractAll(target) { done, total ->
                    _uiState.update { it.copy(status = OperationStatus.InProgress(done, total)) }
                }
                _uiState.update { it.copy(status = OperationStatus.Success) }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = OperationStatus.Failure(e.message ?: "Couldn't extract the archive")) }
            }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(status = OperationStatus.Idle) }
    }

    private fun cleanup() {
        staged?.dispose()
        staged = null
    }

    override fun onCleared() {
        cleanup()
    }
}
