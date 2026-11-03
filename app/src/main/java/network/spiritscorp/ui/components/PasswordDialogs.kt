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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
 * Callback interface for password dialog results.
 */
interface PasswordDialogCallback {
    /**
     * Called when password is confirmed.
     *
     * @param password The entered password
     */
    fun onPasswordConfirmed(password: String)

    /**
     * Called when dialog is dismissed.
     */
    fun onDismiss()
}

/**
 * Callback interface for password verification results.
 */
interface PasswordVerifyCallback {
    /**
     * Called when password verification succeeds.
     */
    fun onVerificationSuccess()

    /**
     * Called when password verification fails.
     */
    fun onVerificationFailed()

    /**
     * Called when dialog is dismissed.
     */
    fun onDismiss()
}

/**
 * Callback interface for password change results.
 */
interface PasswordChangeCallback {
    /**
     * Called when password change succeeds.
     *
     * @param oldPassword Current password
     * @param newPassword New password
     */
    fun onPasswordChanged(oldPassword: String, newPassword: String)

    /**
     * Called when dialog is dismissed.
     */
    fun onDismiss()
}

/**
 * Dialog for setting a new password.
 *
 * @param minLength Minimum password length
 * @param onResult Callback for dialog results
 */
@Composable
fun PasswordSetDialog(
    minLength: Int,
    onResult: PasswordDialogCallback
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        title = { Text(stringResource(R.string.password_set_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.password_set_dialog_message, minLength))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.password_set_dialog_hint)) },
                    placeholder = { Text(stringResource(R.string.password_set_dialog_placeholder, minLength)) },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.password_set_dialog_char_count, password.length, minLength),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.length < minLength) {
                        error = stringResource(R.string.password_set_dialog_error_too_short, minLength)
                    } else {
                        onResult.onPasswordConfirmed(password)
                    }
                },
                enabled = password.length >= minLength
            ) {
                Text(stringResource(R.string.password_set_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult.onDismiss() }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        onDismissRequest = { onResult.onDismiss() }
    )
}

/**
 * Dialog for confirming password setup.
 *
 * @param onResult Callback for dialog results
 */
@Composable
fun PasswordConfirmDialog(
    onResult: PasswordDialogCallback
) {
    AlertDialog(
        title = { Text(stringResource(R.string.password_set_dialog_confirm_title)) },
        text = { Text(stringResource(R.string.password_set_dialog_confirm_message)) },
        confirmButton = {
            Button(onClick = { onResult.onPasswordConfirmed("") }) {
                Text(stringResource(R.string.password_set_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult.onDismiss() }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        onDismissRequest = { onResult.onDismiss() }
    )
}

/**
 * Dialog for verifying a password.
 *
 * @param onResult Callback for verification results
 */
@Composable
fun PasswordVerifyDialog(
    onResult: PasswordVerifyCallback
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        title = { Text(stringResource(R.string.password_verify_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.password_verify_dialog_message))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.password_set_dialog_hint)) },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isEmpty()) {
                        error = stringResource(R.string.password_verify_dialog_failure)
                    } else {
                        onResult.onVerificationSuccess()
                    }
                }
            ) {
                Text(stringResource(R.string.password_verify_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult.onDismiss() }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        onDismissRequest = { onResult.onDismiss() }
    )
}

/**
 * Dialog for changing password.
 *
 * @param onResult Callback for password change results
 */
@Composable
fun PasswordChangeDialog(
    onResult: PasswordChangeCallback
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        title = { Text(stringResource(R.string.password_change_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.password_change_dialog_message))
                Spacer(modifier = Modifier.height(16.dp))
                
                // Old password field
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { 
                        oldPassword = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.password_change_dialog_old_hint)) },
                    visualTransformation = if (showOldPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showOldPassword = !showOldPassword }) {
                            Icon(
                                imageVector = if (showOldPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showOldPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // New password field
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.password_change_dialog_new_hint)) },
                    visualTransformation = if (showNewPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                imageVector = if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showNewPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Confirm password field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.password_change_dialog_confirm_hint)) },
                    visualTransformation = if (showConfirmPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showConfirmPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPassword.isEmpty() || confirmPassword.isEmpty() || oldPassword.isEmpty()) {
                        error = "All fields are required"
                    } else if (newPassword != confirmPassword) {
                        error = stringResource(R.string.password_change_error_no_match)
                    } else {
                        onResult.onPasswordChanged(oldPassword, newPassword)
                    }
                }
            ) {
                Text(stringResource(R.string.password_change_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { onResult.onDismiss() }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        onDismissRequest = { onResult.onDismiss() }
    )
}

/**
 * Simple dialog to display a random password for new installations.
 *
 * @param password The randomly generated password to display
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun RandomPasswordDialog(
    password: String,
    onDismiss: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(stringResource(R.string.password_set_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.password_set_dialog_message, 20))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = if (showPassword) password else "".padEnd(password.length, '*'),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.password_set_dialog_hint)) },
                    readOnly = true,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) {
                                    stringResource(R.string.password_hide_button)
                                } else {
                                    stringResource(R.string.password_show_button)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.password_set_dialog_char_count, password.length, 20),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        onDismissRequest = onDismiss
    )
}
