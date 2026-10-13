package network.spiritscorp.model;

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

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Objects;

import network.spiritscorp.ai.GeminiAiModel;
import network.spiritscorp.ui.theme.AppTheme;

/**
 * Room Entity representing user preferences, therapy factors, carbohydrate units,
 * blood glucose targets, correction rules, and AI model configurations in local storage.
 * <p>
 * Stored as a singleton configuration row (ID = 1) in the Room database to ensure
 * persistent access across application sessions.
 */
@Entity(tableName = "user_settings")
public class UserSettings {

    @PrimaryKey
    private int id;

    /** Insulin factor for morning/breakfast calculations (units of insulin per carbohydrate unit). */
    private double morningFactor;

    /** Insulin factor for noon/lunch calculations. */
    private double noonFactor;

    /** Insulin factor for evening/dinner calculations. */
    private double eveningFactor;

    /** Insulin factor for night/bedtime calculations. */
    private double nightFactor;

    /** Default carbohydrate unit preference (GRAMS, BE, or KE). */
    private CarbUnit defaultCarbUnit;

    /** Grams per BE divisor (typically 12g in German clinical practice). */
    private int beGramsDivisor;

    /** Blood glucose measurement unit preference (MG_DL or MMOL_L). */
    private GlucoseUnit glucoseUnit;

    /** Target blood glucose level stored normalized in mg/dl. */
    private double targetGlucoseMgDl;

    /** Blood glucose correction factor stored in mg/dl drop per 1 unit of insulin. */
    private double correctionFactorMgDl;

    /** Insulin rounding step increment (e.g. 0.5 or 0.1 units). */
    private double roundingStep;

    /** Flag indicating whether the medical disclaimer needs to be accepted or shown. */
    private boolean showDisclaimer;

    /** Selected application theme color scheme. */
    private AppTheme selectedTheme;

    /** Selected application theme appearance mode (System, Light, Dark). */
    private AppTheme.Mode themeMode;

    /** Custom Gemini API key provided by the user for on-device or cloud AI meal estimation. */
    private String geminiApiKey;

    /** Selected Gemini AI model identifier for estimations. */
    private GeminiAiModel selectedAiModel;

    /**
     * Default constructor initializing standard default therapy factors, carbohydrate units,
     * target glucose levels, and AI model preferences.
     */
    public UserSettings() {
        this.id = 1;
        this.morningFactor = 1.50;
        this.noonFactor = 1.00;
        this.eveningFactor = 1.20;
        this.nightFactor = 0.80;
        this.defaultCarbUnit = CarbUnit.GRAMS;
        this.beGramsDivisor = 12;
        this.glucoseUnit = GlucoseUnit.MG_DL;
        this.targetGlucoseMgDl = 120.0;
        this.correctionFactorMgDl = 50.0;
        this.roundingStep = 0.5;
        this.showDisclaimer = true;
        this.selectedTheme = null;
        this.themeMode = null;
        this.geminiApiKey = "";
        this.selectedAiModel = GeminiAiModel.GEMINI_3_5_FLASH_LITE;
    }

    /**
     * Ignored Room constructor allowing explicit initialization of all user settings fields.
     *
     * @param id                   Primary key identifier.
     * @param morningFactor        Morning insulin factor.
     * @param noonFactor           Noon insulin factor.
     * @param eveningFactor        Evening insulin factor.
     * @param nightFactor          Night insulin factor.
     * @param defaultCarbUnit      Default carbohydrate unit.
     * @param beGramsDivisor       Grams per BE divisor.
     * @param glucoseUnit          Blood glucose measurement unit.
     * @param targetGlucoseMgDl    Target glucose level in mg/dl.
     * @param correctionFactorMgDl Correction factor in mg/dl per IE.
     * @param roundingStep         Insulin rounding step increment.
     * @param showDisclaimer       Disclaimer visibility flag.
     * @param selectedTheme        Selected theme color scheme.
     * @param themeMode            Selected theme appearance mode.
     * @param geminiApiKey         Gemini API key.
     * @param selectedAiModel      Selected AI model.
     */
    @Ignore
    public UserSettings(
            int id,
            double morningFactor,
            double noonFactor,
            double eveningFactor,
            double nightFactor,
            CarbUnit defaultCarbUnit,
            int beGramsDivisor,
            GlucoseUnit glucoseUnit,
            double targetGlucoseMgDl,
            double correctionFactorMgDl,
            double roundingStep,
            boolean showDisclaimer,
            AppTheme selectedTheme,
            AppTheme.Mode themeMode,
            String geminiApiKey,
            GeminiAiModel selectedAiModel
    ) {
        this.id = id;
        this.morningFactor = morningFactor;
        this.noonFactor = noonFactor;
        this.eveningFactor = eveningFactor;
        this.nightFactor = nightFactor;
        this.defaultCarbUnit = defaultCarbUnit;
        this.beGramsDivisor = beGramsDivisor;
        this.glucoseUnit = glucoseUnit;
        this.targetGlucoseMgDl = targetGlucoseMgDl;
        this.correctionFactorMgDl = correctionFactorMgDl;
        this.roundingStep = roundingStep;
        this.showDisclaimer = showDisclaimer;
        this.selectedTheme = selectedTheme;
        this.themeMode = themeMode;
        this.geminiApiKey = geminiApiKey;
        this.selectedAiModel = selectedAiModel;
    }

    /**
     * Returns the database record identifier (fixed as 1 for singleton storage).
     *
     * @return The primary key ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the database record identifier.
     *
     * @param id The primary key ID to set.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the insulin-to-carbohydrate factor for morning / breakfast calculations.
     *
     * @return Morning insulin factor (IE per carb unit).
     */
    public double getMorningFactor() {
        return morningFactor;
    }

    /**
     * Sets the insulin-to-carbohydrate factor for morning / breakfast calculations.
     *
     * @param morningFactor The morning insulin factor to set.
     */
    public void setMorningFactor(double morningFactor) {
        this.morningFactor = morningFactor;
    }

    /**
     * Returns the insulin-to-carbohydrate factor for noon / lunch calculations.
     *
     * @return Noon insulin factor (IE per carb unit).
     */
    public double getNoonFactor() {
        return noonFactor;
    }

    /**
     * Sets the insulin-to-carbohydrate factor for noon / lunch calculations.
     *
     * @param noonFactor The noon insulin factor to set.
     */
    public void setNoonFactor(double noonFactor) {
        this.noonFactor = noonFactor;
    }

    /**
     * Returns the insulin-to-carbohydrate factor for evening / dinner calculations.
     *
     * @return Evening insulin factor (IE per carb unit).
     */
    public double getEveningFactor() {
        return eveningFactor;
    }

    /**
     * Sets the insulin-to-carbohydrate factor for evening / dinner calculations.
     *
     * @param eveningFactor The evening insulin factor to set.
     */
    public void setEveningFactor(double eveningFactor) {
        this.eveningFactor = eveningFactor;
    }

    /**
     * Returns the insulin-to-carbohydrate factor for night / bedtime calculations.
     *
     * @return Night insulin factor (IE per carb unit).
     */
    public double getNightFactor() {
        return nightFactor;
    }

    /**
     * Sets the insulin-to-carbohydrate factor for night / bedtime calculations.
     *
     * @param nightFactor The night insulin factor to set.
     */
    public void setNightFactor(double nightFactor) {
        this.nightFactor = nightFactor;
    }

    /**
     * Returns the default carbohydrate unit preference (e.g. GRAMS, BE, KE).
     *
     * @return The default {@link CarbUnit}.
     */
    public CarbUnit getDefaultCarbUnit() {
        return defaultCarbUnit;
    }

    /**
     * Sets the default carbohydrate unit preference.
     *
     * @param defaultCarbUnit The {@link CarbUnit} to set.
     */
    public void setDefaultCarbUnit(CarbUnit defaultCarbUnit) {
        this.defaultCarbUnit = defaultCarbUnit;
    }

    /**
     * Returns the grams per BE divisor (e.g. 12g).
     *
     * @return Grams per Broteinheit.
     */
    public int getBeGramsDivisor() {
        return beGramsDivisor;
    }

    /**
     * Sets the grams per BE divisor.
     *
     * @param beGramsDivisor The divisor integer to set.
     */
    public void setBeGramsDivisor(int beGramsDivisor) {
        this.beGramsDivisor = beGramsDivisor;
    }

    /**
     * Returns the preferred blood glucose measurement unit (MG_DL or MMOL_L).
     *
     * @return The {@link GlucoseUnit}.
     */
    public GlucoseUnit getGlucoseUnit() {
        return glucoseUnit;
    }

    /**
     * Sets the preferred blood glucose measurement unit.
     *
     * @param glucoseUnit The {@link GlucoseUnit} to set.
     */
    public void setGlucoseUnit(GlucoseUnit glucoseUnit) {
        this.glucoseUnit = glucoseUnit;
    }

    /**
     * Returns the target blood glucose level stored normalized in mg/dl.
     *
     * @return Target glucose level in mg/dl.
     */
    public double getTargetGlucoseMgDl() {
        return targetGlucoseMgDl;
    }

    /**
     * Sets the target blood glucose level normalized in mg/dl.
     *
     * @param targetGlucoseMgDl Target glucose in mg/dl to set.
     */
    public void setTargetGlucoseMgDl(double targetGlucoseMgDl) {
        this.targetGlucoseMgDl = targetGlucoseMgDl;
    }

    /**
     * Returns the correction factor stored in mg/dl drop per 1 unit of insulin.
     *
     * @return Correction factor in mg/dl per IE.
     */
    public double getCorrectionFactorMgDl() {
        return correctionFactorMgDl;
    }

    /**
     * Sets the correction factor stored in mg/dl drop per 1 unit of insulin.
     *
     * @param correctionFactorMgDl Correction factor in mg/dl per IE to set.
     */
    public void setCorrectionFactorMgDl(double correctionFactorMgDl) {
        this.correctionFactorMgDl = correctionFactorMgDl;
    }

    /**
     * Returns the insulin rounding step increment (e.g. 0.5 or 0.1 units).
     *
     * @return Rounding step increment.
     */
    public double getRoundingStep() {
        return roundingStep;
    }

    /**
     * Sets the insulin rounding step increment.
     *
     * @param roundingStep Rounding step increment to set.
     */
    public void setRoundingStep(double roundingStep) {
        this.roundingStep = roundingStep;
    }

    /**
     * Returns whether the medical disclaimer prompt should be shown.
     *
     * @return True if disclaimer is pending acceptance, false otherwise.
     */
    public boolean isShowDisclaimer() {
        return showDisclaimer;
    }

    /**
     * Sets whether the medical disclaimer prompt should be shown.
     *
     * @param showDisclaimer Disclaimer visibility flag to set.
     */
    public void setShowDisclaimer(boolean showDisclaimer) {
        this.showDisclaimer = showDisclaimer;
    }

    /**
     * Returns the selected application theme color scheme.
     *
     * @return The active {@link AppTheme}.
     */
    public AppTheme getSelectedTheme() {
        return selectedTheme;
    }

    /**
     * Sets the selected application theme color scheme.
     *
     * @param selectedTheme The {@link AppTheme} to set.
     */
    public void setSelectedTheme(AppTheme selectedTheme) {
        this.selectedTheme = selectedTheme;
    }

    /**
     * Returns the selected application appearance mode (System, Light, Dark).
     *
     * @return The active {@link AppTheme.Mode}.
     */
    public AppTheme.Mode getThemeMode() {
        return themeMode;
    }

    /**
     * Sets the selected application appearance mode.
     *
     * @param themeMode The {@link AppTheme.Mode} to set.
     */
    public void setThemeMode(AppTheme.Mode themeMode) {
        this.themeMode = themeMode;
    }

    /**
     * Returns the user's custom Gemini API key for AI meal estimation.
     *
     * @return API key string.
     */
    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    /**
     * Sets the user's custom Gemini API key for AI meal estimation.
     *
     * @param geminiApiKey API key string to set.
     */
    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    /**
     * Returns the selected Gemini AI model identifier.
     *
     * @return The active {@link GeminiAiModel}.
     */
    public GeminiAiModel getSelectedAiModel() {
        return selectedAiModel;
    }

    /**
     * Sets the selected Gemini AI model identifier.
     *
     * @param selectedAiModel The {@link GeminiAiModel} to set.
     */
    public void setSelectedAiModel(GeminiAiModel selectedAiModel) {
        this.selectedAiModel = selectedAiModel;
    }

    /**
     * Creates and returns a deep copy of this {@link UserSettings} instance.
     *
     * @return A new cloned {@link UserSettings} object.
     */
    public UserSettings copy() {
        return new UserSettings(
                this.id,
                this.morningFactor,
                this.noonFactor,
                this.eveningFactor,
                this.nightFactor,
                this.defaultCarbUnit,
                this.beGramsDivisor,
                this.glucoseUnit,
                this.targetGlucoseMgDl,
                this.correctionFactorMgDl,
                this.roundingStep,
                this.showDisclaimer,
                this.selectedTheme,
                this.themeMode,
                this.geminiApiKey,
                this.selectedAiModel
        );
    }

    /**
     * Compares this UserSettings instance with another object for structural equality.
     *
     * @param o The object to compare with.
     * @return True if all fields match, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSettings that = (UserSettings) o;
        return id == that.id &&
                Double.compare(that.morningFactor, morningFactor) == 0 &&
                Double.compare(that.noonFactor, noonFactor) == 0 &&
                Double.compare(that.eveningFactor, eveningFactor) == 0 &&
                Double.compare(that.nightFactor, nightFactor) == 0 &&
                beGramsDivisor == that.beGramsDivisor &&
                Double.compare(that.targetGlucoseMgDl, targetGlucoseMgDl) == 0 &&
                Double.compare(that.correctionFactorMgDl, correctionFactorMgDl) == 0 &&
                Double.compare(that.roundingStep, roundingStep) == 0 &&
                showDisclaimer == that.showDisclaimer &&
                Objects.equals(defaultCarbUnit, that.defaultCarbUnit) &&
                Objects.equals(glucoseUnit, that.glucoseUnit) &&
                Objects.equals(selectedTheme, that.selectedTheme) &&
                Objects.equals(themeMode, that.themeMode) &&
                Objects.equals(geminiApiKey, that.geminiApiKey) &&
                Objects.equals(selectedAiModel, that.selectedAiModel);
    }

    /**
     * Computes a hash code for this UserSettings instance based on all fields.
     *
     * @return Integer hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                id, morningFactor, noonFactor, eveningFactor, nightFactor,
                defaultCarbUnit, beGramsDivisor, glucoseUnit, targetGlucoseMgDl,
                correctionFactorMgDl, roundingStep, showDisclaimer, selectedTheme,
                themeMode, geminiApiKey, selectedAiModel
        );
    }
}
