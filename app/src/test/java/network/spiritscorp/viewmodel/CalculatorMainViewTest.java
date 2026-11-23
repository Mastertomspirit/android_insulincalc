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

import kotlinx.coroutines.flow.StateFlow;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.TimeOfDay;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import kotlin.jvm.functions.Function1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive Unit Test suite for CalculatorMainView.
 * Tests all state management, input sanitization, unit selection, time slot handling,
 * factor overrides, edge cases, boundary conditions, and callback triggers.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("KotlinInternalInJava")
public class CalculatorMainViewTest {

    private final double delta = 0.001;

    private CalculatorMainView calculatorMainView;
    private StateFlow<CalculatorUiState> state = null;
    private static final double FACTOR_MORNING = 1.50;
    private static final double FACTOR_NOON = 1.00;
    private static final double FACTOR_EVENING = 1.20;
    private static final double FACTOR_NIGHT = 0.80;

    /**
     * Initializes the CalculatorMainView instance and test environment before each test.
     */
    @Before
    public void setUp() {
        Application app = ApplicationProvider.getApplicationContext();

        calculatorMainView = new CalculatorMainView(
                app,
                () -> 12,
                () -> 0.5,
                timeOfDayFactorLookup
        );
        state = calculatorMainView.getCalculatorUiState$app();
    }

    /**
     * Verifies the initial state values of the calculator UI state.
     */
    @Test
    public void testInitialState() {
        assertNotNull("Calculator UI state must not be null", state);
        assertEquals("Carb input must be empty initially", "", state.getValue().getCarbInput());
        assertEquals("Selected carb unit must be GRAMS initially", CarbUnit.GRAMS, state.getValue().getSelectedUnit());
        assertEquals("Selected time of day must match current time initially", TimeOfDay.current(), state.getValue().getSelectedTimeOfDay());
        assertNull("Factor override must be null initially", state.getValue().getFactorOverride());
        assertTrue("Auto time detection must be enabled by default", state.getValue().isAutoTimeDetection());
        assertFalse("Correction must not be shown by default", state.getValue().getShowCorrection());
        assertEquals("Current glucose input must be empty initially", "", state.getValue().getCurrentGlucoseInput());
        assertEquals("Meal title must be empty initially", "", state.getValue().getMealTitle());
        assertEquals("Notes must be empty initially", "", state.getValue().getNotes());
    }

    /**
     * Verifies correct input sanitization and updating behavior for carbohydrate input changes.
     */
    @Test
    public void testOnCarbInputChange_ValidInputsAndSanitization() {
        // Standard integer string
        calculatorMainView.onCarbInputChange$app("50");
        assertEquals("Carb input update failed", "50", state.getValue().getCarbInput());

        // Comma replacement with dot
        calculatorMainView.onCarbInputChange$app("45,5");
        assertEquals("Comma replacement in carb input failed", "45.5", state.getValue().getCarbInput());

        // Non-digit non-dot filtering
        calculatorMainView.onCarbInputChange$app("60g KH!");
        assertEquals("Non-digit filtering in carb input failed", "60", state.getValue().getCarbInput());

        // Invalid multiple dots should be rejected (remains 60)
        calculatorMainView.onCarbInputChange$app("45.5.5");
        assertEquals("Multiple dots filtering failed", "60", state.getValue().getCarbInput());
    }

    /**
     * Verifies edge cases and rejections during carbohydrate input changes such as multiple dots or length limits.
     */
    @Test
    public void testOnCarbInputChange_EdgeCasesAndRejections() {
        calculatorMainView.onCarbInputChange$app("45.5");
        // Multiple dots should be rejected (remains unchanged)
        calculatorMainView.onCarbInputChange$app("45.5.5");
        assertEquals("Carb input with multiple dots rejection failed", "45.5", state.getValue().getCarbInput());

        // Input length > 5 should be rejected (remains unchanged)
        calculatorMainView.onCarbInputChange$app("123456789");
        assertEquals("Carb input length > 5 rejection failed", "45.5", state.getValue().getCarbInput());

        // Input length exactly 5 should be accepted
        calculatorMainView.onCarbInputChange$app("123.4");
        assertEquals("Carb input length 5 acceptance failed", "123.4", state.getValue().getCarbInput());
    }

    /**
     * Verifies positive and decimal carbohydrate increment additions.
     */
    @Test
    public void testAddCarbs_PositiveAndDecimalIncrements() {
        // Add to empty input (0.0 + 10.0 = 10.0) -> "10"
        calculatorMainView.addCarbs$app(10.0);
        assertEquals("Adding carbs to empty input failed", "10", state.getValue().getCarbInput());

        // Add 5 to existing 10 -> "15"
        calculatorMainView.addCarbs$app(5.0);
        assertEquals("Adding carbs increment failed", "15", state.getValue().getCarbInput());

        // Add decimal amount (15 + 2.5 = 17.5)
        calculatorMainView.addCarbs$app(2.5);
        assertTrue("Adding decimal carbs failed", state.getValue().getCarbInput().contains("17.5"));
    }

    /**
     * Verifies that negative carbohydrate increments clamp the carb input to zero.
     */
    @Test
    public void testAddCarbs_NegativeIncrementClampingToZero() {
        calculatorMainView.onCarbInputChange$app("5");
        calculatorMainView.addCarbs$app(-10.0);
        assertEquals("Negative carb increment clamping failed", "0", state.getValue().getCarbInput());
    }

    /**
     * Verifies decrementing carbohydrate values correctly through subtraction.
     */
    @Test
    public void testAddCarbs_Decrements() {
        // Add to empty input (0.0 + 20.0 = 20.0) -> "20.0"
        // Because it is a manual input.
        calculatorMainView.onCarbInputChange$app("20.0");
        assertEquals("Carb input initialization failed", "20.0", state.getValue().getCarbInput());

        // Add -15 to existing 20.0 -> "5"
        calculatorMainView.addCarbs$app(-15.0);
        assertEquals("Carb decrement failed", "5", state.getValue().getCarbInput());

        // Add -15 to existing 5 -> "0"
        calculatorMainView.addCarbs$app(-15.0);
        assertEquals("Carb decrement clamping to zero failed", "0", state.getValue().getCarbInput());

        // Add -15 to existing 0 -> "0"
        calculatorMainView.addCarbs$app(-15.0);
        assertEquals("Carb decrement at zero failed", "0", state.getValue().getCarbInput());

        // Add new input (0.0 + 20.2 = 20.2) -> "20.2"
        calculatorMainView.onCarbInputChange$app("20.2");
        assertEquals("Carb input decimal initialization failed", "20.2", state.getValue().getCarbInput());

        // Add -15 to existing 20.2 -> "5.2"
        calculatorMainView.addCarbs$app(-15.0);
        assertEquals("Carb decimal decrement failed", "5.2", state.getValue().getCarbInput());
    }

    /**
     * Verifies clearing the carbohydrate input field.
     */
    @Test
    public void testClearCarbs() {
        calculatorMainView.onCarbInputChange$app("45.5");
        calculatorMainView.clearCarbs$app();
        assertEquals("Clearing carbs failed", "", state.getValue().getCarbInput());
    }

    /**
     * Verifies resetting and clearing all calculator inputs (carbs, glucose, title, notes).
     */
    @Test
    public void testClearAllCalculatorInputs() {
        calculatorMainView.onCarbInputChange$app("50");
        calculatorMainView.onGlucoseInputChange$app("150");
        calculatorMainView.onMealTitleChange$app("Abendessen");
        calculatorMainView.onNotesChange$app("Test Notiz");

        calculatorMainView.clearAllCalculatorInputs$app();

        assertEquals("Clearing all calculator inputs failed carb", "", state.getValue().getCarbInput());
        assertEquals("Clearing all calculator inputs failed glucose", "", state.getValue().getCurrentGlucoseInput());
        assertEquals("Clearing all calculator inputs failed title", "", state.getValue().getMealTitle());
        assertEquals("Clearing all calculator inputs failed notes", "", state.getValue().getNotes());
    }

    /**
     * Verifies selecting a specific time of day disables auto time detection and clears factor overrides.
     */
    @Test
    public void testSelectTimeOfDay() {
        // Preset factor override and auto detection
        calculatorMainView.adjustFactor$app(0.20);
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.EVENING);

        assertEquals("Selected time of day update failed", TimeOfDay.EVENING, state.getValue().getSelectedTimeOfDay());
        assertNull("Factor override should be null after selecting time of day", state.getValue().getFactorOverride());
        assertFalse("Auto time detection should be false after manual selection", state.getValue().isAutoTimeDetection());
    }

    /**
     * Verifies resetting back to automatic time detection.
     */
    @Test
    public void testResetToAutoTime() {
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.NIGHT);
        calculatorMainView.adjustFactor$app(0.30);

        calculatorMainView.resetToAutoTime$app();

        assertEquals("Reset to auto time selection failed", TimeOfDay.current(), state.getValue().getSelectedTimeOfDay());
        assertNull("Factor override should be null after reset to auto", state.getValue().getFactorOverride());
        assertTrue("Auto time detection should be true after reset", state.getValue().isAutoTimeDetection());
    }

    /**
     * Verifies factor adjustments and clamping within valid bounds (0.05 to 10.00).
     */
    @Test
    public void testAdjustFactor_CalculationsAndClamping() {
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.NOON); // Factor is 1.00
        calculatorMainView.adjustFactor$app(0.30);
        var newState = state.getValue().getFactorOverride();
        assertNotNull("Factor override must not be null after adjustment", newState);
        assertEquals("Factor adjustment override value incorrect", 1.30, newState, delta);

        // Test lower bound clamping (0.05)
        calculatorMainView.adjustFactor$app(-5.0);
        assertNotNull("Factor override must not be null after lower bound adjustment", newState = state.getValue().getFactorOverride());
        assertEquals("Lower bound factor clamping incorrect", 0.05, newState, delta);

        // Test upper bound clamping (10.00)
        calculatorMainView.adjustFactor$app(20.0);
        assertNotNull("Factor override must not be null after upper bound adjustment", newState = state.getValue().getFactorOverride());
        assertEquals("Upper bound factor clamping incorrect", 10.00, newState, delta);
    }

    /**
     * Verifies toggling blood glucose correction visibility.
     */
    @Test
    public void testToggleCorrection() {
        assertFalse("Correction view should be hidden initially", state.getValue().getShowCorrection());
        calculatorMainView.toggleCorrection$app();
        assertTrue("Correction view should be visible after toggle", state.getValue().getShowCorrection());

        calculatorMainView.toggleCorrection$app();
        assertFalse("Correction view should be hidden after second toggle", state.getValue().getShowCorrection());
    }

    /**
     * Verifies updating blood glucose, target glucose, and correction factor input fields.
     */
    @Test
    public void testGlucoseAndCorrectionFactorInputs() {
        calculatorMainView.onGlucoseInputChange$app("180,5");
        assertEquals("Glucose input sanitization failed", "180.5", state.getValue().getCurrentGlucoseInput());

        calculatorMainView.onTargetGlucoseChange$app("110,0");
        assertEquals("Target glucose input sanitization failed", "110.0", state.getValue().getTargetGlucoseInput());

        calculatorMainView.onCorrectionFactorChange$app("40,0");
        assertEquals("Correction factor input sanitization failed", "40.0", state.getValue().getCorrectionFactorInput());
    }

    /**
     * Verifies updating meal title and notes fields.
     */
    @Test
    public void testMealTitleAndNotesChange() {
        calculatorMainView.onMealTitleChange$app("Mittagessen");
        assertEquals("Meal title change failed", "Mittagessen", state.getValue().getMealTitle());

        calculatorMainView.onNotesChange$app("Extra Pizza");
        assertEquals("Notes change failed", "Extra Pizza", state.getValue().getNotes());
    }

    /**
     * Verifies lifecycle onResume behavior regarding auto time detection.
     */
    @Test
    public void testOnResume_AutoTimeDetectionBehavior() {
        // ToDo:
        // Auto time detection enabled (default)
        calculatorMainView.onResume$app();

        // Auto time detection disabled
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.MORNING);
        calculatorMainView.onResume$app();
    }

    /**
     * Verifies calculation of effective insulin factor considering overrides, manual times, and auto time detection.
     */
    @Test
    public void testGetEffectiveFactor() {
        double factor = calculatorMainView.getEffectiveFactor$app();
        // 1. With factor override
        calculatorMainView.adjustFactor$app(0.50);
        assertEquals("Effective factor with override incorrect", factor + 0.50, calculatorMainView.getEffectiveFactor$app(), delta);

        // 2. Without override, manual time selection (MORNING -> 1.50)
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.MORNING);
        assertEquals("Morning effective factor incorrect", 1.50, calculatorMainView.getEffectiveFactor$app(), delta);

        // 3. Without override, manual time selection (NOON -> 1.00)
        calculatorMainView.selectTimeOfDay$app(TimeOfDay.NOON);
        assertEquals("Noon effective factor incorrect", 1.00, calculatorMainView.getEffectiveFactor$app(), delta);

        // 4. Reset to auto time
        calculatorMainView.resetToAutoTime$app();
        double expectedAutoFactor = timeOfDayFactorLookup.invoke(TimeOfDay.current()) ;
        assertEquals("Auto effective factor incorrect", expectedAutoFactor, calculatorMainView.getEffectiveFactor$app(), delta);
    }

    private final Function1<TimeOfDay, Double> timeOfDayFactorLookup = timeOfDay -> switch (timeOfDay) {
        case MORNING -> FACTOR_MORNING;
        case NOON -> FACTOR_NOON;
        case EVENING -> FACTOR_EVENING;
        case NIGHT -> FACTOR_NIGHT;
    };
}
