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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ProductSorterTest {
  private val _product: ProductModel = ProductModel(id = null, name = "")

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by name with primary collator strength`(isAscending: Boolean) {
    val products: List<ProductModel> =
        listOf("Apple", "Banana", "Cherry").mapIndexed { i, name ->
          _product.copy(id = (i + 1) * 111L, name = name)
        }
    val sorter: ProductSorter =
        ProductSorter().apply {
          sortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, isAscending)
        }
    assertEquals(
        if (isAscending) listOf("Apple", "Banana", "Cherry")
        else listOf("Cherry", "Banana", "Apple"),
        sorter.sort(products).map { it.name },
        "Sort products based on their name")
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `sort by price`(isAscending: Boolean) {
    val products: List<ProductModel> =
        listOf(100L, 200L, 300L).mapIndexed { i, price ->
          _product.copy(id = (i + 1) * 111L, price = price)
        }
    val sorter: ProductSorter =
        ProductSorter().apply {
          sortMethod = ProductSortMethod(ProductSortMethod.SortBy.PRICE, isAscending)
        }
    assertEquals(
        if (isAscending) listOf(100L, 200L, 300L) else listOf(300L, 200L, 100L),
        sorter.sort(products).map { it.price },
        "Sort products based on their balance")
  }
}
