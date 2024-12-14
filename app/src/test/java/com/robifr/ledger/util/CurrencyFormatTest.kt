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
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CurrencyFormatTest {
  private val _us = "en-US"
  private val _uk = "en-GB"
  private val _france = "fr-FR"
  private val _german = "de-DE"
  private val _indonesia = "id-ID"
  private val _japan = "ja-JP"
  private val _amount: BigDecimal = 10_000.50.toBigDecimal()

  private fun `_format currency cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf(_us, "$10,000.5"),
          arrayOf(_uk, "£10,000.5"),
          arrayOf(_france, "10\u202F000,5\u00A0€"),
          arrayOf(_german, "10.000,5\u00A0€"),
          arrayOf(_indonesia, "Rp10.000,5"),
          arrayOf(_japan, "￥10,000.5"))

  @ParameterizedTest
  @MethodSource("_format currency cases")
  fun `format currency`(languageTag: String, formattedAmount: String) {
    assertEquals(
        formattedAmount,
        CurrencyFormat.format(_amount, languageTag),
        "Correctly format amount with locale ${languageTag}")
  }

  @ParameterizedTest
  @MethodSource("_format currency cases")
  fun `parse currency with valid argument`(languageTag: String, formattedAmount: String) {
    assertEquals(
        _amount,
        CurrencyFormat.parse(formattedAmount, languageTag),
        "Correctly parse amount with locale ${languageTag}")
  }

  private fun `_format currency unit with different digits cases`(): Array<Array<Any>> =
      arrayOf(
          // Dozen and hundred.
          arrayOf(0.toBigDecimal(), "$0"),
          arrayOf((-100).toBigDecimal(), "-$100"),
          arrayOf(100.toBigDecimal(), "$100"),
          // Hundreds.
          arrayOf((-1000).toBigDecimal(), "-$1K"),
          arrayOf(1000.toBigDecimal(), "$1K"),
          arrayOf(1500.toBigDecimal(), "$1.5K"),
          arrayOf(1555.toBigDecimal(), "$1.5K"),
          // Millions.
          arrayOf((-1_000_000).toBigDecimal(), "-$1M"),
          arrayOf(1_000_000.toBigDecimal(), "$1M"),
          arrayOf(1_555_000.toBigDecimal(), "$1.5M"),
          arrayOf(1_555_555.toBigDecimal(), "$1.5M"),
          // Billions.
          arrayOf((-1_000_000_000).toBigDecimal(), "-$1B"),
          arrayOf(1_000_000_000.toBigDecimal(), "$1B"),
          arrayOf(1_555_000_000.toBigDecimal(), "$1.5B"),
          arrayOf(1_555_555_000.toBigDecimal(), "$1.5B"),
          // Trillions.
          arrayOf((-1_000_000_000_000L).toBigDecimal(), "-$1T"),
          arrayOf(1_000_000_000_000L.toBigDecimal(), "$1T"),
          arrayOf(1_555_000_000_000L.toBigDecimal(), "$1.5T"),
          arrayOf(1_555_555_000_000L.toBigDecimal(), "$1.5T"),
          arrayOf(1_000_000_000_000_000L.toBigDecimal(), "$1,000T"),
          arrayOf(1_555_000_000_000_000L.toBigDecimal(), "$1,555T"),
          arrayOf(1_555_555_000_000_000L.toBigDecimal(), "$1,555.5T"),
          arrayOf(1_000_000_000_000_000_000L.toBigDecimal(), "$1,000,000T"),
          arrayOf(1_555_000_000_000_000_000L.toBigDecimal(), "$1,555,000T"),
          arrayOf(1_555_555_000_000_000_000L.toBigDecimal(), "$1,555,555T"),
          arrayOf(1_555_555_555_000_000_000L.toBigDecimal(), "$1,555,555.5T"))

  @ParameterizedTest
  @MethodSource("_format currency unit with different digits cases")
  fun `format currency unit with different digits`(amount: BigDecimal, formattedAmount: String) {
    val context: Context = mockk()
    every { context.getString(R.string.symbol_thousand) } returns "K"
    every { context.getString(R.string.symbol_million) } returns "M"
    every { context.getString(R.string.symbol_billion) } returns "B"
    every { context.getString(R.string.symbol_trillion) } returns "T"
    assertEquals(
        formattedAmount,
        CurrencyFormat.formatWithUnit(context, amount, _us),
        "Correctly format amount with different digits")
  }

  private fun `_parse currency cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf(_us, "--100", 0.toBigDecimal()),
          arrayOf(_us, "-0.0", 0.toBigDecimal()),
          arrayOf(_us, "-.1", (-0.1).toBigDecimal()),
          arrayOf(_us, "123.0", 123.toBigDecimal()),
          arrayOf(_us, "123", 123.toBigDecimal()),
      )

  @ParameterizedTest
  @MethodSource("_parse currency cases")
  fun `parse currency`(languageTag: String, amount: String, parsedAmount: BigDecimal) {
    assertEquals(
        parsedAmount,
        CurrencyFormat.parse(amount, languageTag),
        "Fallback to zero for anything that can't be parsed")
  }

  private fun `_is valid to parse and format cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf(_us, "", false),
          arrayOf(_us, " ", false),
          arrayOf(_us, "-", false),
          arrayOf(_us, "$-", false),
          arrayOf(_us, ".", false),
          arrayOf(_us, ".-", false),
          arrayOf(_us, "-.", false),
          arrayOf(_us, "-.0", true),
          arrayOf(_us, "-.1", true),
          arrayOf(_us, "--1", false),
          arrayOf(_us, "123", true),
      )

  @ParameterizedTest
  @MethodSource("_is valid to parse and format cases")
  fun `is valid to parse and format`(languageTag: String, amount: String, isValid: Boolean) {
    assertEquals(
        isValid,
        CurrencyFormat.isValidToParseAndFormat(amount, languageTag),
        "Returns true for the amount that can be both parsed and formatted")
  }

  private fun `_is symbol position at start cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf(_us, true),
          arrayOf(_uk, true),
          arrayOf(_france, false),
          arrayOf(_german, false),
          arrayOf(_indonesia, true),
          arrayOf(_japan, true))

  @ParameterizedTest
  @MethodSource("_is symbol position at start cases")
  fun `is symbol position at start`(languageTag: String, isSymbolAtStart: Boolean) {
    assertEquals(
        isSymbolAtStart,
        CurrencyFormat.isSymbolAtStart(languageTag),
        if (isSymbolAtStart) "Locale ${languageTag} symbol is at the start of the string"
        else "Locale ${languageTag} symbol is at the end of the string")
  }
}
