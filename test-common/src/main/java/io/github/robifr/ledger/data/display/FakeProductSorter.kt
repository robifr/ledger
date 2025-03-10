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

package io.github.robifr.ledger.data.display

import io.github.robifr.ledger.data.model.ProductModel
import java.text.Collator
import java.util.Locale

class FakeProductSorter(
    var sortMethod: ProductSortMethod = ProductSortMethod(ProductSortMethod.SortBy.NAME, true)
) {
  fun sort(products: List<ProductModel>): List<ProductModel> =
      when (sortMethod.sortBy) {
        ProductSortMethod.SortBy.NAME -> _sortByName(products)
        ProductSortMethod.SortBy.PRICE -> _sortByPrice(products)
      }

  private fun _sortByName(products: List<ProductModel>): List<ProductModel> {
    val collator: Collator = Collator.getInstance(Locale.US).apply { strength = Collator.SECONDARY }
    val comparator: Comparator<ProductModel> = Comparator.comparing(ProductModel::name, collator)
    return products.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }

  private fun _sortByPrice(products: List<ProductModel>): List<ProductModel> {
    val comparator: Comparator<ProductModel> = Comparator.comparing(ProductModel::price)
    return products.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }
}
