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

import java.util.Objects;

public final class CalculationSummary {
    private final double carbGrams;
    private final double keValue;
    private final double beValue;
    private final double factorUsed;
    private final double mealInsulin;
    private final Double bloodGlucoseInput;
    private final Double targetGlucose;
    private final double correctionInsulin;
    private final double rawTotalInsulin;
    private final double roundedTotalInsulin;
    private final double roundingStep;
    private final boolean isHypoRisk;
    private final String advisoryNote;

    public CalculationSummary() {
        this(0.0, 0.0, 0.0, 0.0, 0.0, null, null, 0.0, 0.0, 0.0, 0.5, false, "");
    }

    public CalculationSummary(
            double carbGrams,
            double keValue,
            double beValue,
            double factorUsed,
            double mealInsulin,
            Double bloodGlucoseInput,
            Double targetGlucose,
            double correctionInsulin,
            double rawTotalInsulin,
            double roundedTotalInsulin,
            double roundingStep,
            boolean isHypoRisk,
            String advisoryNote
    ) {
        this.carbGrams = carbGrams;
        this.keValue = keValue;
        this.beValue = beValue;
        this.factorUsed = factorUsed;
        this.mealInsulin = mealInsulin;
        this.bloodGlucoseInput = bloodGlucoseInput;
        this.targetGlucose = targetGlucose;
        this.correctionInsulin = correctionInsulin;
        this.rawTotalInsulin = rawTotalInsulin;
        this.roundedTotalInsulin = roundedTotalInsulin;
        this.roundingStep = roundingStep;
        this.isHypoRisk = isHypoRisk;
        this.advisoryNote = advisoryNote != null ? advisoryNote : "";
    }

    public double getCarbGrams() {
        return carbGrams;
    }

    public double getKeValue() {
        return keValue;
    }

    public double getBeValue() {
        return beValue;
    }

    public double getFactorUsed() {
        return factorUsed;
    }

    public double getMealInsulin() {
        return mealInsulin;
    }

    public Double getBloodGlucoseInput() {
        return bloodGlucoseInput;
    }

    public Double getTargetGlucose() {
        return targetGlucose;
    }

    public double getCorrectionInsulin() {
        return correctionInsulin;
    }

    public double getRawTotalInsulin() {
        return rawTotalInsulin;
    }

    public double getRoundedTotalInsulin() {
        return roundedTotalInsulin;
    }

    public double getRoundingStep() {
        return roundingStep;
    }

    public boolean isHypoRisk() {
        return isHypoRisk;
    }

    public String getAdvisoryNote() {
        return advisoryNote;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalculationSummary that = (CalculationSummary) o;
        return Double.compare(that.carbGrams, carbGrams) == 0 &&
                Double.compare(that.keValue, keValue) == 0 &&
                Double.compare(that.beValue, beValue) == 0 &&
                Double.compare(that.factorUsed, factorUsed) == 0 &&
                Double.compare(that.mealInsulin, mealInsulin) == 0 &&
                Double.compare(that.correctionInsulin, correctionInsulin) == 0 &&
                Double.compare(that.rawTotalInsulin, rawTotalInsulin) == 0 &&
                Double.compare(that.roundedTotalInsulin, roundedTotalInsulin) == 0 &&
                Double.compare(that.roundingStep, roundingStep) == 0 &&
                isHypoRisk == that.isHypoRisk &&
                Objects.equals(bloodGlucoseInput, that.bloodGlucoseInput) &&
                Objects.equals(targetGlucose, that.targetGlucose) &&
                Objects.equals(advisoryNote, that.advisoryNote);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                carbGrams, keValue, beValue, factorUsed, mealInsulin,
                bloodGlucoseInput, targetGlucose, correctionInsulin,
                rawTotalInsulin, roundedTotalInsulin, roundingStep,
                isHypoRisk, advisoryNote
        );
    }

    @Override
    public String toString() {
        return "CalculationSummary{" +
                "carbGrams=" + carbGrams +
                ", keValue=" + keValue +
                ", beValue=" + beValue +
                ", factorUsed=" + factorUsed +
                ", mealInsulin=" + mealInsulin +
                ", bloodGlucoseInput=" + bloodGlucoseInput +
                ", targetGlucose=" + targetGlucose +
                ", correctionInsulin=" + correctionInsulin +
                ", rawTotalInsulin=" + rawTotalInsulin +
                ", roundedTotalInsulin=" + roundedTotalInsulin +
                ", roundingStep=" + roundingStep +
                ", isHypoRisk=" + isHypoRisk +
                ", advisoryNote='" + advisoryNote + '\'' +
                '}';
    }
}
