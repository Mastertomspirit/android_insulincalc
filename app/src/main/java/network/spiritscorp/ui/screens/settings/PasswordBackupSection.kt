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

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import network.spiritscorp.R
import network.spiritscorp.data.DatabaseSecurityManager
import network.spiritscorp.ui.components.PasswordClearDialog
import network.spiritscorp.ui.components.PasswordConfirmDialog
import network.spiritscorp.ui.components.PasswordSetDialog
import network.spiritscorp.ui.components.PasswordVerifyDialog
import network.spiritscorp.ui.components.SettingsSectionHeader

/**
 * Settings section for managing database recovery password.
 *
 * @param securityManager DatabaseSecurityManager instance.
 * @param modifier Optional modifier for the section.
 * @param isExpanded Whether the section is expanded.
 * @param onToggleExpand Callback to toggle expansion.
 */
@Composable
fun PasswordBackupSection(
    securityManager: DatabaseSecurityManager,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
) {
    val context = LocalContext.current
    var showSetDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showVerifyDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var tempPassword by remember { mutableStateOf("") }
    var isPasswordSet by remember { mutableStateOf(securityManager.isPasswordSet()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSectionHeader(
                icon = Icons.Default.Security,
                title = stringResource(R.string.password_backup_section_title),
                subtitle = stringResource(R.string.password_backup_section_subtitle),
                isExpanded = isExpanded,
                onToggle = onToggleExpand
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Info text
                    Text(
                        text = stringResource(R.string.password_backup_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Status indicator
                    val statusIcon = if (isPasswordSet) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Warning
                    }
                    val statusColor = if (isPasswordSet) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    val statusText = if (isPasswordSet) {
                        stringResource(R.string.password_backup_status_active)
                    } else {
                        stringResource(R.string.password_backup_status_inactive)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (!isPasswordSet) {
                        Text(
                            text = stringResource(R.string.password_backup_status_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    if (isPasswordSet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showVerifyDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.password_verify_button_text))
                            }
                            OutlinedButton(
                                onClick = { showClearDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.password_clear_button))
                            }
                        }
                    } else {
                        Button(
                            onClick = { showSetDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.password_set_button))
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSetDialog) {
        PasswordSetDialog(
            onDismiss = { showSetDialog = false },
            onConfirm = { password ->
                tempPassword = password
                showSetDialog = false
                showConfirmDialog = true
            }
        )
    }

    if (showConfirmDialog) {
        PasswordConfirmDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                if (securityManager.setPassword(context, tempPassword)) {
                    isPasswordSet = true
                    Toast.makeText(
                        context,
                        R.string.password_set_dialog_confirm_button,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Failed to set password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showConfirmDialog = false
                tempPassword = ""
            }
        )
    }

    if (showVerifyDialog) {
        PasswordVerifyDialog(
            onDismiss = { showVerifyDialog = false },
            onVerify = { password ->
                if (securityManager.verifyPassword(password)) {
                    Toast.makeText(
                        context,
                        R.string.password_verify_dialog_success,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        R.string.password_verify_dialog_failure,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showVerifyDialog = false
            }
        )
    }

    if (showClearDialog) {
        PasswordClearDialog(
            onDismiss = { showClearDialog = false },
            onConfirm = {
                if (securityManager.clearPassword()) {
                    isPasswordSet = false
                    Toast.makeText(
                        context,
                        "Password cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showClearDialog = false
            }
        )
    }
}
