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

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueDateTest {
  private fun `_date range cases`(): Array<Array<Any>> =
      arrayOf(
          // spotless:off
          arrayOf(QueueDate.Range.ALL_TIME, "1970-01-01T00:00:00Z", "2024-01-01T23:59:59.999999999Z"),
          arrayOf(QueueDate.Range.TODAY, "2024-01-01T00:00:00Z", "2024-01-01T23:59:59.999999999Z"),
          arrayOf(QueueDate.Range.YESTERDAY, "2023-12-31T00:00:00Z", "2023-12-31T23:59:59.999999999Z"),
          arrayOf(QueueDate.Range.THIS_WEEK, "2024-01-01T00:00:00Z", "2024-01-07T23:59:59.999999999Z"),
          arrayOf(QueueDate.Range.THIS_MONTH, "2024-01-01T00:00:00Z", "2024-01-31T23:59:59.999999999Z"),
          arrayOf(QueueDate.Range.THIS_YEAR, "2024-01-01T00:00:00Z", "2024-12-31T23:59:59.999999999Z"),
          arrayOf(QueueDate.Range.CUSTOM, "1970-01-01T00:00:00Z", "1970-01-01T00:00:00Z"))
          // spotless:on

  @ParameterizedTest
  @MethodSource("_date range cases")
  fun `date range`(range: QueueDate.Range, dateStart: String, dateEnd: String) {
    val fixedClock: Clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"))
    assertAll({
      assertEquals(
          Instant.parse(dateStart),
          when (range) {
            QueueDate.Range.ALL_TIME,
            QueueDate.Range.CUSTOM ->
                range.dateStart(fixedClock).withZoneSameInstant(ZoneId.of("UTC")).toInstant()
            else -> range.dateStart(fixedClock).withZoneSameLocal(ZoneId.of("UTC")).toInstant()
          },
          "Generate date start based on the given range")
      assertEquals(
          Instant.parse(dateEnd),
          when (range) {
            QueueDate.Range.CUSTOM ->
                range.dateEnd(fixedClock).withZoneSameInstant(ZoneId.of("UTC")).toInstant()
            else -> range.dateEnd(fixedClock).withZoneSameLocal(ZoneId.of("UTC")).toInstant()
          },
          "Generate date end based on the given range")
    })
  }
}
