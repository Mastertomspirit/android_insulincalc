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
 * Room Entity representing user preferences and therapy factor settings.
 */
@Entity(tableName = "user_settings")
public class UserSettings {

    @PrimaryKey
    private int id;
    private double morningFactor;
    private double noonFactor;
    private double eveningFactor;
    private double nightFactor;
    private CarbUnit defaultCarbUnit;
    private int beGramsDivisor;
    private GlucoseUnit glucoseUnit;
    private double targetGlucoseMgDl;
    private double correctionFactorMgDl;
    private double roundingStep;
    private boolean showDisclaimer;
    private AppTheme selectedTheme;
    private AppTheme.Mode themeMode;
    private String geminiApiKey;
    private GeminiAiModel selectedAiModel;

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
        this.selectedAiModel = GeminiAiModel.GEMINI_FLASH_LITE_LATEST;
    }

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getMorningFactor() {
        return morningFactor;
    }

    public void setMorningFactor(double morningFactor) {
        this.morningFactor = morningFactor;
    }

    public double getNoonFactor() {
        return noonFactor;
    }

    public void setNoonFactor(double noonFactor) {
        this.noonFactor = noonFactor;
    }

    public double getEveningFactor() {
        return eveningFactor;
    }

    public void setEveningFactor(double eveningFactor) {
        this.eveningFactor = eveningFactor;
    }

    public double getNightFactor() {
        return nightFactor;
    }

    public void setNightFactor(double nightFactor) {
        this.nightFactor = nightFactor;
    }

    public CarbUnit getDefaultCarbUnit() {
        return defaultCarbUnit;
    }

    public void setDefaultCarbUnit(CarbUnit defaultCarbUnit) {
        this.defaultCarbUnit = defaultCarbUnit;
    }

    public int getBeGramsDivisor() {
        return beGramsDivisor;
    }

    public void setBeGramsDivisor(int beGramsDivisor) {
        this.beGramsDivisor = beGramsDivisor;
    }

    public GlucoseUnit getGlucoseUnit() {
        return glucoseUnit;
    }

    public void setGlucoseUnit(GlucoseUnit glucoseUnit) {
        this.glucoseUnit = glucoseUnit;
    }

    public double getTargetGlucoseMgDl() {
        return targetGlucoseMgDl;
    }

    public void setTargetGlucoseMgDl(double targetGlucoseMgDl) {
        this.targetGlucoseMgDl = targetGlucoseMgDl;
    }

    public double getCorrectionFactorMgDl() {
        return correctionFactorMgDl;
    }

    public void setCorrectionFactorMgDl(double correctionFactorMgDl) {
        this.correctionFactorMgDl = correctionFactorMgDl;
    }

    public double getRoundingStep() {
        return roundingStep;
    }

    public void setRoundingStep(double roundingStep) {
        this.roundingStep = roundingStep;
    }

    public boolean isShowDisclaimer() {
        return showDisclaimer;
    }

    public void setShowDisclaimer(boolean showDisclaimer) {
        this.showDisclaimer = showDisclaimer;
    }

    public AppTheme getSelectedTheme() {
        return selectedTheme;
    }

    public void setSelectedTheme(AppTheme selectedTheme) {
        this.selectedTheme = selectedTheme;
    }

    public AppTheme.Mode getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(AppTheme.Mode themeMode) {
        this.themeMode = themeMode;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public GeminiAiModel getSelectedAiModel() {
        return selectedAiModel;
    }

    public void setSelectedAiModel(GeminiAiModel selectedAiModel) {
        this.selectedAiModel = selectedAiModel;
    }

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
