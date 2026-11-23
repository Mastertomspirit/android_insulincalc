package network.spiritscorp.viewmodel;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import kotlinx.coroutines.flow.FlowKt;
import network.spiritscorp.data.CalculationLogDao;
import network.spiritscorp.data.InsulinRepository;
import network.spiritscorp.data.UserSettingsDao;
import network.spiritscorp.model.UserSettings;

/**
 * Unit test class for {@link InsulinCalculatorViewModel}, verifying initial states,
 * disclaimer preferences handling, and behavior on app restart across different database states.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("KotlinInternalInJava")
public class InsulinCalculatorViewModelTest {

    private Application app;
    private InsulinRepository mockRepo;
    private InsulinCalculatorViewModel insulinCalculatorViewModel;

    /**
     * Initializes the application context, mocked Data Access Objects (DAOs),
     * and the repository and ViewModel instances prior to each test execution.
     */
    @Before
    public void setup() {
        app = ApplicationProvider.getApplicationContext();
        CalculationLogDao mockLogDao = mock(CalculationLogDao.class);
        UserSettingsDao mockSettingsDao = mock(UserSettingsDao.class);
        when(mockSettingsDao.getSettingsDirect()).thenReturn(null);
        when(mockSettingsDao.getSettings()).thenReturn(FlowKt.emptyFlow());
        when(mockLogDao.getAllLogs()).thenReturn(FlowKt.emptyFlow());

        mockRepo = new InsulinRepository(mockLogDao, mockSettingsDao);
        insulinCalculatorViewModel = new InsulinCalculatorViewModel(app,mockRepo);
    }

    /**
     * Verifies that the disclaimer banner state correctly respects user preferences upon initial ViewModel creation.
     */
    @Test
    public void testInitialShowDisclaimerBannerRespectsDisclaimerPreferences() {
        assertTrue("Banner should immediately be true in initial StateFlow value when preference was dismissed", insulinCalculatorViewModel.getMainUiState$app().getValue().getShowDisclaimerBanner());
    }

    /**
     * Verifies that when the disclaimer has been dismissed and the ViewModel is re-initialized (app restart),
     * the disclaimer banner is no longer shown.
     */
    @Test
    public void testShowBannerNoBannerOnRestart() {
        UserSettings userSettings = new UserSettings();
        CalculationLogDao mockLogDao = mock(CalculationLogDao.class);
        UserSettingsDao mockSettingsDao = mock(UserSettingsDao.class);
        when(mockSettingsDao.getSettingsDirect()).thenReturn(userSettings);
        when(mockSettingsDao.getSettings()).thenReturn(FlowKt.flowOf(userSettings));
        when(mockLogDao.getAllLogs()).thenReturn(FlowKt.emptyFlow());

        mockRepo = new InsulinRepository(mockLogDao, mockSettingsDao);
        insulinCalculatorViewModel = new InsulinCalculatorViewModel(app, mockRepo);

        // Dismiss disclaimer in preferences first
        insulinCalculatorViewModel.dismissDisclaimer$app();

        insulinCalculatorViewModel = new InsulinCalculatorViewModel(app, mockRepo);

        assertFalse("Banner should immediately be false in initial StateFlow value when preference was dismissed", insulinCalculatorViewModel.getMainUiState$app().getValue().getShowDisclaimerBanner());
    }

    /**
     * Verifies fallback behavior when restarting without a valid database/repository connection.
     */
    @Test
    public void testShowBannerOnRestartNoDatabase(){
        // Dismiss disclaimer in preferences first
        insulinCalculatorViewModel.dismissDisclaimer$app();

        InsulinCalculatorViewModel newInsulinCalculatorViewModel = new InsulinCalculatorViewModel(app, mockRepo);
        assertTrue("Banner should immediately be true in initial StateFlow value when preference was dismissed", newInsulinCalculatorViewModel.getMainUiState$app().getValue().getShowDisclaimerBanner());
    }
}
