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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import network.spiritscorp.R
import network.spiritscorp.util.AppConstants

/**
 * Dialog for setting a new database password.
 *
 * @param onDismiss Callback when dialog is dismissed.
 * @param onConfirm Callback when password is confirmed (receives the password).
 * @param modifier Optional modifier for the dialog.
 */
@Composable
fun PasswordSetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.password_set_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.password_set_dialog_message, AppConstants.MIN_PASSPHRASE_LENGTH))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { newValue ->
                        password = newValue
                        error = if (newValue.length < AppConstants.MIN_PASSPHRASE_LENGTH) {
                            stringResource(R.string.password_set_dialog_error_too_short, AppConstants.MIN_PASSPHRASE_LENGTH)
                        } else {
                            null
                        }
                    },
                    label = { Text(stringResource(R.string.password_set_dialog_hint)) },
                    placeholder = {
                        Text(stringResource(R.string.password_set_dialog_placeholder, AppConstants.MIN_PASSPHRASE_LENGTH))
                    },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                stringResource(
                                    R.string.password_set_dialog_char_count,
                                    password.length,
                                    AppConstants.MIN_PASSPHRASE_LENGTH
                                )
                            )
                        }
                    },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password.length >= AppConstants.MIN_PASSPHRASE_LENGTH) {
                        onConfirm(password)
                    }
                },
                enabled = password.length >= AppConstants.MIN_PASSPHRASE_LENGTH
            ) {
                Text(stringResource(R.string.password_set_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for confirming password setup.
 *
 * @param onDismiss Callback when dialog is dismissed.
 * @param onConfirm Callback when user confirms they have stored the password.
 * @param modifier Optional modifier for the dialog.
 */
@Composable
fun PasswordConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.password_set_dialog_confirm_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.password_set_dialog_confirm_message))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.password_set_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for verifying an existing password.
 *
 * @param onDismiss Callback when dialog is dismissed.
 * @param onVerify Callback when password is verified (receives the password).
 * @param modifier Optional modifier for the dialog.
 */
@Composable
fun PasswordVerifyDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.password_verify_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.password_verify_dialog_message))
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_set_dialog_hint)) },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onVerify(password) },
                enabled = password.isNotEmpty()
            ) {
                Text(stringResource(R.string.password_verify_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for clearing the password.
 *
 * @param onDismiss Callback when dialog is dismissed.
 * @param onConfirm Callback when user confirms clearing.
 * @param modifier Optional modifier for the dialog.
 */
@Composable
fun PasswordClearDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.password_clear_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(stringResource(R.string.password_clear_dialog_message))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = MaterialTheme.colorScheme.error
            ) {
                Text(stringResource(R.string.password_clear_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

// IconButton wrapper for the password visibility toggle
@Composable
private fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        content()
    }
}
