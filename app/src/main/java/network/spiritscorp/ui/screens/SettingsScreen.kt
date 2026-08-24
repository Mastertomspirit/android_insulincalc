package network.spiritscorp.ui.screens

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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.collectAsState
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

    var morningFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.morningFactor) }
    var noonFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.noonFactor) }
    var eveningFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.eveningFactor) }
    var nightFactor by remember(currentSettings) { mutableDoubleStateOf(currentSettings.nightFactor) }

    var defaultCarbUnit by remember(currentSettings) { mutableStateOf(currentSettings.defaultCarbUnit) }
    var beDivisor by remember(currentSettings) { mutableIntStateOf(currentSettings.beGramsDivisor) }
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
                    "GRAMS" to "Gramm Kohlenhydrate (g KH) – Standard",
                    "BE" to "BE (Broteinheit)",
                    "KE" to "KE / KHE (Kohlenhydrateinheit = 10g KH)"
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

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Broteinheit (BE) Divisor:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Wie viel Gramm Kohlenhydrate entsprechen 1 BE?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Database Backup & Export Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Datenbank & Backup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Speichere deine Daten als JSON-Backupdatei oder als CSV-Tabelle auf deinem Gerät bzw. importiere bestehende Backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                val context = LocalContext.current
                val logs by viewModel.historyLogs.collectAsState(initial = emptyList())

                // Activity Result Launchers for file operations
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
        Button(
            onClick = {
                val updated = currentSettings.copy(
                    morningFactor = morningFactor,
                    noonFactor = noonFactor,
                    eveningFactor = eveningFactor,
                    nightFactor = nightFactor,
                    defaultCarbUnit = defaultCarbUnit,
                    beGramsDivisor = beDivisor,
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
        // Footer Info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally // Zentriert alles darin
            ) {
                Text(
                    text = "Insulincalc V1.0.2     by tomSpirit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        .height(44.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1.2f).height(44.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        val newF = BigDecimal(factor + 0.05).setScale(2, RoundingMode.HALF_UP).toDouble().coerceAtMost(10.0)
                        onFactorChange(newF)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
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

