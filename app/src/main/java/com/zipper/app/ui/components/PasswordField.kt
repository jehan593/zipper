package com.zipper.app.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun PasswordField(
    password: String,
    onPasswordChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    label: String = "Password (optional)"
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        // A text toggle rather than a Visibility/VisibilityOff icon button: those two icons
        // (unlike Lock) aren't part of material-icons-core, and a plain word avoids pulling in
        // material-icons-extended just for this.
        trailingIcon = {
            // OutlinedTextField's own `enabled` only greys out the input itself — a composable
            // placed in trailingIcon needs its own `enabled` passed through or it stays clickable
            // even while the field around it looks disabled.
            TextButton(onClick = { visible = !visible }, enabled = enabled) {
                Text(if (visible) "Hide" else "Show")
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = modifier
    )
}
