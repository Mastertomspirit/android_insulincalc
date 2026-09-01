package network.spiritscorp.model

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

data class CalculationSummary(
    val carbGrams: Double,
    val keValue: Double,
    val beValue: Double,
    val factorUsed: Double,
    val mealInsulin: Double,
    val bloodGlucoseInput: Double?,
    val targetGlucose: Double?,
    val correctionInsulin: Double,
    val rawTotalInsulin: Double,
    val roundedTotalInsulin: Double,
    val roundingStep: Double,
    val isHypoRisk: Boolean,
    val advisoryNote: String
)

