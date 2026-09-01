package network.spiritscorp;

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

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import kotlin.Pair;
import network.spiritscorp.data.AppDatabase;
import network.spiritscorp.data.DatabaseBackupManager;
import network.spiritscorp.data.InsulinRepository;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.CarbUnit;
import network.spiritscorp.model.TimeOfDay;
import network.spiritscorp.model.UserSettings;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * On-Device / Instrumentation Test Suite for Android execution environments (in Java).
 *<br><br>
 * Verifies Android Context binding, package identifier, database instantiation
 * on target device, and Android-level serialization / backup operations.
 */
@RunWith(AndroidJUnit4.class)
public class InsulinCalculatorInstrumentedTest {

    @Test
    public void testAppContextAndPackageIdentity() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertNotNull(appContext);
        assertEquals("network.spiritscorp.insulincalc", appContext.getPackageName());
    }

    @Test
    public void testRoomDatabaseInstanceCreationOnDevice() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase database = AppDatabase.getDatabase(appContext);
        assertNotNull(database);
        assertNotNull(database.calculationLogDao());
        assertNotNull(database.userSettingsDao());
    }

    @Test
    public void testRepositoryAndDeviceBackupWorkflow() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppDatabase database = AppDatabase.getDatabase(appContext);
        InsulinRepository repository = new InsulinRepository(database.calculationLogDao(), database.userSettingsDao());

        UserSettings settings = repository.getSettings();
        assertNotNull(settings);
        assertTrue(settings.getMorningFactor() > 0.0);

        CalculationLog sampleLog = new CalculationLog(
                0,
                System.currentTimeMillis(),
                "Test On-Device Mahlzeit",
                40.0,
                CarbUnit.GRAMS.getShortName(),
                40.0,
                3.33,
                4.0,
                TimeOfDay.NOON.getTitle(),
                1.0,
                3.33,
                null,
                null,
                null,
                null,
                3.33,
                3.5,
                "Notiz"
        );

        DatabaseBackupManager backupManager = new DatabaseBackupManager(database);
        String json = backupManager.exportToJson(settings, Collections.singletonList(sampleLog));
        assertTrue(json.contains("Test On-Device Mahlzeit"));
        assertTrue(json.contains("settings"));

        Pair<UserSettings, List<CalculationLog>> parsed = backupManager.parseJson(json);
        assertNotNull(parsed);
        assertNotNull(parsed.getSecond());
        assertEquals(1, parsed.getSecond().size());
        assertEquals("Test On-Device Mahlzeit", parsed.getSecond().getFirst().getMealTitle());
    }
}
