package network.spiritscorp.viewmodel;

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

import android.app.Application;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import kotlinx.coroutines.flow.FlowKt;
import network.spiritscorp.R;
import network.spiritscorp.ai.GeminiAiModel;
import network.spiritscorp.data.CalculationLogDao;
import network.spiritscorp.data.InsulinRepository;
import network.spiritscorp.data.UserSettingsDao;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.CalculationSummary;
import network.spiritscorp.model.GlucoseUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;

import network.spiritscorp.ui.theme.AppTheme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit & integration tests for {@link InsulinCalculatorViewModel} and its reactive UI state engine,
 * testing the real ViewModel methods via Robolectric with Application context.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CalculatorEngineStateTest {

    private static final double DELTA = 0.001;

    private InsulinCalculatorViewModel viewModel;
    private Application app;

    @Before
    public void setup() {
        app = ApplicationProvider.getApplicationContext();
        CalculationLogDao mockLogDao = mock(CalculationLogDao.class);
        UserSettingsDao mockSettingsDao = mock(UserSettingsDao.class);
        when(mockSettingsDao.getSettingsDirect()).thenReturn(null);
        when(mockSettingsDao.getSettings()).thenReturn(FlowKt.emptyFlow());
        when(mockLogDao.getAllLogs()).thenReturn(FlowKt.emptyFlow());

        InsulinRepository mockRepo = new InsulinRepository(mockLogDao, mockSettingsDao);
        viewModel = new InsulinCalculatorViewModel(app, mockRepo);
    }

    @Test
    public void testInitialStateNotNull() {
        CalculatorUiState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertNotNull(state.getCalculationSummary());
        assertEquals("", state.getCarbInput());
        assertEquals(CarbUnit.GRAMS, state.getSelectedUnit());
    }

    @Test
    public void testMorningCalculationWithGrams() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_PRO
        );
        viewModel.updateUserSettings(settings);

        viewModel.setUnit(CarbUnit.GRAMS);
        viewModel.selectTimeOfDay(TimeOfDay.MORNING);
        viewModel.onCarbInputChange("60");

        CalculatorUiState state = viewModel.getUiState().getValue();
        CalculationSummary summary = state.getCalculationSummary();

        // 60g KH / 12 = 5 BE. 5 BE * 1.5 = 7.5 IE
        assertEquals(60.0, summary.carbGrams(), DELTA);
        assertEquals(5.0, summary.beValue(), DELTA);
        assertEquals(6.0, summary.keValue(), DELTA);
        assertEquals(1.50, summary.factorUsed(), DELTA);
        assertEquals(7.50, summary.mealInsulin(), DELTA);
        assertNull(summary.bloodGlucoseInput());
        assertEquals(Double.valueOf(120), summary.targetGlucose());
        assertEquals(0.00, summary.correctionInsulin(), DELTA);
        assertEquals(7.50, summary.rawTotalInsulin(), DELTA);
        assertEquals(7.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
        assertEquals(app.getString(R.string.view_model_advisory_standard), summary.advisoryNote());
    }

    @Test
    public void testNoonCalculationWithBEAndFactorOverride() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_PRO
        );
        viewModel.updateUserSettings(settings);

        viewModel.setUnit(CarbUnit.BE);
        viewModel.selectTimeOfDay(TimeOfDay.NOON);
        viewModel.onCarbInputChange("4.5");

        // User overrides factor from 1.0 to 1.30 by adjusting delta +0.30
        viewModel.adjustFactor(0.30);

        CalculatorUiState state = viewModel.getUiState().getValue();
        CalculationSummary summary = state.getCalculationSummary();

        // 4.5 BE * 12 = 54.0g KH. 4.5 BE * 1.30 = 5.85 IE -> Rounded to step 0.5 = 6.0 IE
        assertEquals(54.0, summary.carbGrams(), DELTA);
        assertEquals(4.5, summary.beValue(), DELTA);
        assertEquals(5.4, summary.keValue(), DELTA);
        assertEquals(1.30, summary.factorUsed(), DELTA);
        assertEquals(5.85, summary.mealInsulin(), DELTA);
        assertNull(summary.bloodGlucoseInput());
        assertEquals(Double.valueOf(120), summary.targetGlucose());
        assertEquals(0.00, summary.correctionInsulin(), DELTA);
        assertEquals(5.85, summary.rawTotalInsulin(), DELTA);
        assertEquals(6.0, summary.roundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
    }

    @Test
    public void testEveningCalculationWithHighGlucoseCorrection() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_PRO
        );
        viewModel.updateUserSettings(settings);

        viewModel.setUnit(CarbUnit.BE);
        viewModel.selectTimeOfDay(TimeOfDay.EVENING);
        viewModel.onCarbInputChange("3.0");

        viewModel.toggleCorrection();
        viewModel.onGlucoseInputChange("220");
        viewModel.onTargetGlucoseChange("100");
        viewModel.onCorrectionFactorChange("40");

        CalculatorUiState state = viewModel.getUiState().getValue();
        CalculationSummary summary = state.getCalculationSummary();

        // 3.0 BE (36g KH) -> 3.0 * 1.2 = 3.6 IE
        // Current BG 220, Target 100, CorrFactor 40 -> (220-100)/40 = +3.0 IE correction
        // Total = 3.6 + 3.0 = 6.6 IE -> Rounded to step 0.5 = 6.5 IE
        assertEquals(36.0, summary.carbGrams(), DELTA);
        assertEquals(3.0, summary.beValue(), DELTA);
        assertEquals(3.6, summary.keValue(), DELTA);
        assertEquals(1.20, summary.factorUsed(), DELTA);
        assertEquals(3.60, summary.mealInsulin(), DELTA);
        assertEquals(220.0, summary.bloodGlucoseInput(), DELTA);
        assertEquals(100.0, summary.targetGlucose(), DELTA);
        assertEquals(3.00, summary.correctionInsulin(), DELTA);
        assertEquals(6.60, summary.rawTotalInsulin(), DELTA);
        assertEquals(6.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
        assertEquals(app.getString(R.string.view_model_advisory_above_target), summary.advisoryNote());
    }

    @Test
    public void testLowBloodGlucoseWarningAndDoseReduction() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_PRO
        );
        viewModel.updateUserSettings(settings);

        viewModel.setUnit(CarbUnit.GRAMS);
        viewModel.selectTimeOfDay(TimeOfDay.NIGHT);
        viewModel.onCarbInputChange("30");

        viewModel.toggleCorrection();
        viewModel.onGlucoseInputChange("65");
        viewModel.onTargetGlucoseChange("100");
        viewModel.onCorrectionFactorChange("40");

        CalculatorUiState state = viewModel.getUiState().getValue();
        CalculationSummary summary = state.getCalculationSummary();

        // 30g KH / 12 = 2.5 BE. 2.5 BE * 0.8 (night factor) = 2.0 IE meal insulin
        // Current BG 65, Target 100, CorrFactor 40 -> (65 - 100) / 40 = -0.875 IE correction
        // Total = 2.0 - 0.875 = 1.125 IE -> Rounded = 1.0 IE
        assertEquals(30.0, summary.carbGrams(), DELTA);
        assertEquals(2.5, summary.beValue(), DELTA);
        assertEquals(3.0, summary.keValue(), DELTA);
        assertEquals(0.80, summary.factorUsed(), DELTA);
        assertEquals(2.0, summary.mealInsulin(), DELTA);
        assertEquals(65.0, summary.bloodGlucoseInput(), DELTA);
        assertEquals(100.0, summary.targetGlucose(), DELTA);
        assertEquals(-0.88, summary.correctionInsulin(), DELTA);
        assertEquals(1.13, summary.rawTotalInsulin(), DELTA);
        assertEquals(1.0, summary.roundedTotalInsulin(), DELTA);
        assertTrue(summary.isHypoRisk());
        assertEquals(app.getString(R.string.view_model_advisory_hypo, "70 " + app.getString(GlucoseUnit.MG_DL.getShortNameResId())), summary.advisoryNote());
    }

    @Test
    public void testNegativeCorrectionDoseReduction() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_PRO
        );
        viewModel.updateUserSettings(settings);

        viewModel.setUnit(CarbUnit.BE);
        viewModel.selectTimeOfDay(TimeOfDay.NOON);
        viewModel.onCarbInputChange("2.0");

        viewModel.toggleCorrection();
        viewModel.onGlucoseInputChange("80");
        viewModel.onTargetGlucoseChange("100");
        viewModel.onCorrectionFactorChange("40");

        CalculatorUiState state = viewModel.getUiState().getValue();
        CalculationSummary summary = state.getCalculationSummary();

        // 2 BE = 2.0 IE meal insulin. BG = 80, Target = 100, Corr = 40
        // Correction = -(20 / 40) = -0.5 IE
        // Total = 2.0 - 0.5 = 1.5 IE
        assertEquals(24.0, summary.carbGrams(), DELTA);
        assertEquals(2.0, summary.beValue(), DELTA);
        assertEquals(2.4, summary.keValue(), DELTA);
        assertEquals(1.00, summary.factorUsed(), DELTA);
        assertEquals(2.0, summary.mealInsulin(), DELTA);
        assertEquals(80.0, summary.bloodGlucoseInput(), DELTA);
        assertEquals(100.0, summary.targetGlucose(), DELTA);
        assertEquals(-0.50, summary.correctionInsulin(), DELTA);
        assertEquals(1.50, summary.rawTotalInsulin(), DELTA);
        assertEquals(1.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
        assertEquals(app.getString(R.string.view_model_advisory_below_target), summary.advisoryNote());
    }

    @Test
    public void testMmolLCorrectionCalculation() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.55, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MMOL_L,
                6.7, 2.8, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_PRO
        );
        viewModel.updateUserSettings(settings);

        viewModel.setUnit(CarbUnit.KE);
        viewModel.selectTimeOfDay(TimeOfDay.EVENING);
        viewModel.onCarbInputChange("3.0");

        viewModel.toggleCorrection();
        viewModel.onGlucoseInputChange("12.3");
        viewModel.onTargetGlucoseChange("6.7");
        viewModel.onCorrectionFactorChange("2.8");

        CalculatorUiState state = viewModel.getUiState().getValue();
        CalculationSummary summary = state.getCalculationSummary();

        // 3 KE = 30g KH -> 3 KE * 1.55 (evening factor) = 4.65 IE meal insulin
        // Current BG: 12.3, Target: 6.7, CorrFactor: 2.8
        // Diff = 12.3 - 6.7 = 5.6 -> Correction = 5.6 / 2.8 = 2.0 IE
        // Total = 4.65 + 2.0 = 6.65 IE -> Rounded to 0.5 step = 6.5 IE
        assertEquals(30.0, summary.carbGrams(), DELTA);
        assertEquals(3.0, summary.keValue(), DELTA);
        assertEquals(2.5, summary.beValue(), DELTA);
        assertEquals(1.55, summary.factorUsed(), DELTA);
        assertEquals(4.65, summary.mealInsulin(), DELTA);
        assertEquals(12.3, summary.bloodGlucoseInput(), DELTA);
        assertEquals(6.7, summary.targetGlucose(), DELTA);
        assertEquals(2.00, summary.correctionInsulin(), DELTA);
        assertEquals(6.65, summary.rawTotalInsulin(), DELTA);
        assertEquals(6.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse(summary.isHypoRisk());
        assertEquals(app.getString(R.string.view_model_advisory_above_target), summary.advisoryNote());
    }

    @Test
    public void testCarbInputSanitizationAndAddCarbs() {
        viewModel.onCarbInputChange("45,5");
        assertEquals("45.5", viewModel.getUiState().getValue().getCarbInput());

        viewModel.onCarbInputChange("60g");
        assertEquals("60", viewModel.getUiState().getValue().getCarbInput());

        // Invalid multiple dots should be rejected (remains 60)
        viewModel.onCarbInputChange("45.5.5");
        assertEquals("60", viewModel.getUiState().getValue().getCarbInput());

        // Quick add carbs button (+10)
        viewModel.addCarbs(10.0);
        assertEquals("70", viewModel.getUiState().getValue().getCarbInput());

        // Clear carbs
        viewModel.clearCarbs();
        assertEquals("0", viewModel.getUiState().getValue().getCarbInput());
    }

    @Test
    public void testClearAllCalculatorInputs() {
        viewModel.onCarbInputChange("50");
        viewModel.onGlucoseInputChange("150");
        viewModel.onMealTitleChange("Abendessen");
        viewModel.onNotesChange("Test Notiz");

        viewModel.clearAllCalculatorInputs();

        CalculatorUiState state = viewModel.getUiState().getValue();
        assertEquals("", state.getCarbInput());
        assertEquals("", state.getCurrentGlucoseInput());
        assertEquals("", state.getMealTitle());
        assertEquals("", state.getNotes());
    }
}
