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

import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.StateFlow;
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
@SuppressWarnings("KotlinInternalInJava")
public class CalculatorEngineStateTest {

    private static final double DELTA = 0.001;

    private InsulinCalculatorViewModel viewModel;
    private CalculatorMainView calculatorMainView;
    StateFlow<CalculatorUiState> state = null;
    private Application app;

    /**
     * Sets up the test environment with mocked repository and ViewModel instances using Robolectric.
     */
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
        calculatorMainView = viewModel.getCalculatorMainView$app();
        state = calculatorMainView.getCalculatorUiState$app();

    }

    /**
     * Verifies that the initial calculator state and calculation summary are non-null and properly initialized.
     */
    @Test
    public void testInitialStateNotNull() {
        assertNotNull("Calculator UI state flow must not be null", state);
        assertNotNull("Calculation summary inside initial state must not be null", state.getValue().getCalculationSummary());
        assertEquals("Initial carb input string must be empty", "", state.getValue().getCarbInput());
        assertEquals("Initial selected carb unit must be GRAMS", CarbUnit.GRAMS, state.getValue().getSelectedUnit());
    }

    /**
     * Verifies insulin calculation accuracy for morning time slot using gram-based carbohydrate units.
     */
    @Test
    public void testMorningCalculationWithGrams() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.GRAMS);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.MORNING);
        calculatorMainView.onCarbInputChange$app("60");

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // 60g KH / 12 = 5 BE. 5 BE * 1.5 = 7.5 IE
        assertEquals("Carb grams calculation incorrect", 60.0, summary.carbGrams(), DELTA);
        assertEquals("BE value calculation incorrect", 5.0, summary.beValue(), DELTA);
        assertEquals("KE value calculation incorrect", 6.0, summary.keValue(), DELTA);
        assertEquals("Factor used incorrect", 1.50, summary.factorUsed(), DELTA);
        assertEquals("Meal insulin calculation incorrect", 7.50, summary.mealInsulin(), DELTA);
        assertNull("Blood glucose input should be null when correction is disabled", summary.bloodGlucoseInput());
        assertEquals("Target glucose incorrect", Double.valueOf(120), summary.targetGlucose());
        assertEquals("Correction insulin should be zero when correction disabled", 0.00, summary.correctionInsulin(), DELTA);
        assertEquals("Raw total insulin incorrect", 7.50, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin incorrect", 7.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse("Hypo risk should be false for normal blood glucose", summary.isHypoRisk());
        assertTrue("Advisory note should be blank for normal range", summary.advisoryNote().isBlank());
    }

    /**
     * Verifies insulin calculation for noon time slot using Bread Units (BE) and user factor overrides.
     */
    @Test
    public void testNoonCalculationWithBEAndFactorOverride() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.BE);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.NOON);
        calculatorMainView.onCarbInputChange$app("4.5");

        // User overrides factor from 1.0 to 1.30 by adjusting delta +0.30
        calculatorMainView.adjustFactor$app(0.30);

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // 4.5 BE * 12 = 54.0g KH. 4.5 BE * 1.30 = 5.85 IE -> Rounded to step 0.5 = 6.0 IE
        assertEquals("Carb grams calculation incorrect with BE", 54.0, summary.carbGrams(), DELTA);
        assertEquals("BE value incorrect", 4.5, summary.beValue(), DELTA);
        assertEquals("KE value incorrect", 5.4, summary.keValue(), DELTA);
        assertEquals("Factor used with override incorrect", 1.30, summary.factorUsed(), DELTA);
        assertEquals("Meal insulin incorrect with override", 5.85, summary.mealInsulin(), DELTA);
        assertNull("Blood glucose input should be null when correction is disabled", summary.bloodGlucoseInput());
        assertEquals("Target glucose incorrect", Double.valueOf(120), summary.targetGlucose());
        assertEquals("Correction insulin should be zero", 0.00, summary.correctionInsulin(), DELTA);
        assertEquals("Raw total insulin incorrect", 5.85, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin incorrect", 6.0, summary.roundedTotalInsulin(), DELTA);
        assertFalse("Hypo risk should be false", summary.isHypoRisk());
    }

    /**
     * Verifies evening insulin calculation including high blood glucose correction and advisory notes.
     */
    @Test
    public void testEveningCalculationWithHighGlucoseCorrection() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.BE);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.EVENING);
        calculatorMainView.onCarbInputChange$app("3.0");

        calculatorMainView.toggleCorrection$app();
        calculatorMainView.onGlucoseInputChange$app("220");
        calculatorMainView.onTargetGlucoseChange$app("100");
        calculatorMainView.onCorrectionFactorChange$app("40");

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // 3.0 BE (36g KH) -> 3.0 * 1.2 = 3.6 IE
        // Current BG 220, Target 100, CorrFactor 40 -> (220-100)/40 = +3.0 IE correction
        // Total = 3.6 + 3.0 = 6.6 IE -> Rounded to step 0.5 = 6.5 IE
        assertEquals("Carb grams calculation incorrect", 36.0, summary.carbGrams(), DELTA);
        assertEquals("BE value incorrect", 3.0, summary.beValue(), DELTA);
        assertEquals("KE value incorrect", 3.6, summary.keValue(), DELTA);
        assertEquals("Factor used incorrect", 1.20, summary.factorUsed(), DELTA);
        assertEquals("Meal insulin calculation incorrect", 3.60, summary.mealInsulin(), DELTA);
        assertEquals("Blood glucose input incorrect", 220.0, summary.bloodGlucoseInput(), DELTA);
        assertEquals("Target glucose incorrect", 100.0, summary.targetGlucose(), DELTA);
        assertEquals("Correction insulin calculation incorrect", 3.00, summary.correctionInsulin(), DELTA);
        assertEquals("Raw total insulin incorrect", 6.60, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin incorrect", 6.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse("Hypo risk should be false for high blood glucose", summary.isHypoRisk());
        assertEquals("Advisory note incorrect for high blood glucose", app.getString(R.string.view_model_advisory_above_target), summary.advisoryNote());
    }

    /**
     * Verifies low blood glucose safety warnings and dose reduction calculations during hypoglycemia risk.
     */
    @Test
    public void testLowBloodGlucoseWarningAndDoseReduction() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.GRAMS);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.NIGHT);
        calculatorMainView.onCarbInputChange$app("30");

        calculatorMainView.toggleCorrection$app();
        calculatorMainView.onGlucoseInputChange$app("65");
        calculatorMainView.onTargetGlucoseChange$app("100");
        calculatorMainView.onCorrectionFactorChange$app("40");

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // 30g KH / 12 = 2.5 BE. 2.5 BE * 0.8 (night factor) = 2.0 IE meal insulin
        // Current BG 65, Target 100, CorrFactor 40 -> (65 - 100) / 40 = -0.875 IE correction
        // Total = 2.0 - 0.875 = 1.125 IE -> Rounded = 1.0 IE
        assertEquals("Carb grams calculation incorrect", 30.0, summary.carbGrams(), DELTA);
        assertEquals("BE value incorrect", 2.5, summary.beValue(), DELTA);
        assertEquals("KE value incorrect", 3.0, summary.keValue(), DELTA);
        assertEquals("Factor used incorrect", 0.80, summary.factorUsed(), DELTA);
        assertEquals("Meal insulin calculation incorrect", 2.0, summary.mealInsulin(), DELTA);
        assertEquals("Blood glucose input incorrect", 65.0, summary.bloodGlucoseInput(), DELTA);
        assertEquals("Target glucose incorrect", 100.0, summary.targetGlucose(), DELTA);
        assertEquals("Correction insulin calculation incorrect", -0.88, summary.correctionInsulin(), DELTA);
        assertEquals("Raw total insulin incorrect", 1.13, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin incorrect", 1.0, summary.roundedTotalInsulin(), DELTA);
        assertTrue("Hypo risk must be true when blood glucose is below 70 mg/dl", summary.isHypoRisk());
        assertEquals("Hypoglycemia advisory note incorrect", app.getString(R.string.view_model_advisory_hypo, "70 " + app.getString(GlucoseUnit.MG_DL.getShortNameResId())), summary.advisoryNote());
    }

    /**
     * Verifies negative correction calculations resulting in dose reductions below target blood glucose.
     */
    @Test
    public void testNegativeCorrectionDoseReduction() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.BE);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.NOON);
        calculatorMainView.onCarbInputChange$app("2.0");

        calculatorMainView.toggleCorrection$app();
        calculatorMainView.onGlucoseInputChange$app("80");
        calculatorMainView.onTargetGlucoseChange$app("100");
        calculatorMainView.onCorrectionFactorChange$app("40");

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // 2 BE = 2.0 IE meal insulin. BG = 80, Target = 100, Corr = 40
        // Correction = -(20 / 40) = -0.5 IE
        // Total = 2.0 - 0.5 = 1.5 IE
        assertEquals("Carb grams calculation incorrect", 24.0, summary.carbGrams(), DELTA);
        assertEquals("BE value incorrect", 2.0, summary.beValue(), DELTA);
        assertEquals("KE value incorrect", 2.4, summary.keValue(), DELTA);
        assertEquals("Factor used incorrect", 1.00, summary.factorUsed(), DELTA);
        assertEquals("Meal insulin calculation incorrect", 2.0, summary.mealInsulin(), DELTA);
        assertEquals("Blood glucose input incorrect", 80.0, summary.bloodGlucoseInput(), DELTA);
        assertEquals("Target glucose incorrect", 100.0, summary.targetGlucose(), DELTA);
        assertEquals("Correction insulin calculation incorrect", -0.50, summary.correctionInsulin(), DELTA);
        assertEquals("Raw total insulin incorrect", 1.50, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin incorrect", 1.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse("Hypo risk should be false for 80 mg/dl", summary.isHypoRisk());
        assertEquals("Advisory note for below target incorrect", app.getString(R.string.view_model_advisory_below_target), summary.advisoryNote());
    }

    /**
     * Verifies calculations and corrections when using mmol/L blood glucose units.
     */
    @Test
    public void testMmolLCorrectionCalculation() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.55, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MMOL_L,
                6.7, 2.8, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.KE);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.EVENING);
        calculatorMainView.onCarbInputChange$app("3.0");

        calculatorMainView.toggleCorrection$app();
        calculatorMainView.onGlucoseInputChange$app("12.3");
        calculatorMainView.onTargetGlucoseChange$app("6.7");
        calculatorMainView.onCorrectionFactorChange$app("2.8");

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // 3 KE = 30g KH -> 3 KE * 1.55 (evening factor) = 4.65 IE meal insulin
        // Current BG: 12.3, Target: 6.7, CorrFactor: 2.8
        // Diff = 12.3 - 6.7 = 5.6 -> Correction = 5.6 / 2.8 = 2.0 IE
        // Total = 4.65 + 2.0 = 6.65 IE -> Rounded to 0.5 step = 6.5 IE
        assertEquals("Carb grams calculation incorrect", 30.0, summary.carbGrams(), DELTA);
        assertEquals("KE value incorrect", 3.0, summary.keValue(), DELTA);
        assertEquals("BE value incorrect", 2.5, summary.beValue(), DELTA);
        assertEquals("Factor used incorrect", 1.55, summary.factorUsed(), DELTA);
        assertEquals("Meal insulin calculation incorrect", 4.65, summary.mealInsulin(), DELTA);
        assertEquals("Blood glucose input incorrect", 12.3, summary.bloodGlucoseInput(), DELTA);
        assertEquals("Target glucose incorrect", 6.7, summary.targetGlucose(), DELTA);
        assertEquals("Correction insulin calculation incorrect", 2.00, summary.correctionInsulin(), DELTA);
        assertEquals("Raw total insulin incorrect", 6.65, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin incorrect", 6.5, summary.roundedTotalInsulin(), DELTA);
        assertFalse("Hypo risk should be false", summary.isHypoRisk());
        assertEquals("Advisory note for above target incorrect", app.getString(R.string.view_model_advisory_above_target), summary.advisoryNote());
    }

    /**
     * Verifies that total insulin is correctly clamped to zero when negative corrections outweigh meal insulin.
     */
    @Test
    public void testTotalInsulinClampedToZeroWhenNegative() {
        UserSettings settings = new UserSettings(
                1, 1.50, 1.00, 1.20, 0.80, CarbUnit.GRAMS, 12, GlucoseUnit.MG_DL,
                120.0, 50.0, 0.5, true, AppTheme.MEDICAL_TEAL, AppTheme.Mode.SYSTEM, "", GeminiAiModel.GEMINI_3_1_FLASH_LITE
        );
        viewModel.getDatabaseMainView$app().updateUserSettings$app(settings);

        calculatorMainView.setCarbUnit$app(CarbUnit.GRAMS);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.MORNING);
        calculatorMainView.onCarbInputChange$app("5"); // Small meal: 5g KH / 12 * 1.5 = 0.625 IE meal insulin

        calculatorMainView.toggleCorrection$app();
        calculatorMainView.onGlucoseInputChange$app("60"); // Low BG (below target 120)
        calculatorMainView.onTargetGlucoseChange$app("120");
        calculatorMainView.onCorrectionFactorChange$app("40"); // (120 - 60) / 40 = -1.5 IE correction reduction

        CalculationSummary summary = state.getValue().getCalculationSummary();

        // Meal (0.625) + Correction (-1.5) = -0.875 -> Must be clamped to 0.0 IE
        assertEquals("Raw total insulin must be clamped to zero", 0.0, summary.rawTotalInsulin(), DELTA);
        assertEquals("Rounded total insulin must be clamped to zero", 0.0, summary.roundedTotalInsulin(), DELTA);
        assertTrue("Raw total insulin must be non-negative", summary.rawTotalInsulin() >= 0.0);
        assertTrue("Rounded total insulin must be non-negative", summary.roundedTotalInsulin() >= 0.0);
    }
}
