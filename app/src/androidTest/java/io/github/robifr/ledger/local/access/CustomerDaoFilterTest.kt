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

import io.github.robifr.ledger.data.display.CustomerSortMethod
import io.github.robifr.ledger.data.display.FakeCustomerFilterer
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import java.time.Instant
import kotlin.math.absoluteValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CustomerDaoFilterByBalanceTest(
    private val _balances: List<Long>,
    private val _filteredMinBalance: Long?,
    private val _filteredMaxBalance: Long?,
    private val _filteredBalances: List<Long>
) : CustomerDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, balance) in _balances.withIndex()) {
      _customerDao.insert(_customer.copy(id = (i + 1) * 111L, balance = balance))
    }
  }

  @Test
  fun `select all paginated info with filtered balance`() {
    val tableSize: Int = _customerDao.selectAll().size

    assertThat(
            _customerDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = CustomerSortMethod.SortBy.NAME,
                    isAscending = true,
                    filteredMinBalance = _filteredMinBalance,
                    filteredMaxBalance = _filteredMaxBalance,
                    filteredMinDebt = _filters.filteredDebt.first,
                    filteredMaxDebt = _filters.filteredDebt.second)
                .map { it.balance })
        .describedAs("Select all customers filtered based on their balance")
        .containsExactlyInAnyOrderElementsOf(_filteredBalances)
  }

  @Test
  fun `count customers with filtered balance`() {
    assertThat(
            _customerDao.countFilteredCustomers(
                filteredMinBalance = _filteredMinBalance,
                filteredMaxBalance = _filteredMaxBalance,
                filteredMinDebt = _filters.filteredDebt.first,
                filteredMaxDebt = _filters.filteredDebt.second))
        .describedAs("Count total customers filtered based on their balance")
        .isEqualTo(_filteredBalances.size.toLong())
  }

  @Test
  fun `replicate balance filtering query with fake filterer`() {
    assertThat(
            FakeCustomerFilterer(
                    _filters.copy(filteredBalance = _filteredMinBalance to _filteredMaxBalance))
                .filter(_customerDao.selectAll())
                .map { it.balance })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(_filteredBalances)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    fun cases(): Array<Array<Any?>> =
        arrayOf(
            arrayOf(listOf(0L, 100L, 200L), null, null, listOf(0L, 100L, 200L)),
            arrayOf(listOf(0L, 100L, 200L), null, 100L, listOf(0L, 100L)),
            arrayOf(listOf(0L, 100L, 200L), 100L, null, listOf(100L, 200L)),
            arrayOf(listOf(0L, 100L, 200L), 100L, 100L, listOf(100L)))
  }
}

@RunWith(Parameterized::class)
class CustomerDaoFilterByDebtTest(
    private val _debts: List<Int>,
    private val _filteredMinDebt: Int?,
    private val _filteredMaxDebt: Int?,
    private val _filteredDebts: List<Int>
) : CustomerDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, debt) in _debts.withIndex()) {
      val customerId: Long = (i + 1) * 111L
      _customerDao.insert(_customer.copy(id = customerId))
      val queueId: Long = (i + 1) * 111L
      _database
          .queueDao()
          .insert(
              QueueModel(
                  id = queueId,
                  customerId = customerId,
                  status = QueueModel.Status.UNPAID,
                  date = Instant.now(),
                  paymentMethod = QueueModel.PaymentMethod.CASH))
      _database
          .productOrderDao()
          .insert(
              ProductOrderModel(
                  id = (i + 1) * 111L,
                  queueId = queueId,
                  productPrice = debt.absoluteValue.toLong(),
                  quantity = 1.0,
                  totalPrice = debt.absoluteValue.toBigDecimal()))
    }
  }

  @Test
  fun `select all paginated info with filtered debt`() {
    val tableSize: Int = _customerDao.selectAll().size

    assertThat(
            _customerDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = CustomerSortMethod.SortBy.NAME,
                    isAscending = true,
                    filteredMinBalance = _filters.filteredBalance.first,
                    filteredMaxBalance = _filters.filteredBalance.second,
                    filteredMinDebt = _filteredMinDebt?.toBigDecimal(),
                    filteredMaxDebt = _filteredMaxDebt?.toBigDecimal())
                .map { it.debt.toInt() })
        .describedAs("Select all customers filtered based on their debt")
        .containsExactlyInAnyOrderElementsOf(_filteredDebts)
  }

  @Test
  fun `count customers with filtered debt`() {
    assertThat(
            _customerDao.countFilteredCustomers(
                filteredMinBalance = _filters.filteredBalance.first,
                filteredMaxBalance = _filters.filteredBalance.second,
                filteredMinDebt = _filteredMinDebt?.toBigDecimal(),
                filteredMaxDebt = _filteredMaxDebt?.toBigDecimal()))
        .describedAs("Count total customers filtered based on their debt")
        .isEqualTo(_filteredDebts.size.toLong())
  }

  @Test
  fun `replicate debt filtering query with fake filterer`() {
    val productOrdersByQueueId: Map<Long?, List<ProductOrderModel>> =
        _database.productOrderDao().selectAll().groupBy { it.queueId }
    val queuesByCustomerId: Map<Long?, List<QueueModel>> =
        _database
            .queueDao()
            .selectAll()
            .map { it.copy(productOrders = productOrdersByQueueId[it.id] ?: it.productOrders) }
            .groupBy { it.customerId }
    val mappedCustomers: List<CustomerModel> =
        _customerDao.selectAll().map { customer ->
          customer.copy(
              debt =
                  queuesByCustomerId[customer.id]?.sumOf { it.grandTotalPrice() }?.negate()
                      ?: customer.debt)
        }

    assertThat(
            FakeCustomerFilterer(
                    _filters.copy(
                        filteredDebt =
                            _filteredMinDebt?.toBigDecimal() to _filteredMaxDebt?.toBigDecimal()))
                .filter(mappedCustomers)
                .map { it.debt.toInt() })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(_filteredDebts)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}, {3}")
    fun cases(): Array<Array<Any?>> =
        arrayOf(
            arrayOf(listOf(0, -100, -200), null, null, listOf(0, -100, -200)),
            arrayOf(listOf(0, -100, -200), null, 100, listOf(0, -100)),
            arrayOf(listOf(0, -100, -200), null, -100, listOf(0, -100)),
            arrayOf(listOf(0, -100, -200), 100, null, listOf(-100, -200)),
            arrayOf(listOf(0, -100, -200), -100, null, listOf(-100, -200)),
            arrayOf(listOf(0, -100, -200), 100, 100, listOf(-100)),
            arrayOf(listOf(0, -100, -200), -100, -100, listOf(-100)))
  }
}
