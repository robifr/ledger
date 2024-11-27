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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
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

  private fun `_format currency unit with different digits`(): Array<Array<Any>> =
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
  @MethodSource("_format currency unit with different digits")
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

  @Test
  fun `parse currency with invalid argument`() {
    val decimalSeparator: String = CurrencyFormat.decimalSeparator(_us)
    assertAll({
      assertEquals(
          0.toBigDecimal(),
          CurrencyFormat.parse("", _us),
          "Parse to zero when there's only an empty string")
      assertEquals(
          0.toBigDecimal(),
          CurrencyFormat.parse(" ", _us),
          "Parse to zero when there's only a blank string")
      assertEquals(
          0.toBigDecimal(),
          CurrencyFormat.parse("-", _us),
          "Parse to zero when there's only a minus sign")
      assertEquals(
          0.toBigDecimal(),
          CurrencyFormat.parse(decimalSeparator, _us),
          "Parse to zero when there's only a decimal separator")
      assertEquals(
          0.toBigDecimal(),
          CurrencyFormat.parse("-${decimalSeparator}", _us),
          "Parse to zero when only '-${decimalSeparator}' presented")
      assertEquals(
          0.toBigDecimal(),
          CurrencyFormat.parse("--1", _us),
          "Parse to zero when there are multiple minus sign")
      assertEquals(
          _amount, CurrencyFormat.parse("${_amount}0", _us), "Remove trailing zero when parsing")
    })
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
