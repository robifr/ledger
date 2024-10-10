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

package com.robifr.ledger.util

import android.content.Context
import com.robifr.ledger.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale
import kotlin.math.max

// TODO: Remove `JvmStatic` annotations after Kotlin migration.
object CurrencyFormat {
  const val MINIMUM_FRACTION_DIGITS: Int = 0
  const val MAXIMUM_FRACTION_DIGITS: Int = 5

  /** @return Formatted amount into local specific currency. */
  @JvmOverloads
  @JvmStatic
  fun format(
      amount: BigDecimal,
      languageTag: String,
      symbol: String = symbol(languageTag)
  ): String {
    val format: DecimalFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag(languageTag)) as DecimalFormat
    val symbols: DecimalFormatSymbols = format.decimalFormatSymbols
    symbols.currencySymbol = symbol
    format.roundingMode = RoundingMode.DOWN
    format.minimumFractionDigits = MINIMUM_FRACTION_DIGITS
    format.maximumFractionDigits = MAXIMUM_FRACTION_DIGITS
    format.decimalFormatSymbols = symbols
    return format.format(amount)
  }

  /**
   * @return Formatted amount into local specific currency with an appropriate suffix (such as K for
   *   thousands or M for millions) appended at the end of the string.
   */
  @JvmOverloads
  @JvmStatic
  fun formatWithUnit(
      context: Context,
      amount: BigDecimal,
      languageTag: String,
      symbol: String = symbol(languageTag)
  ): String {
    val thousand: BigDecimal = 1000.toBigDecimal()
    val million: BigDecimal = 1_000_000.toBigDecimal()
    val billion: BigDecimal = 1_000_000_000.toBigDecimal()
    val trillion: BigDecimal = 1_000_000_000_000L.toBigDecimal()

    // Convert negative amount to positive to handle where negative amounts
    // can't be formatted due to division.
    val negativePrefix: String = if (amount.compareTo(0.toBigDecimal()) < 0) "-" else ""
    val positiveAmount: BigDecimal = amount.abs()
    return with(positiveAmount) {
      when {
        compareTo(thousand) < 0 -> negativePrefix + format(this, languageTag, symbol)
        compareTo(million) < 0 ->
            negativePrefix +
                format(divide(thousand, 1, RoundingMode.DOWN), languageTag, symbol) +
                context.getString(R.string.symbol_thousand)
        compareTo(billion) < 0 ->
            negativePrefix +
                format(this.divide(million, 1, RoundingMode.DOWN), languageTag, symbol) +
                context.getString(R.string.symbol_million)
        compareTo(trillion) < 0 ->
            negativePrefix +
                format(divide(billion, 1, RoundingMode.DOWN), languageTag, symbol) +
                context.getString(R.string.symbol_billion)
        else ->
            negativePrefix +
                format(divide(trillion, 1, RoundingMode.DOWN), languageTag, symbol) +
                context.getString(R.string.symbol_trillion)
      }
    }
  }

  /** @return Parsed amount from local specific currency. */
  @JvmStatic
  @Throws(ParseException::class)
  fun parse(amount: String, languageTag: String): BigDecimal {
    val format: DecimalFormat =
        NumberFormat.getNumberInstance(Locale.forLanguageTag(languageTag)) as DecimalFormat
    format.isParseBigDecimal = true
    val decimalSeparator: String = decimalSeparator(languageTag)
    var amountToParse: String = amount.replace("[^\\d\\-\\${decimalSeparator}]".toRegex(), "")

    // Edge case.
    if (amountToParse.isBlank() ||
        amountToParse == decimalSeparator ||
        amountToParse == "-" ||
        amountToParse == "-$decimalSeparator" ||
        amountToParse.countOccurrence("-") > 1) {
      amountToParse = "0"
    }
    return (format.parse(amountToParse) as BigDecimal).stripTrailingZeros()
  }

  @JvmStatic
  fun symbol(languageTag: String): String =
      DecimalFormatSymbols(Locale.forLanguageTag(languageTag)).currencySymbol

  @JvmStatic
  fun groupingSeparator(languageTag: String): String =
      DecimalFormatSymbols(Locale.forLanguageTag(languageTag)).groupingSeparator.toString()

  @JvmStatic
  fun decimalSeparator(languageTag: String): String =
      DecimalFormatSymbols(Locale.forLanguageTag(languageTag)).decimalSeparator.toString()

  @JvmStatic
  fun isSymbolAtStart(languageTag: String): Boolean =
      (NumberFormat.getCurrencyInstance(Locale.forLanguageTag(languageTag)) as DecimalFormat)
          .toLocalizedPattern()
          .indexOf('\u00A4') == 0

  @JvmStatic
  fun countDecimalPlace(amount: BigDecimal): Int =
      max(0.0, amount.stripTrailingZeros().scale().toDouble()).toInt()
}
