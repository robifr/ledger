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

import io.github.robifr.ledger.data.display.FakeProductFilterer
import io.github.robifr.ledger.data.display.ProductSortMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProductDaoFilterByPriceTest(
    private val _prices: List<Long>,
    private val _filteredMinPrice: Long?,
    private val _filteredMaxPrice: Long?,
    private val _filteredPrices: List<Long>
) : ProductDaoBaseTest() {
  override fun before() {
    super.before()
    for ((i, price) in _prices.withIndex()) {
      _productDao.insert(_product.copy(id = (i + 1) * 111L, price = price))
    }
  }

  @Test
  fun `select all paginated info with filtered price`() {
    val tableSize: Int = _productDao.selectAll().size

    assertThat(
            _productDao
                .selectPaginatedInfoByOffset(
                    pageNumber = 1,
                    itemPerPage = tableSize,
                    limit = tableSize,
                    sortBy = ProductSortMethod.SortBy.NAME,
                    isAscending = true,
                    filteredMinPrice = _filteredMinPrice,
                    filteredMaxPrice = _filteredMaxPrice)
                .map { it.price })
        .describedAs("Select all products filtered based on their price")
        .containsExactlyInAnyOrderElementsOf(_filteredPrices)
  }

  @Test
  fun `count products with filtered price`() {
    assertThat(
            _productDao.countFilteredProducts(
                filteredMinPrice = _filteredMinPrice, filteredMaxPrice = _filteredMaxPrice))
        .describedAs("Count total products filtered based on their price")
        .isEqualTo(_filteredPrices.size.toLong())
  }

  @Test
  fun `replicate price filtering query with fake filterer`() {
    assertThat(
            FakeProductFilterer(
                    _filters.copy(filteredPrice = _filteredMinPrice to _filteredMaxPrice))
                .filter(_productDao.selectAll())
                .map { it.price })
        .describedAs("Replicate fake filterer behavior with the actual filtering query")
        .containsExactlyInAnyOrderElementsOf(_filteredPrices)
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
