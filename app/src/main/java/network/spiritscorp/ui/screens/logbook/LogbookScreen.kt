package network.spiritscorp.ui.screens.logbook

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
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.spiritscorp.R
import network.spiritscorp.model.CalculationLog
import network.spiritscorp.util.DateTimeUtils
import network.spiritscorp.util.LogbookExportHelper
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import java.util.Calendar

/**
 * Filter options for the calculation history.
 *
 * @param titleRes Resource ID pointing to the localized display string.
 *                 This decouples the enum from Android Context while guaranteeing full i18n support.
 */
enum class HistoryFilter(@StringRes val titleRes: Int) {
    ALL(R.string.history_filter_all),
    TODAY(R.string.history_filter_today),
    DAYS_7(R.string.history_filter_7_days),
    DAYS_30(R.string.history_filter_30_days),
    CUSTOM(R.string.history_filter_custom);

    /**
     * Backward-compatible property or helper to retrieve the translated label via a Composable.
     */
    val title: String
        @Composable
        get() = stringResource(titleRes)

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
    var selectedFilter by remember { mutableStateOf(HistoryFilter.TODAY) }
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

    val exportHelper = remember(context) { LogbookExportHelper(context) }

    val filterDescription by remember(selectedFilter, sliceOffset, customStartDateMillis, customEndDateMillis) {
        derivedStateOf {
            getFilterDescription( context,selectedFilter, sliceOffset, customStartDateMillis, customEndDateMillis)
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
                        text = stringResource(R.string.logbook_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.logbook_entry_count, filteredLogs.size, logs.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val shareChooserTitle = stringResource(R.string.logbook_share_chooser_title, filterDescription)
                    IconButton(
                        onClick = {
                            val exportText = exportHelper.generateExportText(filteredLogs, filterDescription, viewModel.userSettings.value.glucoseUnit)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, exportText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser( sendIntent, shareChooserTitle )
                            context.startActivity(shareIntent)
                        },
                        enabled = filteredLogs.isNotEmpty(),
                        modifier = Modifier.testTag("share_logbook_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.logbook_share_content_description),
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
                            contentDescription = stringResource(R.string.logbook_cleared_toast),
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
                            text = if (logs.isEmpty()) {
                                stringResource(R.string.logbook_empty_title_no_logs)
                            } else {
                                stringResource(R.string.logbook_empty_title_filtered)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (logs.isEmpty()) {
                                stringResource(R.string.logbook_empty_desc_no_logs)
                            } else {
                                stringResource(R.string.logbook_empty_desc_filtered)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(items = filteredLogs, key = { it.id }) { log ->
                val shareSingleTitle = stringResource(R.string.logbook_share_single_title, log.mealTitle)
                LogbookItemCard(
                    log = log,
                    onShareRequest = {
                        val shareText = exportHelper.formatSingleLogShare(log, viewModel.userSettings.value.glucoseUnit)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(
                            sendIntent,
                            shareSingleTitle
                        )
                        context.startActivity(shareIntent)
                    },
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
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}
private fun getFilterDescription(
    context: Context,
    filter: HistoryFilter,
    sliceOffset: Int,
    customStart: Long,
    customEnd: Long
): String {
    val sdf = DateTimeUtils.getDisplayDateFormatter()
    return when (filter) {
        HistoryFilter.ALL -> context.getString(R.string.logbook_filter_desc_all)
        HistoryFilter.TODAY -> {
            when (sliceOffset) {
                0 -> context.getString(R.string.logbook_filter_desc_today)
                1 -> context.getString(R.string.logbook_filter_desc_yesterday)
                else -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -sliceOffset) }
                    sdf.format(cal.time)
                }
            }
        }
        HistoryFilter.DAYS_7 -> {
            if (sliceOffset == 0) context.getString(R.string.logbook_filter_desc_last_7_days)
            else context.getString(R.string.logbook_filter_desc_7_days_offset, sliceOffset)
        }
        HistoryFilter.DAYS_30 -> {
            if (sliceOffset == 0) context.getString(R.string.logbook_filter_desc_last_30_days)
            else context.getString(R.string.logbook_filter_desc_30_days_offset, sliceOffset)
        }
        HistoryFilter.CUSTOM -> context.getString(
            R.string.logbook_filter_desc_custom_range,
            DateTimeUtils.formatDisplayDate(customStart),
            DateTimeUtils.formatDisplayDate(customEnd)
        )
    }
}
