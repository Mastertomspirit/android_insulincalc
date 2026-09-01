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

import androidx.annotation.NonNull;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import network.spiritscorp.data.CalculationLogDao;
import network.spiritscorp.data.InsulinRepository;
import network.spiritscorp.data.UserSettingsDao;
import network.spiritscorp.model.CalculationLog;
import network.spiritscorp.model.UserSettings;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Java integration tests verifying the interaction between the data access layer,
 * reactive Flows, and the InsulinRepository domain coordinator.
 */
public class InsulinRepositoryIntegrationTest {

    private static final double DELTA = 0.001;

    private FakeUserSettingsDao fakeSettingsDao;
    private InsulinRepository repository;

    @Before
    public void setup() {
        FakeCalculationLogDao fakeLogDao = new FakeCalculationLogDao();
        fakeSettingsDao = new FakeUserSettingsDao();
        repository = new InsulinRepository(fakeLogDao, fakeSettingsDao);
    }

    @Test
    public void testDefaultSettingsCreatedWhenNoneExist() {
        UserSettings settings = repository.getSettings();
        assertNotNull(settings);
        assertEquals(1.50, settings.getMorningFactor(), DELTA);
        assertEquals("GRAMS", settings.getDefaultCarbUnit());
        assertEquals(120.0, settings.getTargetGlucoseMgDl(), DELTA);
        assertEquals(50.0, settings.getCorrectionFactorMgDl(), DELTA);

        // Verify default settings were persisted to DAO
        UserSettings direct = fakeSettingsDao.getSettingsDirect();
        assertNotNull(direct);
        assertEquals(1.50, direct.getMorningFactor(), DELTA);
    }

    @Test
    public void testSaveAndRetrieveCustomSettings() {
        UserSettings custom = new UserSettings(
                1,
                1.75,
                1.15,
                1.40,
                0.85,
                "BE",
                12,
                "mg/dl",
                105.0,
                45.0,
                0.5,
                true,
                "AMBER_WARM",
                "SYSTEM"
        );

        repository.saveSettings(custom);

        UserSettings retrieved = repository.getSettings();
        assertEquals(1.75, retrieved.getMorningFactor(), DELTA);
        assertEquals("BE", retrieved.getDefaultCarbUnit());
        assertEquals("AMBER_WARM", retrieved.getSelectedTheme());

        UserSettings flowValue = firstFromFlow(repository.getSettingsFlow());
        assertNotNull(flowValue);
        assertEquals("AMBER_WARM", flowValue.getSelectedTheme());
    }

    @Test
    public void testSaveSingleCalculationLogAndObserveFlow() {
        CalculationLog log = new CalculationLog(
                0L,
                System.currentTimeMillis(),
                "Mittagessen (Reis mit Hühnchen)",
                60.0,
                "g KH",
                60.0,
                5.0,
                6.0,
                "Mittags",
                1.0,
                5.0,
                125.0,
                100.0,
                40.0,
                0.63,
                5.63,
                5.5,
                "Leichte Sporteinheit danach"
        );

        long id = repository.saveCalculation(log);
        assertTrue(id > 0);

        List<CalculationLog> logsFromFlow = firstFromFlow(repository.getAllLogs());
        assertNotNull(logsFromFlow);
        assertEquals(1, logsFromFlow.size());
        assertEquals("Mittagessen (Reis mit Hühnchen)", logsFromFlow.getFirst().getMealTitle());
        assertEquals(5.5, logsFromFlow.getFirst().getRoundedInsulin(), DELTA);

        List<CalculationLog> directLogs = repository.getAllLogsDirect();
        assertEquals(1, directLogs.size());
    }

    @Test
    public void testSaveMultipleLogsBatchAndOrdering() {
        CalculationLog log1 = new CalculationLog(1L, 1000L, "Mahlzeit 1", 0.0, "g KH", 0.0, 0.0, 0.0, "Morgens", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log2 = new CalculationLog(2L, 2000L, "Mahlzeit 2", 0.0, "g KH", 0.0, 0.0, 0.0, "Mittags", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log3 = new CalculationLog(3L, 3000L, "Mahlzeit 3", 0.0, "g KH", 0.0, 0.0, 0.0, "Abends", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");

        long[] insertedIds = repository.saveLogs(Arrays.asList(log1, log2, log3));
        assertEquals(3, insertedIds.length);

        List<CalculationLog> allLogs = repository.getAllLogsDirect();
        assertEquals(3, allLogs.size());
        // Verify chronological descending order
        assertEquals("Mahlzeit 3", allLogs.get(0).getMealTitle());
        assertEquals("Mahlzeit 2", allLogs.get(1).getMealTitle());
        assertEquals("Mahlzeit 1", allLogs.get(2).getMealTitle());
    }

    @Test
    public void testDeleteLogById() {
        CalculationLog log1 = new CalculationLog(10L, 1000L, "Frühstück", 0.0, "g KH", 0.0, 0.0, 0.0, "Morgens", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log2 = new CalculationLog(20L, 2000L, "Abendessen", 0.0, "g KH", 0.0, 0.0, 0.0, "Abends", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");

        repository.saveLogs(Arrays.asList(log1, log2));
        List<CalculationLog> initialLogs = repository.getAllLogsDirect();
        assertEquals(2, initialLogs.size());

        repository.deleteLog(10L);
        List<CalculationLog> remaining = repository.getAllLogsDirect();
        assertEquals(1, remaining.size());
        assertEquals("Abendessen", remaining.getFirst().getMealTitle());
    }

    @Test
    public void testClearAllLogs() {
        CalculationLog log1 = new CalculationLog(1L, 1000L, "Eintrag 1", 0.0, "g KH", 0.0, 0.0, 0.0, "Morgens", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log2 = new CalculationLog(2L, 2000L, "Eintrag 2", 0.0, "g KH", 0.0, 0.0, 0.0, "Mittags", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        repository.saveLogs(Arrays.asList(log1, log2));

        List<CalculationLog> initialLogs = repository.getAllLogsDirect();
        assertEquals(2, initialLogs.size());

        repository.clearLogs();
        List<CalculationLog> remainingLogs = repository.getAllLogsDirect();
        assertEquals(0, remainingLogs.size());
        List<CalculationLog> flowLogs = firstFromFlow(repository.getAllLogs());
        assertEquals(0, flowLogs.size());
    }

    // --- Coroutine Helper Methods for Flow collection in Java ---

    public static <T> T firstFromFlow(Flow<T> flow) {
        try {
            return BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (_, continuation) -> FlowKt.first(flow, continuation)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Flow collection interrupted", e);
        }
    }

    // --- Fake DAO Implementations ---

    public static class FakeCalculationLogDao implements CalculationLogDao {
        private final List<CalculationLog> logs = new ArrayList<>();
        private final MutableStateFlow<List<CalculationLog>> flow = StateFlowKt.MutableStateFlow(Collections.emptyList());
        private long nextId = 1L;

        private synchronized void updateFlow() {
            List<CalculationLog> sorted = new ArrayList<>(logs);
            sorted.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            flow.setValue(Collections.unmodifiableList(sorted));
        }

        @NonNull
        @Override
        public Flow<List<CalculationLog>> getAllLogs() {
            return flow;
        }

        @NonNull
        @Override
        public List<CalculationLog> getAllLogsDirect() {
            synchronized (this) {
                List<CalculationLog> sorted = new ArrayList<>(logs);
                sorted.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                return sorted;
            }
        }

        @Override
        public long insertLog(@NonNull CalculationLog log) {
            synchronized (this) {
                long assignedId = log.getId() == 0L ? nextId++ : log.getId();
                CalculationLog toSave = log.copy();
                toSave.setId(assignedId);
                logs.removeIf(l -> l.getId() == assignedId);
                logs.add(toSave);
                updateFlow();
                return assignedId;
            }
        }

        @Override
        public long[] insertLogs(@NonNull List<CalculationLog> newLogs) {
            synchronized (this) {
                long[] ids = new long[newLogs.size()];
                for (int i = 0; i < newLogs.size(); i++) {
                    CalculationLog log = newLogs.get(i);
                    long assignedId = log.getId() == 0L ? nextId++ : log.getId();
                    CalculationLog toSave = log.copy();
                    toSave.setId(assignedId);
                    logs.removeIf(l -> l.getId() == assignedId);
                    logs.add(toSave);
                    ids[i] = assignedId;
                }
                updateFlow();
                return ids;
            }
        }

        @Override
        public void deleteLogById(long logId) {
            synchronized (this) {
                logs.removeIf(l -> l.getId() == logId);
                updateFlow();
            }
        }

        @Override
        public void clearAllLogs() {
            synchronized (this) {
                logs.clear();
                updateFlow();
            }
        }
    }

    public static class FakeUserSettingsDao implements UserSettingsDao {
        private UserSettings storedSettings = null;
        private final MutableStateFlow<UserSettings> flow = StateFlowKt.MutableStateFlow(null);

        @NonNull
        @Override
        public Flow<UserSettings> getSettings() {
            return flow;
        }

        @Override
        public UserSettings getSettingsDirect() {
            return storedSettings;
        }

        @Override
        public void saveSettings(@NonNull UserSettings settings) {
            storedSettings = settings;
            flow.setValue(settings);
        }
    }
}
