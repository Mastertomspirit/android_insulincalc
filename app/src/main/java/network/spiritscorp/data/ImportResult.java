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

    public final boolean success;
    public final int importedLogsCount;
    public final boolean importedSettings;
    @NonNull
    public final String message;

    public ImportResult(boolean success, int importedLogsCount, boolean importedSettings, @NonNull String message) {
        this.success = success;
        this.importedLogsCount = importedLogsCount;
        this.importedSettings = importedSettings;
        this.message = message;
    }

    public ImportResult(boolean success, @NonNull String message) {
        this(success, 0, false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean getSuccess() {
        return success;
    }

    public int getImportedLogsCount() {
        return importedLogsCount;
    }

    public boolean isImportedSettings() {
        return importedSettings;
    }

    public boolean getImportedSettings() {
        return importedSettings;
    }

    @NonNull
    public String getMessage() {
        return message;
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
