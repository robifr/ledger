/**
 * Copyright 2024 Robi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.robifr.ledger.assetbinding.chart

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object ChartUtil {
  fun toDateTime(
      dateToConvert: ZonedDateTime,
      dateStartEnd: Pair<ZonedDateTime, ZonedDateTime>
  ): String {
    // Determine how the data will be grouped based on the date range.
    val groupBy: ChronoUnit =
        if (dateStartEnd.first.year != dateStartEnd.second.year) {
          ChronoUnit.YEARS
        } else if (dateStartEnd.first.monthValue != dateStartEnd.second.monthValue) {
          ChronoUnit.MONTHS
        } else {
          ChronoUnit.DAYS
        }
    return when (groupBy) {
      ChronoUnit.DAYS -> dateToConvert.dayOfMonth.toString()
      ChronoUnit.MONTHS -> dateToConvert.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
      else -> dateToConvert.year.toString()
    }
  }

  fun toDateTimeDomain(dateStartEnd: Pair<ZonedDateTime, ZonedDateTime>): List<String> {
    val result: MutableList<String> = mutableListOf()
    // Determine how the data will be grouped based on the date range.
    val groupBy: ChronoUnit =
        if (dateStartEnd.first.year != dateStartEnd.second.year) {
          ChronoUnit.YEARS
        } else if (dateStartEnd.first.monthValue != dateStartEnd.second.monthValue) {
          ChronoUnit.MONTHS
        } else {
          ChronoUnit.DAYS
        }
    // Fill the map with summed values for each key in the date range.
    var date: ZonedDateTime = dateStartEnd.first
    while (!date.isAfter(dateStartEnd.second)) {
      result.add(toDateTime(date, dateStartEnd))
      date = date.plus(1, groupBy)
    }

    // Ensure that the end date is included. Because there's an edge case when
    // the month total between the start date and end date is less than 12. For example,
    // in the range 2023/7/1 - 2024/6/1, the map will not show the end date's year.
    if (!result.contains(toDateTime(dateStartEnd.second, dateStartEnd))) {
      result.add(toDateTime(dateStartEnd.second, dateStartEnd))
    }
    return result
  }

  fun toPercentageLinear(valueToConvert: BigDecimal, maxValue: BigDecimal, ticks: Int): Double {
    // Set to one as the minimum to prevent zero division.
    val paddedMaxValue: BigDecimal = 1.toBigDecimal().max(_ceilToNearestNiceNumber(maxValue, ticks))
    return valueToConvert
        .divide(paddedMaxValue, 2, RoundingMode.HALF_UP)
        .multiply(100.toBigDecimal())
        .toDouble()
  }

  fun toPercentageLinearDomain(context: Context, maxValue: BigDecimal, ticks: Int): List<String> {
    // Set to one as the minimum to prevent zero division.
    val paddedMaxValue: BigDecimal = 1.toBigDecimal().max(_ceilToNearestNiceNumber(maxValue, ticks))
    val gap: BigDecimal = paddedMaxValue.divide(100.toBigDecimal(), 2, RoundingMode.HALF_UP)
    // From percentages, linearly map the amount up to the top value.
    return (0..100).map {
      CurrencyFormat.formatWithUnit(
          context,
          it.toBigDecimal().multiply(gap),
          AppCompatDelegate.getApplicationLocales().toLanguageTags())
    }
  }

  /**
   * The algorithm is based from [this Stack Overflow answer](https://stackoverflow.com/a/16363437).
   *
   * @return Array of:
   * - Index 0: Nice minimum value.
   * - Index 1: Nice maximum value.
   * - Index 2: Ticks spacing.
   */
  fun calculateNiceScale(minPoint: Double, maxPoint: Double, ticks: Double): DoubleArray {
    val obtainNiceNumber: (Double, Boolean) -> Double = { range, shouldRound ->
      val exponent: Double = floor(log10(range))
      val fraction: Double = range / 10.0.pow(exponent)
      val niceFraction: Double =
          if (shouldRound) {
            if (fraction < 1.5) 1.0 else if (fraction < 3) 2.0 else if (fraction < 7) 5.0 else 10.0
          } else {
            if (fraction <= 1) 1.0 else if (fraction <= 2) 2.0 else if (fraction <= 5) 5.0 else 10.0
          }
      niceFraction * 10.0.pow(exponent)
    }
    val range: Double = obtainNiceNumber(maxPoint - minPoint, false)
    val tickSpacing: Double = obtainNiceNumber(range / (ticks - 1), true)
    val niceMin: Double = floor(minPoint / tickSpacing) * tickSpacing
    val niceMax: Double = ceil(maxPoint / tickSpacing) * tickSpacing
    return doubleArrayOf(niceMin, niceMax, tickSpacing)
  }

  private fun _ceilToNearestNiceNumber(amount: BigDecimal, ticks: Int): BigDecimal {
    val hundred: BigDecimal = 100.toBigDecimal()
    val thousand: BigDecimal = 1000.toBigDecimal()
    val million: BigDecimal = 1_000_000.toBigDecimal()
    val billion: BigDecimal = 1_000_000_000.toBigDecimal()
    val trillion: BigDecimal = 1_000_000_000_000L.toBigDecimal()
    var scaleFactor: BigDecimal =
        if (amount.compareTo(trillion) >= 0) trillion
        else if (amount.compareTo(billion) >= 0) billion
        else if (amount.compareTo(million) >= 0) million
        else if (amount.compareTo(thousand) >= 0) thousand
        else if (amount.compareTo(hundred) >= 0) hundred else 1.toBigDecimal()
    // Scale down the value because `Math#log10` doesn't work with `BigDecimal`,
    // while the algorithm requires it.
    val minScaled: Double = 0.toBigDecimal().divide(scaleFactor, MathContext.DECIMAL128).toDouble()
    val maxScaled: Double =
        amount.max(1.toBigDecimal()).divide(scaleFactor, MathContext.DECIMAL128).toDouble()
    return (calculateNiceScale(minScaled, maxScaled, ticks.toDouble())[1])
        .toBigDecimal()
        .multiply(scaleFactor) // Scale up to the actual value.
  }
}
