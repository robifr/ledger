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
import com.robifr.ledger.local.access.ProductOrderDao
import kotlin.collections.isNotEmpty
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductOrderRepository(
    private val _dispatcher: CoroutineDispatcher,
    private val _localDao: ProductOrderDao
) : QueryReadable<ProductOrderModel>, QueryModifiable<ProductOrderModel> {
  private val _modelChangedListeners: HashSet<ModelChangedListener<ProductOrderModel>> = hashSetOf()

  fun addModelChangedListener(listener: ModelChangedListener<ProductOrderModel>) {
    _modelChangedListeners.add(listener)
  }

  fun removeModelChangedListener(listener: ModelChangedListener<ProductOrderModel>) {
    _modelChangedListeners.remove(listener)
  }

  override suspend fun selectAll(): List<ProductOrderModel> =
      withContext(_dispatcher) { _localDao.selectAll() }

  override suspend fun selectById(id: Long?): ProductOrderModel? =
      withContext(_dispatcher) { _localDao.selectById(id) }

  override suspend fun selectById(ids: List<Long>): List<ProductOrderModel> =
      withContext(_dispatcher) { _localDao.selectById(ids) }

  override suspend fun isExistsById(id: Long?): Boolean =
      withContext(_dispatcher) { _localDao.isExistsById(id) }

  override suspend fun add(model: ProductOrderModel): Long =
      withContext(_dispatcher) {
        _localDao
            .insert(model)
            .let { rowId -> _localDao.selectIdByRowId(rowId) }
            .also { insertedId -> selectById(insertedId)?.let { _notifyModelAdded(listOf(it)) } }
      }

  /** @return Inserted model IDs. Empty list for a failed operation. */
  suspend fun add(models: List<ProductOrderModel>): List<Long> =
      withContext(_dispatcher) {
        _localDao
            .insert(models)
            .let { rowIds -> _localDao.selectIdByRowId(rowIds) }
            .also { insertedIds ->
              selectById(insertedIds).let { if (it.isNotEmpty()) _notifyModelAdded(it) }
            }
      }

  override suspend fun update(model: ProductOrderModel): Int =
      withContext(_dispatcher) {
        _localDao.update(model).also { effectedRows ->
          if (effectedRows > 0) selectById(model.id)?.let { _notifyModelUpdated(listOf(it)) }
        }
      }

  /** @return Number of row effected. 0 for a failed operation. */
  suspend fun update(models: List<ProductOrderModel>): Int =
      withContext(_dispatcher) {
        _localDao.update(models).also { effectedRows ->
          if (effectedRows == 0) return@also
          selectById(models.mapNotNull { it.id }).let {
            if (it.isNotEmpty()) _notifyModelUpdated(it)
          }
        }
      }

  override suspend fun delete(model: ProductOrderModel): Int =
      withContext(_dispatcher) {
        val deletedOrder: ProductOrderModel = selectById(model.id) ?: return@withContext 0
        _localDao.delete(model).also { effectedRows ->
          if (effectedRows > 0) _notifyModelDeleted(listOf(deletedOrder))
        }
      }

  /** @return Number of row effected. 0 for a failed operation. */
  suspend fun delete(models: List<ProductOrderModel>): Int =
      withContext(_dispatcher) {
        val deletedOrders: List<ProductOrderModel> = selectById(models.mapNotNull { it.id })
        _localDao.delete(models).also { effectedRows ->
          if (deletedOrders.isNotEmpty() && effectedRows > 0) _notifyModelDeleted(deletedOrders)
        }
      }

  /** @return Upserted product order ID. 0 for a failed operation. */
  suspend fun upsert(model: ProductOrderModel): Long =
      withContext(_dispatcher) {
        _localDao
            .upsert(model)
            .let { rowId -> _localDao.selectIdByRowId(rowId) }
            .also { upsertedId -> selectById(upsertedId)?.let { _notifyModelUpserted(listOf(it)) } }
      }

  /** @return Upserted product order IDs. Empty list for a failed operation. */
  suspend fun upsert(models: List<ProductOrderModel>): List<Long> =
      withContext(_dispatcher) {
        _localDao
            .upsert(models)
            .let { rowIds -> _localDao.selectIdByRowId(rowIds) }
            .also { upsertedIds ->
              selectById(upsertedIds).let { if (it.isNotEmpty()) _notifyModelUpserted(it) }
            }
      }

  suspend fun selectAllByQueueId(queueId: Long?): List<ProductOrderModel> =
      withContext(_dispatcher) { _localDao.selectAllByQueueId(queueId) }

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
    fun instance(
        dispatcher: CoroutineDispatcher,
        productOrderDao: ProductOrderDao
    ): ProductOrderRepository =
        _instance ?: ProductOrderRepository(dispatcher, productOrderDao).apply { _instance = this }
  }
}
