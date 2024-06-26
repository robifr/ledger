/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.util;

import androidx.annotation.NonNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

public class CurrencyFormat {
  public static final int MINIMUM_FRACTION_DIGITS = 0;
  public static final int MAXIMUM_FRACTION_DIGITS = 5;

  private CurrencyFormat() {}

  public static String format(
      @NonNull BigDecimal amount, @NonNull String language, @NonNull String country) {
    return CurrencyFormat.format(
        amount, language, country, CurrencyFormat.symbol(language, country));
  }

  /**
   * @return Formatted amount into local specific currency.
   */
  @NonNull
  public static String format(
      @NonNull BigDecimal amount,
      @NonNull String language,
      @NonNull String country,
      @NonNull String symbol) {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);
    Objects.requireNonNull(symbol);

    final DecimalFormat format =
        (DecimalFormat) NumberFormat.getCurrencyInstance(new Locale(language, country));
    final DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();

    symbols.setCurrencySymbol(symbol);
    format.setRoundingMode(RoundingMode.DOWN);
    format.setMinimumFractionDigits(CurrencyFormat.MINIMUM_FRACTION_DIGITS);
    format.setMaximumFractionDigits(CurrencyFormat.MAXIMUM_FRACTION_DIGITS);
    format.setDecimalFormatSymbols(symbols);
    return format.format(amount);
  }

  /** {@link CurrencyFormat#formatWithUnit(BigDecimal, String, String, String)} */
  @NonNull
  public static String formatWithUnit(
      @NonNull BigDecimal amount, @NonNull String language, @NonNull String country) {
    return CurrencyFormat.formatWithUnit(
        amount, language, country, CurrencyFormat.symbol(language, country));
  }

  /**
   * @return Formatted amount into local specific currency with an appropriate suffix (such as K for
   *     thousands or M for millions) appended at the end of the string.
   */
  @NonNull
  public static String formatWithUnit(
      @NonNull BigDecimal amount,
      @NonNull String language,
      @NonNull String country,
      @NonNull String symbol) {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);
    Objects.requireNonNull(symbol);

    final BigDecimal thousand = BigDecimal.valueOf(1000);
    final BigDecimal million = BigDecimal.valueOf(1_000_000);
    final BigDecimal billion = BigDecimal.valueOf(1_000_000_000);
    final BigDecimal trillion = BigDecimal.valueOf(1_000_000_000_000L);

    // Convert negative amount to positive to handle where negative amounts
    // can't be formatted due to division.
    final String negativePrefix = amount.compareTo(BigDecimal.ZERO) < 0 ? "-" : "";
    final BigDecimal positiveAmount = amount.abs();

    if (positiveAmount.compareTo(thousand) < 0) {
      return negativePrefix + CurrencyFormat.format(positiveAmount, language, country, symbol);

    } else if (positiveAmount.compareTo(million) < 0) {
      return negativePrefix
          + CurrencyFormat.format(
              positiveAmount.divide(thousand, 1, RoundingMode.DOWN), language, country, symbol)
          + "K";

    } else if (positiveAmount.compareTo(billion) < 0) {
      return negativePrefix
          + CurrencyFormat.format(
              positiveAmount.divide(million, 1, RoundingMode.DOWN), language, country, symbol)
          + "M";

    } else if (positiveAmount.compareTo(trillion) < 0) {
      return negativePrefix
          + CurrencyFormat.format(
              positiveAmount.divide(billion, 1, RoundingMode.DOWN), language, country, symbol)
          + "B";
    }

    return negativePrefix
        + CurrencyFormat.format(
            positiveAmount.divide(trillion, 1, RoundingMode.DOWN), language, country, symbol)
        + "T";
  }

  /**
   * @return Parsed amount from local specific currency.
   */
  @NonNull
  public static BigDecimal parse(
      @NonNull String amount, @NonNull String language, @NonNull String country)
      throws ParseException {
    Objects.requireNonNull(amount);
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);

    final DecimalFormat format =
        (DecimalFormat) NumberFormat.getNumberInstance(new Locale(language, country));
    format.setParseBigDecimal(true);

    final String decimalSeparator = CurrencyFormat.decimalSeparator(language, country);
    String amountToParse = amount.replaceAll("[^\\d\\-\\" + decimalSeparator + "]", "");

    // Edge case.
    if (amountToParse.isBlank()
        || amountToParse.equals(decimalSeparator)
        || amountToParse.equals("-")
        || amountToParse.equals("-" + decimalSeparator)
        || Strings.countOccurrence(amountToParse, "-") > 1) {
      amountToParse = "0";
    }

    return ((BigDecimal) format.parse(amountToParse)).stripTrailingZeros();
  }

  @NonNull
  public static String symbol(@NonNull String language, @NonNull String country) {
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);

    return new DecimalFormatSymbols(new Locale(language, country)).getCurrencySymbol();
  }

  @NonNull
  public static String groupingSeparator(@NonNull String language, @NonNull String country) {
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);

    final char separator =
        new DecimalFormatSymbols(new Locale(language, country)).getGroupingSeparator();
    return Character.toString(separator);
  }

  @NonNull
  public static String decimalSeparator(@NonNull String language, @NonNull String country) {
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);

    final char separator =
        new DecimalFormatSymbols(new Locale(language, country)).getDecimalSeparator();
    return Character.toString(separator);
  }

  public static int countDecimalPlace(@NonNull BigDecimal amount) {
    Objects.requireNonNull(amount);

    return Math.max(0, amount.stripTrailingZeros().scale());
  }

  public static boolean isSymbolAtStart(@NonNull String language, @NonNull String country) {
    Objects.requireNonNull(language);
    Objects.requireNonNull(country);

    final DecimalFormat format =
        (DecimalFormat) NumberFormat.getCurrencyInstance(new Locale(language, country));
    return format.toLocalizedPattern().indexOf('\u00A4') == 0;
  }
}
