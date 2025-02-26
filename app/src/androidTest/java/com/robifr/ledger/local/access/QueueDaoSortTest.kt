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

package com.robifr.ledger.local.access

import com.robifr.ledger.assertAllSoftly
import com.robifr.ledger.data.display.FakeQueueSorter
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class QueueDaoSortByCustomerNameTest(
    private val _customerNames: List<String?>,
    private val _isAscending: Boolean,
    private val _sortedCustomerNames: List<String>
) : QueueDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, customerName) in _customerNames.withIndex()) {
      val customer: CustomerModel? =
          customerName?.let { CustomerModel(id = (i + 1) * 111L, name = it) }
      customer?.let { _database.customerDao().insert(customer) }
      _queueDao.insert(
          _queue.copy(id = (i + 1) * 111L, customerId = customer?.id, customer = customer))
    }
  }

  @Test
  fun `select all paginated info with sort by customer name`() {
    val tableSize: Int = _queueDao.selectAll().size

    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.CUSTOMER_NAME,
                          isAscending = _isAscending,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.customerName })
              .describedAs("Select all queues sorted based on their customer name")
              .isEqualTo(_sortedCustomerNames)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.CUSTOMER_NAME,
                          isAscending = _isAscending,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.customerName })
              .describedAs("Select queues sorted based on their customer name")
              .isEqualTo(_sortedCustomerNames)
        })
  }

  @Test
  fun `replicate customer name sorting query with fake sorter`() {
    val customers: List<CustomerModel> = _database.customerDao().selectAll()
    val mappedQueues: List<QueueModel> =
        _queueDao.selectAll().map { queue ->
          queue.copy(
              customer = customers.find { queue.customerId != null && queue.customerId == it.id })
        }

    assertThat(
            FakeQueueSorter(QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, _isAscending))
                .sort(mappedQueues)
                .map { it.customer?.name })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedCustomerNames)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            arrayOf(listOf("Cal", null, "Amy", "Ben"), true, listOf("Amy", "Ben", "Cal", null)),
            arrayOf(listOf("Cal", null, "Amy", "Ben"), false, listOf(null, "Cal", "Ben", "Amy")))
  }
}

@RunWith(Parameterized::class)
class QueueDaoSortByDateTest(
    private val _dates: List<Instant>,
    private val _isAscending: Boolean,
    private val _sortedDates: List<Instant>
) : QueueDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, date) in _dates.withIndex()) {
      _queueDao.insert(_queue.copy(id = (i + 1) * 111L, date = date))
    }
  }

  @Test
  fun `select all paginated info with sort by date`() {
    val tableSize: Int = _queueDao.selectAll().size

    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = false,
                          sortBy = QueueSortMethod.SortBy.DATE,
                          isAscending = _isAscending,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.date })
              .describedAs("Select all queues sorted based on their date")
              .isEqualTo(_sortedDates)
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
                          isAscending = _isAscending,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.date })
              .describedAs("Select queues sorted based on their date")
              .isEqualTo(_sortedDates)
        })
  }

  @Test
  fun `replicate date sorting query with fake sorter`() {
    assertThat(
            FakeQueueSorter(QueueSortMethod(QueueSortMethod.SortBy.DATE, _isAscending))
                .sort(_queueDao.selectAll())
                .map { it.date })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedDates)
  }

  companion object {
    private val _DEC_2023: Instant = Instant.parse("2023-12-01T00:00:00Z")
    private val _JAN_2024: Instant = Instant.parse("2024-01-01T00:00:00Z")
    private val _FEB_2024: Instant = Instant.parse("2024-02-01T00:00:00Z")

    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            // spotless:off
            arrayOf(listOf(_JAN_2024, _DEC_2023, _FEB_2024), true, listOf(_DEC_2023, _JAN_2024, _FEB_2024)),
            arrayOf(listOf(_JAN_2024, _DEC_2023, _FEB_2024), false, listOf(_FEB_2024, _JAN_2024, _DEC_2023)))
            // spotless:on
  }
}

@RunWith(Parameterized::class)
class QueueDaoSortByGrandTotalPriceTest(
    private val _grandTotalPrices: List<Long>,
    private val _isAscending: Boolean,
    private val _sortedGrandTotalPrices: List<Long>
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
  fun `select all paginated info with sort by grand total price`() {
    val tableSize: Int = _queueDao.selectAll().size

    assertAllSoftly(
        {
          assertThat(
                  _queueDao
                      .selectAllPaginatedInfo(
                          shouldCalculateGrandTotalPrice = true,
                          sortBy = QueueSortMethod.SortBy.TOTAL_PRICE,
                          isAscending = _isAscending,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.grandTotalPrice.toLong() })
              .describedAs("Select all queues sorted based on their grand total price")
              .isEqualTo(_sortedGrandTotalPrices)
        },
        {
          assertThat(
                  _queueDao
                      .selectPaginatedInfoByOffset(
                          pageNumber = 1,
                          itemPerPage = tableSize,
                          limit = tableSize,
                          shouldCalculateGrandTotalPrice = true,
                          sortBy = QueueSortMethod.SortBy.TOTAL_PRICE,
                          isAscending = _isAscending,
                          filteredCustomerIds = _filters.filteredCustomerIds,
                          isNullCustomerShown = _filters.isNullCustomerShown,
                          filteredStatus = _filters.filteredStatus,
                          filteredMinTotalPrice = _filters.filteredTotalPrice.first,
                          filteredMaxTotalPrice = _filters.filteredTotalPrice.second,
                          filteredDateStart = _filters.filteredDate.dateStart.toInstant(),
                          filteredDateEnd = _filters.filteredDate.dateEnd.toInstant())
                      .map { it.grandTotalPrice.toLong() })
              .describedAs("Select queues sorted based on their grand total price")
              .isEqualTo(_sortedGrandTotalPrices)
        })
  }

  @Test
  fun `replicate grand total price sorting query with fake sorter`() {
    val productOrdersByQueueId: Map<Long?, List<ProductOrderModel>> =
        _database.productOrderDao().selectAll().groupBy { it.queueId }
    val mappedQueues: List<QueueModel> =
        _queueDao.selectAll().map {
          it.copy(productOrders = productOrdersByQueueId[it.id] ?: it.productOrders)
        }

    assertThat(
            FakeQueueSorter(QueueSortMethod(QueueSortMethod.SortBy.TOTAL_PRICE, _isAscending))
                .sort(mappedQueues)
                .map { it.grandTotalPrice().toLong() })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedGrandTotalPrices)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            arrayOf(listOf(200L, 100L, 300L), true, listOf(100L, 200L, 300L)),
            arrayOf(listOf(200L, 100L, 300L), false, listOf(300L, 200L, 100L)))
  }
}
