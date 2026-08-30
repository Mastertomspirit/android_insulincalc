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

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.ui.screens.logbook.ClearAllLogsDialog
import network.spiritscorp.ui.screens.logbook.LogbookFilterBar
import network.spiritscorp.ui.screens.logbook.LogbookItemCard
import network.spiritscorp.ui.screens.logbook.LogbookStatsHeader
import network.spiritscorp.ui.screens.logbook.SingleLogDeleteDialog
import network.spiritscorp.util.LogbookExportHelper
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

@Composable
fun LogbookScreen(
    viewModel: InsulinCalculatorViewModel,
    logs: List<CalculationLog>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearAllDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<CalculationLog?>(null) }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var sliceOffset by remember { mutableIntStateOf(0) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var customStartDateMillis by remember { mutableLongStateOf(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000) }
    var customEndDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Filter computation
    val filteredLogs by remember(logs, selectedFilter, sliceOffset, customStartDateMillis, customEndDateMillis) {
        derivedStateOf {
            val now = Calendar.getInstance()
            when (selectedFilter) {
                HistoryFilter.ALL -> logs
                HistoryFilter.TODAY -> {
                    val targetDay = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -sliceOffset) }
                    logs.filter {
                        val itemCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        itemCal.get(Calendar.YEAR) == targetDay.get(Calendar.YEAR) &&
                                itemCal.get(Calendar.DAY_OF_YEAR) == targetDay.get(Calendar.DAY_OF_YEAR)
                    }
                }
                HistoryFilter.DAYS_7 -> {
                    val endCal = (now.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -sliceOffset * 7)
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                    }
                    val startCal = (endCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }
                    logs.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis }
                }
                HistoryFilter.DAYS_30 -> {
                    val endCal = (now.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -sliceOffset * 30)
                        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
                    }
                    val startCal = (endCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, -30)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
                    }
                    logs.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis }
                }
                HistoryFilter.CUSTOM -> {
                    logs.filter { it.timestamp in customStartDateMillis..(customEndDateMillis + 86400000L) }
                }
            }
        }
    }

    val filterDescription by remember(selectedFilter, sliceOffset, customStartDateMillis, customEndDateMillis) {
        derivedStateOf {
            getFilterDescription(selectedFilter, sliceOffset, customStartDateMillis, customEndDateMillis)
        }
    }

    val totalCarbs = remember(filteredLogs) { filteredLogs.sumOf { it.carbGrams } }
    val totalInsulin = remember(filteredLogs) { filteredLogs.sumOf { it.roundedInsulin } }
    val avgBloodGlucose = remember(filteredLogs) {
        val bgEntries = filteredLogs.mapNotNull { it.bloodGlucose }
        if (bgEntries.isNotEmpty()) bgEntries.average() else null
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Insulin-Tagebuch",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredLogs.size} von ${logs.size} Einträgen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            val exportText = LogbookExportHelper.generateExportText(filteredLogs, filterDescription)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Tagebuch teilen ($filterDescription)")
                            context.startActivity(shareIntent)
                        },
                        enabled = filteredLogs.isNotEmpty(),
                        modifier = Modifier.testTag("share_logbook_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Tagebuch teilen",
                            tint = if (filteredLogs.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = { showClearAllDialog = true },
                        enabled = logs.isNotEmpty(),
                        modifier = Modifier.testTag("clear_all_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Tagebuch leeren",
                            tint = if (logs.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        item {
            // Filter Bar with timeframe navigators
            LogbookFilterBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    if (filter == HistoryFilter.CUSTOM) {
                        showDatePickerDialog = true
                    } else {
                        selectedFilter = filter
                        sliceOffset = 0
                    }
                },
                offsetIndex = sliceOffset,
                onPreviousOffset = { sliceOffset++ },
                onNextOffset = { if (sliceOffset > 0) sliceOffset-- },
                onResetOffset = { sliceOffset = 0 },
                filterDescription = filterDescription,
                showDatePickerDialog = showDatePickerDialog,
                onShowDatePickerDialogChange = { showDatePickerDialog = it },
                onCustomRangeSelected = { start, end ->
                    customStartDateMillis = start
                    customEndDateMillis = end
                    selectedFilter = HistoryFilter.CUSTOM
                }
            )
        }

        if (filteredLogs.isNotEmpty()) {
            item {
                // Statistical Summary Header
                LogbookStatsHeader(
                    totalCarbs = totalCarbs,
                    totalInsulin = totalInsulin,
                    entryCount = filteredLogs.size,
                    avgBloodGlucose = avgBloodGlucose
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (logs.isEmpty()) "Noch keine Berechnungen gespeichert" else "Keine Einträge für diesen Zeitraum",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (logs.isEmpty()) "Berechne im Rechner eine Dosis und tippe auf 'Im Tagebuch speichern'." else "Wähle oben einen anderen Filter oder springe im Zeitraum.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(items = filteredLogs, key = { it.id }) { log ->
                LogbookItemCard(
                    log = log,
                    onDeleteRequest = { logToDelete = log }
                )
            }
        }
    }

    // Dialogs
    logToDelete?.let { targetItem ->
        SingleLogDeleteDialog(
            log = targetItem,
            onConfirm = {
                viewModel.deleteLog(targetItem.id)
                logToDelete = null
                Toast.makeText(context, "Eintrag gelöscht", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { logToDelete = null }
        )
    }

    if (showClearAllDialog) {
        ClearAllLogsDialog(
            totalLogsCount = logs.size,
            onConfirm = {
                viewModel.clearAllLogs()
                showClearAllDialog = false
                Toast.makeText(context, "Tagebuch vollständig geleert", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

private fun getFilterDescription(
    filter: HistoryFilter,
    sliceOffset: Int,
    customStart: Long,
    customEnd: Long
): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return when (filter) {
        HistoryFilter.ALL -> "Alle Einträge"
        HistoryFilter.TODAY -> {
            when (sliceOffset) {
                0 -> "Heute"
                1 -> "Gestern"
                else -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -sliceOffset) }
                    sdf.format(cal.time)
                }
            }
        }
        HistoryFilter.DAYS_7 -> {
            if (sliceOffset == 0) "Letzte 7 Tage"
            else "7 Tage (vor $sliceOffset Woche(n))"
        }
        HistoryFilter.DAYS_30 -> {
            if (sliceOffset == 0) "Letzte 30 Tage"
            else "30 Tage (vor $sliceOffset Monat(en))"
        }
        HistoryFilter.CUSTOM -> "${sdf.format(Date(customStart))} bis ${sdf.format(Date(customEnd))}"
    }
}
