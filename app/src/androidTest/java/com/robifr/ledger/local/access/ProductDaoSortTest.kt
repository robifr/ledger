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

import com.robifr.ledger.data.display.FakeProductSorter
import com.robifr.ledger.data.display.ProductSortMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProductDaoSortByNameTest(
    private val _names: List<String>,
    private val _isAscending: Boolean,
    private val _sortedNames: List<String>
) : ProductDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, name) in _names.withIndex()) {
      _productDao.insert(_product.copy(id = (i + 1) * 111L, name = name))
    }
  }

  @Test
  fun `select all paginated info with sort by name`() {
    val tableSize: Int = _productDao.selectAll().size

    assertThat(
            _productDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = ProductSortMethod.SortBy.NAME,
                    isAscending = _isAscending,
                    filteredMinPrice = _filters.filteredPrice.first,
                    filteredMaxPrice = _filters.filteredPrice.second)
                .map { it.name })
        .describedAs("Select products sorted based on their name")
        .isEqualTo(_sortedNames)
  }

  @Test
  fun `replicate name sorting query with fake sorter`() {
    assertThat(
            FakeProductSorter(ProductSortMethod(ProductSortMethod.SortBy.NAME, _isAscending))
                .sort(_productDao.selectAll())
                .map { it.name })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedNames)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{index}: {0}, {1}, {2}")
    fun cases(): Array<Array<Any>> =
        arrayOf(
            // spotless:off
            arrayOf(listOf("Cherry", "Apple", "Banana"), true, listOf("Apple", "Banana", "Cherry")),
            arrayOf(listOf("Cherry", "Apple", "Banana"), false, listOf("Cherry", "Banana", "Apple")))
            // spotless:on
  }
}

@RunWith(Parameterized::class)
class ProductDaoSortByBalanceTest(
    private val _prices: List<Long>,
    private val _isAscending: Boolean,
    private val _sortedPrices: List<Long>
) : ProductDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, price) in _prices.withIndex()) {
      _database.productDao().insert(_product.copy(id = (i + 1) * 111L, price = price))
    }
  }

  @Test
  fun `select all paginated info with sort by price`() {
    val tableSize: Int = _productDao.selectAll().size

    assertThat(
            _productDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = ProductSortMethod.SortBy.PRICE,
                    isAscending = _isAscending,
                    filteredMinPrice = _filters.filteredPrice.first,
                    filteredMaxPrice = _filters.filteredPrice.second)
                .map { it.price })
        .describedAs("Select products sorted based on their price")
        .isEqualTo(_sortedPrices)
  }

  @Test
  fun `replicate price sorting query with fake sorter`() {
    assertThat(
            FakeProductSorter(ProductSortMethod(ProductSortMethod.SortBy.PRICE, _isAscending))
                .sort(_productDao.selectAll())
                .map { it.price })
        .describedAs("Replicate fake sorter behavior with the actual sorting query")
        .isEqualTo(_sortedPrices)
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
