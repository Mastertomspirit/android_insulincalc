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

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.util.DateTimeUtils
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.text.style.TextAlign

@Composable
fun BackupRestoreSection(
    viewModel: InsulinCalculatorViewModel,
    onShowResetDbDialog: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
) {
    val context = LocalContext.current
    val backupManager = DatabaseBackupManager(context)

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val json = viewModel.exportJsonBackup()
                val success = backupManager.writeTextToUri(context, uri, json)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "JSON-Backupdatei erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Fehler beim Speichern der JSON-Datei.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val csv = viewModel.exportCsvBackup()
                val success = backupManager.writeTextToUri(context, uri, csv)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "Tagebuch erfolgreich als CSV gespeichert!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Fehler beim Speichern der CSV-Datei.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val content = backupManager.readTextFromUri(context, uri)
                if (content.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Konnte Datei nicht lesen oder Datei ist leer.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val result = viewModel.importBackupContent(content)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Import fehlgeschlagen: ${result.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsSectionHeader(
                icon = Icons.Default.Save,
                title = "5. Datensicherung & Backup",
                subtitle = "Sichere deine Daten lokal oder importiere Backups",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateTag = DateTimeUtils.getFilenameTimestamp()
                                jsonExportLauncher.launch("insulin_backup_$dateTag.json")
                            },
                            modifier = Modifier.weight(1f).testTag("export_json_file_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Backup\nExport",
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                val dateTag = DateTimeUtils.getFilenameTimestamp()
                                csvExportLauncher.launch("insulin_tagebuch_$dateTag.csv")
                            },
                            modifier = Modifier.weight(1f).testTag("export_csv_file_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Tagebuch\nExport",
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    val json = viewModel.exportJsonBackup()
                                    withContext(Dispatchers.Main) {
                                        try {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, json)
                                                putExtra(Intent.EXTRA_SUBJECT, "InsulinCalculator JSON Backup")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "JSON Backup teilen"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Fehler beim Teilen: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("share_json_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("JSON teilen")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.viewModelScope.launch(Dispatchers.IO) {
                                    val csv = viewModel.exportCsvBackup()
                                    withContext(Dispatchers.Main) {
                                        try {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, csv)
                                                putExtra(Intent.EXTRA_SUBJECT, "InsulinCalculator CSV Export")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "CSV Tagebuch teilen"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Fehler beim Teilen: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("share_csv_backup_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV teilen")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            fileImportLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth().testTag("import_file_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup-Datei importieren (.json / .csv)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onShowResetDbDialog,
                        modifier = Modifier.fillMaxWidth().testTag("reset_all_data_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alle Daten & Tagebuch unwiderruflich löschen")
                    }
                }
            }
        }
    }
}
