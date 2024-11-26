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

package com.robifr.ledger.data.display

import androidx.annotation.StringRes
import com.robifr.ledger.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters

data class QueueDate(val range: Range, val dateStart: ZonedDateTime, val dateEnd: ZonedDateTime) {
  constructor(
      dateStart: ZonedDateTime,
      dateEnd: ZonedDateTime
  ) : this(Range.CUSTOM, dateStart, dateEnd)

  /**
   * Use this constructor for ranges other than [Range.CUSTOM]. For a custom date range, use another
   * constructor that takes a [dateStart] and [dateEnd]. Otherwise, initial epoch time will be set.
   */
  constructor(range: Range) : this(range, range.dateStart(), range.dateEnd())

  /**
   * For [CUSTOM] enum, the default value for both [dateStart] and [dateEnd] will be set to initial
   * epoch time.
   */
  enum class Range(
      @StringRes val stringRes: Int,
      val dateStart: () -> ZonedDateTime,
      val dateEnd: () -> ZonedDateTime
  ) {
    ALL_TIME(
        R.string.enum_queueDate_allTime,
        { Instant.EPOCH.atZone(ZoneId.systemDefault()) },
        { LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()) }),
    TODAY(
        R.string.enum_queueDate_today,
        { LocalDate.now().atStartOfDay(ZoneId.systemDefault()) },
        { LocalDate.now().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()) }),
    YESTERDAY(
        R.string.enum_queueDate_yesterday,
        { LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()) },
        { LocalDate.now().minusDays(1).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()) }),
    THIS_WEEK(
        R.string.enum_queueDate_thisWeek,
        {
          LocalDate.now()
              .with(ChronoField.DAY_OF_WEEK, 1)
              .atStartOfDay()
              .atZone(ZoneId.systemDefault())
        },
        {
          LocalDate.now()
              .with(ChronoField.DAY_OF_WEEK, 7)
              .atTime(LocalTime.MAX)
              .atZone(ZoneId.systemDefault())
        }),
    THIS_MONTH(
        R.string.enum_queueDate_thisMonth,
        {
          LocalDate.now()
              .with(TemporalAdjusters.firstDayOfMonth())
              .atStartOfDay(ZoneId.systemDefault())
        },
        {
          LocalDate.now()
              .with(TemporalAdjusters.lastDayOfMonth())
              .atTime(LocalTime.MAX)
              .atZone(ZoneId.systemDefault())
        }),
    THIS_YEAR(
        R.string.enum_queueDate_thisYear,
        {
          LocalDate.now()
              .with(TemporalAdjusters.firstDayOfYear())
              .atStartOfDay(ZoneId.systemDefault())
        },
        {
          LocalDate.now()
              .with(TemporalAdjusters.lastDayOfYear())
              .atTime(LocalTime.MAX)
              .atZone(ZoneId.systemDefault())
        }),
    CUSTOM(
        R.string.enum_queueDate_custom,
        { Instant.EPOCH.atZone(ZoneId.systemDefault()) },
        { Instant.EPOCH.atZone(ZoneId.systemDefault()) })
  }
}
