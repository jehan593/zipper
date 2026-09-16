package com.zipper.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zipper.app.R

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
fun ResultDialog(status: OperationStatus, successMessage: String, onDismiss: () -> Unit) {
    when (status) {
        is OperationStatus.Success -> AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Success") },
            text = { Text(successMessage) }
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
