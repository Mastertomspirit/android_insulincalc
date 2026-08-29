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

import kotlin.Unit;
import kotlin.coroutines.Continuation;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
 * reactive Flows, suspend queries, and the InsulinRepository domain coordinator.
 */
public class InsulinRepositoryIntegrationTest {

    private static final double DELTA = 0.001;

    private FakeCalculationLogDao fakeLogDao;
    private FakeUserSettingsDao fakeSettingsDao;
    private InsulinRepository repository;

    @Before
    public void setup() {
        fakeLogDao = new FakeCalculationLogDao();
        fakeSettingsDao = new FakeUserSettingsDao();
        repository = new InsulinRepository(fakeLogDao, fakeSettingsDao);
    }

    @Test
    public void testDefaultSettingsCreatedWhenNoneExist() {
        UserSettings settings = runBlocking((scope, cont) -> repository.getSettings(cont));
        assertNotNull(settings);
        assertEquals(1.50, settings.getMorningFactor(), DELTA);
        assertEquals("GRAMS", settings.getDefaultCarbUnit());
        assertEquals(120.0, settings.getTargetGlucoseMgDl(), DELTA);
        assertEquals(50.0, settings.getCorrectionFactorMgDl(), DELTA);

        // Verify default settings were persisted to DAO
        UserSettings direct = runBlocking((scope, cont) -> fakeSettingsDao.getSettingsDirect(cont));
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

        runBlocking((scope, cont) -> repository.saveSettings(custom, cont));

        UserSettings retrieved = runBlocking((scope, cont) -> repository.getSettings(cont));
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

        Long id = runBlocking((scope, cont) -> repository.saveCalculation(log, cont));
        assertNotNull(id);
        assertTrue(id > 0);

        List<CalculationLog> logsFromFlow = firstFromFlow(repository.getAllLogs());
        assertNotNull(logsFromFlow);
        assertEquals(1, logsFromFlow.size());
        assertEquals("Mittagessen (Reis mit Hühnchen)", logsFromFlow.get(0).getMealTitle());
        assertEquals(5.5, logsFromFlow.get(0).getRoundedInsulin(), DELTA);

        List<CalculationLog> directLogs = runBlocking((scope, cont) -> repository.getAllLogsDirect(cont));
        assertEquals(1, directLogs.size());
    }

    @Test
    public void testSaveMultipleLogsBatchAndOrdering() {
        CalculationLog log1 = new CalculationLog(1L, 1000L, "Mahlzeit 1", 0.0, "g KH", 0.0, 0.0, 0.0, "Morgens", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log2 = new CalculationLog(2L, 2000L, "Mahlzeit 2", 0.0, "g KH", 0.0, 0.0, 0.0, "Mittags", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log3 = new CalculationLog(3L, 3000L, "Mahlzeit 3", 0.0, "g KH", 0.0, 0.0, 0.0, "Abends", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");

        List<Long> insertedIds = runBlocking((scope, cont) -> repository.saveLogs(Arrays.asList(log1, log2, log3), cont));
        assertEquals(3, insertedIds.size());

        List<CalculationLog> allLogs = runBlocking((scope, cont) -> repository.getAllLogsDirect(cont));
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

        runBlocking((scope, cont) -> repository.saveLogs(Arrays.asList(log1, log2), cont));
        List<CalculationLog> initialLogs = runBlocking((scope, cont) -> repository.getAllLogsDirect(cont));
        assertEquals(2, initialLogs.size());

        runBlocking((scope, cont) -> repository.deleteLog(10L, cont));
        List<CalculationLog> remaining = runBlocking((scope, cont) -> repository.getAllLogsDirect(cont));
        assertEquals(1, remaining.size());
        assertEquals("Abendessen", remaining.get(0).getMealTitle());
    }

    @Test
    public void testClearAllLogs() {
        CalculationLog log1 = new CalculationLog(1L, 1000L, "Eintrag 1", 0.0, "g KH", 0.0, 0.0, 0.0, "Morgens", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        CalculationLog log2 = new CalculationLog(2L, 2000L, "Eintrag 2", 0.0, "g KH", 0.0, 0.0, 0.0, "Mittags", 1.0, 0.0, null, null, null, 0.0, 0.0, 0.0, "");
        runBlocking((scope, cont) -> repository.saveLogs(Arrays.asList(log1, log2), cont));

        List<CalculationLog> initialLogs = runBlocking((scope, cont) -> repository.getAllLogsDirect(cont));
        assertEquals(2, initialLogs.size());

        runBlocking((scope, cont) -> repository.clearLogs(cont));
        List<CalculationLog> remainingLogs = runBlocking((scope, cont) -> repository.getAllLogsDirect(cont));
        assertEquals(0, remainingLogs.size());
        List<CalculationLog> flowLogs = firstFromFlow(repository.getAllLogs());
        assertEquals(0, flowLogs.size());
    }

    // --- Coroutine Helper Methods for Java ---

    @SuppressWarnings("unchecked")
    public static <T> T runBlocking(kotlin.jvm.functions.Function2<kotlinx.coroutines.CoroutineScope, Continuation<? super T>, ?> block) {
        try {
            return (T) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    block
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Coroutine execution interrupted", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T firstFromFlow(Flow<T> flow) {
        try {
            return (T) BuildersKt.runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> FlowKt.first(flow, continuation)
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

        @NotNull
        @Override
        public Flow<List<CalculationLog>> getAllLogs() {
            return flow;
        }

        @Nullable
        @Override
        public Object getAllLogsDirect(@NotNull Continuation<? super List<CalculationLog>> continuation) {
            synchronized (this) {
                List<CalculationLog> sorted = new ArrayList<>(logs);
                sorted.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                return sorted;
            }
        }

        @Nullable
        @Override
        public Object insertLog(@NotNull CalculationLog log, @NotNull Continuation<? super Long> continuation) {
            synchronized (this) {
                long assignedId = log.getId() == 0L ? nextId++ : log.getId();
                CalculationLog toSave = log.copy(
                        assignedId,
                        log.getTimestamp(),
                        log.getMealTitle(),
                        log.getRawCarbInput(),
                        log.getCarbUnit(),
                        log.getCarbGrams(),
                        log.getBeValue(),
                        log.getKeValue(),
                        log.getTimeOfDay(),
                        log.getInsulinFactor(),
                        log.getMealInsulin(),
                        log.getBloodGlucose(),
                        log.getTargetGlucose(),
                        log.getCorrectionFactor(),
                        log.getCorrectionInsulin(),
                        log.getTotalInsulin(),
                        log.getRoundedInsulin(),
                        log.getNotes()
                );
                logs.removeIf(l -> l.getId() == assignedId);
                logs.add(toSave);
                updateFlow();
                return assignedId;
            }
        }

        @Nullable
        @Override
        public Object insertLogs(@NotNull List<CalculationLog> newLogs, @NotNull Continuation<? super List<Long>> continuation) {
            synchronized (this) {
                List<Long> ids = new ArrayList<>();
                for (CalculationLog log : newLogs) {
                    long assignedId = log.getId() == 0L ? nextId++ : log.getId();
                    CalculationLog toSave = log.copy(
                            assignedId,
                            log.getTimestamp(),
                            log.getMealTitle(),
                            log.getRawCarbInput(),
                            log.getCarbUnit(),
                            log.getCarbGrams(),
                            log.getBeValue(),
                            log.getKeValue(),
                            log.getTimeOfDay(),
                            log.getInsulinFactor(),
                            log.getMealInsulin(),
                            log.getBloodGlucose(),
                            log.getTargetGlucose(),
                            log.getCorrectionFactor(),
                            log.getCorrectionInsulin(),
                            log.getTotalInsulin(),
                            log.getRoundedInsulin(),
                            log.getNotes()
                    );
                    logs.removeIf(l -> l.getId() == assignedId);
                    logs.add(toSave);
                    ids.add(assignedId);
                }
                updateFlow();
                return ids;
            }
        }

        @Nullable
        @Override
        public Object deleteLogById(long logId, @NotNull Continuation<? super Unit> continuation) {
            synchronized (this) {
                logs.removeIf(l -> l.getId() == logId);
                updateFlow();
            }
            return Unit.INSTANCE;
        }

        @Nullable
        @Override
        public Object clearAllLogs(@NotNull Continuation<? super Unit> continuation) {
            synchronized (this) {
                logs.clear();
                updateFlow();
            }
            return Unit.INSTANCE;
        }
    }

    public static class FakeUserSettingsDao implements UserSettingsDao {
        private UserSettings storedSettings = null;
        private final MutableStateFlow<UserSettings> flow = StateFlowKt.MutableStateFlow(null);

        @NotNull
        @Override
        public Flow<UserSettings> getSettings() {
            return flow;
        }

        @Nullable
        @Override
        public Object getSettingsDirect(@NotNull Continuation<? super UserSettings> continuation) {
            return storedSettings;
        }

        @Nullable
        @Override
        public Object saveSettings(@NotNull UserSettings settings, @NotNull Continuation<? super Unit> continuation) {
            storedSettings = settings;
            flow.setValue(settings);
            return Unit.INSTANCE;
        }
    }
}
