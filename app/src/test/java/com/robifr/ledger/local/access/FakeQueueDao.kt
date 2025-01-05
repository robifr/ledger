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
import com.robifr.ledger.data.model.QueueModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class FakeQueueDao(
    override val data: MutableList<QueueModel>,
    override val idGenerator: FakeIdGenerator = FakeIdGenerator(data.size)
) : QueueDao(), FakeQueryAccessible<QueueModel> {
  override fun assignId(model: QueueModel, id: Long): QueueModel = model.copy(id = id)

  override fun insert(queue: QueueModel): Long = super<FakeQueryAccessible>.insert(queue)

  override fun update(queue: QueueModel): Int = super<FakeQueryAccessible>.update(queue)

  override fun delete(queue: QueueModel): Int = super<FakeQueryAccessible>.delete(queue)

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

  override fun selectAllInRange(startDate: Instant, endDate: Instant): List<QueueModel> =
      QueueFilterer()
          .apply {
            filters =
                filters.copy(
                    filteredDate =
                        QueueDate(
                            ZonedDateTime.ofInstant(startDate, ZoneId.systemDefault()),
                            ZonedDateTime.ofInstant(endDate, ZoneId.systemDefault())))
          }
          .filter(data)
}
