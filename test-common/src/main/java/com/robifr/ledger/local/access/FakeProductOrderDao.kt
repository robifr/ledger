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
import com.robifr.ledger.data.model.ProductOrderProductInfo
import com.robifr.ledger.data.model.QueueModel
import java.time.Instant

data class FakeProductOrderDao(
    override val data: MutableList<ProductOrderModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size),
    val queueData: MutableList<QueueModel>
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

  override fun delete(productOrderId: Long?): Int =
      super<FakeQueryAccessible>.delete(productOrderId)

  override fun delete(productOrders: List<ProductOrderModel>): Int =
      productOrders.reversed().sumOf { delete(it.id) }

  override fun selectAll(): List<ProductOrderModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(productOrderId: Long?): ProductOrderModel? =
      super<FakeQueryAccessible>.selectById(productOrderId)

  override fun selectById(ids: List<Long>): List<ProductOrderModel> =
      super<FakeQueryAccessible>.selectById(ids)

  override fun selectByRowId(rowId: Long): ProductOrderModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectByRowId(rowIds: List<Long>): List<ProductOrderModel> =
      rowIds.asSequence().mapNotNull { selectByRowId(it) }.toList()

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectIdByRowId(rowIds: List<Long>): List<Long> =
      rowIds.asSequence().map { selectIdByRowId(it) }.filter { it != 0L }.toList()

  override fun selectRowIdById(productOrderId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(productOrderId)

  override fun isExistsById(productOrderId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(productOrderId)

  override fun isTableEmpty(): Boolean = super<FakeQueryAccessible>.isTableEmpty()

  override fun upsert(productOrder: ProductOrderModel): Long =
      super<FakeQueryAccessible>.upsert(productOrder)

  override fun upsert(productOrders: List<ProductOrderModel>): List<Long> =
      productOrders.map { upsert(it) }

  override fun selectAllByQueueId(queueId: Long?): List<ProductOrderModel> =
      data.filter { it.queueId != null && queueId != null && it.queueId == queueId }

  override fun selectAllProductInfoInRange(
      dateStart: Instant,
      dateEnd: Instant
  ): List<ProductOrderProductInfo> {
    val queueIds: List<Long> =
        queueData
            .filterNot { it.date.isBefore(dateStart) || it.date.isAfter(dateEnd) }
            .mapNotNull { it.id }
    return data.filter { queueIds.contains(it.queueId) }.map { ProductOrderProductInfo(it) }
  }
}
