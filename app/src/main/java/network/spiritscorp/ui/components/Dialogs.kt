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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import network.spiritscorp.insulincalc.R

/**
 * Displays a non-dismissible recovery dialog when the encrypted database cannot be opened or initialized.
 *
 * Offers the user three recovery actions:
 * - Importing an existing backup file.
 * - Resetting the database (deleting the corrupted database and reinitializing it).
 * - Closing the application without modifying existing data.
 *
 * @param onImportBackup Callback invoked when the user chooses to restore from a backup.
 * @param onResetDatabase Callback invoked when the user confirms resetting the database.
 * @param onCloseApp Callback invoked when the user chooses to exit the application.
 */
@Composable
internal fun DatabaseRecoveryDialog(
    onImportBackup: () -> Unit,
    onResetDatabase: () -> Unit,
    onCloseApp: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("db_recovery_dialog"),
        onDismissRequest = {},
        title = { Text(stringResource(R.string.dialog_db_recovery_title)) },
        text = { Text(stringResource(R.string.dialog_db_recovery_message)) },
        confirmButton = {
            TextButton(
                onClick = onImportBackup,
                modifier = Modifier.testTag("btn_db_recovery_import")
            ) {
                Text(stringResource(R.string.dialog_db_recovery_import_backup))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onResetDatabase,
                    modifier = Modifier.testTag("btn_db_recovery_reset")
                ) {
                    Text(stringResource(R.string.dialog_db_recovery_reset_db))
                }
                TextButton(
                    onClick = onCloseApp,
                    modifier = Modifier.testTag("btn_db_recovery_close")
                ) {
                    Text(stringResource(R.string.dialog_db_recovery_close_app))
                }
            }
        },
    )
}

