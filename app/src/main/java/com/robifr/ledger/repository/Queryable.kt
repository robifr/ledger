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

import com.robifr.ledger.data.model.Model
import com.robifr.ledger.data.model.QueueModel

/**
 * Every method with ID as a parameter should be using an object instead of its primitive type. So
 * that we can easily query a foreign-key (nullable). Like when querying [QueueModel.customerId].
 */
sealed interface Queryable<M : Model> {
  /** @return Inserted model ID. 0 for a failed operation. */
  suspend fun add(model: M): Long

  /** @return Number of row effected. 0 for a failed operation. */
  suspend fun update(model: M): Int

  /** @return Number of row effected. 0 for a failed operation. */
  suspend fun delete(id: Long?): Int

  /** @return List of selected models. Empty list for a failed operation. */
  suspend fun selectAll(): List<M>

  /** @return Selected model. Null for a failed operation. */
  suspend fun selectById(id: Long?): M?

  /** @return List of selected models. Empty list for a failed operation. */
  suspend fun selectById(ids: List<Long>): List<M>

  suspend fun isExistsById(id: Long?): Boolean

  suspend fun isTableEmpty(): Boolean
}
