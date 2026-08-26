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

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.UserSettings
import network.spiritscorp.ui.screens.settings.AboutAndLegalSection
import network.spiritscorp.ui.screens.settings.AppearanceSection
import network.spiritscorp.ui.screens.settings.BackupRestoreSection
import network.spiritscorp.ui.screens.settings.CarbUnitSection
import network.spiritscorp.ui.screens.settings.GlucoseCorrectionSection
import network.spiritscorp.ui.screens.settings.TherapyFactorsSection
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel

@Composable
fun SettingsScreen(
    viewModel: InsulinCalculatorViewModel,
    settings: UserSettings?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings = settings ?: UserSettings()

    var morningFactor by remember(settings) { mutableDoubleStateOf(currentSettings.morningFactor) }
    var noonFactor by remember(settings) { mutableDoubleStateOf(currentSettings.noonFactor) }
    var eveningFactor by remember(settings) { mutableDoubleStateOf(currentSettings.eveningFactor) }
    var nightFactor by remember(settings) { mutableDoubleStateOf(currentSettings.nightFactor) }

    var defaultCarbUnit by remember(settings) { mutableStateOf(currentSettings.defaultCarbUnit) }
    var beDivisor by remember(settings) { mutableIntStateOf(currentSettings.beGramsDivisor) }

    var glucoseUnit by remember(settings) {
        mutableStateOf(GlucoseUnit.fromString(currentSettings.glucoseUnit))
    }

    var targetGlucose by remember(settings, glucoseUnit) {
        val initialVal = if (glucoseUnit == GlucoseUnit.MMOL_L) {
            String.format(java.util.Locale.getDefault(), "%.1f", GlucoseUnit.MMOL_L.fromMgDl(currentSettings.targetGlucoseMgDl))
        } else {
            currentSettings.targetGlucoseMgDl.toInt().toString()
        }
        mutableStateOf(initialVal)
    }

    var correctionFactor by remember(settings, glucoseUnit) {
        val initialVal = if (glucoseUnit == GlucoseUnit.MMOL_L) {
            String.format(java.util.Locale.getDefault(), "%.1f", GlucoseUnit.MMOL_L.fromMgDl(currentSettings.correctionFactorMgDl))
        } else {
            currentSettings.correctionFactorMgDl.toInt().toString()
        }
        mutableStateOf(initialVal)
    }

    var roundingStep by remember(settings) { mutableDoubleStateOf(currentSettings.roundingStep) }
    var selectedThemeName by remember(settings) { mutableStateOf(currentSettings.selectedTheme) }
    var themeMode by remember(settings) { mutableStateOf(currentSettings.themeMode) }

    var showResetDbDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: Mahlzeiten-Faktoren & Rundung
        TherapyFactorsSection(
            morningFactor = morningFactor,
            onMorningFactorChange = { morningFactor = it },
            noonFactor = noonFactor,
            onNoonFactorChange = { noonFactor = it },
            eveningFactor = eveningFactor,
            onEveningFactorChange = { eveningFactor = it },
            nightFactor = nightFactor,
            onNightFactorChange = { nightFactor = it },
            roundingStep = roundingStep,
            onRoundingStepChange = { roundingStep = it }
        )

        // SECTION 2: Blutzucker & Korrektur
        GlucoseCorrectionSection(
            glucoseUnit = glucoseUnit,
            onGlucoseUnitChange = { newUnit ->
                if (newUnit != glucoseUnit) {
                    val currentTarget = targetGlucose.toDoubleOrNull() ?: 120.0
                    val currentCorr = correctionFactor.toDoubleOrNull() ?: 50.0
                    if (newUnit == GlucoseUnit.MMOL_L) {
                        targetGlucose = String.format(java.util.Locale.getDefault(), "%.1f", GlucoseUnit.MMOL_L.fromMgDl(currentTarget))
                        correctionFactor = String.format(java.util.Locale.getDefault(), "%.1f", GlucoseUnit.MMOL_L.fromMgDl(currentCorr))
                    } else {
                        targetGlucose = GlucoseUnit.MMOL_L.toMgDl(currentTarget).toInt().toString()
                        correctionFactor = GlucoseUnit.MMOL_L.toMgDl(currentCorr).toInt().toString()
                    }
                    glucoseUnit = newUnit
                }
            },
            targetGlucose = targetGlucose,
            onTargetGlucoseChange = { targetGlucose = it },
            correctionFactor = correctionFactor,
            onCorrectionFactorChange = { correctionFactor = it }
        )

        // SECTION 3: Kohlenhydrat-Einheit
        CarbUnitSection(
            defaultCarbUnit = defaultCarbUnit,
            onDefaultCarbUnitChange = { defaultCarbUnit = it },
            beDivisor = beDivisor,
            onBeDivisorChange = { beDivisor = it }
        )

        // SECTION 4: Farbdesign & Erscheinungsbild
        AppearanceSection(
            selectedThemeName = selectedThemeName,
            onThemeSelected = { selectedThemeName = it },
            themeMode = themeMode,
            onThemeModeSelected = { themeMode = it }
        )

        // SECTION 5: Datensicherung & Backup
        BackupRestoreSection(
            viewModel = viewModel,
            onShowResetDbDialog = { showResetDbDialog = true }
        )

        // SAVE ACTION BUTTON
        Button(
            onClick = {
                val targetNum = targetGlucose.toDoubleOrNull() ?: (if (glucoseUnit == GlucoseUnit.MMOL_L) 6.7 else 120.0)
                val corrNum = correctionFactor.toDoubleOrNull() ?: (if (glucoseUnit == GlucoseUnit.MMOL_L) 2.8 else 50.0)

                val targetMgDl = if (glucoseUnit == GlucoseUnit.MMOL_L) GlucoseUnit.MMOL_L.toMgDl(targetNum) else targetNum
                val corrMgDl = if (glucoseUnit == GlucoseUnit.MMOL_L) GlucoseUnit.MMOL_L.toMgDl(corrNum) else corrNum

                val updated = currentSettings.copy(
                    morningFactor = morningFactor,
                    noonFactor = noonFactor,
                    eveningFactor = eveningFactor,
                    nightFactor = nightFactor,
                    defaultCarbUnit = defaultCarbUnit,
                    beGramsDivisor = beDivisor,
                    glucoseUnit = glucoseUnit.shortName,
                    targetGlucoseMgDl = targetMgDl,
                    correctionFactorMgDl = corrMgDl,
                    roundingStep = roundingStep,
                    selectedTheme = selectedThemeName,
                    themeMode = themeMode
                )
                viewModel.updateUserSettings(updated)
                Toast.makeText(context, "Einstellungen gespeichert", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_settings_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Einstellungen speichern",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        // SECTION 6: Medizinischer Hinweis & Version
        AboutAndLegalSection()

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showResetDbDialog) {
        AlertDialog(
            onDismissRequest = { showResetDbDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Komplette Datenbank löschen?") },
            text = {
                Text("Bist du sicher? Dies löscht alle gespeicherten Tagebucheinträge und setzt die Einstellungen zurück. Es wird empfohlen, vorher ein JSON- oder CSV-Backup zu erstellen.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllLogs()
                        showResetDbDialog = false
                        Toast.makeText(context, "Tagebuch & Daten wurden gelöscht", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_reset_all_data_button")
                ) {
                    Text("Unwiderruflich löschen", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDbDialog = false },
                    modifier = Modifier.testTag("cancel_reset_all_data_button")
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
