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

import com.robifr.ledger.data.model.Model
import com.robifr.ledger.data.model.QueueModel

/**
 * Every method with ID as a parameter are using an object instead of its primitive type. So that we
 * can easily query a foreign-key (nullable). Like [QueueModel.customerId].
 */
interface QueryAccessible<M : Model> {
  /** @return Inserted row ID. -1 for a failed operation. */
  fun insert(model: M): Long

  /** @return Number of row effected. 0 for a failed operation. */
  fun update(model: M): Int

  /** @return Number of row effected. 0 for a failed operation. */
  fun delete(model: M): Int

  fun selectAll(): List<M>

  fun selectById(id: Long?): M?

  fun selectById(ids: List<Long>): List<M>

  fun selectByRowId(rowId: Long): M?

  /** @return Model ID for the specified row ID. 0 for a failed operation. */
  fun selectIdByRowId(rowId: Long): Long

  /** @return Row ID (hidden column) for the specified model ID. -1 for a failed operation. */
  fun selectRowIdById(id: Long?): Long

  fun isExistsById(id: Long?): Boolean
}
