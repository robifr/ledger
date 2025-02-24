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

import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.display.QueueFilterer
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.data.display.QueueSorter
import com.robifr.ledger.data.model.QueueDateInfo
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.data.model.QueuePaginatedInfo
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

data class FakeQueueDao(
    override val data: MutableList<QueueModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size)
) : QueueDao(), FakeQueryAccessible<QueueModel> {
  override fun assignId(model: QueueModel, id: Long): QueueModel = model.copy(id = id)

  override fun insert(queue: QueueModel): Long = super<FakeQueryAccessible>.insert(queue)

  override fun update(queue: QueueModel): Int = super<FakeQueryAccessible>.update(queue)

  override fun delete(queueId: Long?): Int = super<FakeQueryAccessible>.delete(queueId)

  override fun selectAll(): List<QueueModel> = super<FakeQueryAccessible>.selectAll()

  override fun selectById(queueId: Long?): QueueModel? =
      super<FakeQueryAccessible>.selectById(queueId)

  override fun selectById(ids: List<Long>): List<QueueModel> =
      super<FakeQueryAccessible>.selectById(ids)

  override fun selectByRowId(rowId: Long): QueueModel? =
      super<FakeQueryAccessible>.selectByRowId(rowId)

  override fun selectIdByRowId(rowId: Long): Long =
      super<FakeQueryAccessible>.selectIdByRowId(rowId)

  override fun selectRowIdById(queueId: Long?): Long =
      super<FakeQueryAccessible>.selectRowIdById(queueId)

  override fun isExistsById(queueId: Long?): Boolean =
      super<FakeQueryAccessible>.isExistsById(queueId)

  override fun isTableEmpty(): Boolean = super<FakeQueryAccessible>.isTableEmpty()

  override fun selectAllPaginatedInfo(
      shouldCalculateGrandTotalPrice: Boolean,
      sortBy: QueueSortMethod.SortBy,
      isAscending: Boolean,
      filteredCustomerIds: List<Long>,
      isFilteredCustomerIdsEmpty: Boolean,
      isNullCustomerShown: Boolean,
      filteredStatus: Set<QueueModel.Status>,
      filteredMinTotalPrice: BigDecimal?,
      filteredMaxTotalPrice: BigDecimal?,
      filteredDateStart: Instant,
      filteredDateEnd: Instant
  ): List<QueuePaginatedInfo> {
    val filterer: QueueFilterer =
        QueueFilterer().apply {
          filters =
              filters.copy(
                  filteredCustomerIds = filteredCustomerIds,
                  isNullCustomerShown = isNullCustomerShown,
                  filteredStatus = filteredStatus,
                  filteredTotalPrice = filteredMinTotalPrice to filteredMaxTotalPrice,
                  filteredDate =
                      QueueDate(
                          filteredDateStart.atZone(ZoneId.systemDefault()),
                          filteredDateEnd.atZone(ZoneId.systemDefault())))
        }
    val sorter: QueueSorter =
        QueueSorter().apply {
          sortMethod = sortMethod.copy(sortBy = sortBy, isAscending = isAscending)
        }
    return sorter.sort(filterer.filter(data)).map { QueuePaginatedInfo(it) }
  }

  override fun selectPaginatedInfoByOffset(
      pageNumber: Int,
      itemPerPage: Int,
      limit: Int,
      shouldCalculateGrandTotalPrice: Boolean,
      sortBy: QueueSortMethod.SortBy,
      isAscending: Boolean,
      filteredCustomerIds: List<Long>,
      isFilteredCustomerIdsEmpty: Boolean,
      isNullCustomerShown: Boolean,
      filteredStatus: Set<QueueModel.Status>,
      filteredMinTotalPrice: BigDecimal?,
      filteredMaxTotalPrice: BigDecimal?,
      filteredDateStart: Instant,
      filteredDateEnd: Instant
  ): List<QueuePaginatedInfo> {
    val filterer: QueueFilterer =
        QueueFilterer().apply {
          filters =
              filters.copy(
                  filteredCustomerIds = filteredCustomerIds,
                  isNullCustomerShown = isNullCustomerShown,
                  filteredStatus = filteredStatus,
                  filteredTotalPrice = filteredMinTotalPrice to filteredMaxTotalPrice,
                  filteredDate =
                      QueueDate(
                          filteredDateStart.atZone(ZoneId.systemDefault()),
                          filteredDateEnd.atZone(ZoneId.systemDefault())))
        }
    val sorter: QueueSorter =
        QueueSorter().apply {
          sortMethod = sortMethod.copy(sortBy = sortBy, isAscending = isAscending)
        }
    return sorter
        .sort(filterer.filter(data))
        .asSequence()
        .drop((pageNumber - 1) * limit)
        .take(limit)
        .map { QueuePaginatedInfo(it) }
        .toList()
  }

  override fun countFilteredQueues(
      shouldCalculateGrandTotalPrice: Boolean,
      filteredCustomerIds: List<Long>,
      isFilteredCustomerIdsEmpty: Boolean,
      isNullCustomerShown: Boolean,
      filteredStatus: Set<QueueModel.Status>,
      filteredMinTotalPrice: BigDecimal?,
      filteredMaxTotalPrice: BigDecimal?,
      filteredDateStart: Instant,
      filteredDateEnd: Instant
  ): Long {
    val filterer: QueueFilterer =
        QueueFilterer().apply {
          filters =
              filters.copy(
                  filteredCustomerIds = filteredCustomerIds,
                  isNullCustomerShown = isNullCustomerShown,
                  filteredStatus = filteredStatus,
                  filteredTotalPrice = filteredMinTotalPrice to filteredMaxTotalPrice,
                  filteredDate =
                      QueueDate(
                          filteredDateStart.atZone(ZoneId.systemDefault()),
                          filteredDateEnd.atZone(ZoneId.systemDefault())))
        }
    return filterer.filter(data).size.toLong()
  }

  override fun selectDateInfoById(queueIds: List<Long>): List<QueueDateInfo> =
      data.asSequence().filter { it.id in queueIds }.map { QueueDateInfo(it) }.toList()
}
