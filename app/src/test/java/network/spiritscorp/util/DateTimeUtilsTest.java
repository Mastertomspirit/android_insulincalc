package network.spiritscorp.util;

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

import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class DateTimeUtilsTest {

    @Test
    public void testPatternConstantsAreValid() {
        assertEquals("dd.MM.yyyy HH:mm", DateTimeUtils.PATTERN_DISPLAY_DATETIME);
        assertEquals("dd.MM.yyyy • HH:mm", DateTimeUtils.PATTERN_DISPLAY_DATETIME_BULLET);
        assertEquals("dd.MM.yyyy", DateTimeUtils.PATTERN_DISPLAY_DATE);
        assertEquals("HH:mm", DateTimeUtils.PATTERN_DISPLAY_TIME);
        assertEquals("yyyy-MM-dd HH:mm:ss", DateTimeUtils.PATTERN_ISO_DATETIME);
        assertEquals("yyyyMMdd_HHmm", DateTimeUtils.PATTERN_FILENAME_TIMESTAMP);
    }

    @Test
    public void testFormattersProduceNonEmptyOutputs() {
        long now = System.currentTimeMillis();

        String displayDateTime = DateTimeUtils.formatDisplayDateTime(now);
        assertNotNull(displayDateTime);
        assertTrue(displayDateTime.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}"));

        String bulletDateTime = DateTimeUtils.formatDisplayDateTimeBullet(now);
        assertNotNull(bulletDateTime);
        assertTrue(bulletDateTime.matches("\\d{2}\\.\\d{2}\\.\\d{4} • \\d{2}:\\d{2}"));

        String displayDate = DateTimeUtils.formatDisplayDate(now);
        assertNotNull(displayDate);
        assertTrue(displayDate.matches("\\d{2}\\.\\d{2}\\.\\d{4}"));

        String displayTime = DateTimeUtils.formatDisplayTime(now);
        assertNotNull(displayTime);
        assertTrue(displayTime.matches("\\d{2}:\\d{2}"));

        String isoDateTime = DateTimeUtils.formatIsoDateTime(now);
        assertNotNull(isoDateTime);
        assertTrue(isoDateTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));

        String filenameTimestamp = DateTimeUtils.getFilenameTimestamp();
        assertNotNull(filenameTimestamp);
        assertTrue(filenameTimestamp.matches("\\d{8}_\\d{4}"));
    }

    @Test
    public void testAppConstantsVersions() {
        assertEquals(1, AppConstants.DATABASE_VERSION);
        assertEquals(1, AppConstants.JSON_BACKUP_VERSION);
        assertEquals(1, AppConstants.CSV_BACKUP_VERSION);
        assertEquals(2, AppConstants.SECURITY_KEY_VERSION);
        assertEquals("insulin_calculator.db", AppConstants.DATABASE_NAME);
    }
}
