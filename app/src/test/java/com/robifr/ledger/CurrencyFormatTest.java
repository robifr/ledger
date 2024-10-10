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

package com.robifr.ledger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.kotlin.OngoingStubbingKt.whenever;

import android.content.Context;
import androidx.annotation.NonNull;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CurrencyFormatTest {
  @NonNull private final BigDecimal _amount = BigDecimal.valueOf(10_000.50);

  @NonNull private final String _us = "en-US";
  @NonNull private final String _uk = "en-GB";
  @NonNull private final String _france = "fr-FR";
  @NonNull private final String _german = "de-DE";
  @NonNull private final String _indonesia = "id-ID";
  @NonNull private final String _japan = "ja-JP";

  @Test
  public void format() {
    assertAll( // spotless:off
        () -> assertEquals("$10,000.5", CurrencyFormat.format(this._amount, this._us), "Wrong format for US currency"),
        () -> assertEquals("£10,000.5", CurrencyFormat.format(this._amount, this._uk), "Wrong format for UK currency"),
        () -> assertEquals("10\u202F000,5\u00A0€", CurrencyFormat.format(this._amount, this._france), "Wrong format for France currency"),
        () -> assertEquals("10.000,5\u00A0€", CurrencyFormat.format(this._amount, this._german), "Wrong format for German currency"),
        () -> assertEquals("Rp10.000,5", CurrencyFormat.format(this._amount, this._indonesia), "Wrong format for Indonesia currency"),
        () -> assertEquals("￥10,000.5", CurrencyFormat.format(this._amount, this._japan), "Wrong format for Japan currency")
    ); // spotless:on
  }

  @Test
  public void formatWithUnit() {
    final Context context = Mockito.mock(Context.class);
    whenever(context.getString(R.string.symbol_thousand)).thenReturn("K");
    whenever(context.getString(R.string.symbol_million)).thenReturn("M");
    whenever(context.getString(R.string.symbol_billion)).thenReturn("B");
    whenever(context.getString(R.string.symbol_trillion)).thenReturn("T");

    assertAll( // spotless:off
        // Dozen and hundred.
        () -> assertEquals("$0", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(0), this._us), "Wrong formatted unit for zero"),
        () -> assertEquals("-$100", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(-100), this._us), "Wrong formatted unit for negative hundreds"),
        () -> assertEquals("$100", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(100), this._us), "Wrong formatted unit for hundreds"),

        // Thousands.
        () -> assertEquals("-$1K", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(-1000), this._us), "Wrong formatted unit for negative thousands"),
        () -> assertEquals("$1K", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1000), this._us), "Wrong formatted unit for thousands"),
        () -> assertEquals("$1.5K", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1500), this._us), "Wrong formatted unit for thousands"),
        () -> assertEquals("$1.5K", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1555), this._us), "Wrong formatted unit for thousands"),

        // Millions.
        () -> assertEquals("-$1M", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(-1_000_000), this._us), "Wrong formatted unit for negative millions"),
        () -> assertEquals("$1M", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_000_000), this._us), "Wrong formatted unit for millions"),
        () -> assertEquals("$1.5M", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_000), this._us), "Wrong formatted unit for millions"),
        () -> assertEquals("$1.5M", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_555), this._us), "Wrong formatted unit for millions"),

        // Billions.
        () -> assertEquals("-$1B", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(-1_000_000_000), this._us), "Wrong formatted unit for negative billions"),
        () -> assertEquals("$1B", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_000_000_000), this._us), "Wrong formatted unit for billions"),
        () -> assertEquals("$1.5B", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_000_000), this._us), "Wrong formatted unit for billions"),
        () -> assertEquals("$1.5B", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_555_000), this._us), "Wrong formatted unit for billions"),

        // Trillions.
        () -> assertEquals("-$1T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(-1_000_000_000_000L), this._us), "Wrong formatted unit for negative trillions"),
        () -> assertEquals("$1T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_000_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1.5T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1.5T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_555_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,000T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_000_000_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,555T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_000_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,555.5T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_555_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,000,000T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_000_000_000_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,555,000T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_000_000_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,555,555T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_555_000_000_000_000L), this._us), "Wrong formatted unit for trillions"),
        () -> assertEquals("$1,555,555.5T", CurrencyFormat.formatWithUnit(context, BigDecimal.valueOf(1_555_555_555_000_000_000L), this._us), "Wrong formatted unit for trillions")
    ); // spotless:on
  }

  @Test
  public void parse() {
    final String decimalSeparator = CurrencyFormat.decimalSeparator(this._us);

    assertAll( // spotless:off
        () -> assertEquals(BigDecimal.ZERO, CurrencyFormat.parse("", this._us), "Parse to zero when there's only an empty string"),
        () -> assertEquals(BigDecimal.ZERO, CurrencyFormat.parse(" ", this._us), "Parse to zero when there's only a blank string"),
        () -> assertEquals(BigDecimal.ZERO, CurrencyFormat.parse("-", this._us), "Parse to zero when there's only a minus sign"),
        () -> assertEquals(BigDecimal.ZERO, CurrencyFormat.parse(decimalSeparator, this._us), "Parse to zero when there's only a decimal separator"),
        () -> assertEquals(BigDecimal.ZERO, CurrencyFormat.parse("-" + decimalSeparator, this._us), "Parse to zero when only '-" + decimalSeparator + "' presented"),
        () -> assertEquals(BigDecimal.ZERO, CurrencyFormat.parse("--1", this._us), "Parse to zero when there are multiple minus sign"),
        () -> assertEquals(this._amount, CurrencyFormat.parse(this._amount.toString() + "0", this._us), "Remove trailing zero when parsing"),

        () -> assertEquals(this._amount, CurrencyFormat.parse("$10,000.5", this._us), "Wrong parsing from US currency"),
        () -> assertEquals(this._amount, CurrencyFormat.parse("£10,000.5", this._uk), "Wrong parsing from UK currency"),
        () -> assertEquals(this._amount, CurrencyFormat.parse("10\u202F000,5\u00A0€", this._france), "Wrong parsing from France currency"),
        () -> assertEquals(this._amount, CurrencyFormat.parse("10.000,5\u00A0€", this._german), "Wrong parsing from German currency"),
        () -> assertEquals(this._amount, CurrencyFormat.parse("Rp10.000,5", this._indonesia), "Wrong parsing from Indonesia currency"),
        () -> assertEquals(this._amount, CurrencyFormat.parse("￥10,000.5", this._japan), "Wrong parsing from Japan currency")
    ); // spotless:on
  }

  @Test
  public void symbolPosition() {
    assertAll( // spotless:off
        () -> assertTrue(CurrencyFormat.isSymbolAtStart(this._us), "US dollar symbol is at the start of the string"),
        () -> assertTrue(CurrencyFormat.isSymbolAtStart(this._uk), "UK pound symbol is at the start of the string"),
        () -> assertFalse(CurrencyFormat.isSymbolAtStart(this._france), "France euro symbol is at the end of the string"),
        () -> assertFalse(CurrencyFormat.isSymbolAtStart(this._german), "German euro symbol is at the end of the string"),
        () -> assertTrue(CurrencyFormat.isSymbolAtStart(this._indonesia), "Indonesia rupiah symbol is at the start of the string"),
        () -> assertTrue(CurrencyFormat.isSymbolAtStart(this._japan), "Japan yen symbol is at the start of the string")
    ); // spotless:on
  }
}
