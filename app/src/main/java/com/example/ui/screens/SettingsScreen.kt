package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserSettings
import com.example.ui.theme.AppTheme
import com.example.ui.theme.EveningColor
import com.example.ui.theme.MorningColor
import com.example.ui.theme.NightColor
import com.example.ui.theme.NoonColor
import com.example.viewmodel.InsulinCalculatorViewModel
import java.math.BigDecimal
import java.math.RoundingMode
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

    var morningFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.morningFactor) }
    var noonFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.noonFactor) }
    var eveningFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.eveningFactor) }
    var nightFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.nightFactor) }

    var defaultCarbUnit by remember(currentSettings) { mutableStateOf(currentSettings.defaultCarbUnit) }
    var selectedThemeName by remember(currentSettings) { mutableStateOf(currentSettings.selectedTheme) }
    var themeMode by remember(currentSettings) { mutableStateOf(currentSettings.themeMode) }

    var targetGlucose by remember(currentSettings) { mutableStateOf(currentSettings.targetGlucoseMgDl.toInt().toString()) }
    var correctionFactor by remember(currentSettings) { mutableStateOf(currentSettings.correctionFactorMgDl.toInt().toString()) }
    var roundingStep by remember(currentSettings) { mutableDoubleStateOf(currentSettings.roundingStep) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daytime Factors Card (with 0.05 step resolution)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_daytime_factors_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tageszeit-Faktoren (Schritte in 0,05 IE)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Stelle deine individuellen Faktoren präzise in 0,05er Schritten ein (z.B. 0,45 • 0,50 • 0,55 • 1,25 IE / KE).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                // Morning
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

                // Noon
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

                // Evening
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

                // Night
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

        // Standard Unit Card (Default = BE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_default_unit_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Standard-Einheit für Kohlenhydrate",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val unitOptions = listOf(
                    "BE" to "BE (Broteinheit = 12g KH) – Standard",
                    "KE" to "KE / KHE (Kohlenhydrateinheit = 10g KH)",
                    "g KH" to "Gramm Kohlenhydrate (g KH)"
                )

                unitOptions.forEach { (unitKey, label) ->
                    val isSelected = defaultCarbUnit == unitKey
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
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
            }
        }

        // Color Themes & Appearance Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_theme_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Farbdesign & Themes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Wähle deinen bevorzugten Farbton für die Benutzeroberfläche.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

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
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                // Dark/Light Mode Selection
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

        // Blood Glucose Target & Correction Settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_bg_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Blutzucker-Korrektur Parameter",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetGlucose,
                        onValueChange = { targetGlucose = it },
                        label = { Text("Ziel-BZ (mg/dl)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_target_glucose_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = correctionFactor,
                        onValueChange = { correctionFactor = it },
                        label = { Text("Korrekturfaktor") },
                        placeholder = { Text("z.B. 40") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_correction_factor_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
                Text(
                    text = "Korrekturfaktor = um wie viel mg/dl 1 IE Insulin den Blutzucker senkt (z.B. 40 mg/dl).",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Rounding Preference
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Rundung der Insulindosis",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val roundingOptions = listOf(
                    0.5 to "Halbe Einheiten (0.5 IE) – Standard für Pen",
                    0.1 to "Zehntel Einheiten (0.1 IE) – für Insulinpumpe",
                    1.0 to "Ganze Einheiten (1.0 IE)"
                )

                roundingOptions.forEach { (step, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = roundingStep == step,
                            onClick = { roundingStep = step }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                val updated = currentSettings.copy(
                    morningFactor = morningFactor,
                    noonFactor = noonFactor,
                    eveningFactor = eveningFactor,
                    nightFactor = nightFactor,
                    defaultCarbUnit = defaultCarbUnit,
                    selectedTheme = selectedThemeName,
                    themeMode = themeMode,
                    targetGlucoseMgDl = targetGlucose.toDoubleOrNull() ?: 100.0,
                    correctionFactorMgDl = correctionFactor.toDoubleOrNull() ?: 40.0,
                    roundingStep = roundingStep
                )
                viewModel.updateUserSettings(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_settings_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Einstellungen & Design speichern",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Medical Safety & Professional Advice Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
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
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        val newF = BigDecimal(factor - 0.05).setScale(2, RoundingMode.HALF_UP).toDouble().coerceAtLeast(0.05)
                        onFactorChange(newF)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .testTag("decrease_${tagPrefix}_factor")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "-0.05",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = String.format(Locale.GERMAN, "%.2f", factor),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val newF = BigDecimal(factor + 0.05).setScale(2, RoundingMode.HALF_UP).toDouble().coerceAtMost(10.0)
                        onFactorChange(newF)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .testTag("increase_${tagPrefix}_factor")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "+0.05",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

