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

import com.robifr.ledger.data.model.ProductModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductFiltererTest {
  private val _product: ProductModel = ProductModel(id = null, name = "")

  private fun `_filter by price cases`(): Array<Array<Any?>> =
      arrayOf(
          arrayOf(listOf(100L, 200L, 300L), null, null, listOf(100L, 200L, 300L)),
          arrayOf(listOf(100L, 200L, 300L), null, 200L, listOf(100L, 200L)),
          arrayOf(listOf(100L, 200L, 300L), 200L, null, listOf(200L, 300L)),
          arrayOf(listOf(100L, 200L, 300L), 200L, 200L, listOf(200L)))

  @ParameterizedTest
  @MethodSource("_filter by price cases")
  fun `filter by price`(
      prices: List<Long>,
      filteredMinPrice: Long?,
      filteredMaxPrice: Long?,
      filteredPrices: List<Long>
  ) {
    val products: List<ProductModel> =
        prices.mapIndexed { i, price -> _product.copy(id = (i + 1) * 111L, price = price) }
    val filterer: FakeProductFilterer =
        FakeProductFilterer().apply {
          filters = filters.copy(filteredPrice = filteredMinPrice to filteredMaxPrice)
        }
    assertEquals(
        filteredPrices,
        filterer.filter(products).map { it.price },
        "Filter products based on their price")
  }
}
