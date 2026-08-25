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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import network.spiritscorp.data.DatabaseBackupManager
import network.spiritscorp.model.GlucoseUnit
import network.spiritscorp.model.UserSettings
import network.spiritscorp.ui.theme.AppTheme
import network.spiritscorp.ui.theme.EveningColor
import network.spiritscorp.ui.theme.MorningColor
import network.spiritscorp.ui.theme.NightColor
import network.spiritscorp.ui.theme.NoonColor
import network.spiritscorp.viewmodel.InsulinCalculatorViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: InsulinCalculatorViewModel,
    settings: UserSettings?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val currentSettings = settings ?: UserSettings()

    // Factor states
    var morningFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.morningFactor) }
    var noonFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.noonFactor) }
    var eveningFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.eveningFactor) }
    var nightFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.nightFactor) }

    // Unit states
    var glucoseUnit by remember(currentSettings) {
        mutableStateOf(if (currentSettings.glucoseUnit.lowercase().contains("mmol")) GlucoseUnit.MMOL_L else GlucoseUnit.MG_DL)
    }
    var defaultCarbUnit by remember(currentSettings) { mutableStateOf(currentSettings.defaultCarbUnit) }
    var beDivisor by remember(currentSettings) { mutableIntStateOf(currentSettings.beGramsDivisor) }

    // BG & Correction Target values
    var targetGlucose by remember(currentSettings, glucoseUnit) {
        mutableStateOf(
            if (glucoseUnit == GlucoseUnit.MMOL_L) {
                val mmol = GlucoseUnit.MMOL_L.fromMgDl(currentSettings.targetGlucoseMgDl)
                if (mmol % 1.0 == 0.0) mmol.toInt().toString() else String.format(Locale.US, "%.1f", mmol)
            } else {
                currentSettings.targetGlucoseMgDl.toInt().toString()
            }
        )
    }

    var correctionFactor by remember(currentSettings, glucoseUnit) {
        mutableStateOf(
            if (glucoseUnit == GlucoseUnit.MMOL_L) {
                val mmol = GlucoseUnit.MMOL_L.fromMgDl(currentSettings.correctionFactorMgDl)
                if (mmol % 1.0 == 0.0) mmol.toInt().toString() else String.format(Locale.US, "%.1f", mmol)
            } else {
                currentSettings.correctionFactorMgDl.toInt().toString()
            }
        )
    }

    // Appearance & Rounding
    var selectedThemeName by remember(currentSettings) { mutableStateOf(currentSettings.selectedTheme) }
    var themeMode by remember(currentSettings) { mutableStateOf(currentSettings.themeMode) }
    var roundingStep by remember(currentSettings) { mutableDoubleStateOf(currentSettings.roundingStep) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SECTION 1: Tageszeit-Faktoren
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_daytime_factors_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSectionHeader(
                    icon = Icons.Default.Tune,
                    title = "1. Tageszeit-Faktoren",
                    subtitle = "Insulin-Faktoren pro KE (10g KH) in 0,05er Schritten"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                FactorRow(
                    title = "Morgens (Frühstück)",
                    subtitle = "06:00 - 11:00 Uhr (oft erhöht)",
                    icon = Icons.Default.Brightness5,
                    iconColor = MorningColor,
                    factor = morningFactor,
                    onFactorChange = { morningFactor = it },
                    tagPrefix = "morning"
                )

                Spacer(modifier = Modifier.height(8.dp))

                FactorRow(
                    title = "Mittags (Mittagessen)",
                    subtitle = "11:00 - 17:00 Uhr",
                    icon = Icons.Default.WbSunny,
                    iconColor = NoonColor,
                    factor = noonFactor,
                    onFactorChange = { noonFactor = it },
                    tagPrefix = "noon"
                )

                Spacer(modifier = Modifier.height(8.dp))

                FactorRow(
                    title = "Abends (Abendessen)",
                    subtitle = "17:00 - 22:00 Uhr",
                    icon = Icons.Default.WbTwilight,
                    iconColor = EveningColor,
                    factor = eveningFactor,
                    onFactorChange = { eveningFactor = it },
                    tagPrefix = "evening"
                )

                Spacer(modifier = Modifier.height(8.dp))

                FactorRow(
                    title = "Nachts (Spätmahlzeit)",
                    subtitle = "22:00 - 06:00 Uhr",
                    icon = Icons.Default.Bedtime,
                    iconColor = NightColor,
                    factor = nightFactor,
                    onFactorChange = { nightFactor = it },
                    tagPrefix = "night"
                )
            }
        }

        // SECTION 2: Blutzucker-Einheit & Korrektur-Parameter
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_bg_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSectionHeader(
                    icon = Icons.Default.Opacity,
                    title = "2. Blutzucker & Korrektur",
                    subtitle = "Einheit (mg/dl vs. mmol/l), Zielwert und Korrekturfaktor"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                // Glucose Unit Switcher (Segmented Control)
                Text(
                    text = "Blutzucker-Messeinheit:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMgDl = glucoseUnit == GlucoseUnit.MG_DL
                    val isMmolL = glucoseUnit == GlucoseUnit.MMOL_L

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (glucoseUnit != GlucoseUnit.MG_DL) {
                                    // Convert current values to mg/dl
                                    val targetVal = targetGlucose.toDoubleOrNull() ?: 6.7
                                    val corrVal = correctionFactor.toDoubleOrNull() ?: 2.8
                                    targetGlucose = (GlucoseUnit.MMOL_L.toMgDl(targetVal)).toInt().toString()
                                    correctionFactor = (GlucoseUnit.MMOL_L.toMgDl(corrVal)).toInt().toString()
                                    glucoseUnit = GlucoseUnit.MG_DL
                                }
                            }
                            .testTag("settings_glucose_unit_mgdl"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMgDl) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isMgDl) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "mg/dl",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isMgDl) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Milligramm / Deziliter",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = if (isMgDl) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (glucoseUnit != GlucoseUnit.MMOL_L) {
                                    // Convert current values to mmol/l
                                    val targetVal = targetGlucose.toDoubleOrNull() ?: 120.0
                                    val corrVal = correctionFactor.toDoubleOrNull() ?: 50.0
                                    val targetMmol = GlucoseUnit.MMOL_L.fromMgDl(targetVal)
                                    val corrMmol = GlucoseUnit.MMOL_L.fromMgDl(corrVal)
                                    targetGlucose = String.format(Locale.US, "%.1f", targetMmol)
                                    correctionFactor = String.format(Locale.US, "%.1f", corrMmol)
                                    glucoseUnit = GlucoseUnit.MMOL_L
                                }
                            }
                            .testTag("settings_glucose_unit_mmoll"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMmolL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isMmolL) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "mmol/l",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isMmolL) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Millimol / Liter",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                color = if (isMmolL) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target and Correction inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetGlucose,
                        onValueChange = { targetGlucose = it.replace(',', '.') },
                        label = { Text("Ziel-BZ (${glucoseUnit.shortName})") },
                        placeholder = { Text(if (glucoseUnit == GlucoseUnit.MMOL_L) "6.7" else "120") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_target_glucose_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = correctionFactor,
                        onValueChange = { correctionFactor = it.replace(',', '.') },
                        label = { Text("Korrektur (${glucoseUnit.shortName})") },
                        placeholder = { Text(if (glucoseUnit == GlucoseUnit.MMOL_L) "2.8" else "50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_correction_factor_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Text(
                    text = "Korrekturfaktor = um wie viel ${glucoseUnit.shortName} 1 IE Insulin deinen Blutzucker senkt.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // SECTION 3: Kohlenhydrat-Einheiten & Rundung
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_default_unit_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSectionHeader(
                    icon = Icons.Default.Scale,
                    title = "3. Kohlenhydrate & Rundung",
                    subtitle = "Standard-Einheit, BE-Divisor und Dosierungs-Rundung"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Standard-Einheit im Rechner:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val unitOptions = listOf(
                    "GRAMS" to "Gramm Kohlenhydrate (g KH)",
                    "BE" to "Broteinheit (BE)",
                    "KE" to "Kohlenhydrateinheit (KE / 10g)"
                )

                unitOptions.forEach { (unitKey, label) ->
                    val isSelected = defaultCarbUnit == unitKey
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { defaultCarbUnit = unitKey },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { defaultCarbUnit = unitKey }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Broteinheit (BE) Divisor:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val divisors = listOf(12, 10)
                    divisors.forEach { div ->
                        val isDivSelected = beDivisor == div
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { beDivisor = div },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDivSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isDivSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null
                        ) {
                            Text(
                                text = "$div Gramm (1 BE = ${div}g)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isDivSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isDivSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Rundung der Insulindosis:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val roundingOptions = listOf(
                    0.5 to "Halbe Einheiten (0.5 IE) – Standard für Pen",
                    0.1 to "Zehntel Einheiten (0.1 IE) – für Insulinpumpe",
                    1.0 to "Ganze Einheiten (1.0 IE)"
                )

                roundingOptions.forEach { (step, label) ->
                    val isStepSelected = roundingStep == step
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { roundingStep = step }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isStepSelected,
                            onClick = { roundingStep = step }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isStepSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // SECTION 4: Farbdesign & Erscheinungsbild
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_theme_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSectionHeader(
                    icon = Icons.Default.Palette,
                    title = "4. Farbdesign & Design",
                    subtitle = "Akzentfarben und Hell-/Dunkel-Modus"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Farbschema:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppTheme.entries.forEach { appTheme ->
                        val isSelected = selectedThemeName == appTheme.name
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedThemeName = appTheme.name }
                                .testTag("theme_chip_${appTheme.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(appTheme.previewColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appTheme.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Erscheinungsbild:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        "SYSTEM" to "System",
                        "LIGHT" to "Hell",
                        "DARK" to "Dunkel"
                    )

                    modes.forEach { (modeKey, modeTitle) ->
                        val isModeSelected = themeMode == modeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { themeMode = modeKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isModeSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (isModeSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null
                        ) {
                            Text(
                                text = modeTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isModeSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // SECTION 5: Datensicherung & Backup
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSectionHeader(
                    icon = Icons.Default.Save,
                    title = "5. Datensicherung & Backup",
                    subtitle = "Sichere deine Daten lokal oder importiere Backups"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(12.dp))

                val context = LocalContext.current

                val jsonExportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        viewModel.viewModelScope.launch {
                            val json = DatabaseBackupManager.exportToJson(context)
                            val success = DatabaseBackupManager.writeTextToUri(context, uri, json)
                            if (success) {
                                Toast.makeText(context, "JSON-Backupdatei erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Fehler beim Speichern der JSON-Datei.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                val csvExportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    if (uri != null) {
                        viewModel.viewModelScope.launch {
                            val allLogs = viewModel.getAllLogsDirect()
                            val csv = DatabaseBackupManager.exportToCsv(allLogs)
                            val success = DatabaseBackupManager.writeTextToUri(context, uri, csv)
                            if (success) {
                                Toast.makeText(context, "Tagebuch (${allLogs.size} Einträge) erfolgreich als CSV gespeichert!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Fehler beim Speichern der CSV-Datei.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                val fileImportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        viewModel.viewModelScope.launch {
                            val result = DatabaseBackupManager.importFromUri(context, uri)
                            if (result.success) {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Import fehlgeschlagen: ${result.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("JSON Export")
                    }

                    Button(
                        onClick = {
                            val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV Export")
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
            }
        }

        // SAVE BUTTON (Prominent Primary Action)
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Wichtiger medizinischer Hinweis",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Diese App ist eine Berechnungshilfe und ersetzt keine ärztliche Beratung. Faktoren unterliegen Schwankungen durch Bewegung, Krankheit, Stress oder Hormone. Passe deine Dosis im Zweifel mit deinem behandelnden Diabetesteam an.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Footer Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "insulin_calc V1.1.1 • by Tom Spirit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FactorRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    factor: Double,
    onFactorChange: (Double) -> Unit,
    tagPrefix: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val newF = BigDecimal(factor - 0.05).setScale(2, RoundingMode.HALF_UP).toDouble().coerceAtLeast(0.05)
                        onFactorChange(newF)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("decrease_${tagPrefix}_factor"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(text = "-0,05", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1.2f).height(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${String.format(Locale.GERMAN, "%.2f", factor)} IE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = {
                        val newF = BigDecimal(factor + 0.05).setScale(2, RoundingMode.HALF_UP).toDouble().coerceAtMost(10.0)
                        onFactorChange(newF)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("increase_${tagPrefix}_factor"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(text = "+0,05", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
