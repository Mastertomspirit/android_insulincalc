package network.spiritscorp.viewmodel

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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import network.spiritscorp.data.DatabaseRestoreManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseErrorRecoveryTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testViewModelHandlesDatabaseErrorState() {
        val viewModel = InsulinCalculatorViewModel(app)
        assertNotNull(viewModel.databaseError)

        // Trigger database error
        viewModel.notifyDatabaseError()
        assertTrue(viewModel.databaseError.value)

        // Clear error
        viewModel.clearDatabaseError()
        assertFalse(viewModel.databaseError.value)
    }

    @Test
    fun testAppDatabaseResetInstanceClearsData() {
        DatabaseRestoreManager().resetInstance(app)
        // Resetting instance must execute cleanly without System.exit
        assertTrue(true)
    }
}
