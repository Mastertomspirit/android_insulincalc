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

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.Objects;

/**
 * Room Entity representing a persisted insulin calculation log entry.
 * Stores comprehensive details of a meal calculation, including carbohydrate intake,
 * blood glucose measurements, correction parameters, calculated insulin doses, and user notes.
 */
@Entity(tableName = "calculation_logs")
public class CalculationLog {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long timestamp;
    private String mealTitle;
    private double rawCarbInput;
    private CarbUnit carbUnit;
    private double carbGrams;
    private double beValue;
    private double keValue;
    private TimeOfDay timeOfDay;
    private double insulinFactor;
    private double mealInsulin;
    @Nullable
    private Double bloodGlucose;
    @Nullable
    private Double targetGlucose;
    @Nullable
    private Double correctionFactor;
    @Nullable
    private Double correctionInsulin;
    private double totalInsulin;
    private double roundedInsulin;
    private String notes;

    /**
     * Default constructor for {@link CalculationLog}.
     * Initializes a new log entry with default values: current timestamp, empty meal title,
     * zero carbohydrate inputs, default unit {@link CarbUnit#GRAMS}, current {@link TimeOfDay},
     * default insulin factor of 1.0, null glucose and correction fields, zero insulin totals, and empty notes.
     */
    public CalculationLog() {
        this.id = 0;
        this.timestamp = System.currentTimeMillis();
        this.mealTitle = "";
        this.rawCarbInput = 0.0;
        this.carbUnit = CarbUnit.GRAMS;
        this.carbGrams = 0.0;
        this.beValue = 0.0;
        this.keValue = 0.0;
        this.timeOfDay = TimeOfDay.current();
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

    /**
     * Parameterized constructor for {@link CalculationLog} annotated with {@link Ignore} for Room.
     * Used to instantiate a fully populated calculation log with specific database IDs and values.
     *
     * @param id                  the unique database primary key ID
     * @param timestamp           the epoch timestamp in milliseconds when the calculation occurred
     * @param mealTitle           the title or description of the meal
     * @param rawCarbInput        the raw carbohydrate amount entered by the user
     * @param carbUnit            the carbohydrate unit used ({@link CarbUnit})
     * @param carbGrams           the converted carbohydrate amount in grams
     * @param beValue             the calculated Bread Units (BE)
     * @param keValue             the calculated Carbohydrate Units (KE)
     * @param timeOfDay           the time of day category ({@link TimeOfDay})
     * @param insulinFactor       the insulin-to-carb factor applied
     * @param mealInsulin         the calculated insulin units required for the meal
     * @param bloodGlucose        the measured blood glucose level (nullable)
     * @param targetGlucose       the target blood glucose level (nullable)
     * @param correctionFactor    the correction factor used (nullable)
     * @param correctionInsulin   the calculated correction insulin units (nullable)
     * @param totalInsulin        the exact total insulin sum (meal + correction)
     * @param roundedInsulin      the final rounded insulin dose administered
     * @param notes               additional user notes or remarks
     */
    @Ignore
    public CalculationLog(
            long id,
            long timestamp,
            String mealTitle,
            double rawCarbInput,
            CarbUnit carbUnit,
            double carbGrams,
            double beValue,
            double keValue,
            TimeOfDay timeOfDay,
            double insulinFactor,
            double mealInsulin,
            @Nullable
            Double bloodGlucose,
            @Nullable
            Double targetGlucose,
            @Nullable
            Double correctionFactor,
            @Nullable
            Double correctionInsulin,
            double totalInsulin,
            double roundedInsulin,
            String notes
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.mealTitle = mealTitle;
        this.rawCarbInput = rawCarbInput;
        this.carbUnit = carbUnit;
        this.carbGrams = carbGrams;
        this.beValue = beValue;
        this.keValue = keValue;
        this.timeOfDay = timeOfDay;
        this.insulinFactor = insulinFactor;
        this.mealInsulin = mealInsulin;
        this.bloodGlucose = bloodGlucose;
        this.targetGlucose = targetGlucose;
        this.correctionFactor = correctionFactor;
        this.correctionInsulin = correctionInsulin;
        this.totalInsulin = totalInsulin;
        this.roundedInsulin = roundedInsulin;
        this.notes = notes;
    }

    /**
     * Returns the unique database primary key ID.
     *
     * @return the record ID
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the unique database primary key ID.
     *
     * @param id the record ID to set
     */
    public void setId(long id) { this.id = id; }

    /**
     * Returns the epoch timestamp in milliseconds when the calculation was logged.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the epoch timestamp in milliseconds when the calculation was logged.
     *
     * @param timestamp the timestamp in milliseconds to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the title or description of the meal.
     *
     * @return the meal title string
     */
    public String getMealTitle() {
        return mealTitle;
    }

    /**
     * Sets the title or description of the meal.
     *
     * @param mealTitle the meal title string to set
     */
    public void setMealTitle(String mealTitle) {
        this.mealTitle = mealTitle;
    }

    /**
     * Returns the raw carbohydrate input entered by the user.
     *
     * @return the raw carb input value
     */
    public double getRawCarbInput() {
        return rawCarbInput;
    }

    /**
     * Sets the raw carbohydrate input entered by the user.
     *
     * @param rawCarbInput the raw carb input value to set
     */
    public void setRawCarbInput(double rawCarbInput) {
        this.rawCarbInput = rawCarbInput;
    }

    /**
     * Returns the carbohydrate unit used for the entry.
     *
     * @return the {@link CarbUnit} instance
     */
    public CarbUnit getCarbUnit() {
        return carbUnit;
    }

    /**
     * Sets the carbohydrate unit used for the entry.
     *
     * @param carbUnit the {@link CarbUnit} to set
     */
    public void setCarbUnit(CarbUnit carbUnit) {
        this.carbUnit = carbUnit;
    }

    /**
     * Returns the carbohydrate amount converted to grams.
     *
     * @return the carbohydrates in grams
     */
    public double getCarbGrams() {
        return carbGrams;
    }

    /**
     * Sets the carbohydrate amount converted to grams.
     *
     * @param carbGrams the carbohydrates in grams to set
     */
    public void setCarbGrams(double carbGrams) {
        this.carbGrams = carbGrams;
    }

    /**
     * Returns the calculated Bread Units (BE).
     *
     * @return the BE value
     */
    public double getBeValue() {
        return beValue;
    }

    /**
     * Sets the calculated Bread Units (BE).
     *
     * @param beValue the BE value to set
     */
    public void setBeValue(double beValue) {
        this.beValue = beValue;
    }

    /**
     * Returns the calculated Carbohydrate Units (KE).
     *
     * @return the KE value
     */
    public double getKeValue() {
        return keValue;
    }

    /**
     * Sets the calculated Carbohydrate Units (KE).
     *
     * @param keValue the KE value to set
     */
    public void setKeValue(double keValue) {
        this.keValue = keValue;
    }

    /**
     * Returns the time of day category associated with the calculation.
     *
     * @return the {@link TimeOfDay} instance
     */
    public TimeOfDay getTimeOfDay() {
        return timeOfDay;
    }

    /**
     * Sets the time of day category associated with the calculation.
     *
     * @param timeOfDay the {@link TimeOfDay} to set
     */
    public void setTimeOfDay(TimeOfDay timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    /**
     * Returns the insulin factor applied during the calculation.
     *
     * @return the insulin factor
     */
    public double getInsulinFactor() {
        return insulinFactor;
    }

    /**
     * Sets the insulin factor applied during the calculation.
     *
     * @param insulinFactor the insulin factor to set
     */
    public void setInsulinFactor(double insulinFactor) {
        this.insulinFactor = insulinFactor;
    }

    /**
     * Returns the calculated insulin units required specifically for the meal.
     *
     * @return the meal insulin units
     */
    public double getMealInsulin() {
        return mealInsulin;
    }

    /**
     * Sets the calculated insulin units required specifically for the meal.
     *
     * @param mealInsulin the meal insulin units to set
     */
    public void setMealInsulin(double mealInsulin) {
        this.mealInsulin = mealInsulin;
    }

    /**
     * Returns the measured blood glucose level at the time of calculation, if available.
     *
     * @return the blood glucose value, or {@code null} if not provided
     */
    @Nullable
    public Double getBloodGlucose() {
        return bloodGlucose;
    }

    /**
     * Sets the measured blood glucose level at the time of calculation.
     *
     * @param bloodGlucose the blood glucose value to set, or {@code null}
     */
    public void setBloodGlucose(@Nullable Double bloodGlucose) {
        this.bloodGlucose = bloodGlucose;
    }

    /**
     * Returns the target blood glucose level used for correction, if available.
     *
     * @return the target blood glucose value, or {@code null} if not provided
     */
    @Nullable
    public Double getTargetGlucose() {
        return targetGlucose;
    }

    /**
     * Sets the target blood glucose level used for correction.
     *
     * @param targetGlucose the target blood glucose value to set, or {@code null}
     */
    public void setTargetGlucose(@Nullable Double targetGlucose) {
        this.targetGlucose = targetGlucose;
    }

    /**
     * Returns the correction factor used, if available.
     *
     * @return the correction factor, or {@code null} if not provided
     */
    @Nullable
    public Double getCorrectionFactor() {
        return correctionFactor;
    }

    /**
     * Sets the correction factor used.
     *
     * @param correctionFactor the correction factor to set, or {@code null}
     */
    public void setCorrectionFactor(@Nullable Double correctionFactor) {
        this.correctionFactor = correctionFactor;
    }

    /**
     * Returns the calculated correction insulin units (positive for high glucose, negative for low), if available.
     *
     * @return the correction insulin units, or {@code null} if not provided
     */
    @Nullable
    public Double getCorrectionInsulin() {
        return correctionInsulin;
    }

    /**
     * Sets the calculated correction insulin units.
     *
     * @param correctionInsulin the correction insulin units to set, or {@code null}
     */
    public void setCorrectionInsulin(@Nullable Double correctionInsulin) {
        this.correctionInsulin = correctionInsulin;
    }

    /**
     * Returns the exact unrounded total insulin sum (meal insulin plus correction insulin).
     *
     * @return the total raw insulin units
     */
    public double getTotalInsulin() {
        return totalInsulin;
    }

    /**
     * Sets the exact unrounded total insulin sum.
     *
     * @param totalInsulin the total raw insulin units to set
     */
    public void setTotalInsulin(double totalInsulin) {
        this.totalInsulin = totalInsulin;
    }

    /**
     * Returns the final rounded insulin dose administered or recommended.
     *
     * @return the rounded insulin units
     */
    public double getRoundedInsulin() {
        return roundedInsulin;
    }

    /**
     * Sets the final rounded insulin dose.
     *
     * @param roundedInsulin the rounded insulin units to set
     */
    public void setRoundedInsulin(double roundedInsulin) {
        this.roundedInsulin = roundedInsulin;
    }

    /**
     * Returns additional user notes or remarks associated with the entry.
     *
     * @return the notes string
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets additional user notes or remarks associated with the entry.
     *
     * @param notes the notes string to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Creates and returns a deep copy of this {@link CalculationLog} instance.
     *
     * @return a new {@link CalculationLog} object containing identical field values
     */
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

    /**
     * Compares this calculation log with another object for equality based on all fields.
     * Note: Compares all primitive and object properties.
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
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

    /**
     * Returns a hash code value for this calculation log based on all fields.
     *
     * @return the integer hash code
     */
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
