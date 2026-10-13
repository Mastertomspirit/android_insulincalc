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
import java.util.Objects;

/**
 * Result model for database backup import operations in Java, providing detailed status feedback.
 */
public class ImportResult {

    /** Indicates whether the import operation completed successfully. */
    public final boolean success;
    /** Number of calculation log entries successfully imported. */
    public final int importedLogsCount;
    /** Indicates whether user settings were successfully imported and updated. */
    public final boolean importedSettings;
    /** Detailed human-readable status or error message describing the import outcome. */
    @NonNull
    public final String message;

    /**
     * Constructs a new ImportResult instance.
     *
     * @param success Whether the import was successful.
     * @param importedLogsCount Number of imported calculation logs.
     * @param importedSettings Whether settings were imported.
     * @param message Status message.
     */
    public ImportResult(boolean success, int importedLogsCount, boolean importedSettings, @NonNull String message) {
        this.success = success;
        this.importedLogsCount = importedLogsCount;
        this.importedSettings = importedSettings;
        this.message = message;
    }

    /** Returns whether the import operation completed successfully. */
    public boolean isSuccess() {
        return success;
    }

    /** Returns the number of calculation log entries successfully imported. */
    public int getImportedLogsCount() {
        return importedLogsCount;
    }

    /** Returns whether user settings were successfully imported and updated. */
    public boolean isImportedSettings() {
        return importedSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportResult that)) return false;
        return success == that.success &&
                importedLogsCount == that.importedLogsCount &&
                importedSettings == that.importedSettings &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, importedLogsCount, importedSettings, message);
    }

    @NonNull
    @Override
    public String toString() {
        return "ImportResult{" +
                "success=" + success +
                ", importedLogsCount=" + importedLogsCount +
                ", importedSettings=" + importedSettings +
                ", message='" + message + '\'' +
                '}';
    }
}
