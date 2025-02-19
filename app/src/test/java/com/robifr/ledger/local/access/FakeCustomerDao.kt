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

import com.robifr.ledger.data.display.CustomerFilterer
import com.robifr.ledger.data.display.CustomerSortMethod
import com.robifr.ledger.data.display.CustomerSorter
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerFtsModel
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.CustomerPaginatedInfo
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import java.math.BigDecimal

data class FakeCustomerDao(
    override val data: MutableList<CustomerModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size),
    val queueData: MutableList<QueueModel>,
    val productOrderData: MutableList<ProductOrderModel>
) : CustomerDao(), FakeQueryAccessible<CustomerModel> {
  override fun assignId(model: CustomerModel, id: Long): CustomerModel = model.copy(id = id)

  override fun insert(customer: CustomerModel): Long = super<FakeQueryAccessible>.insert(customer)

  override fun _insert(customer: CustomerModel): Long = insert(customer)

  override fun update(customer: CustomerModel): Int = super<FakeQueryAccessible>.update(customer)

  override fun _update(customer: CustomerModel): Int = update(customer)

  override fun delete(customerId: Long?): Int = super<FakeQueryAccessible>.delete(customerId)

  override fun _delete(customerId: Long?): Int = delete(customerId)

  override fun selectAll(): List<CustomerModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(customerId: Long?): CustomerModel? =
      super<FakeQueryAccessible>.selectById(customerId)

  override fun selectById(ids: List<Long>): List<CustomerModel> =
      super<FakeQueryAccessible>.selectById(ids)

  override fun selectByRowId(rowId: Long): CustomerModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectRowIdById(customerId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(customerId)

  override fun isExistsById(customerId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(customerId)

  override fun isTableEmpty(): Boolean = super<FakeQueryAccessible>.isTableEmpty()

  override fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      limit: Int,
      sortBy: CustomerSortMethod.SortBy,
      isAscending: Boolean,
      filteredMinBalance: Long?,
      filteredMaxBalance: Long?,
      filteredMinDebt: BigDecimal?,
      filteredMaxDebt: BigDecimal?
  ): List<CustomerPaginatedInfo> {
    val filterer: CustomerFilterer =
        CustomerFilterer().apply {
          filters =
              filters.copy(
                  filteredBalance = filteredMinBalance to filteredMaxBalance,
                  filteredDebt = filteredMinDebt to filteredMaxDebt)
        }
    val sorter: CustomerSorter =
        CustomerSorter().apply {
          sortMethod = sortMethod.copy(sortBy = sortBy, isAscending = isAscending)
        }
    return sorter
        .sort(filterer.filter(data))
        .asSequence()
        .drop((pageNumber - 1) * limit)
        .take(limit)
        .map { CustomerPaginatedInfo(it) }
        .toList()
  }

  override fun countFilteredCustomers(
      filteredMinBalance: Long?,
      filteredMaxBalance: Long?,
      filteredMinDebt: BigDecimal?,
      filteredMaxDebt: BigDecimal?
  ): Long {
    val filterer: CustomerFilterer =
        CustomerFilterer().apply {
          filters =
              filters.copy(
                  filteredBalance = filteredMinBalance to filteredMaxBalance,
                  filteredDebt = filteredMinDebt to filteredMaxDebt)
        }
    return filterer.filter(data).size.toLong()
  }

  override fun selectAllBalanceInfoWithBalance(): List<CustomerBalanceInfo> =
      data
          .asSequence()
          .filter { it.balance > 0L }
          .map { CustomerBalanceInfo(id = it.id, balance = it.balance) }
          .toList()

  override fun search(query: String): List<CustomerModel> =
      _search(query.replace("\"".toRegex(), "\"\""))

  override fun _search(query: String): List<CustomerModel> =
      data.filter { it.name.contains(query, true) }

  override fun totalDebtById(customerId: Long?): BigDecimal =
      if (customerId != null) {
        queueData
            .asSequence()
            .filter { it.customerId == customerId && it.status == QueueModel.Status.UNPAID }
            .flatMap { queue -> productOrderData.filter { it.queueId == queue.id } }
            .map { it.totalPrice }
            .fold(0.toBigDecimal()) { acc, totalPrice -> acc.subtract(totalPrice) }
      } else {
        0.toBigDecimal()
      }

  override fun selectAllDebtInfoWithDebt(): List<CustomerDebtInfo> =
      data
          .asSequence()
          .filter { it.debt.compareTo(0.toBigDecimal()) < 0 }
          .map { CustomerDebtInfo(it) }
          .toList()

  override fun _selectAllDebtInfoWithDebt(customerId: Long?): List<CustomerDebtInfo> =
      selectAllDebtInfoWithDebt()

  override fun _deleteFts(rowId: Long) {}

  override fun _insertFts(customerFts: CustomerFtsModel): Long = -1L
}
