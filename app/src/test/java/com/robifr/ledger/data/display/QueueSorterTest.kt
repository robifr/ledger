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
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class QueueSorterTest {
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

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by customer name with primary collator strength`(isAscending: Boolean) {
    val queues: List<QueueModel> =
        listOf("Amy", "Ben", "Cal", null).mapIndexed { i, customerName ->
          _queue.copy(
              id = (i + 1) * 111L,
              customer = customerName?.let { _customer.copy(id = (i + 1) * 111L, name = it) })
        }
    val sorter: QueueSorter =
        QueueSorter().apply {
          sortMethod = QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, isAscending)
        }
    assertEquals(
        if (isAscending) listOf("Amy", "Ben", "Cal", null) else listOf(null, "Cal", "Ben", "Amy"),
        sorter.sort(queues).map { it.customer?.name },
        "Sort queues based on their customer name")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by date`(isAscending: Boolean) {
    val dec2023: Instant = Instant.parse("2023-12-01T00:00:00Z")
    val jan2024: Instant = Instant.parse("2024-01-01T00:00:00Z")
    val feb2024: Instant = Instant.parse("2024-02-01T00:00:00Z")
    val queues: List<QueueModel> =
        listOf(dec2023, jan2024, feb2024).mapIndexed { i, date ->
          _queue.copy(id = (i + 1) * 111L, date = date)
        }
    val sorter: QueueSorter =
        QueueSorter().apply {
          sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, isAscending)
        }
    assertEquals(
        if (isAscending) listOf(dec2023, jan2024, feb2024) else listOf(feb2024, jan2024, dec2023),
        sorter.sort(queues).map { it.date },
        "Sort queues based on their date")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by grand total price`(isAscending: Boolean) {
    val queues: List<QueueModel> =
        listOf(100L, 200L, 300L).mapIndexed { i, totalPrice ->
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
    val sorter: QueueSorter =
        QueueSorter().apply {
          sortMethod = QueueSortMethod(QueueSortMethod.SortBy.TOTAL_PRICE, isAscending)
        }
    assertEquals(
        if (isAscending) listOf(100L, 200L, 300L) else listOf(300L, 200L, 100L),
        sorter.sort(queues).map { it.grandTotalPrice().toLong() },
        "Sort queues based on their grand total price")
  }
}
