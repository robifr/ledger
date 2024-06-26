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

package com.robifr.ledger.assetbinding.chart;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import com.robifr.ledger.util.CurrencyFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ChartUtil {
  private ChartUtil() {}

  @NonNull
  public static Map<String, BigDecimal> toDateTimeData(
      @NonNull Map<ZonedDateTime, BigDecimal> data,
      @NonNull Pair<ZonedDateTime, ZonedDateTime> dateStartEnd) {
    Objects.requireNonNull(data);
    Objects.requireNonNull(dateStartEnd);

    final Map<String, BigDecimal> result = new LinkedHashMap<>();
    final BiFunction<ChronoUnit, ZonedDateTime, String> formatKey =
        (unit, date) ->
            switch (unit) {
              case DAYS -> Integer.toString(date.getDayOfMonth());
              case MONTHS ->
                  date.getMonth().name().substring(0, 1).toUpperCase()
                      + date.getMonth().name().substring(1, 3).toLowerCase();
              default -> Integer.toString(date.getYear());
            };
    final BiFunction<ChronoUnit, String, BigDecimal> sumValueForKey =
        (unit, key) ->
            data.entrySet().stream()
                .filter(entry -> formatKey.apply(unit, entry.getKey()).equals(key))
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    ChronoUnit groupBy;
    // Determine how the data will be grouped based on the date range.
    if (dateStartEnd.first.getYear() != dateStartEnd.second.getYear()) {
      groupBy = ChronoUnit.YEARS;
    } else if (dateStartEnd.first.getMonthValue() != dateStartEnd.second.getMonthValue()) {
      groupBy = ChronoUnit.MONTHS;
    } else {
      groupBy = ChronoUnit.DAYS;
    }

    // Fill the map with summed values for each key in the date range.
    for (ZonedDateTime date = dateStartEnd.first;
        !date.isAfter(dateStartEnd.second);
        date = date.plus(1, groupBy)) {
      final String formattedKey = formatKey.apply(groupBy, date);

      result.put(formattedKey, sumValueForKey.apply(groupBy, formattedKey));
    }

    // Ensure that the end date is included.
    // There's a bug when the month total between the start date and end date is less than 12.
    // For example, in the range 2023/7/1 - 2024/6/1, the map will not show the end date's year.
    if (!result.containsKey(formatKey.apply(groupBy, dateStartEnd.second))) {
      final String formattedKey = formatKey.apply(groupBy, dateStartEnd.second);

      result.put(formattedKey, sumValueForKey.apply(groupBy, formattedKey));
    }

    return result;
  }

  /**
   * @param data Map of data to convert.
   * @param mapType Type of map to be used to determine its sorting behavior.
   */
  @NonNull
  public static Map<String, Double> toPercentageData(
      @NonNull Map<String, BigDecimal> data, @NonNull Supplier<Map<String, Double>> mapType) {
    Objects.requireNonNull(data);
    Objects.requireNonNull(mapType);

    final Map<String, Double> result =
        mapType.get() instanceof TreeMap
            ? new TreeMap<>(
                (a, b) -> {
                  // Special case where string of numbers sorted wrongly.
                  if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;
                  return a.compareTo(b);
                })
            : mapType.get();
    final BigDecimal actualMaxValue =
        data.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    final BigDecimal paddedMaxValue =
        actualMaxValue.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ONE // Prevent zero division.
            : ChartUtil._ceilToNearestTen(
                actualMaxValue.add(
                    actualMaxValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));

    for (Map.Entry<String, BigDecimal> d : data.entrySet()) {
      result.put(
          d.getKey(),
          d.getValue()
              .divide(paddedMaxValue, 2, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .doubleValue());
    }

    return result;
  }

  @NonNull
  public static List<String> toPercentageLinearDomain(@NonNull Map<String, BigDecimal> data) {
    Objects.requireNonNull(data);

    final BigDecimal actualMaxValue =
        data.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    final BigDecimal paddedMaxValue =
        ChartUtil._ceilToNearestTen(
            actualMaxValue.add(
                actualMaxValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)));
    final BigDecimal gap = paddedMaxValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

    return IntStream.rangeClosed(0, 100)
        .mapToObj(
            // From percentages, linearly map the amount up to the top value.
            percent ->
                CurrencyFormat.formatWithUnit(
                    BigDecimal.valueOf(percent).multiply(gap), "id", "ID", ""))
        .collect(Collectors.toList());
  }

  @NonNull
  private static BigDecimal _ceilToNearestTen(@NonNull BigDecimal amount) {
    Objects.requireNonNull(amount);

    // Calculate the amount ensuring it's rounded
    // to the nearest ceiling multiple of 10 (e.g. 10, 1K, 10K).
    amount = amount.max(BigDecimal.ONE);
    final int magnitude = amount.precision() - amount.scale();
    final BigDecimal rounding = BigDecimal.TEN.pow(magnitude - 1);

    return amount
        .divide(rounding, 0, RoundingMode.CEILING)
        .multiply(rounding)
        .max(BigDecimal.TEN); // Set the minimum value to 10.
  }
}
