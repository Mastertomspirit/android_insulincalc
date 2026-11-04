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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import network.spiritscorp.ui.components.PasswordChangeDialog

/**
 * Settings section for database password management.
 * Database password is always active and cannot be removed, only changed.
 */
@Composable
fun PasswordBackupSection(
    viewModel: network.spiritscorp.viewmodel.InsulinCalculatorViewModel,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    val context = LocalContext.current
    val securityManager = remember { DatabaseSecurityManager(context) }
    val passwordManager = remember { securityManager.passwordManager }

    // Check if password is set
    val isPasswordSet = remember { passwordManager.loadFromKeyStore() != null }

    // State for password change dialog
    var showPasswordChangeDialog by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(modifier = modifier.padding(top = 8.dp)) {
            // Status card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("password_status_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.settings_passwort_status_active),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.settings_passwort_status_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showPasswordChangeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_change_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_passwort_change_button))
                }
            }

            // Password change dialog
            if (showPasswordChangeDialog) {
                PasswordChangeDialog(
                    onPasswordChange = { oldPassword, newPassword ->
                        if (passwordManager.verifyPassword(oldPassword)) {
                            if (passwordManager.setPassword(newPassword)) {
                                viewModel.showToast(context.getString(R.string.settings_passwort_change_success))
                            } else {
                                viewModel.showToast("Failed to change password")
                            }
                        } else {
                            viewModel.showToast(context.getString(R.string.settings_passwort_change_error_wrong_old))
                        }
                        showPasswordChangeDialog = false
                    },
                    onDismiss = { showPasswordChangeDialog = false }
                )
            }
        }
    }

    // Section header
    SettingsSectionHeader(
        icon = Icons.Default.Lock,
        title = stringResource(R.string.settings_passwort_section_title),
        subtitle = stringResource(R.string.settings_passwort_section_subtitle),
        isExpanded = isExpanded,
        onToggle = onToggleExpand
    )
}
