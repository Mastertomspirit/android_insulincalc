package network.spiritscorp.ui.screens

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

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.ui.theme.AlertRed
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Filter options for the calculation history.
 */
enum class HistoryFilter(val title: String) {
    ALL("Alle"),
    TODAY("Heute"),
    DAYS_7("7 Tage"),
    DAYS_30("30 Tage"),
    CUSTOM("Zeitraum")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    viewModel: InsulinCalculatorViewModel,
    logs: List<CalculationLog>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearAllDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<CalculationLog?>(null) }

    // Filter states
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var sliceOffsetDays by remember { mutableIntStateOf(0) } // For sliding window navigation (z.B. -7 Tage, +7 Tage)
    var customStartDateMillis by remember { mutableStateOf<Long?>(null) }
    var customEndDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Filtered logs computation
    val filteredLogs = remember(logs, selectedFilter, sliceOffsetDays, customStartDateMillis, customEndDateMillis) {
        val now = Calendar.getInstance()
        when (selectedFilter) {
            HistoryFilter.ALL -> logs
            HistoryFilter.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, sliceOffsetDays)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val end = cal.timeInMillis
                logs.filter { it.timestamp in start until end }
            }
            HistoryFilter.DAYS_7 -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, sliceOffsetDays * 7)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val start = cal.timeInMillis
                logs.filter { it.timestamp in start..end }
            }
            HistoryFilter.DAYS_30 -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, sliceOffsetDays * 30)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                val start = cal.timeInMillis
                logs.filter { it.timestamp in start..end }
            }
            HistoryFilter.CUSTOM -> {
                val start = customStartDateMillis
                val end = customEndDateMillis
                if (start != null && end != null) {
                    val endInclusive = Calendar.getInstance().apply {
                        timeInMillis = end
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    logs.filter { it.timestamp in start..endInclusive }
                } else {
                    logs
                }
            }
        }
    }

    // CSV File Export Launcher
    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.viewModelScope.launch {
                val exportList = if (filteredLogs.isNotEmpty()) filteredLogs else logs
                val csvContent = DatabaseBackupManager.exportToCsv(exportList)
                val success = DatabaseBackupManager.writeTextToUri(context, uri, csvContent)
                if (success) {
                    Toast.makeText(context, "Tagebuch (${exportList.size} Einträge) erfolgreich als CSV gespeichert!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Fehler beim Speichern der CSV-Datei.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Calculations for the current visible slice/filter
    val totalCarbsVisible = remember(filteredLogs) {
        filteredLogs.sumOf { it.carbGrams }
    }
    val totalInsulinVisible = remember(filteredLogs) {
        filteredLogs.sumOf { it.roundedInsulin }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logbook_summary_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tagebuch & Verlauf",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getFilterDescription(selectedFilter, sliceOffsetDays, customStartDateMillis, customEndDateMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Teilen-Button
                        IconButton(
                            onClick = {
                                if (filteredLogs.isNotEmpty()) {
                                    val textToShare = generateExportText(filteredLogs, selectedFilter.title)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, textToShare)
                                        putExtra(Intent.EXTRA_SUBJECT, "Insulin-Tagebuch (${filteredLogs.size} Einträge)")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Tagebuch teilen")
                                    context.startActivity(shareIntent)
                                } else {
                                    Toast.makeText(context, "Keine Einträge zum Teilen vorhanden", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("share_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Tagebuch per Text teilen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // CSV-Export Button
                        IconButton(
                            onClick = {
                                if (filteredLogs.isNotEmpty()) {
                                    val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    csvFileLauncher.launch("insulin_tagebuch_${selectedFilter.name.lowercase()}_$dateTag.csv")
                                } else {
                                    Toast.makeText(context, "Keine Einträge vorhanden", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("export_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Tagebuch als CSV-Datei exportieren",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Alle Löschen Button
                        if (logs.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearAllDialog = true },
                                modifier = Modifier.testTag("clear_all_logs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Alle Einträge löschen",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Gesamt KH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${totalCarbsVisible.toInt()} g",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Gesamt Insulin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f IE", totalInsulinVisible),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Einträge",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${filteredLogs.size}${if (filteredLogs.size != logs.size) " / ${logs.size}" else ""}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Filter Bar (Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedFilter = filter
                            sliceOffsetDays = 0 // Reset slice offset when switching filter type
                            if (filter == HistoryFilter.CUSTOM && customStartDateMillis == null) {
                                showDateRangePicker = true
                            }
                        }
                        .testTag("filter_chip_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = filter.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Slice / Window Navigation Controls (für "Heute", "7 Tage", "30 Tage" oder Custom Datepicker)
        if (selectedFilter != HistoryFilter.ALL) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedFilter == HistoryFilter.CUSTOM) {
                        Text(
                            text = if (customStartDateMillis != null && customEndDateMillis != null) {
                                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                                "${sdf.format(Date(customStartDateMillis!!))} - ${sdf.format(Date(customEndDateMillis!!))}"
                            } else "Kein Zeitraum gewählt",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        TextButton(
                            onClick = { showDateRangePicker = true },
                            modifier = Modifier.testTag("change_custom_date_button")
                        ) {
                            Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ändern")
                        }
                    } else {
                        IconButton(
                            onClick = { sliceOffsetDays -= 1 },
                            modifier = Modifier.testTag("slice_prev_button")
                        ) {
                            Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Vorheriger Zeitraum")
                        }

                        Text(
                            text = when (selectedFilter) {
                                HistoryFilter.TODAY -> {
                                    if (sliceOffsetDays == 0) "Heute"
                                    else if (sliceOffsetDays == -1) "Gestern"
                                    else {
                                        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, sliceOffsetDays) }
                                        SimpleDateFormat("dd. MMMM yyyy", Locale.GERMANY).format(cal.time)
                                    }
                                }
                                HistoryFilter.DAYS_7 -> {
                                    if (sliceOffsetDays == 0) "Letzte 7 Tage"
                                    else "Vor ${-sliceOffsetDays} Woche(n)"
                                }
                                HistoryFilter.DAYS_30 -> {
                                    if (sliceOffsetDays == 0) "Letzte 30 Tage"
                                    else "Vor ${-sliceOffsetDays} Monat(en)"
                                }
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row {
                            if (sliceOffsetDays != 0) {
                                TextButton(
                                    onClick = { sliceOffsetDays = 0 },
                                    modifier = Modifier.testTag("slice_reset_today_button")
                                ) {
                                    Text("Aktuell", fontSize = 11.sp)
                                }
                            }

                            IconButton(
                                onClick = { if (sliceOffsetDays < 0) sliceOffsetDays += 1 },
                                enabled = sliceOffsetDays < 0,
                                modifier = Modifier.testTag("slice_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Nächster Zeitraum",
                                    tint = if (sliceOffsetDays < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // List of entries
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (logs.isEmpty()) "Noch keine Berechnungen gespeichert" else "Keine Einträge im gewählten Zeitraum",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (logs.isEmpty()) "Berechne deine Mahlzeit im Rechner und tippe auf 'Im Tagebuch speichern'." else "Wähle oben einen anderen Filter oder springe zu einem anderen Zeitraum.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs, key = { it.id }) { item ->
                    LogbookItemCard(
                        log = item,
                        onDeleteRequest = { logToDelete = item }
                    )
                }
            }
        }
    }

    // Single item deletion confirmation popup
    if (logToDelete != null) {
        val targetItem = logToDelete!!
        val dateText = remember(targetItem.timestamp) {
            SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY).format(Date(targetItem.timestamp))
        }

        AlertDialog(
            onDismissRequest = { logToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Eintrag löschen?") },
            text = {
                Column {
                    Text("Möchtest du diesen Eintrag wirklich aus deinem Tagebuch entfernen?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = targetItem.mealTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$dateText • ${targetItem.roundedInsulin} IE (${targetItem.carbGrams}g KH)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLog(targetItem.id)
                        logToDelete = null
                        Toast.makeText(context, "Eintrag gelöscht", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_delete_single_log_button")
                ) {
                    Text("Löschen", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { logToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_single_log_button")
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Clear ALL logs confirmation popup
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Komplettes Tagebuch leeren?") },
            text = {
                Text("Bist du sicher? Alle ${logs.size} gespeicherten Berechnungen werden unwiderruflich aus der Datenbank gelöscht. Es wird empfohlen, vorher ein Backup oder einen CSV-Export zu erstellen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearAllDialog = false
                        Toast.makeText(context, "Tagebuch vollständig geleert", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_clear_all_logs_button")
                ) {
                    Text("Alles löschen", color = AlertRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false },
                    modifier = Modifier.testTag("cancel_clear_all_logs_button")
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Material 3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStartDateMillis ?: System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000),
            initialSelectedEndDateMillis = customEndDateMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        customStartDateMillis = dateRangePickerState.selectedStartDateMillis
                        customEndDateMillis = dateRangePickerState.selectedEndDateMillis
                        selectedFilter = HistoryFilter.CUSTOM
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Übernehmen", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Zeitraum für Tagebuch auswählen", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LogbookItemCard(
    log: CalculationLog,
    onDeleteRequest: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.GERMANY) }
    val dateString = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_${log.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.mealTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = log.timeOfDay,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${log.carbGrams}g KH (${log.beValue} BE • ${log.keValue} KE) • Faktor: ${log.insulinFactor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (log.bloodGlucose != null) {
                    val bzDisplay = if (log.bloodGlucose % 1.0 == 0.0) log.bloodGlucose.toInt().toString() else String.format(Locale.US, "%.1f", log.bloodGlucose)
                    Text(
                        text = "BZ: $bzDisplay (Korrektur: ${if ((log.correctionInsulin ?: 0.0) > 0) "+" else ""}${log.correctionInsulin} IE)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${log.roundedInsulin} IE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteRequest,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_single_log_${log.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eintrag löschen",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun getFilterDescription(
    filter: HistoryFilter,
    sliceOffsetDays: Int,
    customStart: Long?,
    customEnd: Long?
): String {
    return when (filter) {
        HistoryFilter.ALL -> "Alle Einträge"
        HistoryFilter.TODAY -> {
            if (sliceOffsetDays == 0) "Heute"
            else if (sliceOffsetDays == -1) "Gestern"
            else {
                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, sliceOffsetDays) }
                SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(cal.time)
            }
        }
        HistoryFilter.DAYS_7 -> {
            if (sliceOffsetDays == 0) "Letzte 7 Tage"
            else "7 Tage (vor ${-sliceOffsetDays} Woche(n))"
        }
        HistoryFilter.DAYS_30 -> {
            if (sliceOffsetDays == 0) "Letzte 30 Tage"
            else "30 Tage (vor ${-sliceOffsetDays} Monat(en))"
        }
        HistoryFilter.CUSTOM -> {
            if (customStart != null && customEnd != null) {
                val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)
                "${sdf.format(Date(customStart))} bis ${sdf.format(Date(customEnd))}"
            } else "Benutzerdefinierter Zeitraum"
        }
    }
}

private fun generateExportText(logs: List<CalculationLog>, filterName: String): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMANY)
    val sb = StringBuilder()
    sb.appendLine("=== DIABETES INSULIN-TAGEBUCH ($filterName) ===")
    sb.appendLine("Exportiert am: ${sdf.format(Date())}")
    sb.appendLine("Anzahl Einträge: ${logs.size}")
    sb.appendLine("Gesamte Kohlenhydrate: ${logs.sumOf { it.carbGrams }.toInt()} g")
    sb.appendLine("Gesamtes Insulin: ${String.format(Locale.US, "%.1f", logs.sumOf { it.roundedInsulin })} IE")
    sb.appendLine("---------------------------------")
    logs.forEach { log ->
        sb.appendLine("${sdf.format(Date(log.timestamp))} | ${log.timeOfDay}: ${log.mealTitle}")
        sb.appendLine("• Kohlenhydrate: ${log.carbGrams}g KH (${log.beValue} BE / ${log.keValue} KE) | Faktor: ${log.insulinFactor}")
        if (log.bloodGlucose != null) {
            val bzDisplay = if (log.bloodGlucose % 1.0 == 0.0) log.bloodGlucose.toInt().toString() else String.format(Locale.US, "%.1f", log.bloodGlucose)
            sb.appendLine("• Blutzucker: $bzDisplay (Korrektur: ${if ((log.correctionInsulin ?: 0.0) > 0) "+" else ""}${log.correctionInsulin} IE)")
        }
        sb.appendLine("• Dosis: ${log.roundedInsulin} IE (Mahlzeit: ${log.mealInsulin} IE)")
        sb.appendLine("---------------------------------")
    }
    return sb.toString()
}
