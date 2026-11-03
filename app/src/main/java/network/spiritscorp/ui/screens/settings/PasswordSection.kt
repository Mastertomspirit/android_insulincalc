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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import network.spiritscorp.R
import network.spiritscorp.data.DatabaseSecurityManager

/**
 * Settings section composable for database password management.
 * Allows users to set, verify, and change their database password for cross-device recovery.
 *
 * @param viewModel The view model for accessing database operations and showing toasts
 * @param modifier Optional Modifier for layout styling
 * @param isExpanded Boolean flag indicating whether the accordion section is expanded
 * @param onToggleExpand Callback invoked to toggle section expansion
 */
@Composable
fun PasswordSection(
    viewModel: network.spiritscorp.viewmodel.InsulinCalculatorViewModel,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
) {
    val context = LocalContext.current
    val securityManager = remember { DatabaseSecurityManager(context) }
    val passwordManager = remember { securityManager.passwordManager }

    // State for tracking password status
    val isPasswordSet = remember { passwordManager.isPasswordSet }

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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
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
                // Password is set - show verify and change buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // TODO: Implement password verification
                            onShowToast("Password verification not yet implemented")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_verify_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.password_verify_button_text))
                    }

                    OutlinedButton(
                        onClick = {
                            // TODO: Implement password change
                            onShowToast("Password change not yet implemented")
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
                }
            } else {
                // Password not set - show set password button
                Button(
                    onClick = {
                        // Generate random password for new installations
                        val randomPassword = passwordManager.generateRandomPassword()
                        // Set the password
                        if (passwordManager.setPassword(randomPassword)) {
                            viewModel.showToast("Random password set. Please change it to something memorable.")
                        } else {
                            viewModel.showToast("Failed to set password")
                        }
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

    // Section header
    SettingsSectionHeader(
        icon = if (isPasswordSet) Icons.Default.Lock else Icons.Default.LockOpen,
        title = stringResource(R.string.settings_password_section_title),
        subtitle = stringResource(R.string.settings_password_section_subtitle),
        isExpanded = isExpanded,
        onToggle = onToggleExpand
    )
}
