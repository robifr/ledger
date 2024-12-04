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

import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.local.access.QueueDao
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext

@Singleton
class QueueRepository
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _localDao: QueueDao,
    private val _customerRepository: CustomerRepository,
    private val _productOrderRepository: ProductOrderRepository
) : QueryReadable<QueueModel>, QueryModifiable<QueueModel> {
  private val _modelChangedListeners: HashSet<ModelChangedListener<QueueModel>> = hashSetOf()

  fun addModelChangedListener(listener: ModelChangedListener<QueueModel>) {
    _modelChangedListeners.add(listener)
  }

  fun removeModelChangedListener(listener: ModelChangedListener<QueueModel>) {
    _modelChangedListeners.remove(listener)
  }

  override suspend fun selectAll(): List<QueueModel> =
      withContext(_dispatcher) { _mapFields(_localDao.selectAll()) }

  override suspend fun selectById(id: Long?): QueueModel? =
      withContext(_dispatcher) { _localDao.selectById(id)?.let { _mapFields(it) } }

  override suspend fun selectById(ids: List<Long>): List<QueueModel> =
      withContext(_dispatcher) { _localDao.selectById(ids).map { _mapFields(it) } }

  override suspend fun isExistsById(id: Long?): Boolean =
      withContext(_dispatcher) { _localDao.isExistsById(id) }

  override suspend fun add(model: QueueModel): Long =
      withContext(_dispatcher) {
        _localDao
            .insert(model)
            .let { rowId -> _localDao.selectIdByRowId(rowId) }
            .also { insertedId ->
              _productOrderRepository
                  .add(model.productOrders.map { it.copy(queueId = insertedId) })
                  .await()
              // First select query is to get a queue mapped with newly inserted product orders.
              selectById(insertedId)?.let { insertedQueueWithOrders ->
                _customerRepository.selectById(insertedQueueWithOrders.customerId)?.let {
                  // Make customer pay the already inserted queue.
                  _customerRepository.update(
                      it.copy(balance = it.balanceOnMadePayment(insertedQueueWithOrders)))
                }
              }
              // Re-select to get a queue mapped with both added product orders
              // and updated customer.
              selectById(insertedId)?.let { _notifyModelAdded(it) }
            }
      }

  override suspend fun update(model: QueueModel): Int =
      withContext(_dispatcher) {
        val oldQueue: QueueModel? = selectById(model.id)
        val ordersToUpsert: MutableList<ProductOrderModel> = mutableListOf()
        val ordersToDelete: MutableList<ProductOrderModel> =
            oldQueue?.productOrders?.toMutableList() ?: mutableListOf()

        for (productOrder in model.productOrders) {
          // Set the queue ID, in case they're newly created.
          ordersToUpsert.add(productOrder.copy(queueId = model.id))
          // Remove product order with equal ID if they're exists inside `ordersToDelete`,
          // so that they will get an upsert, while leaving product orders to delete.
          ordersToDelete.removeIf { it.id != null && it.id == productOrder.id }
        }

        val updateCustomer =
            BiFunction<QueueModel, QueueModel, CompletableFuture<Void>> {
                oldQueue: QueueModel,
                updatedQueue: QueueModel ->
              val selectUpdatedCustomer: CompletableFuture<CustomerModel> =
                  _customerRepository.selectById(updatedQueue.customerId)
              val selectOldCustomer: CompletableFuture<CustomerModel> =
                  _customerRepository.selectById(oldQueue.customerId)

              val updateOldCustomer =
                  BiFunction<CustomerModel, CustomerModel, CompletableFuture<Int>> {
                      oldCustomer: CustomerModel?,
                      updatedCustomer: CustomerModel? ->
                    if (oldCustomer?.id != null &&
                        (updatedCustomer == null ||
                            oldCustomer.id !=
                                updatedCustomer
                                    .id) // Revert back old customer balance when different customer
                    // selected,
                    // even when the new one is null.
                    )
                        _customerRepository.update(
                            oldCustomer.withBalance(oldCustomer.balanceOnRevertedPayment(oldQueue)))
                    else CompletableFuture.completedFuture<Int>(0)
                  }
              val updateNewCustomer =
                  BiFunction<CustomerModel, CustomerModel, CompletableFuture<Int>> {
                      oldCustomer: CustomerModel?,
                      updatedCustomer: CustomerModel? ->
                    if (updatedCustomer !=
                        null // Update customer balance for newly selected customer.
                    )
                        _customerRepository.update(
                            updatedCustomer.withBalance(
                                updatedCustomer.balanceOnUpdatedPayment(oldQueue, updatedQueue)))
                    else CompletableFuture.completedFuture<Int>(0)
                  }
              selectUpdatedCustomer.thenAcceptAsync { updatedCustomer: CustomerModel ->
                selectOldCustomer.thenAcceptAsync { oldCustomer: CustomerModel ->
                  CompletableFuture.allOf(
                      updateOldCustomer.apply(oldCustomer, updatedCustomer),
                      updateNewCustomer.apply(oldCustomer, updatedCustomer))
                }
              }
            }

        _localDao.update(model).also { selectById(model.id)?.let { _notifyModelUpdated(it) } }
      }

  override suspend fun delete(model: QueueModel): Int =
      withContext(_dispatcher) {
        // Note: Associated rows on product order table will automatically
        // deleted upon queue deletion.
        val deletedQueue: QueueModel? = selectById(model.id)
        _localDao.delete(model).also { effectedRows ->
          // Failed query returns 0.
          if (deletedQueue == null || effectedRows == 0) return@also
          _customerRepository.selectById(deletedQueue.customerId)?.let {
            _customerRepository.update(it.copy(balance = it.balanceOnRevertedPayment(deletedQueue)))
          }
          _notifyModelDeleted(deletedQueue)
        }
      }

  suspend fun selectAllInRange(startDate: ZonedDateTime, endDate: ZonedDateTime): List<QueueModel> =
      withContext(_dispatcher) {
        _localDao.selectAllInRange(startDate.toInstant(), endDate.toInstant()).map {
          _mapFields(it)
        }
      }

  private suspend fun _notifyModelAdded(vararg queues: QueueModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelAdded(queues.toList()) }
    }
  }

  private suspend fun _notifyModelUpdated(vararg queues: QueueModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelUpdated(queues.toList()) }
    }
  }

  private suspend fun _notifyModelDeleted(vararg queues: QueueModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelDeleted(queues.toList()) }
    }
  }

  private suspend fun _notifyModelUpserted(vararg queues: QueueModel) {
    withContext(Dispatchers.Main) {
      _modelChangedListeners.forEach { it.onModelUpserted(queues.toList()) }
    }
  }

  /**
   * Specifically used when query returning object model, mostly select query. Such as
   * [QueueModel.customer] field, which can only be obtained from database. We have to make sure
   * those field mapped into it.
   */
  private suspend fun _mapFields(queue: QueueModel): QueueModel =
      withContext(_dispatcher) {
        queue.copy(
            customer = _customerRepository.selectById(queue.customer?.id),
            productOrders = _productOrderRepository.selectAllByQueueId(queue.id).await())
      }

  /** @see _mapFields */
  private suspend fun _mapFields(queues: List<QueueModel>): List<QueueModel> =
      withContext(_dispatcher) { queues.map { _mapFields(it) } }
}
