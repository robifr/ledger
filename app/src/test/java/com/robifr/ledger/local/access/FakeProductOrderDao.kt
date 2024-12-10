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

import com.robifr.ledger.data.model.ProductOrderModel

data class FakeProductOrderDao(
    override val data: MutableList<ProductOrderModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size)
) : ProductOrderDao(), FakeQueryAccessible<ProductOrderModel> {
  override fun assignId(model: ProductOrderModel, id: Long): ProductOrderModel = model.copy(id = id)

  override fun insert(productOrder: ProductOrderModel): Long =
      super<FakeQueryAccessible>.insert(productOrder)

  override fun insert(productOrders: List<ProductOrderModel>): List<Long> =
      productOrders.asSequence().map { insert(it) }.filter { it != -1L }.toList()

  override fun update(productOrder: ProductOrderModel): Int =
      super<FakeQueryAccessible>.update(productOrder)

  override fun update(productOrders: List<ProductOrderModel>): Int =
      productOrders.sumOf { update(it) }

  override fun delete(productOrder: ProductOrderModel): Int =
      super<FakeQueryAccessible>.delete(productOrder)

  override fun delete(productOrders: List<ProductOrderModel>): Int =
      productOrders.reversed().sumOf { delete(it) }

  override fun selectAll(): List<ProductOrderModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(productOrderId: Long?): ProductOrderModel? =
      super<FakeQueryAccessible>.selectById(productOrderId)

  override fun selectByRowId(rowId: Long): ProductOrderModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectRowIdById(productOrderId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(productOrderId)

  override fun isExistsById(productOrderId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(productOrderId)

  override fun upsert(productOrder: ProductOrderModel): Long =
      if (isExistsById(productOrder.id)) {
        update(productOrder)
        selectRowIdById(productOrder.id)
      } else {
        insert(productOrder)
      }

  override fun upsert(productOrders: List<ProductOrderModel>): List<Long> =
      productOrders.asSequence().map { upsert(it) }.filter { it != -1L }.toList()

  override fun selectAllByQueueId(queueId: Long?): List<ProductOrderModel> =
      data.filter { it.queueId != null && queueId != null && it.queueId == queueId }
}
