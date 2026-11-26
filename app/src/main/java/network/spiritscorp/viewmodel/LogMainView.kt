package network.spiritscorp.viewmodel

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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.spiritscorp.R
import network.spiritscorp.data.InsulinRepository
import network.spiritscorp.model.CalculationLog

/**
 * ViewModel responsible for handling logbook calculation actions (saving, deleting, clearing logs)
 * using reactive calculator state and repository providers.
 *
 * @param application The Android application context used for string resources.
 * @param repository Provider lambda supplying the [InsulinRepository] instance or null if unavailable.
 * @param calculatorUiState Provider lambda supplying the current [CalculatorUiState].
 * @param onShowToast Callback lambda invoked to display transient toast notification messages.
 */
internal class LogMainView(
    application: Application,
//    todo: replace with dbviewmain.save
    private val repository: () -> InsulinRepository?,
    private val calculatorUiState: () -> CalculatorUiState,
    private val onShowToast: (String) -> Unit,
) : AndroidViewModel(application) {

    /**
     * Builds and asynchronously saves a calculation log entry into the logbook database via [InsulinRepository],
     * and notifies the user with a success toast message upon successful insertion.
     *
     * @param notesOverride Optional override string for meal notes.
     */
    internal fun saveCalculationToLog(notesOverride: String? = null) {
        val state = calculatorUiState()
        val summary = state.calculationSummary
        val autoMealTitle = state.mealTitle.ifBlank {
            "${application.getString(state.selectedTimeOfDay.titleResId)} (${summary.carbGrams()}g KH)"
        }
        val finalNotes = notesOverride ?: state.notes

        val log = CalculationLog(
            0L,
            System.currentTimeMillis(),
            autoMealTitle,
            state.carbInput.replace(',', '.').toDoubleOrNull() ?: 0.0,
            state.selectedUnit,
            summary.carbGrams(),
            summary.beValue(),
            summary.keValue(),
            state.selectedTimeOfDay,
            summary.factorUsed(),
            summary.mealInsulin(),
            summary.bloodGlucoseInput(),
            summary.targetGlucose(),
            state.correctionFactorInput.replace(',', '.').toDoubleOrNull(),
            summary.correctionInsulin(),
            summary.rawTotalInsulin(),
            summary.roundedTotalInsulin(),
            finalNotes
        )

        viewModelScope.launch(Dispatchers.IO) {
            val deferredResult: Long = repository()?.saveCalculation(log) ?: -1L
            if(deferredResult > 1){
                onShowToast(application.getString(R.string.view_model_diary_saved_toast))
            }
        }
    }

    /**
     * Asynchronously deletes a specific calculation log entry from the database by its primary key ID
     * and triggers a success toast message upon successful deletion.
     *
     * @param logId ID of the log entry to delete.
     */
    internal fun deleteLog(logId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val deferredResult: Boolean = repository()?.deleteLog(logId) ?: false
            if(deferredResult){
                onShowToast(application.getString(R.string.logbook_entry_deleted_toast))
            }
        }
    }

    /**
     * Asynchronously clears all calculation logs from the database
     * and triggers a success toast message upon successful clearance.
     */
    internal fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val deferredResult: Boolean = repository()?.clearLogs() ?: false
            if(deferredResult){
                onShowToast(application.getString(R.string.view_model_logbook_cleared))
            }
        }
    }
}
