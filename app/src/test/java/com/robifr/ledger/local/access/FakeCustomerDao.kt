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

import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.local.FtsStringConverter
import java.math.BigDecimal

data class FakeCustomerDao(
    override val data: MutableList<CustomerModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size),
    val queueData: MutableList<QueueModel>,
    val productOrderData: MutableList<ProductOrderModel>
) : CustomerDao(), FakeQueryAccessible<CustomerModel> {
  override fun assignId(model: CustomerModel, id: Long): CustomerModel = model.copy(id = id)

  override fun insert(customer: CustomerModel): Long = super<FakeQueryAccessible>.insert(customer)

  override fun update(customer: CustomerModel): Int = super<FakeQueryAccessible>.update(customer)

  override fun delete(customer: CustomerModel): Int = super<FakeQueryAccessible>.delete(customer)

  override fun selectAll(): List<CustomerModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(customerId: Long?): CustomerModel? =
      super<FakeQueryAccessible>.selectById(customerId)

  override fun selectByRowId(rowId: Long): CustomerModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectRowIdById(customerId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(customerId)

  override fun isExistsById(customerId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(customerId)

  override fun selectAllInfoWithBalance(): List<CustomerBalanceInfo> =
      data
          .asSequence()
          .filter { it.balance > 0L }
          .map { CustomerBalanceInfo(id = it.id, balance = it.balance) }
          .toList()

  override fun _insert(customer: CustomerModel): Long = insert(customer)

  override fun _update(customer: CustomerModel): Int = update(customer)

  override fun _delete(customer: CustomerModel): Int = delete(customer)

  override fun _selectAllIds(): List<Long> = selectAll().mapNotNull { it.id }

  override fun _search(query: String): List<CustomerModel> =
      data.filter { query in "*\"${FtsStringConverter.toFtsSpacedString(it.name)}\"*" }

  override fun _selectUnpaidQueueTotalPrice(customerId: Long?): List<BigDecimal> =
      if (customerId != null) {
        queueData
            .asSequence()
            .filter { it.customerId == customerId && it.status == QueueModel.Status.UNPAID }
            .mapNotNull { it.id }
            .let { queueIds ->
              productOrderData.filter { it.queueId in queueIds }.map { it.totalPrice }
            }
      } else {
        listOf()
      }

  override fun _deleteFts(rowId: Long) {}

  override fun _insertFts(rowId: Long, customerName: String): Long = -1L
}
