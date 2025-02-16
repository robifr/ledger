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

package com.robifr.ledger.repository

import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.ProductOrderProductInfo
import com.robifr.ledger.local.access.ProductOrderDao
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductOrderRepository(private val _localDao: ProductOrderDao) :
    Queryable<ProductOrderModel> {
  private val _modelChangedListeners: HashSet<ModelChangedListener<ProductOrderModel>> = hashSetOf()

  fun addModelChangedListener(listener: ModelChangedListener<ProductOrderModel>) {
    _modelChangedListeners.add(listener)
  }

  fun removeModelChangedListener(listener: ModelChangedListener<ProductOrderModel>) {
    _modelChangedListeners.remove(listener)
  }

  override suspend fun selectAll(): List<ProductOrderModel> = _localDao.selectAll()

  override suspend fun selectById(id: Long?): ProductOrderModel? = _localDao.selectById(id)

  override suspend fun selectById(ids: List<Long>): List<ProductOrderModel> =
      _localDao.selectById(ids)

  override suspend fun isExistsById(id: Long?): Boolean = _localDao.isExistsById(id)

  override suspend fun isTableEmpty(): Boolean = _localDao.isTableEmpty()

  override suspend fun add(model: ProductOrderModel): Long =
      _localDao
          .insert(model)
          .let { rowId -> _localDao.selectIdByRowId(rowId) }
          .also { insertedId -> selectById(insertedId)?.let { _notifyModelAdded(listOf(it)) } }

  /** @return Inserted model IDs. Empty list for a failed operation. */
  suspend fun add(models: List<ProductOrderModel>): List<Long> =
      _localDao
          .insert(models)
          .let { rowIds -> _localDao.selectIdByRowId(rowIds) }
          .also { insertedIds ->
            selectById(insertedIds).let { if (it.isNotEmpty()) _notifyModelAdded(it) }
          }

  override suspend fun update(model: ProductOrderModel): Int =
      _localDao.update(model).also { effectedRows ->
        if (effectedRows > 0) selectById(model.id)?.let { _notifyModelUpdated(listOf(it)) }
      }

  /** @return Number of row effected. 0 for a failed operation. */
  suspend fun update(models: List<ProductOrderModel>): Int =
      _localDao.update(models).also { effectedRows ->
        if (effectedRows == 0) return@also
        selectById(models.mapNotNull { it.id }).let { if (it.isNotEmpty()) _notifyModelUpdated(it) }
      }

  override suspend fun delete(id: Long?): Int {
    val deletedOrder: ProductOrderModel = selectById(id) ?: return 0
    return _localDao.delete(id).also { effectedRows ->
      if (effectedRows > 0) _notifyModelDeleted(listOf(deletedOrder))
    }
  }

  /** @return Number of row effected. 0 for a failed operation. */
  suspend fun delete(models: List<ProductOrderModel>): Int {
    val deletedOrders: List<ProductOrderModel> = selectById(models.mapNotNull { it.id })
    return _localDao.delete(models).also { effectedRows ->
      if (deletedOrders.isNotEmpty() && effectedRows > 0) _notifyModelDeleted(deletedOrders)
    }
  }

  /** @return Upserted product order ID. */
  suspend fun upsert(model: ProductOrderModel): Long =
      _localDao
          .upsert(model)
          .let { rowId -> model.id.takeIf { rowId == -1L } ?: _localDao.selectIdByRowId(rowId) }
          .also { upsertedId -> selectById(upsertedId)?.let { _notifyModelUpserted(listOf(it)) } }

  /** @return Upserted product order IDs. */
  suspend fun upsert(models: List<ProductOrderModel>): List<Long> =
      _localDao
          .upsert(models)
          .let { rowIds ->
            val insertedIds: List<Long> = _localDao.selectIdByRowId(rowIds.filter { it != -1L })
            models.mapNotNull { it.id }.plus(insertedIds)
          }
          .also { upsertedIds ->
            selectById(upsertedIds).let { if (it.isNotEmpty()) _notifyModelUpserted(it) }
          }

  suspend fun selectAllByQueueId(queueId: Long?): List<ProductOrderModel> =
      _localDao.selectAllByQueueId(queueId)

  suspend fun selectAllProductInfoInRange(
      dateStart: ZonedDateTime,
      dateEnd: ZonedDateTime
  ): List<ProductOrderProductInfo> =
      _localDao.selectAllProductInfoInRange(dateStart.toInstant(), dateEnd.toInstant())

  private suspend fun _notifyModelAdded(models: List<ProductOrderModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelAdded(models) } }
  }

  private suspend fun _notifyModelUpdated(models: List<ProductOrderModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelUpdated(models) } }
  }

  private suspend fun _notifyModelDeleted(models: List<ProductOrderModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelDeleted(models) } }
  }

  private suspend fun _notifyModelUpserted(models: List<ProductOrderModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelUpserted(models) } }
  }

  companion object {
    @Volatile private var _instance: ProductOrderRepository? = null

    @Synchronized
    fun instance(productOrderDao: ProductOrderDao): ProductOrderRepository =
        _instance ?: ProductOrderRepository(productOrderDao).apply { _instance = this }
  }
}
