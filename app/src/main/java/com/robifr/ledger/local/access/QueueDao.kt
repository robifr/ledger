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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.local.InstantConverter
import java.time.Instant

@Dao
abstract class QueueDao : QueryAccessible<QueueModel> {
  @Insert abstract override fun insert(queue: QueueModel): Long

  @Update abstract override fun update(queue: QueueModel): Int

  @Delete abstract override fun delete(queue: QueueModel): Int

  @Query("SELECT * FROM queue") abstract override fun selectAll(): List<QueueModel>

  @Query("SELECT * FROM queue WHERE id = :queueId")
  abstract override fun selectById(queueId: Long?): QueueModel?

  @Query("SELECT * FROM queue WHERE id IN (:queueIds)")
  abstract override fun selectById(queueIds: List<Long>): List<QueueModel>

  @Query("SELECT * FROM queue WHERE rowid = :rowId")
  abstract override fun selectByRowId(rowId: Long): QueueModel?

  @Query("SELECT id FROM queue WHERE rowid = :rowId")
  abstract override fun selectIdByRowId(rowId: Long): Long

  @Query("SELECT rowid FROM queue WHERE id = :queueId")
  abstract override fun selectRowIdById(queueId: Long?): Long

  @Query("SELECT EXISTS(SELECT id FROM queue WHERE id = :queueId)")
  abstract override fun isExistsById(queueId: Long?): Boolean

  @Query("SELECT NOT EXISTS(SELECT 1 FROM queue)") abstract override fun isTableEmpty(): Boolean

  @Query("SELECT * FROM queue WHERE date >= :startDate AND date <= :endDate")
  @TypeConverters(InstantConverter::class)
  abstract fun selectAllInRange(startDate: Instant, endDate: Instant): List<QueueModel>
}
