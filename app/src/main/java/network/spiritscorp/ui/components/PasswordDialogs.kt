package network.spiritscorp.ui.components

/*
 * Copyright (C) 2026 Tom Spirit
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import network.spiritscorp.R

/**
 * Callback for password change results.
 */
fun interface OnPasswordChange {
    fun onPasswordChange(oldPassword: String, newPassword: String)
}

/**
 * Dialog for changing password.
 */
@Composable
fun PasswordChangeDialog(
    onPasswordChange: OnPasswordChange,
    onDismiss: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        title = { Text(stringResource(R.string.settings_passwort_change_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.settings_passwort_change_dialog_message))
                Spacer(Modifier.height(16.dp))
                
                PasswordField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it; error = null },
                    label = stringResource(R.string.settings_passwort_change_dialog_old_hint),
                    showPassword = showOld,
                    onToggleShow = { showOld = !showOld }
                )
                Spacer(Modifier.height(8.dp))
                
                PasswordField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = stringResource(R.string.settings_passwort_change_dialog_new_hint),
                    showPassword = showNew,
                    onToggleShow = { showNew = !showNew }
                )
                Spacer(Modifier.height(8.dp))
                
                PasswordField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = stringResource(R.string.settings_passwort_change_dialog_confirm_hint),
                    showPassword = showConfirm,
                    onToggleShow = { showConfirm = !showConfirm }
                )
                
                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (newPassword != confirmPassword) {
                    error = stringResource(R.string.settings_passwort_change_error_no_match)
                } else if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                    error = stringResource(R.string.settings_passwort_change_error_wrong_old)
                } else {
                    onPasswordChange.onPasswordChange(oldPassword, newPassword)
                }
            }) { Text(stringResource(R.string.settings_passwort_change_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        onDismissRequest = onDismiss
    )
}

/**
 * Reusable password field component.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPassword: Boolean,
    onToggleShow: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    if (showPassword) stringResource(R.string.settings_passwort_hide_button) 
                    else stringResource(R.string.settings_passwort_show_button)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
