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

import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.display.FakeCustomerSorter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CustomerDaoSortByNameTest(
    private val _names: List<String>,
    private val _isAscending: Boolean,
    private val _sortedNames: List<String>
) : CustomerDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, name) in _names.withIndex()) {
      _customerDao.insert(_customer.copy(id = (i + 1) * 111L, name = name))
    }
  }

  @Test
  fun `select all paginated info with sort by name`() {
    val tableSize: Int = _customerDao.selectAll().size

    assertThat(
            _customerDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = CustomerSortMethod.SortBy.NAME,
                    isAscending = _isAscending,
                    filteredMinBalance = _filters.filteredBalance.first,
                    filteredMaxBalance = _filters.filteredBalance.second,
                    filteredMinDebt = _filters.filteredDebt.first,
                    filteredMaxDebt = _filters.filteredDebt.second)
                .map { it.name })
        .describedAs("Select customers sorted based on their name")
        .isEqualTo(_sortedNames)
  }

  @Test
  fun `replicate name sorting query with fake sorter`() {
    assertThat(
            FakeCustomerSorter(CustomerSortMethod(CustomerSortMethod.SortBy.NAME, _isAscending))
                .sort(_customerDao.selectAll())
                .map { it.name })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedNames)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            arrayOf(listOf("Cal", "Amy", "Ben"), true, listOf("Amy", "Ben", "Cal")),
            arrayOf(listOf("Cal", "Amy", "Ben"), false, listOf("Cal", "Ben", "Amy")))
  }
}

@RunWith(Parameterized::class)
class CustomerDaoSortByBalanceTest(
    private val _balances: List<Long>,
    private val _isAscending: Boolean,
    private val _sortedBalances: List<Long>
) : CustomerDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, balance) in _balances.withIndex()) {
      _customerDao.insert(_customer.copy(id = (i + 1) * 111L, balance = balance))
    }
  }

  @Test
  fun `select all paginated info with sort by balance`() {
    val tableSize: Int = _customerDao.selectAll().size

    assertThat(
            _customerDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = CustomerSortMethod.SortBy.BALANCE,
                    isAscending = _isAscending,
                    filteredMinBalance = _filters.filteredBalance.first,
                    filteredMaxBalance = _filters.filteredBalance.second,
                    filteredMinDebt = _filters.filteredDebt.first,
                    filteredMaxDebt = _filters.filteredDebt.second)
                .map { it.balance })
        .describedAs("Select customers sorted based on their balance")
        .isEqualTo(_sortedBalances)
  }

  @Test
  fun `replicate balance sorting query with fake sorter`() {
    assertThat(
            FakeCustomerSorter(CustomerSortMethod(CustomerSortMethod.SortBy.BALANCE, _isAscending))
                .sort(_customerDao.selectAll())
                .map { it.balance })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedBalances)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            arrayOf(listOf(100L, 0L, 200L), true, listOf(0L, 100L, 200L)),
            arrayOf(listOf(100L, 0L, 200L), false, listOf(200L, 100L, 0L)))
  }
}
