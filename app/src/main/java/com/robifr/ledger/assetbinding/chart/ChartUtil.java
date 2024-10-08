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

package com.robifr.ledger.assetbinding.chart;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.util.Pair;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChartUtil {
  private ChartUtil() {}

  @NonNull
  public static String toDateTime(
      @NonNull ZonedDateTime dateToConvert,
      @NonNull Pair<ZonedDateTime, ZonedDateTime> dateStartEnd) {
    Objects.requireNonNull(dateToConvert);
    Objects.requireNonNull(dateStartEnd);

    ChronoUnit groupBy;
    // Determine how the data will be grouped based on the date range.
    if (dateStartEnd.first.getYear() != dateStartEnd.second.getYear()) {
      groupBy = ChronoUnit.YEARS;
    } else if (dateStartEnd.first.getMonthValue() != dateStartEnd.second.getMonthValue()) {
      groupBy = ChronoUnit.MONTHS;
    } else {
      groupBy = ChronoUnit.DAYS;
    }

    return switch (groupBy) {
      case DAYS -> Integer.toString(dateToConvert.getDayOfMonth());
      case MONTHS -> dateToConvert.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
      default -> Integer.toString(dateToConvert.getYear());
    };
  }

  @NonNull
  public static List<String> toDateTimeDomain(
      @NonNull Pair<ZonedDateTime, ZonedDateTime> dateStartEnd) {
    Objects.requireNonNull(dateStartEnd);

    ChronoUnit groupBy;
    // Determine how the data will be grouped based on the date range.
    if (dateStartEnd.first.getYear() != dateStartEnd.second.getYear()) {
      groupBy = ChronoUnit.YEARS;
    } else if (dateStartEnd.first.getMonthValue() != dateStartEnd.second.getMonthValue()) {
      groupBy = ChronoUnit.MONTHS;
    } else {
      groupBy = ChronoUnit.DAYS;
    }

    final ArrayList<String> result = new ArrayList<>();

    // Fill the map with summed values for each key in the date range.
    for (ZonedDateTime date = dateStartEnd.first;
        !date.isAfter(dateStartEnd.second);
        date = date.plus(1, groupBy)) {
      result.add(ChartUtil.toDateTime(date, dateStartEnd));
    }

    // Ensure that the end date is included.
    // There's a bug when the month total between the start date and end date is less than 12.
    // For example, in the range 2023/7/1 - 2024/6/1, the map will not show the end date's year.
    if (!result.contains(ChartUtil.toDateTime(dateStartEnd.second, dateStartEnd))) {
      result.add(ChartUtil.toDateTime(dateStartEnd.second, dateStartEnd));
    }

    return result;
  }

  public static double toPercentageLinear(
      @NonNull BigDecimal valueToConvert, @NonNull BigDecimal maxValue, int ticks) {
    Objects.requireNonNull(valueToConvert);
    Objects.requireNonNull(maxValue);

    // Set to one as the minimum to prevent zero division.
    final BigDecimal paddedMaxValue =
        BigDecimal.ONE.max(ChartUtil._ceilToNearestNiceNumber(maxValue, ticks));

    return valueToConvert
        .divide(paddedMaxValue, 2, RoundingMode.HALF_UP)
        .multiply(BigDecimal.valueOf(100))
        .doubleValue();
  }

  @NonNull
  public static List<String> toPercentageLinearDomain(
      @NonNull Context context, @NonNull BigDecimal maxValue, int ticks) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(maxValue);

    // Set to one as the minimum to prevent zero division.
    final BigDecimal paddedMaxValue =
        BigDecimal.ONE.max(ChartUtil._ceilToNearestNiceNumber(maxValue, ticks));
    final BigDecimal gap = paddedMaxValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

    return IntStream.rangeClosed(0, 100)
        .mapToObj(
            // From percentages, linearly map the amount up to the top value.
            percent ->
                CurrencyFormat.formatWithUnit(
                    context,
                    BigDecimal.valueOf(percent).multiply(gap),
                    AppCompatDelegate.getApplicationLocales().toLanguageTags()))
        .collect(Collectors.toList());
  }

  /**
   * @implNote The algorithm is based on <a href="https://stackoverflow.com/a/16363437">this Stack
   *     Overflow answer</a>.
   * @return Array of nice minimum value (index 0), nice maximum value (index 1), and ticks spacing
   *     (index 2).
   */
  @NonNull
  public static double[] calculateNiceScale(double minPoint, double maxPoint, double ticks) {
    final BiFunction<Double, Boolean, Double> obtainNiceNumber =
        (range, shouldRound) -> {
          final double exponent = Math.floor(Math.log10(range));
          final double fraction = range / Math.pow(10, exponent);
          double niceFraction;

          if (shouldRound) {
            if (fraction < 1.5) niceFraction = 1;
            else if (fraction < 3) niceFraction = 2;
            else if (fraction < 7) niceFraction = 5;
            else niceFraction = 10;
          } else {
            if (fraction <= 1) niceFraction = 1;
            else if (fraction <= 2) niceFraction = 2;
            else if (fraction <= 5) niceFraction = 5;
            else niceFraction = 10;
          }

          return niceFraction * Math.pow(10, exponent);
        };

    final double range = obtainNiceNumber.apply(maxPoint - minPoint, false);
    final double tickSpacing = obtainNiceNumber.apply(range / (ticks - 1), true);
    final double niceMin = Math.floor(minPoint / tickSpacing) * tickSpacing;
    final double niceMax = Math.ceil(maxPoint / tickSpacing) * tickSpacing;

    return new double[] {niceMin, niceMax, tickSpacing};
  }

  @NonNull
  private static BigDecimal _ceilToNearestNiceNumber(@NonNull BigDecimal amount, int ticks) {
    Objects.requireNonNull(amount);

    final BigDecimal hundred = BigDecimal.valueOf(100);
    final BigDecimal thousand = BigDecimal.valueOf(1000);
    final BigDecimal million = BigDecimal.valueOf(1_000_000);
    final BigDecimal billion = BigDecimal.valueOf(1_000_000_000);
    final BigDecimal trillion = BigDecimal.valueOf(1_000_000_000_000L);
    BigDecimal scaleFactor = BigDecimal.ONE;

    if (amount.compareTo(trillion) >= 0) scaleFactor = trillion;
    else if (amount.compareTo(billion) >= 0) scaleFactor = billion;
    else if (amount.compareTo(million) >= 0) scaleFactor = million;
    else if (amount.compareTo(thousand) >= 0) scaleFactor = thousand;
    else if (amount.compareTo(hundred) >= 0) scaleFactor = hundred;

    // Scale down the value because `Math#log10` doesn't work with `BigDecimal`,
    // but the algorithm occasionally requires it.
    final double minScaled =
        BigDecimal.ZERO.divide(scaleFactor, MathContext.DECIMAL128).doubleValue();
    final double maxScaled =
        amount.max(BigDecimal.ONE).divide(scaleFactor, MathContext.DECIMAL128).doubleValue();

    return BigDecimal.valueOf(ChartUtil.calculateNiceScale(minScaled, maxScaled, ticks)[1])
        .multiply(scaleFactor); // Scale up to the actual value.
  }
}
