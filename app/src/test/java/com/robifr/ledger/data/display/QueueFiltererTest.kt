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

import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueFiltererTest {
  private val _queue: QueueModel =
      QueueModel(
          id = null,
          customerId = null,
          customer = null,
          status = QueueModel.Status.IN_QUEUE,
          date = Instant.now(),
          paymentMethod = QueueModel.PaymentMethod.CASH,
          productOrders = listOf())
  private val _customer: CustomerModel = CustomerModel(id = null, name = "")
  private val _productOrder: ProductOrderModel =
      ProductOrderModel(productId = null, productName = null, productPrice = null, quantity = 0.0)

  private fun `_filter by customer ids with null customer cases`(): Array<Array<Any>> =
      arrayOf(
          arrayOf(listOf(111L, 222L, null), listOf(111L), true, listOf(111L, null)),
          arrayOf(listOf(111L, 222L, null), listOf(111L), false, listOf(111L)),
          // The filtered customer ID isn't in the customers.
          arrayOf(listOf(111L, 222L, null), listOf(333L), true, listOf(null)),
          arrayOf(listOf(111L, 222L, null), listOf(333L), false, listOf<Long>()),
          // Show all queues when the filtered customer IDs is empty.
          arrayOf(listOf(111L, 222L, null), listOf<Long>(), true, listOf(111L, 222L, null)),
          arrayOf(listOf(111L, 222L, null), listOf<Long>(), false, listOf(111L, 222L)))

  @ParameterizedTest
  @MethodSource("_filter by customer ids with null customer cases")
  fun `filter by customer ids with null customer`(
      customerIds: List<Long?>,
      filteredCustomerIds: List<Long>,
      isNullCustomerShown: Boolean,
      filteredQueueCustomerIds: List<Long?>
  ) {
    val queues: List<QueueModel> =
        customerIds.mapIndexed { i, customerId ->
          _queue.copy(
              id = (i + 1) * 111L,
              customerId = customerId,
              customer = _customer.copy(id = customerId))
        }
    val filterer: FakeQueueFilterer =
        FakeQueueFilterer().apply {
          filters =
              filters.copy(
                  isNullCustomerShown = isNullCustomerShown,
                  filteredCustomerIds = filteredCustomerIds)
        }
    assertEquals(
        filteredQueueCustomerIds,
        filterer.filter(queues).map { it.customerId },
        "Filter queues based on their customer id")
  }

  @ParameterizedTest
  @EnumSource(QueueModel.Status::class)
  fun `filter by status`(filteredStatus: QueueModel.Status) {
    val queues: List<QueueModel> =
        QueueModel.Status.entries.mapIndexed { i, status ->
          _queue.copy(id = (i + 1) * 111L, status = status)
        }
    val filterer: FakeQueueFilterer =
        FakeQueueFilterer().apply { filters = filters.copy(filteredStatus = setOf(filteredStatus)) }
    assertEquals(
        setOf(filteredStatus),
        filterer.filter(queues).map { it.status }.toSet(),
        "Filter queues based on their status")
  }

  private fun `_filter by ranged date cases`(): Array<Array<Any>> =
      arrayOf(
          // spotless:off
          // Subtract one day for week, month, and year to ensure inclusion
          // when the range is on their current period.
          arrayOf(listOf(0L, 1L, 6L, 29L, 364L, 500L), QueueDate.Range.ALL_TIME, listOf(0L, 1L, 6L, 29L, 364L, 500L)),
          arrayOf(listOf(0L, 1L, 6L, 29L, 364L, 500L), QueueDate.Range.TODAY, listOf(0L)),
          arrayOf(listOf(0L, 1L, 6L, 29L, 364L, 500L), QueueDate.Range.YESTERDAY, listOf(1L)),
          arrayOf(listOf(0L, 1L, 6L, 29L, 364L, 500L), QueueDate.Range.THIS_WEEK, listOf(0L, 1L, 6L)),
          arrayOf(listOf(0L, 1L, 6L, 29L, 364L, 500L), QueueDate.Range.THIS_MONTH, listOf(0L, 1L, 6L, 29L)),
          arrayOf(listOf(0L, 1L, 6L, 29L, 364L, 500L), QueueDate.Range.THIS_YEAR, listOf(0L, 1L, 6L, 29L, 364L)))
          // spotless:on

  @ParameterizedTest
  @MethodSource("_filter by ranged date cases")
  fun `filter by ranged date`(
      minusDaysFromNow: List<Long>,
      dateRange: QueueDate.Range,
      filteredMinusDaysFromNow: List<Long>
  ) {
    // Simulate the current date as 2023/Dec/31. This is an ideal date because the inclusion
    // range works perfectly, e.g., 31 (Sun) - 6 = 25 (Mon), which remains within the same week.
    // Same applies to month and year.
    //
    // Sun Mon Tue Wed Thu Fri Sat
    //                      1   2
    //  3   4   5   6   7   8   9
    // 10  11  12  13  14  15  16
    // 17  18  19  20  21  22  23
    // 24  25  26  27  28  29  30
    // 31
    val fixedClock: Clock = Clock.fixed(Instant.parse("2023-12-31T00:00:00Z"), ZoneId.of("UTC"))
    val queues: List<QueueModel> =
        minusDaysFromNow.mapIndexed { i, minusDay ->
          _queue.copy(
              id = (i + 1) * 111L,
              date =
                  LocalDate.now(fixedClock)
                      .minusDays(minusDay)
                      .atStartOfDay(ZoneId.of("UTC"))
                      .toInstant())
        }
    val filterer: FakeQueueFilterer =
        FakeQueueFilterer().apply {
          filters =
              filters.copy(
                  filteredDate =
                      QueueDate(
                          dateRange,
                          dateRange.dateStart(fixedClock),
                          dateRange.dateEnd(fixedClock)))
        }
    assertEquals(
        filteredMinusDaysFromNow.map {
          LocalDate.now(fixedClock).minusDays(it).atStartOfDay(ZoneId.of("UTC")).toInstant()
        },
        filterer.filter(queues).map { it.date },
        "Filter queues based on their date")
  }

  @Test
  fun `filter by custom date`() {
    val dec2023: Instant = Instant.parse("2023-12-01T00:00:00Z")
    val jan2024: Instant = Instant.parse("2024-01-01T00:00:00Z")
    val feb2024: Instant = Instant.parse("2024-02-01T00:00:00Z")
    val queues: List<QueueModel> =
        listOf(dec2023, jan2024, feb2024).mapIndexed { i, date ->
          _queue.copy(id = (i + 1) * 111L, date = date)
        }
    val filterer: FakeQueueFilterer =
        FakeQueueFilterer().apply {
          filters =
              filters.copy(
                  filteredDate =
                      QueueDate(dec2023.atZone(ZoneId.of("UTC")), jan2024.atZone(ZoneId.of("UTC"))))
        }
    assertEquals(
        listOf(dec2023, jan2024),
        filterer.filter(queues).map { it.date },
        "Filter queues based on their date")
  }

  private fun `_filter by grand total price cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(listOf(100L, 200L, 300L), null, null, listOf(100L, 200L, 300L)),
          arrayOf(listOf(100L, 200L, 300L), null, 200L, listOf(100L, 200L)),
          arrayOf(listOf(100L, 200L, 300L), 200L, null, listOf(200L, 300L)),
          arrayOf(listOf(100L, 200L, 300L), 200L, 200L, listOf(200L)))

  @ParameterizedTest
  @MethodSource("_filter by grand total price cases")
  fun `filter by grand total price`(
      totalPrices: List<Long>,
      filteredMinGrandTotalPrice: Long?,
      filteredMaxGrandTotalPrice: Long?,
      filteredGrandTotalPrices: List<Long>
  ) {
    val queues: List<QueueModel> =
        totalPrices.mapIndexed { i, totalPrice ->
          _queue.copy(
              id = (i + 1) * 111L,
              productOrders =
                  listOf(
                      _productOrder.copy(
                          id = (i + 1) * 111L,
                          productPrice = totalPrice,
                          quantity = 1.0,
                          totalPrice = totalPrice.toBigDecimal())))
        }
    val filterer: FakeQueueFilterer =
        FakeQueueFilterer().apply {
          filters =
              filters.copy(
                  filteredTotalPrice =
                      filteredMinGrandTotalPrice?.toBigDecimal() to
                          filteredMaxGrandTotalPrice?.toBigDecimal())
        }
    assertEquals(
        filteredGrandTotalPrices,
        filterer.filter(queues).map { it.grandTotalPrice().toLong() },
        "Filter queues based on their grand total price")
  }
}
