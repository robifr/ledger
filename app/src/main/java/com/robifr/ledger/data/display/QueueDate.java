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

package com.robifr.ledger.data.display;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.util.Pair;
import com.robifr.ledger.R;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

@Deprecated
public record QueueDate(
    @NonNull Range range, @NonNull ZonedDateTime dateStart, @NonNull ZonedDateTime dateEnd) {
  public QueueDate {
    Objects.requireNonNull(range);
    Objects.requireNonNull(dateStart);
    Objects.requireNonNull(dateEnd);
  }

  /**
   * @apiNote Use {@link #withCustomRange(ZonedDateTime, ZonedDateTime)} specifically for {@link
   *     Range#CUSTOM} enum. Otherwise, the initial epoch time will be set for both {@link
   *     #dateStart} and {@link #dateEnd}.
   */
  @NonNull
  public static QueueDate withRange(@NonNull Range range) {
    Objects.requireNonNull(range);

    return new QueueDate(range, range._dateStartEnd().first, range._dateStartEnd().second);
  }

  @NonNull
  public static QueueDate withCustomRange(
      @NonNull ZonedDateTime dateStart, @NonNull ZonedDateTime dateEnd) {
    Objects.requireNonNull(dateStart);
    Objects.requireNonNull(dateEnd);

    return new QueueDate(Range.CUSTOM, dateStart, dateEnd);
  }

  @Deprecated
  public enum Range {
    ALL_TIME(R.string.enum_queueDate_allTime),
    TODAY(R.string.enum_queueDate_today),
    YESTERDAY(R.string.enum_queueDate_yesterday),
    THIS_WEEK(R.string.enum_queueDate_thisWeek),
    THIS_MONTH(R.string.enum_queueDate_thisMonth),
    THIS_YEAR(R.string.enum_queueDate_thisYear),
    CUSTOM(R.string.enum_queueDate_custom);

    @StringRes private final int _resourceString;

    private Range(@StringRes int resourceString) {
      this._resourceString = resourceString;
    }

    @StringRes
    public int resourceString() {
      return this._resourceString;
    }

    /**
     * @return Pair of start (first) and end (second) date.
     * @apiNote For {@link #CUSTOM} enum, you have to manually specify the value, otherwise initial
     *     epoch time will be set.
     */
    @NonNull
    private Pair<ZonedDateTime, ZonedDateTime> _dateStartEnd() {
      return switch (this) {
        case TODAY ->
            new Pair<>(
                LocalDate.now().atStartOfDay(ZoneId.systemDefault()),
                LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()));

        case YESTERDAY ->
            new Pair<>(
                LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()),
                LocalDate.now().minusDays(1).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()));

        case THIS_WEEK ->
            new Pair<>(
                LocalDate.now()
                    .with(ChronoField.DAY_OF_WEEK, 1)
                    .atStartOfDay()
                    .atZone(ZoneId.systemDefault()),
                LocalDate.now()
                    .with(ChronoField.DAY_OF_WEEK, 7)
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault()));

        case THIS_MONTH ->
            new Pair<>(
                LocalDate.now()
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .atStartOfDay(ZoneId.systemDefault()),
                LocalDate.now()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault()));

        case THIS_YEAR ->
            new Pair<>(
                LocalDate.now()
                    .with(TemporalAdjusters.firstDayOfYear())
                    .atStartOfDay(ZoneId.systemDefault()),
                LocalDate.now()
                    .with(TemporalAdjusters.lastDayOfYear())
                    .atTime(LocalTime.MAX)
                    .atZone(ZoneId.systemDefault()));

        case CUSTOM ->
            new Pair<>(
                Instant.EPOCH.atZone(ZoneId.systemDefault()),
                Instant.EPOCH.atZone(ZoneId.systemDefault()));

          // Defaults to `Range#ALL_TIME`.
        default ->
            new Pair<>(
                Instant.EPOCH.atZone(ZoneId.systemDefault()),
                LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()));
      };
    }
  }
}
