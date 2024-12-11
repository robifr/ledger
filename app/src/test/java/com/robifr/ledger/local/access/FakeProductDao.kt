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

import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.local.FtsStringConverter

data class FakeProductDao(
    override val data: MutableList<ProductModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size)
) : ProductDao(), FakeQueryAccessible<ProductModel> {
  override fun assignId(model: ProductModel, id: Long): ProductModel = model.copy(id = id)

  override fun insert(product: ProductModel): Long = super<FakeQueryAccessible>.insert(product)

  override fun update(product: ProductModel): Int = super<FakeQueryAccessible>.update(product)

  override fun delete(product: ProductModel): Int = super<FakeQueryAccessible>.delete(product)

  override fun selectAll(): List<ProductModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(productId: Long?): ProductModel? =
      super<FakeQueryAccessible>.selectById(productId)

  override fun selectByRowId(rowId: Long): ProductModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectRowIdById(productId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(productId)

  override fun isExistsById(productId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(productId)

  override fun _insert(product: ProductModel): Long = insert(product)

  override fun _update(product: ProductModel): Int = update(product)

  override fun _delete(product: ProductModel): Int = delete(product)

  override fun _search(query: String): List<ProductModel> =
      data.filter { query in "*\"${FtsStringConverter.toFtsSpacedString(it.name)}\"*" }

  override fun _deleteFts(rowId: Long) {}

  override fun _insertFts(rowId: Long, productName: String): Long = -1L
}
