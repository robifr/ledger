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
import com.robifr.ledger.local.TransactionProvider
import com.robifr.ledger.local.access.QueueDao
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueueRepository(
    private val _localDao: QueueDao,
    private val _transactionProvider: TransactionProvider,
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

  override suspend fun selectAll(): List<QueueModel> = _mapFields(_localDao.selectAll())

  override suspend fun selectById(id: Long?): QueueModel? =
      _localDao.selectById(id)?.let { _mapFields(it) }

  override suspend fun selectById(ids: List<Long>): List<QueueModel> =
      _localDao.selectById(ids).map { _mapFields(it) }

  override suspend fun isExistsById(id: Long?): Boolean = _localDao.isExistsById(id)

  override suspend fun add(model: QueueModel): Long =
      _transactionProvider.withTransaction {
        _localDao
            .insert(model)
            .let { rowId -> _localDao.selectIdByRowId(rowId) }
            .also { insertedId ->
              if (insertedId != 0L) {
                _productOrderRepository.add(
                    model.productOrders.map { it.copy(queueId = insertedId) })
              }
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
              selectById(insertedId)?.let { _notifyModelAdded(listOf(it)) }
            }
      }

  override suspend fun update(model: QueueModel): Int =
      _transactionProvider.withTransaction {
        val oldQueue: QueueModel = selectById(model.id) ?: return@withTransaction 0

        val productOrdersToUpsert: MutableList<ProductOrderModel> = mutableListOf()
        val productOrdersToDelete: MutableList<ProductOrderModel> =
            oldQueue.productOrders.toMutableList()
        for (productOrder in model.productOrders) {
          // Set the queue ID, in case they're newly created.
          productOrdersToUpsert.add(productOrder.copy(queueId = model.id))
          // Remove product order with equal ID if they're exists inside `ordersToDelete`, so
          // that they will get an upsert, while leaving the list with product orders to delete.
          productOrdersToDelete.removeIf { it.id != null && it.id == productOrder.id }
        }
        _productOrderRepository.upsert(productOrdersToUpsert)
        _productOrderRepository.delete(productOrdersToDelete)

        val oldCustomer: CustomerModel? = _customerRepository.selectById(oldQueue.customerId)
        val updatedCustomer: CustomerModel? = _customerRepository.selectById(model.customerId)
        oldCustomer?.let {
          if (updatedCustomer == null || oldCustomer.id != updatedCustomer.id) {
            // Revert back old customer balance when different customer selected,
            // including when the new one is null.
            _customerRepository.update(it.copy(balance = it.balanceOnRevertedPayment(oldQueue)))
          }
        }
        updatedCustomer?.let {
          // Update customer balance for newly selected customer.
          _customerRepository.update(it.copy(balance = it.balanceOnUpdatedPayment(oldQueue, model)))
        }

        // Only update after foreign column updated (product orders and customer),
        // so that queue already provided with an updated value when doing select query.
        _localDao.update(model).also { effectedRows ->
          if (effectedRows > 0) selectById(model.id)?.let { _notifyModelUpdated(listOf(it)) }
        }
      }

  override suspend fun delete(model: QueueModel): Int =
      _transactionProvider.withTransaction {
        // Note: Associated rows on product order table will automatically
        //    deleted upon queue deletion.
        val deletedQueue: QueueModel = selectById(model.id) ?: return@withTransaction 0
        _localDao.delete(model).also { effectedRows ->
          if (effectedRows == 0) return@also
          _customerRepository.selectById(deletedQueue.customerId)?.let {
            _customerRepository.update(it.copy(balance = it.balanceOnRevertedPayment(deletedQueue)))
          }
          _notifyModelDeleted(listOf(deletedQueue))
        }
      }

  suspend fun selectAllInRange(startDate: ZonedDateTime, endDate: ZonedDateTime): List<QueueModel> =
      _localDao.selectAllInRange(startDate.toInstant(), endDate.toInstant()).map { _mapFields(it) }

  private suspend fun _notifyModelAdded(models: List<QueueModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelAdded(models) } }
  }

  private suspend fun _notifyModelUpdated(models: List<QueueModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelUpdated(models) } }
  }

  private suspend fun _notifyModelDeleted(models: List<QueueModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelDeleted(models) } }
  }

  /**
   * Specifically used when query returning object model, mostly select query. Such as
   * [QueueModel.customer] field, which can only be obtained from database. We have to make sure
   * those field mapped into it.
   */
  private suspend fun _mapFields(queue: QueueModel): QueueModel =
      queue.copy(
          customer = _customerRepository.selectById(queue.customerId),
          productOrders = _productOrderRepository.selectAllByQueueId(queue.id))

  private suspend fun _mapFields(queues: List<QueueModel>): List<QueueModel> =
      queues.map { _mapFields(it) }

  companion object {
    @Volatile private var _instance: QueueRepository? = null

    @Synchronized
    fun instance(
        queueDao: QueueDao,
        transactionProvider: TransactionProvider,
        customerRepository: CustomerRepository,
        productOrderRepository: ProductOrderRepository
    ): QueueRepository =
        _instance
            ?: QueueRepository(
                    queueDao, transactionProvider, customerRepository, productOrderRepository)
                .apply { _instance = this }
  }
}
