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

import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.local.access.CustomerDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CustomerRepository(
    private val _dispatcher: CoroutineDispatcher,
    private val _localDao: CustomerDao,
) : QueryReadable<CustomerModel>, QueryModifiable<CustomerModel> {
  private val _modelChangedListeners: HashSet<ModelChangedListener<CustomerModel>> = hashSetOf()

  fun addModelChangedListener(listener: ModelChangedListener<CustomerModel>) {
    _modelChangedListeners.add(listener)
  }

  fun removeModelChangedListener(listener: ModelChangedListener<CustomerModel>) {
    _modelChangedListeners.remove(listener)
  }

  override suspend fun selectAll(): List<CustomerModel> =
      withContext(_dispatcher) { _mapFields(_localDao.selectAll()) }

  override suspend fun selectById(id: Long?): CustomerModel? =
      withContext(_dispatcher) { _localDao.selectById(id)?.let { _mapFields(it) } }

  override suspend fun selectById(ids: List<Long>): List<CustomerModel> =
      withContext(_dispatcher) { _localDao.selectById(ids).map { _mapFields(it) } }

  override suspend fun isExistsById(id: Long?): Boolean =
      withContext(_dispatcher) { _localDao.isExistsById(id) }

  override suspend fun add(model: CustomerModel): Long =
      withContext(_dispatcher) {
        _localDao
            .insert(model)
            .let { rowId -> _localDao.selectIdByRowId(rowId) }
            .also { insertedId -> selectById(insertedId)?.let { _notifyModelAdded(listOf(it)) } }
      }

  override suspend fun update(model: CustomerModel): Int =
      withContext(_dispatcher) {
        _localDao.update(model).also { effectedRows ->
          if (effectedRows > 0) selectById(model.id)?.let { _notifyModelUpdated(listOf(it)) }
        }
      }

  override suspend fun delete(model: CustomerModel): Int =
      withContext(_dispatcher) {
        // Note: Referenced customer ID on queue table will automatically set
        //    to null upon customer deletion.
        val deletedCustomer: CustomerModel = selectById(model.id) ?: return@withContext 0
        _localDao.delete(model).also { effectedRows ->
          if (effectedRows > 0) _notifyModelDeleted(listOf(deletedCustomer))
        }
      }

  suspend fun search(query: String): List<CustomerModel> =
      withContext(_dispatcher) { _mapFields(_localDao.search(query)) }

  suspend fun selectAllInfoWithBalance(): List<CustomerBalanceInfo> =
      withContext(_dispatcher) { _localDao.selectAllInfoWithBalance() }

  suspend fun selectAllInfoWithDebt(): List<CustomerDebtInfo> =
      withContext(_dispatcher) { _localDao.selectAllInfoWithDebt() }

  private suspend fun _notifyModelAdded(models: List<CustomerModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelAdded(models) } }
  }

  private suspend fun _notifyModelUpdated(models: List<CustomerModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelUpdated(models) } }
  }

  private suspend fun _notifyModelDeleted(models: List<CustomerModel>) {
    withContext(Dispatchers.Main) { _modelChangedListeners.forEach { it.onModelDeleted(models) } }
  }

  /**
   * Specifically used when query returning object model, mostly select query. Such as
   * [CustomerModel.debt] field, which can only be obtained from database. We have to make sure
   * those field mapped into it.
   */
  private suspend fun _mapFields(customer: CustomerModel): CustomerModel =
      withContext(_dispatcher) { customer.copy(debt = _localDao.totalDebtById(customer.id)) }

  private suspend fun _mapFields(customers: List<CustomerModel>): List<CustomerModel> =
      withContext(_dispatcher) { customers.map { _mapFields(it) } }

  companion object {
    @Volatile private var _instance: CustomerRepository? = null

    @Synchronized
    fun instance(dispatcher: CoroutineDispatcher, customerDao: CustomerDao): CustomerRepository =
        _instance ?: CustomerRepository(dispatcher, customerDao).apply { _instance = this }
  }
}
