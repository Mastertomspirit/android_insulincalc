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

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Defines SQL schema migration paths for Room database upgrades between schema versions.
 * <p>
 * Ensures safe, non-destructive schema evolution as new entities or columns are introduced.
 */
public class DatabaseMigrations {

    /**
     * Migrates the database from version 1 to version 2.
     *
     * <p>Converts human-readable legacy strings and localized values to proper Room enum constant names
     * across multiple tables and columns:</p>
     * <ul>
     *     <li><b>{@code user_settings.glucoseUnit}:</b> Maps {@code "mg/dl"} and {@code "mmol/l"}
     *         to enum names {@code "MG_DL"} and {@code "MMOL_L"} (default: {@code "MG_DL"}).</li>
     *     <li><b>{@code user_settings.defaultCarbUnit} & {@code calculation_logs.carbUnit}:</b>
     *         Maps legacy unit strings ({@code "g KH"}, {@code "KE"}, {@code "BE"}) to enum names
     *         ({@code "GRAMS"}, {@code "KE"}, {@code "BE"} - default: {@code "GRAMS"}).</li>
     *     <li><b>{@code calculation_logs.timeOfDay}:</b> Maps German time-of-day descriptors
     *         ({@code "Morgens"}, {@code "Mittags"}, {@code "Abend"}, {@code "Nacht"})
     *         to standard enum names ({@code "MORNING"}, {@code "NOON"}, {@code "EVENING"}, {@code "NIGHT"} - default: {@code "MORNING"}).</li>
     *     <li><b>{@code user_settings.selectedAiModel}:</b> Maps legacy model identifiers
     *         ({@code "gemini.3.5"}, {@code "gemini-flash-lite-latest"}) to valid enum constants
     *         ({@code "GEMINI_FLASH_3_5"}, {@code "GEMINI_FLASH_LITE_LATEST"} - default: {@code "GEMINI_FLASH_LITE_LATEST"}).</li>
     * </ul>
     */
    final static Migration MIGRATION_1_2 = new Migration(1, 2) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Convert glucose units to enum names
            database.execSQL(
                    "UPDATE user_settings " +
                            "SET glucoseUnit = CASE glucoseUnit " +
                            "WHEN 'mg/dl' THEN 'MG_DL' " +
                            "WHEN 'mmol/l' THEN 'MMOL_L' " +
                            "ELSE 'MG_DL' END"
            );

            // Convert carbohydrate units to enum names
            database.execSQL(
                    "UPDATE user_settings " +
                            "SET defaultCarbUnit = CASE defaultCarbUnit " +
                            "WHEN 'g KH' THEN 'GRAMS' " +
                            "WHEN 'KE' THEN 'KE' " +
                            "WHEN 'BE' THEN 'BE' " +
                            "ELSE 'GRAMS' END"
            );

            // Convert Time of Day to enum names
            database.execSQL(
                    "UPDATE calculation_logs " +
                            "SET timeOfDay = CASE timeOfDay " +
                            "WHEN 'Morgens' THEN 'MORNING' " +
                            "WHEN 'Mittags' THEN 'NOON' " +
                            "WHEN 'Abend' THEN 'EVENING' " +
                            "WHEN 'Nacht' THEN 'NIGHT' " +
                            "ELSE 'MORNING' END"
            );

            // Convert carbohydrate units to enum names
            database.execSQL(
                    "UPDATE calculation_logs " +
                            "SET carbUnit = CASE carbUnit " +
                            "WHEN 'g KH' THEN 'GRAMS' " +
                            "WHEN 'KE' THEN 'KE' " +
                            "WHEN 'BE' THEN 'BE' " +
                            "ELSE 'GRAMS' END"
            );

            database.execSQL("UPDATE user_settings " +
                    "SET selectedAiModel = CASE selectedAiModel " +
                    "WHEN 'gemini.3.5' THEN 'GEMINI_FLASH_3_5' " +
                    "WHEN 'gemini-flash-lite-latest' THEN 'GEMINI_FLASH_LITE_LATEST' " +
                    "ELSE 'GEMINI_FLASH_LITE_LATEST' END"
            );
        }
    };

    /**
     * Migrates the database from version 2 to version 3.
     *
     * <p>The model identifiers stored in the {@code selectedAiModel} column of the
     * {@code user_settings} table are updated due to API changes. Deprecated or
     * removed model constants are mapped to their valid modern equivalents
     * (e.g., mapping {@code GEMINI_3_1_PRO} to {@code GEMINI_3_1_FLASH_LITE},
     * and updating legacy flash aliases to {@code GEMINI_3_5_FLASH_LITE}
     * and {@code GEMINI_3_5_FLASH}).</p>
     */
    final static Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("UPDATE user_settings " +
                    "SET selectedAiModel = CASE selectedAiModel " +
                    "WHEN 'GEMINI_3_1_PRO' THEN 'GEMINI_3_1_FLASH_LITE' " +
                    "WHEN 'GEMINI_FLASH_LITE_LATEST' THEN 'GEMINI_3_5_FLASH_LITE' " +
                    "WHEN 'GEMINI_FLASH_LATEST' THEN 'GEMINI_3_5_FLASH' " +
                    "ELSE selectedAiModel END");
        }
    };
}
