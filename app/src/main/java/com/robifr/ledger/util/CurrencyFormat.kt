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

object CurrencyFormat {
  const val MINIMUM_FRACTION_DIGITS: Int = 0
  const val MAXIMUM_FRACTION_DIGITS: Int = 5

  /** @return Formatted amount into local specific currency. */
  fun format(
      amount: BigDecimal,
      languageTag: String,
      symbol: String = symbol(languageTag)
  ): String =
      (NumberFormat.getCurrencyInstance(Locale.forLanguageTag(languageTag)) as DecimalFormat)
          .apply {
            roundingMode = RoundingMode.DOWN
            minimumFractionDigits = MINIMUM_FRACTION_DIGITS
            maximumFractionDigits = MAXIMUM_FRACTION_DIGITS
            // Directly modifying `currencySymbol` may not work due to immutability.
            decimalFormatSymbols = decimalFormatSymbols.apply { currencySymbol = symbol }
          }
          .format(amount)

  /**
   * @return Formatted amount into local specific currency with an appropriate suffix (such as K for
   *   thousands or M for millions) appended at the end of the string.
   */
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
    return if (positiveAmount.compareTo(thousand) < 0) {
      negativePrefix + format(positiveAmount, languageTag, symbol)
    } else if (positiveAmount.compareTo(million) < 0) {
      negativePrefix +
          format(positiveAmount.divide(thousand, 1, RoundingMode.DOWN), languageTag, symbol) +
          context.getString(R.string.symbol_thousand)
    } else if (positiveAmount.compareTo(billion) < 0) {
      negativePrefix +
          format(positiveAmount.divide(million, 1, RoundingMode.DOWN), languageTag, symbol) +
          context.getString(R.string.symbol_million)
    } else if (positiveAmount.compareTo(trillion) < 0) {
      negativePrefix +
          format(positiveAmount.divide(billion, 1, RoundingMode.DOWN), languageTag, symbol) +
          context.getString(R.string.symbol_billion)
    } else {
      negativePrefix +
          format(positiveAmount.divide(trillion, 1, RoundingMode.DOWN), languageTag, symbol) +
          context.getString(R.string.symbol_trillion)
    }
  }

  /** @return Parsed amount from local specific currency. */
  @Throws(ParseException::class)
  fun parse(amount: String, languageTag: String): BigDecimal {
    val format: DecimalFormat =
        (NumberFormat.getNumberInstance(Locale.forLanguageTag(languageTag)) as DecimalFormat)
            .apply { isParseBigDecimal = true }
    // Replace every character except those that can be edited by the user
    // (digits, negative sign, and decimal separator).
    val amountToParse: String =
        amount
            .replace("""[^\d\-${Regex.escape(decimalSeparator(languageTag))}]""".toRegex(), "")
            .let { if (!isValidToParseAndFormat(amount, languageTag)) "0" else it }
    return (format.parse(amountToParse) as BigDecimal).stripTrailingZeros()
  }

  fun isValidToParseAndFormat(amount: String, languageTag: String): Boolean {
    val decimalSeparator: String = decimalSeparator(languageTag)
    // Replace every character except those that can be edited by the user
    // (digits, negative sign, and decimal separator).
    val amount: String =
        amount.replace("""[^\d\-${Regex.escape(decimalSeparator)}]""".toRegex(), "")
    // Can't find any digit.
    return !(amount.count { it.isDigit() } == 0 ||
        // Found multiple decimal separator.
        amount.count { it.toString() == decimalSeparator } > 1 ||
        // Found multiple negative sign.
        amount.count { it.toString() == "-" } > 1 ||
        // Found any character before negative sign.
        """([^\-]+)(?=-)""".toRegex().findAll(amount).count() > 0 ||
        // Found the digit occurrence in decimal place is more than max allowed.
        ("""(?<=\.)\d+""".toRegex().find(amount)?.value?.length ?: 0) > MAXIMUM_FRACTION_DIGITS)
  }

  fun symbol(languageTag: String): String =
      DecimalFormatSymbols(Locale.forLanguageTag(languageTag)).currencySymbol

  fun isSymbolAtStart(languageTag: String): Boolean =
      (NumberFormat.getCurrencyInstance(Locale.forLanguageTag(languageTag)) as DecimalFormat)
          .toLocalizedPattern()
          .indexOf('\u00A4') == 0

  fun groupingSeparator(languageTag: String): String =
      DecimalFormatSymbols(Locale.forLanguageTag(languageTag)).groupingSeparator.toString()

  fun decimalSeparator(languageTag: String): String =
      DecimalFormatSymbols(Locale.forLanguageTag(languageTag)).decimalSeparator.toString()

  fun countDecimalPlace(amount: BigDecimal): Int =
      max(0.0, amount.stripTrailingZeros().scale().toDouble()).toInt()
}
