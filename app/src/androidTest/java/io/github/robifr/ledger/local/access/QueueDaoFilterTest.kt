/**
 * Copyright 2025 Robi
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

package io.github.robifr.ledger.local.access

import io.github.robifr.ledger.assertAllSoftly
import io.github.robifr.ledger.data.display.FakeQueueFilterer
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.display.QueueSortMethod
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class QueueDaoFilterByCustomerTest(
    private val _customerIds: List<Long?>,
    private val _filteredCustomerIds: List<Long>,
    private val _isNullCustomerShown: Boolean,
    private val _filteredQueueCustomerIds: List<Long?>
) : QueueDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, customerId) in _customerIds.withIndex()) {
      customerId?.let { _database.customerDao().insert(CustomerModel(id = customerId, name = "")) }
      _queueDao.insert(_queue.copy(id = (i + 1) * 111L, customerId = customerId))
    }
  }

  @Test
  fun `select all paginated info with filtered customers`() {
    val tableSize: Int = _queueDao.selectAll().size

    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filteredCustomerIds,
                          isNullCustomerShown = _isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.customerId })
              .describedAs("Select all queues filtered based on their customer ID")
              .containsExactlyInAnyOrderElementsOf(_filteredQueueCustomerIds)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filteredCustomerIds,
                          isNullCustomerShown = _isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.customerId })
              .describedAs("Select queues filtered based on their customer ID")
              .containsExactlyInAnyOrderElementsOf(_filteredQueueCustomerIds)
        })
  }

  @Test
  fun `count queues with filtered customers`() {
    assertThat(
            _queueDao.countFilteredQueues(
                shouldCalculateGrandTotalPrice = false,
                filteredCustomerIds = _filteredCustomerIds,
                isNullCustomerShown = _isNullCustomerShown,
                filteredStatus = _filters.filteredStatus,
                filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                filteredDateEnd = _filters.filteredDate.dateEnd.toInstant()))
        .describedAs("Count total queues filtered based on their customer ID")
        .isEqualTo(_filteredQueueCustomerIds.size.toLong())
  }

  @Test
  fun `replicate customers filtering query with fake filterer`() {
    assertThat(
            FakeQueueFilterer(
                    _filters.copy(
                        isNullCustomerShown = _isNullCustomerShown,
                        filteredCustomerIds = _filteredCustomerIds))
                .filter(_queueDao.selectAll())
                .map { it.customerId })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(_filteredQueueCustomerIds)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            arrayOf(listOf(111L, 222L, null), listOf(111L), true, listOf(111L, null)),
            arrayOf(listOf(111L, 222L, null), listOf(111L), false, listOf(111L)),
            // The filtered customer ID isn't in the customers.
            arrayOf(listOf(111L, 222L, null), listOf(333L), true, listOf(null)),
            arrayOf(listOf(111L, 222L, null), listOf(333L), false, listOf<Long>()),
            // Show all queues when the filtered customer IDs is empty.
            arrayOf(listOf(111L, 222L, null), listOf<Long>(), true, listOf(111L, 222L, null)),
            arrayOf(listOf(111L, 222L, null), listOf<Long>(), false, listOf(111L, 222L)))
  }
}

@RunWith(Parameterized::class)
class QueueDaoFilterByStatusTest(private val _filteredStatus: Set<QueueModel.Status>) :
    QueueDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, status) in QueueModel.Status.entries.withIndex()) {
      _queueDao.insert(_queue.copy(id = (i + 1) * 111L, status = status))
    }
  }

  @Test
  fun `select all paginated info with filtered status`() {
    val tableSize: Int = _queueDao.selectAll().size

    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.status })
              .describedAs("Select all queues filtered based on their status")
              .containsExactlyInAnyOrderElementsOf(_filteredStatus)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.status })
              .describedAs("Select queues filtered based on their status")
              .containsExactlyInAnyOrderElementsOf(_filteredStatus)
        })
  }

  @Test
  fun `count queues with filtered status`() {
    assertThat(
            _queueDao.countFilteredQueues(
                shouldCalculateGrandTotalPrice = false,
                filteredCustomerIds = _filters.filteredCustomerIds,
                isNullCustomerShown = _filters.isNullCustomerShown,
                filteredStatus = _filteredStatus,
                filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                filteredDateEnd = _filters.filteredDate.dateEnd.toInstant()))
        .describedAs("Count total queues filtered based on their status")
        .isEqualTo(_filteredStatus.size.toLong())
  }

  @Test
  fun `replicate status filtering query with fake filterer`() {
    assertThat(
            FakeQueueFilterer(_filters.copy(filteredStatus = _filteredStatus))
                .filter(_queueDao.selectAll())
                .map { it.status })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(_filteredStatus)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            *QueueModel.Status.entries.map { arrayOf<Any>(setOf(it)) }.toTypedArray(),
            arrayOf(setOf<QueueModel.Status>()))
  }
}

@RunWith(Parameterized::class)
class QueueDaoFilterByDateRangeTest(
    private val _minusDaysFromNow: List<Long>,
    private val _dateRange: QueueDate.Range,
    private val _filteredMinusDaysFromNow: List<Long>
) : QueueDaoBaseTest() {
  /**
   * Simulate the current date as 2023/Dec/31. This is an ideal date because the inclusion range
   * works perfectly, e.g., 31 (Sun) - 6 = 25 (Mon), which remains within the same week. Same
   * applies to month and year.
   *
   * ```
   * Sun Mon Tue Wed Thu Fri Sat
   *                      1   2
   *  3   4   5   6   7   8   9
   * 10  11  12  13  14  15  16
   * 17  18  19  20  21  22  23
   * 24  25  26  27  28  29  30
   * 31
   * ```
   */
  private val _fixedClock: Clock =
      Clock.fixed(Instant.parse("2023-12-31T00:00:00Z"), ZoneId.of("UTC"))

  override fun before() {
    super.before()
    for ((i, minusDays) in _minusDaysFromNow.withIndex()) {
      _queueDao.insert(
          _queue.copy(
              id = (i + 1) * 111L,
              date =
                  LocalDate.now(_fixedClock)
                      .minusDays(minusDays)
                      .atStartOfDay(ZoneId.of("UTC"))
                      .toInstant()))
    }
  }

  @Test
  fun `select all paginated info with filtered date range`() {
    val tableSize: Int = _queueDao.selectAll().size

    val filteredDates: List<Instant> =
        _filteredMinusDaysFromNow.map {
          LocalDate.now(_fixedClock).minusDays(it).atStartOfDay(ZoneId.of("UTC")).toInstant()
        }
    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _dateRange.dateStart(_fixedClock).toInstant(),
                          filteredDateEnd = _dateRange.dateEnd(_fixedClock).toInstant())
                      .map { it.date })
              .describedAs("Select all queues filtered based on their date")
              .containsExactlyInAnyOrderElementsOf(filteredDates)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _dateRange.dateStart(_fixedClock).toInstant(),
                          filteredDateEnd = _dateRange.dateEnd(_fixedClock).toInstant())
                      .map { it.date })
              .describedAs("Select queues filtered based on their date")
              .containsExactlyInAnyOrderElementsOf(filteredDates)
        })
  }

  @Test
  fun `count queues with filtered date range`() {
    assertThat(
            _queueDao.countFilteredQueues(
                shouldCalculateGrandTotalPrice = false,
                filteredCustomerIds = _filters.filteredCustomerIds,
                isNullCustomerShown = _filters.isNullCustomerShown,
                filteredStatus = _filters.filteredStatus,
                filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                filteredDateStart = _dateRange.dateStart(_fixedClock).toInstant(),
                filteredDateEnd = _dateRange.dateEnd(_fixedClock).toInstant()))
        .describedAs("Count total queues filtered based on their date")
        .isEqualTo(_filteredMinusDaysFromNow.size.toLong())
  }

  @Test
  fun `replicate date range filtering query with fake filterer`() {
    assertThat(
            FakeQueueFilterer(
                    _filters.copy(
                        filteredDate =
                            QueueDate(
                                _dateRange,
                                _dateRange.dateStart(_fixedClock),
                                _dateRange.dateEnd(_fixedClock))))
                .filter(_queueDao.selectAll())
                .map { it.date })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(
            _filteredMinusDaysFromNow.map {
              LocalDate.now(_fixedClock).minusDays(it).atStartOfDay(ZoneId.of("UTC")).toInstant()
            })
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
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
  }
}

class QueueDaoFilterByDateCustomTest : QueueDaoBaseTest() {
  private val _dec2023: Instant = Instant.parse("2023-12-01T00:00:00Z")
  private val _jan2024: Instant = Instant.parse("2024-01-01T00:00:00Z")
  private val _feb2024: Instant = Instant.parse("2024-02-01T00:00:00Z")

  override fun before() {
    super.before()
    for ((i, date) in listOf(_dec2023, _jan2024, _feb2024).withIndex()) {
      _queueDao.insert(_queue.copy(id = (i + 1) * 111L, date = date))
    }
  }

  @Test
  fun `select all paginated info with filtered date custom`() {
    val tableSize: Int = _queueDao.selectAll().size

    val filteredDates: List<Instant> = listOf(_dec2023, _jan2024)
    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _dec2023.atZone(ZoneId.of("UTC")).toInstant(),
                          filteredDateEnd = _jan2024.atZone(ZoneId.of("UTC")).toInstant())
                      .map { it.date })
              .describedAs("Select all queues filtered based on their date")
              .containsExactlyInAnyOrderElementsOf(filteredDates)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _dec2023.atZone(ZoneId.of("UTC")).toInstant(),
                          filteredDateEnd = _jan2024.atZone(ZoneId.of("UTC")).toInstant())
                      .map { it.date })
              .describedAs("Select queues filtered based on their date")
              .containsExactlyInAnyOrderElementsOf(filteredDates)
        })
  }

  @Test
  fun `count queues with filtered date custom`() {
    assertThat(
            _queueDao.countFilteredQueues(
                shouldCalculateGrandTotalPrice = false,
                filteredCustomerIds = _filters.filteredCustomerIds,
                isNullCustomerShown = _filters.isNullCustomerShown,
                filteredStatus = _filters.filteredStatus,
                filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                filteredDateStart = _dec2023.atZone(ZoneId.of("UTC")).toInstant(),
                filteredDateEnd = _jan2024.atZone(ZoneId.of("UTC")).toInstant()))
        .describedAs("Count total queues filtered based on their date")
        .isEqualTo(2L)
  }

  @Test
  fun `replicate date custom filtering query with fake filterer`() {
    assertThat(
            FakeQueueFilterer(
                    _filters.copy(
                        filteredDate =
                            QueueDate(
                                _dec2023.atZone(ZoneId.of("UTC")),
                                _jan2024.atZone(ZoneId.of("UTC")))))
                .filter(_queueDao.selectAll())
                .map { it.date })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(listOf(_dec2023, _jan2024))
  }
}

@RunWith(Parameterized::class)
class QueueDaoFilterByGrandTotalPriceTest(
    private val _grandTotalPrices: List<Long>,
    private val _filteredMinGrandTotalPrice: Long?,
    private val _filteredMaxGrandTotalPrice: Long?,
    private val _filteredGrandTotalPrices: List<Long>
) : QueueDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, totalPrice) in _grandTotalPrices.withIndex()) {
      val queueId: Long = (i + 1) * 111L
      _queueDao.insert(_queue.copy(id = queueId))
      _database
          .productOrderDao()
          .insert(
              ProductOrderModel(
                  id = (i + 1) * 111L,
                  queueId = queueId,
                  productPrice = totalPrice,
                  quantity = 1.0,
                  totalPrice = totalPrice.toBigDecimal()))
    }
  }

  @Test
  fun `select all paginated info with filtered grand total price`() {
    val tableSize: Int = _queueDao.selectAll().size

    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = true,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filteredMinGrandTotalPrice?.toBigDecimal(),
                          filteredMaxTotalPrice = _filteredMaxGrandTotalPrice?.toBigDecimal(),
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.grandTotalPrice.toLong() })
              .describedAs("Select all queues filtered based on their grand total price")
              .containsExactlyInAnyOrderElementsOf(_filteredGrandTotalPrices)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = true,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = true,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filteredMinGrandTotalPrice?.toBigDecimal(),
                          filteredMaxTotalPrice = _filteredMaxGrandTotalPrice?.toBigDecimal(),
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.grandTotalPrice.toLong() })
              .describedAs("Select queues filtered based on their grand total price")
              .containsExactlyInAnyOrderElementsOf(_filteredGrandTotalPrices)
        })
  }

  @Test
  fun `count queues with filtered grand total price`() {
    assertThat(
            _queueDao.countFilteredQueues(
                shouldCalculateGrandTotalPrice = true,
                filteredCustomerIds = _filters.filteredCustomerIds,
                isNullCustomerShown = _filters.isNullCustomerShown,
                filteredStatus = _filters.filteredStatus,
                filteredMinTotalPrice = _filteredMinGrandTotalPrice?.toBigDecimal(),
                filteredMaxTotalPrice = _filteredMaxGrandTotalPrice?.toBigDecimal(),
                filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                filteredDateEnd = _filters.filteredDate.dateEnd.toInstant()))
        .describedAs("Count total queues filtered based on their grand total price")
        .isEqualTo(_filteredGrandTotalPrices.size.toLong())
  }

  @Test
  fun `replicate grand total price filtering query with fake filterer`() {
    val productOrdersByQueueId: Map<Long?, List<ProductOrderModel>> =
        _database.productOrderDao().selectAll().groupBy { it.queueId }
    val mappedQueues: List<QueueModel> =
        _queueDao.selectAll().map {
          it.copy(productOrders = productOrdersByQueueId[it.id] ?: it.productOrders)
        }

    assertThat(
            FakeQueueFilterer(
                    _filters.copy(
                        filteredTotalPrice =
                            _filteredMinGrandTotalPrice?.toBigDecimal() to
                                _filteredMaxGrandTotalPrice?.toBigDecimal()))
                .filter(mappedQueues)
                .map { it.grandTotalPrice().toLong() })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(_filteredGrandTotalPrices)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    fun cases(): Array<Array<Any?>> =
        arrayOf(
            arrayOf(listOf(100L, 200L, 300L), null, null, listOf(100L, 200L, 300L)),
            arrayOf(listOf(100L, 200L, 300L), null, 200L, listOf(100L, 200L)),
            arrayOf(listOf(100L, 200L, 300L), 200L, null, listOf(200L, 300L)),
            arrayOf(listOf(100L, 200L, 300L), 200L, 200L, listOf(200L)))
  }
}
