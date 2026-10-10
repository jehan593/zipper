package com.zipper.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zipper.app.R
import com.zipper.app.ui.theme.nord0
import com.zipper.app.ui.theme.nord14
import com.zipper.app.ui.theme.nord15

/** Keep the dialog open because archive operations do not support cancellation. */
@Composable
fun ProgressDialog(status: OperationStatus.InProgress, label: String) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (status.total > 0) {
                    LinearProgressIndicator(progress = { status.done.toFloat() / status.total.toFloat() })
                } else {
                    CircularProgressIndicator()
                }
                Text("$label (${status.done}/${status.total.coerceAtLeast(1)})")
            }
        }
    }
}

@Composable
fun ResultDialog(
    status: OperationStatus,
    successMessage: String,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    savedPath: String? = null
) {
    when (status) {
        is OperationStatus.Success -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Surface(color = nord14, contentColor = nord0, shape = CircleShape) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        modifier = Modifier.padding(12.dp).size(28.dp))
                }
            },
            title = { Text("Success") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(successMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (savedPath != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Saved archive", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer {
                                    Text(savedPath, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    if (onOpen != null || onShare != null) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (onOpen != null) {
                                Button(onClick = onOpen, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary)) {
                                    Text("Open archive")
                                }
                            }
                            if (onShare != null) {
                                FilledIconButton(onClick = onShare, modifier = Modifier.size(48.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = nord15, contentColor = nord0)) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share archive")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text("Done")
                }
            }
        )
        is OperationStatus.Failure -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } },
            icon = { Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Failed") },
            text = { Text(status.message) }
        )
        else -> Unit
    }
}
