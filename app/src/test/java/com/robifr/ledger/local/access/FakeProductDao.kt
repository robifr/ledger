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

package com.robifr.ledger.local.access

import com.robifr.ledger.data.display.ProductFilterer
import com.robifr.ledger.data.display.ProductSortMethod
import com.robifr.ledger.data.display.ProductSorter
import com.robifr.ledger.data.model.ProductFtsModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductPaginatedInfo

data class FakeProductDao(
    override val data: MutableList<ProductModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size)
) : ProductDao(), FakeQueryAccessible<ProductModel> {
  override fun assignId(model: ProductModel, id: Long): ProductModel = model.copy(id = id)

  override fun insert(product: ProductModel): Long = super<FakeQueryAccessible>.insert(product)

  override fun _insert(product: ProductModel): Long = insert(product)

  override fun update(product: ProductModel): Int = super<FakeQueryAccessible>.update(product)

  override fun _update(product: ProductModel): Int = update(product)

  override fun delete(productId: Long?): Int = super<FakeQueryAccessible>.delete(productId)

  override fun _delete(productId: Long?): Int = delete(productId)

  override fun selectAll(): List<ProductModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(productId: Long?): ProductModel? =
      super<FakeQueryAccessible>.selectById(productId)

  override fun selectById(ids: List<Long>): List<ProductModel> =
      super<FakeQueryAccessible>.selectById(ids)

  override fun selectByRowId(rowId: Long): ProductModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectRowIdById(productId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(productId)

  override fun isExistsById(productId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(productId)

  override fun isTableEmpty(): Boolean = super<FakeQueryAccessible>.isTableEmpty()

  override fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      limit: Int,
      sortBy: ProductSortMethod.SortBy,
      isAscending: Boolean,
      filteredMinPrice: Long?,
      filteredMaxPrice: Long?
  ): List<ProductPaginatedInfo> {
    val filterer: ProductFilterer =
        ProductFilterer().apply {
          filters = filters.copy(filteredPrice = filteredMinPrice to filteredMaxPrice)
        }
    val sorter: ProductSorter =
        ProductSorter().apply {
          sortMethod = sortMethod.copy(sortBy = sortBy, isAscending = isAscending)
        }
    return sorter
        .sort(filterer.filter(data))
        .asSequence()
        .drop((pageNumber - 1) * limit)
        .take(limit)
        .map { ProductPaginatedInfo(it) }
        .toList()
  }

  override fun countFilteredProducts(filteredMinPrice: Long?, filteredMaxPrice: Long?): Long {
    val filterer: ProductFilterer =
        ProductFilterer().apply {
          filters = filters.copy(filteredPrice = filteredMinPrice to filteredMaxPrice)
        }
    return filterer.filter(data).size.toLong()
  }

  override fun search(query: String): List<ProductModel> =
      _search(query.replace("\"".toRegex(), "\"\""))

  override fun _search(query: String): List<ProductModel> =
      data.filter { it.name.contains(query, true) }

  override fun _deleteFts(rowId: Long) {}

  override fun _insertFts(productFts: ProductFtsModel): Long = -1L
}
