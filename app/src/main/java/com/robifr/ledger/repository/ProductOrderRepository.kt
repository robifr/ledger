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
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.local.access.ProductOrderDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class ProductOrderRepository
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
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
            .also { insertedId -> selectById(insertedId)?.let { _notifyModelAdded(it) } }
      }

  /** @return Inserted model IDs. */
  suspend fun add(models: List<ProductOrderModel>): List<Long> =
      withContext(_dispatcher) {
        _localDao
            .insert(models)
            .let { rowIds -> _localDao.selectIdByRowId(rowIds) }
            .also { insertedIds -> _notifyModelAdded(*selectById(insertedIds).toTypedArray()) }
      }

  override suspend fun update(model: ProductOrderModel): Int =
      withContext(_dispatcher) {
        _localDao.update(model).also {
          // Failed query returns 0.
          if (it > 0) selectById(model.id)?.let { _notifyModelUpdated(it) }
        }
      }

  /** @return Number of row effected. */
  suspend fun update(models: List<ProductOrderModel>): Int =
      withContext(_dispatcher) {
        _localDao.update(models).also {
          // Failed query returns 0.
          if (it > 0) _notifyModelUpdated(*selectById(models.mapNotNull { it.id }).toTypedArray())
        }
      }

  override suspend fun delete(model: ProductOrderModel): Int =
      withContext(_dispatcher) {
        val deletedOrder: ProductOrderModel? = selectById(model.id)
        _localDao.delete(model).also { effectedRows ->
          // Failed query returns 0.
          if (deletedOrder != null && effectedRows > 0) _notifyModelDeleted(deletedOrder)
        }
      }

  /** @return Number of row effected. 0 for a failed operation. */
  fun delete(productOrders: MutableList<ProductOrderModel?>): CompletableFuture<Int?> {
    Objects.requireNonNull<MutableList<ProductOrderModel?>?>(productOrders)

    val ids = productOrders.stream().map<Long?>(ProductOrderModel::id).collect(Collectors.toList())

    return this.selectById(ids)
        .thenComposeAsync<Int?>(
            Function { ordersToDelete: MutableList<ProductOrderModel?>? ->
              val delete =
                  CompletableFuture.supplyAsync<Int?>(
                      Supplier { this._localDao.delete(ordersToDelete) })
              delete.thenAcceptAsync(
                  Consumer { effected: Int? ->
                    if (effected == 0) return@thenAcceptAsync
                    val notifiedOrders =
                        ordersToDelete!!
                            .stream()
                            .filter { obj: ProductOrderModel? ->
                              Objects.nonNull(obj)
                            } // Only notify the one with a valid ID.
                            .collect(Collectors.toList())
                    this.notifyModelDeleted(notifiedOrders)
                  })
              delete
            })
  }

  /** @return Upserted product order ID. Null for a failed operation. */
  fun upsert(productOrder: ProductOrderModel): CompletableFuture<Long?> {
    Objects.requireNonNull<ProductOrderModel?>(productOrder)

    val upsert =
        CompletableFuture.supplyAsync<Long?>(Supplier { this._localDao.upsert(productOrder) })
            .thenComposeAsync<Long?>(
                Function { rowId: Long? ->
                  CompletableFuture.supplyAsync<Long?>(
                      Supplier { this._localDao.selectIdByRowId(rowId) })
                })

    return upsert.thenComposeAsync<Long?>(
        Function { upsertedOrderId: Long? ->
          this.selectById(upsertedOrderId)
              .thenAcceptAsync(
                  Consumer { upsertedOrder: ProductOrderModel? ->
                    if (upsertedOrder != null)
                        this.notifyModelUpserted(List.of<ProductOrderModel?>(upsertedOrder))
                  })
          CompletableFuture.completedFuture<Long?>(upsertedOrderId)
        })
  }

  /** @return Upserted product order IDs. List of null for a failed operation. */
  fun upsert(
      productOrders: MutableList<ProductOrderModel?>
  ): CompletableFuture<MutableList<Long?>?> {
    Objects.requireNonNull<MutableList<ProductOrderModel?>?>(productOrders)

    val upsert =
        CompletableFuture.supplyAsync<MutableList<Long?>?>(
                Supplier { this._localDao.upsert(productOrders) })
            .thenComposeAsync<MutableList<Long?>?>(
                Function { rowIds: MutableList<Long?>? ->
                  CompletableFuture.supplyAsync<MutableList<Long?>?>(
                      Supplier { this._localDao.selectIdByRowId(rowIds) })
                })

    return upsert.thenComposeAsync<MutableList<Long?>?>(
        Function { upsertedOrderIds: MutableList<Long?>? ->
          this.selectById(upsertedOrderIds)
              .thenAcceptAsync(
                  Consumer { upsertedOrders: MutableList<ProductOrderModel?>? ->
                    // Only notify the one with a valid ID and successfully upserted.
                    val notifiedOrders =
                        upsertedOrders!!
                            .stream()
                            .filter { obj: ProductOrderModel? -> Objects.nonNull(obj) }
                            .collect(Collectors.toList())
                    this.notifyModelUpserted(notifiedOrders)
                  })
          CompletableFuture.completedFuture<MutableList<Long?>?>(upsertedOrderIds)
        })
  }

  suspend fun selectAllByQueueId(queueId: Long?): List<ProductOrderModel> =
      withContext(_dispatcher) { _localDao.selectAllByQueueId(queueId) }

  private suspend fun _notifyModelAdded(vararg productOrders: ProductOrderModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelAdded(productOrders.toList()) }
    }
  }

  private suspend fun _notifyModelUpdated(vararg productOrders: ProductOrderModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelUpdated(productOrders.toList()) }
    }
  }

  private suspend fun _notifyModelDeleted(vararg productOrders: ProductOrderModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelDeleted(productOrders.toList()) }
    }
  }

  private suspend fun _notifyModelUpserted(vararg productOrders: ProductOrderModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelUpserted(productOrders.toList()) }
    }
  }
}
