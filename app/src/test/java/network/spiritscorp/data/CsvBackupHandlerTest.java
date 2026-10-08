package network.spiritscorp.data;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static network.spiritscorp.data.CsvBackupHandler.CSV_HEAD;

import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Unit tests for CsvBackupHandler with complete field verification.
 * All tests use static test data sets for deterministic export/parsing verification.
 */
public class CsvBackupHandlerTest {

    private List<LogCsvPair> testList;
    private CsvBackupHandler csvHandler;

    @BeforeEach
    public void setUp() {
        csvHandler = new CsvBackupHandler();

        LogCsvPair testData1 = createLogPair(53, 1790242521279L, "Kürbis-Spinat-Auflauf", 19.0, CarbUnit.GRAMS, 19.0, 1.58, 1.9, TimeOfDay.NOON, 0.4, 0.63, 0.0, 140.0, 150.0, 0.0, 0.63, 0.5, "");
        LogCsvPair testData2 = createLogPair(54, 1790287151943L, "Eine Banane", 2.0, CarbUnit.BE, 24.0, 2.0, 2.4, TimeOfDay.EVENING, 0.65, 1.3, 0.0, 110.0, 50.0, 0.0, 1.3, 1.5, "__$SS");
        LogCsvPair testData3 = createLogPair(49, 1789870724133L, "Nachts (88.0g KH)", 88.0, CarbUnit.GRAMS, 88.0, 7.33, 8.8, TimeOfDay.NIGHT, 0.65, 4.77, 70.0, 110.0, 65.0, -0.8, 3.97, 4.0, "HallÖÖ daßasas");
        LogCsvPair testData4 = createLogPair(42, 1789016219837L, "Morgens (70.0g KH)", 7.0, CarbUnit.KE, 70.0, 5.83, 7.0, TimeOfDay.MORNING, 0.5, 2.92, 130.0, 100.0, 40.0, 0.4, 3.32, 3.5, "Text");

        testList = List.of(testData1, testData2, testData3, testData4);
    }

    private record LogCsvPair(CalculationLog log, String csvLine) {
    }

    private LogCsvPair createLogPair(
            int id, long time, String title, double rawCarb, CarbUnit carbUnit, double carbG, double beValue, double keValue, TimeOfDay timeOfDay, double insFac, double mIns,
            Double bGlc, Double tGlc, Double cFct, Double cIns, double tIns, double rIns, String note) {

        CalculationLog log = new CalculationLog();
        log.setId(id);
        log.setTimestamp(time);
        log.setMealTitle(title);
        log.setRawCarbInput(rawCarb);
        log.setCarbUnit(carbUnit);
        log.setCarbGrams(carbG);
        log.setBeValue(beValue);
        log.setKeValue(keValue);
        log.setTimeOfDay(timeOfDay);
        log.setInsulinFactor(insFac);
        log.setMealInsulin(mIns);
        log.setBloodGlucose(bGlc);
        log.setTargetGlucose(tGlc);
        log.setCorrectionFactor(cFct);
        log.setCorrectionInsulin(cIns);
        log.setTotalInsulin(tIns);
        log.setRoundedInsulin(rIns);
        log.setNotes(note);

        String csvLine = log.getId() + "," +
                log.getTimestamp() + "," +
                csvHandler.escapeCsv(DateTimeUtils.formatIsoDateTime(log.getTimestamp())) + "," +
                csvHandler.escapeCsv(log.getMealTitle()) + "," +
                log.getRawCarbInput() + "," +
                csvHandler.escapeCsv(log.getCarbUnit().name()) + "," +
                log.getCarbGrams() + "," +
                log.getBeValue() + "," +
                log.getKeValue() + "," +
                csvHandler.escapeCsv(log.getTimeOfDay().name()) + "," +
                log.getInsulinFactor() + "," +
                log.getMealInsulin() + "," +
                (log.getBloodGlucose() != null ? log.getBloodGlucose() : "") + "," +
                (log.getTargetGlucose() != null ? log.getTargetGlucose() : "") + "," +
                (log.getCorrectionFactor() != null ? log.getCorrectionFactor() : "") + "," +
                (log.getCorrectionInsulin() != null ? log.getCorrectionInsulin() : "") + "," +
                log.getTotalInsulin() + "," +
                log.getRoundedInsulin() + "," +
                csvHandler.escapeCsv(log.getNotes());

        return new LogCsvPair(log, csvLine);
    }

    @Test
    public void exportToCsvFullVerifyTest() {
        StringBuilder expectedCsv = new StringBuilder(CSV_HEAD);
        List<CalculationLog> logs = new ArrayList<>();
        for (LogCsvPair pair : testList) {
            expectedCsv.append(pair.csvLine).append("\n");
            logs.add(pair.log);
        }

        String actualCsv = csvHandler.exportToCsv(logs);

        assertEquals(expectedCsv.toString(), actualCsv, "Exported CSV must match the expected full output exactly");
    }

    @Test
    public void exportToCsvSpecialCharactersAndEmojisTest() {
        LogCsvPair emojiPair = createLogPair(
                99, 1790242521279L, "Café & Tarte 🥐☕", 45.0, CarbUnit.GRAMS, 45.0, 3.75,4.5, TimeOfDay.MORNING,
                0.5, 2.25, 120.0, 100.0, 40.0, 0.5, 2.75, 3.0, "TestingNotesWithEmoji 😊🚀 🎉 & quotes \"quoted\"!"
        );

        String actualCsv = csvHandler.exportToCsv(List.of(emojiPair.log));
        String expectedCsv = CSV_HEAD + emojiPair.csvLine + "\n";

        assertEquals(expectedCsv, actualCsv, "Exported CSV must correctly escape special characters, quotes, and emojis");
    }

    @Test
    public void exportToCsvHeaderOnlyTest() {
        String actualCsv = csvHandler.exportToCsv(Collections.emptyList());
        assertEquals(CSV_HEAD, actualCsv, "Exported CSV with empty list must contain header and newline only");
    }

    @Test
    public void parseCsvFullVerifyTest() {
        StringBuilder csvContent = new StringBuilder(CSV_HEAD);
        List<CalculationLog> originalLogs = new ArrayList<>();
        for (LogCsvPair pair : testList) {
            csvContent.append(pair.csvLine).append("\n");
            originalLogs.add(pair.log);
        }

        List<CalculationLog> parsedLogs = csvHandler.parseCsv(csvContent.toString());

        assertNotNull(parsedLogs, "Parsed logs should not be null");
        assertEquals(originalLogs.size(), parsedLogs.size(), "Should parse exactly all logs");

        List<Executable> executables = new ArrayList<>();
        for (int i = 0; i < originalLogs.size(); ++i) {
            final int index = i;
            CalculationLog original = originalLogs.get(index);
            CalculationLog parsed = parsedLogs.get(index);

            executables.add(() -> assertEquals(original.getId(), parsed.getId(), "ID mismatch at index " + index));
            executables.add(() -> assertEquals(original.getTimestamp(), parsed.getTimestamp(), "Timestamp mismatch at index " + index));
            executables.add(() -> assertEquals(original.getMealTitle(), parsed.getMealTitle(), "MealTitle mismatch at index " + index));
            executables.add(() -> assertEquals(original.getRawCarbInput(), parsed.getRawCarbInput(), "RawCarbInput mismatch at index " + index));
            executables.add(() -> assertEquals(original.getCarbUnit(), parsed.getCarbUnit(), "CarbUnit mismatch at index " + index));
            executables.add(() -> assertEquals(original.getCarbGrams(), parsed.getCarbGrams(), "CarbGrams mismatch at index " + index));
            executables.add(() -> assertEquals(original.getBeValue(), parsed.getBeValue(), "BE mismatch at index " + index));
            executables.add(() -> assertEquals(original.getKeValue(), parsed.getKeValue(), "KE mismatch at index " + index));
            executables.add(() -> assertEquals(original.getTimeOfDay(), parsed.getTimeOfDay(), "TimeOfDay mismatch at index " + index));
            executables.add(() -> assertEquals(original.getInsulinFactor(), parsed.getInsulinFactor(), "InsulinFactor mismatch at index " + index));
            executables.add(() -> assertEquals(original.getMealInsulin(), parsed.getMealInsulin(), "MealInsulin mismatch at index " + index));
            executables.add(() -> assertEquals(original.getBloodGlucose(), parsed.getBloodGlucose(), "BloodGlucose mismatch at index " + index));
            executables.add(() -> assertEquals(original.getTargetGlucose(), parsed.getTargetGlucose(), "TargetGlucose mismatch at index " + index));
            executables.add(() -> assertEquals(original.getCorrectionFactor(), parsed.getCorrectionFactor(), "CorrectionFactor mismatch at index " + index));
            executables.add(() -> assertEquals(original.getCorrectionInsulin(), parsed.getCorrectionInsulin(), "CorrectionInsulin mismatch at index " + index));
            executables.add(() -> assertEquals(original.getTotalInsulin(), parsed.getTotalInsulin(), "TotalInsulin mismatch at index " + index));
            executables.add(() -> assertEquals(original.getRoundedInsulin(), parsed.getRoundedInsulin(), "RoundedInsulin mismatch at index " + index));
            executables.add(() -> assertEquals(original.getNotes(), parsed.getNotes(), "Notes mismatch at index " + index));
        }

        assertAll("Parse CSV full verification with field-by-field check", executables);
    }

    @Test
    public void parseCsvMalformedDefaultsVerificationTest() {
        // Construct a CSV line with invalid / malformed numeric fields to verify fallback default values in parseCsv
        // Format: ID,Timestamp,Date,MealTitle,RawCarbInput,CarbUnit,CarbGrams,BE,KE,TimeOfDay,InsulinFactor,MealInsulin,BloodGlucose,TargetGlucose,CorrectionFactor,CorrectionInsulin,TotalInsulin,RoundedInsulin,Notes
        String malformedRow = "abc,invalidTime,2026-03-01T12:00:00,InvalidTest,badCarb,UNKNOWN_UNIT,badGrams,badBE,badKE,UNKNOWN_TIME,badFac,badMealIns,badGlc,badTarget,badCorrFct,badCorrIns,badTotal,badRounded";
        String csvContent = CSV_HEAD + malformedRow + "\n";

        List<CalculationLog> parsedLogs = csvHandler.parseCsv(csvContent);

        assertNotNull(parsedLogs, "Parsed logs should not be null");
        assertEquals(1, parsedLogs.size(), "Should parse exactly one log with fallback defaults");

        CalculationLog log = parsedLogs.getFirst();
        List<Executable> executables = new ArrayList<>();
        executables.add(() -> assertEquals(0L, log.getId(), "Default ID should be 0"));
        executables.add(() -> assertEquals(0.0, log.getRawCarbInput(), "Default raw carb input should be 0.0"));
        executables.add(() -> assertEquals(CarbUnit.GRAMS, log.getCarbUnit(), "Default carb unit should be GRAMS"));
        executables.add(() -> assertEquals(0.0, log.getCarbGrams(), "Default carb grams should be 0.0"));
        executables.add(() -> assertEquals(0.0, log.getBeValue(), "Default BE should be 0.0"));
        executables.add(() -> assertEquals(0.0, log.getKeValue(), "Default KE should be 0.0"));
        executables.add(() -> assertEquals(TimeOfDay.MORNING, log.getTimeOfDay(), "Default time of day should be MORNING"));
        executables.add(() -> assertEquals(1.0, log.getInsulinFactor(), "Default insulin factor should be 1.0"));
        executables.add(() -> assertEquals(0.0, log.getMealInsulin(), "Default meal insulin should be 0.0"));
        executables.add(() -> assertNull(log.getBloodGlucose(), "Default blood glucose should be null"));
        executables.add(() -> assertNull(log.getTargetGlucose(), "Default target glucose should be null"));
        executables.add(() -> assertNull(log.getCorrectionFactor(), "Default correction factor should be null"));
        executables.add(() -> assertNull(log.getCorrectionInsulin(), "Default correction insulin should be null"));
        executables.add(() -> assertEquals(0.0, log.getTotalInsulin(), "Default total insulin should be 0.0"));
        executables.add(() -> assertEquals(0.0, log.getRoundedInsulin(), "Default rounded insulin should be 0.0"));
        executables.add(() -> assertEquals("", log.getNotes(), "Notes should correctly preserve unicode and emojis"));

        assertAll("Verify fallback defaults and special characters / emojis in parseCsv", executables);
    }

    @Test
    public void parseCsvEmptyOrBlankTest() {
        List<CalculationLog> emptyResult = csvHandler.parseCsv("");
        List<CalculationLog> blankResult = csvHandler.parseCsv("   \n  ");

        List<Executable> executables = new ArrayList<>();
        executables.add(() -> assertTrue(emptyResult.isEmpty(), "Empty string should yield empty log list"));
        executables.add(() -> assertTrue(blankResult.isEmpty(), "Blank string should yield empty log list"));

        assertAll("Parse empty/blank edge cases", executables);
    }

    @Test
    public void parseCsvMalformedLineTest() {
        StringBuilder csvContent = new StringBuilder(CSV_HEAD);
        List<CalculationLog> originalLogs = new ArrayList<>();
        int i = 0;
        for (LogCsvPair pair : testList) {
            csvContent.append(pair.csvLine).append("\n");
            originalLogs.add(pair.log);
            if (++i == 2) {
                csvContent.append("_MALFORMED,_LINE,_TOO,_FEW,_TOKENS\n");
            }
        }
        List<CalculationLog> parsedLogs = csvHandler.parseCsv(csvContent.toString());

        assertNotNull(parsedLogs, "Parsed logs should not be null");
        assertEquals(originalLogs.size(), parsedLogs.size(), "Should skip malformed line and parse valid ones");
        
        List<Executable> executables = new ArrayList<>();
        for (int idx = 0; idx < originalLogs.size(); idx++) {
            final int index = idx;
            executables.add(() -> assertEquals(originalLogs.get(index), parsedLogs.get(index), "Parsed log at index " + index + " must match original"));
        }
        assertAll("Malformed line test verification", executables);
    }
}
