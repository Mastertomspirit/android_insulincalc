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

/**
 * Room Entity representing a persisted insulin calculation log entry.
 */
@Entity(tableName = "calculation_logs")
public class CalculationLog {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    private String mealTitle;
    private double rawCarbInput;
    private String carbUnit;
    private double carbGrams;
    private double beValue;
    private double keValue;
    private String timeOfDay;
    private double insulinFactor;
    private double mealInsulin;
    private Double bloodGlucose;
    private Double targetGlucose;
    private Double correctionFactor;
    private Double correctionInsulin;
    private double totalInsulin;
    private double roundedInsulin;
    private String notes;

    public CalculationLog() {
        this.id = 0;
        this.timestamp = System.currentTimeMillis();
        this.mealTitle = "Mahlzeit";
        this.rawCarbInput = 0.0;
        this.carbUnit = "g KH";
        this.carbGrams = 0.0;
        this.beValue = 0.0;
        this.keValue = 0.0;
        this.timeOfDay = "Morgens";
        this.insulinFactor = 1.0;
        this.mealInsulin = 0.0;
        this.bloodGlucose = null;
        this.targetGlucose = null;
        this.correctionFactor = null;
        this.correctionInsulin = null;
        this.totalInsulin = 0.0;
        this.roundedInsulin = 0.0;
        this.notes = "";
    }

    @Ignore
    public CalculationLog(
            long id,
            long timestamp,
            String mealTitle,
            double rawCarbInput,
            String carbUnit,
            double carbGrams,
            double beValue,
            double keValue,
            String timeOfDay,
            double insulinFactor,
            double mealInsulin,
            Double bloodGlucose,
            Double targetGlucose,
            Double correctionFactor,
            Double correctionInsulin,
            double totalInsulin,
            double roundedInsulin,
            String notes
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.mealTitle = mealTitle != null ? mealTitle : "Mahlzeit";
        this.rawCarbInput = rawCarbInput;
        this.carbUnit = carbUnit != null ? carbUnit : "g KH";
        this.carbGrams = carbGrams;
        this.beValue = beValue;
        this.keValue = keValue;
        this.timeOfDay = timeOfDay != null ? timeOfDay : "Morgens";
        this.insulinFactor = insulinFactor;
        this.mealInsulin = mealInsulin;
        this.bloodGlucose = bloodGlucose;
        this.targetGlucose = targetGlucose;
        this.correctionFactor = correctionFactor;
        this.correctionInsulin = correctionInsulin;
        this.totalInsulin = totalInsulin;
        this.roundedInsulin = roundedInsulin;
        this.notes = notes != null ? notes : "";
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMealTitle() {
        return mealTitle;
    }

    public void setMealTitle(String mealTitle) {
        this.mealTitle = mealTitle != null ? mealTitle : "Mahlzeit";
    }

    public double getRawCarbInput() {
        return rawCarbInput;
    }

    public void setRawCarbInput(double rawCarbInput) {
        this.rawCarbInput = rawCarbInput;
    }

    public String getCarbUnit() {
        return carbUnit;
    }

    public void setCarbUnit(String carbUnit) {
        this.carbUnit = carbUnit != null ? carbUnit : "g KH";
    }

    public double getCarbGrams() {
        return carbGrams;
    }

    public void setCarbGrams(double carbGrams) {
        this.carbGrams = carbGrams;
    }

    public double getBeValue() {
        return beValue;
    }

    public void setBeValue(double beValue) {
        this.beValue = beValue;
    }

    public double getKeValue() {
        return keValue;
    }

    public void setKeValue(double keValue) {
        this.keValue = keValue;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay != null ? timeOfDay : "Morgens";
    }

    public double getInsulinFactor() {
        return insulinFactor;
    }

    public void setInsulinFactor(double insulinFactor) {
        this.insulinFactor = insulinFactor;
    }

    public double getMealInsulin() {
        return mealInsulin;
    }

    public void setMealInsulin(double mealInsulin) {
        this.mealInsulin = mealInsulin;
    }

    public Double getBloodGlucose() {
        return bloodGlucose;
    }

    public void setBloodGlucose(Double bloodGlucose) {
        this.bloodGlucose = bloodGlucose;
    }

    public Double getTargetGlucose() {
        return targetGlucose;
    }

    public void setTargetGlucose(Double targetGlucose) {
        this.targetGlucose = targetGlucose;
    }

    public Double getCorrectionFactor() {
        return correctionFactor;
    }

    public void setCorrectionFactor(Double correctionFactor) {
        this.correctionFactor = correctionFactor;
    }

    public Double getCorrectionInsulin() {
        return correctionInsulin;
    }

    public void setCorrectionInsulin(Double correctionInsulin) {
        this.correctionInsulin = correctionInsulin;
    }

    public double getTotalInsulin() {
        return totalInsulin;
    }

    public void setTotalInsulin(double totalInsulin) {
        this.totalInsulin = totalInsulin;
    }

    public double getRoundedInsulin() {
        return roundedInsulin;
    }

    public void setRoundedInsulin(double roundedInsulin) {
        this.roundedInsulin = roundedInsulin;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes : "";
    }

    public CalculationLog copy() {
        return new CalculationLog(
                this.id,
                this.timestamp,
                this.mealTitle,
                this.rawCarbInput,
                this.carbUnit,
                this.carbGrams,
                this.beValue,
                this.keValue,
                this.timeOfDay,
                this.insulinFactor,
                this.mealInsulin,
                this.bloodGlucose,
                this.targetGlucose,
                this.correctionFactor,
                this.correctionInsulin,
                this.totalInsulin,
                this.roundedInsulin,
                this.notes
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CalculationLog that = (CalculationLog) o;
        return id == that.id &&
                timestamp == that.timestamp &&
                Double.compare(that.rawCarbInput, rawCarbInput) == 0 &&
                Double.compare(that.carbGrams, carbGrams) == 0 &&
                Double.compare(that.beValue, beValue) == 0 &&
                Double.compare(that.keValue, keValue) == 0 &&
                Double.compare(that.insulinFactor, insulinFactor) == 0 &&
                Double.compare(that.mealInsulin, mealInsulin) == 0 &&
                Double.compare(that.totalInsulin, totalInsulin) == 0 &&
                Double.compare(that.roundedInsulin, roundedInsulin) == 0 &&
                Objects.equals(mealTitle, that.mealTitle) &&
                Objects.equals(carbUnit, that.carbUnit) &&
                Objects.equals(timeOfDay, that.timeOfDay) &&
                Objects.equals(bloodGlucose, that.bloodGlucose) &&
                Objects.equals(targetGlucose, that.targetGlucose) &&
                Objects.equals(correctionFactor, that.correctionFactor) &&
                Objects.equals(correctionInsulin, that.correctionInsulin) &&
                Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, timestamp, mealTitle, rawCarbInput, carbUnit, carbGrams, beValue,
                keValue, timeOfDay, insulinFactor, mealInsulin, bloodGlucose,
                targetGlucose, correctionFactor, correctionInsulin, totalInsulin,
                roundedInsulin, notes
        );
    }
}
