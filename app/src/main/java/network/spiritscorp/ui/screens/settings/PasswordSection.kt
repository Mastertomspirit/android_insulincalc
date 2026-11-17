package network.spiritscorp.ui.screens.settings

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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import network.spiritscorp.R
import network.spiritscorp.data.DatabaseSecurityManager
import network.spiritscorp.ui.components.PasswordChangeCallback
import network.spiritscorp.ui.components.PasswordChangeDialog
import network.spiritscorp.ui.components.PasswordConfirmDialog
import network.spiritscorp.ui.components.PasswordDialogCallback
import network.spiritscorp.ui.components.PasswordSetDialog
import network.spiritscorp.util.AppConstants
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel

/**
 * Settings section composable for database password management.
 * Allows users to set and change their database password.
 *
 * @param viewModel The view model for accessing database operations and showing toasts
 * @param modifier Optional Modifier for layout styling
 * @param isExpanded Boolean flag indicating whether the accordion section is expanded
 * @param onToggleExpand Callback invoked to toggle section expansion
 */
@Composable
fun PasswordSection(
    viewModel: InsulinCalculatorViewModel,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
) {
    val context = LocalContext.current
    val securityManager = remember { DatabaseSecurityManager(context) }

    // State for tracking password status
    var isPasswordSet by remember { mutableStateOf(securityManager.isPasswordSet) }

    // State for tracking dialog visibility
    var showSetDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showChangeDialog by remember { mutableStateOf(false) }
    var pendingPassword by remember { mutableStateOf("") }

    // Determine status text and icon based on password state
    val (statusText, statusIcon, statusColor) = if (isPasswordSet) {
        Triple(
            stringResource(R.string.password_status_active),
            Icons.Default.Check,
            MaterialTheme.colorScheme.primary
        )
    } else {
        Triple(
            stringResource(R.string.password_status_inactive),
            Icons.Default.Warning,
            MaterialTheme.colorScheme.error
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("settings_password_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section header
            SettingsSectionHeader(
                icon = if (isPasswordSet) Icons.Default.Lock else Icons.Default.LockOpen,
                title = stringResource(R.string.settings_password_section_title),
                subtitle = stringResource(R.string.settings_password_section_subtitle),
                isExpanded = isExpanded,
                onToggle = onToggleExpand
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Status card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_status_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isPasswordSet) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            if (!isPasswordSet) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.password_status_warning),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    if (isPasswordSet) {
                        // Password is set - show change button
                        OutlinedButton(
                            onClick = {
                                showChangeDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_change_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.password_change_button))
                        }
                    } else {
                        // Password not set - show set password button
                        Button(
                            onClick = {
                                showSetDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_set_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.password_set_button))
                        }
                    }
                }
            }
        }
    }

    // Toast messages
    val passSetSuccessStr = stringResource(R.string.password_set_success)
    val passSetFailedStr = stringResource(R.string.password_set_failed)
    val passChangeSuccessStr = stringResource(R.string.password_change_success)
    val passChangeFailedStr = stringResource(R.string.password_change_failed)

    // Dialogs
    if (showSetDialog) {
        val suggestedPassword = remember { securityManager.generateRandomPassword() }
        PasswordSetDialog(
            minLength = AppConstants.MIN_PASSWORD_LENGTH,
            initialPassword = suggestedPassword,
            onResult = object : PasswordDialogCallback {
                override fun onPasswordConfirmed(password: String) {
                    pendingPassword = password
                    showSetDialog = false
                    showConfirmDialog = true
                }

                override fun onDismiss() {
                    showSetDialog = false
                }
            }
        )
    }

    if (showConfirmDialog) {
        PasswordConfirmDialog(
            onResult = object : PasswordDialogCallback {
                override fun onPasswordConfirmed(password: String) {
                    val success = securityManager.changePassword("",  pendingPassword)
                    if (success) {
                        isPasswordSet = securityManager.isPasswordSet
                        viewModel.showToast(passSetSuccessStr)
                    } else {
                        viewModel.showToast(passSetFailedStr)
                    }
                    showConfirmDialog = false
                    pendingPassword = ""
                }

                override fun onDismiss() {
                    showConfirmDialog = false
                    pendingPassword = ""
                }
            }
        )
    }

    if (showChangeDialog) {
        PasswordChangeDialog(
            onResult = object : PasswordChangeCallback {
                override fun onPasswordChanged(oldPassword: String, newPassword: String) {
                    val success = securityManager.changePassword(oldPassword, newPassword)
                    if (success) {
                        isPasswordSet = securityManager.isPasswordSet
                        viewModel.showToast(passChangeSuccessStr)
                    } else {
                        viewModel.showToast(passChangeFailedStr)
                    }
                    showChangeDialog = false
                }

                override fun onDismiss() {
                    showChangeDialog = false
                }
            }
        )
    }
}
