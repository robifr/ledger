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

/**
 * Fake DAO's query interface to mimics the behavior of the actual local database query. The [data]
 * index used as a replacement for the row ID.
 */
interface FakeQueryAccessible<M : Model> : QueryAccessible<M> {
  val data: MutableList<M>
  val idGenerator: FakeIdGenerator

  override fun insert(model: M): Long =
      if (model.id == null) {
        data.add(assignId(model, idGenerator.generate()))
        data.size - 1L
      } else {
        -1L
      }

  override fun update(model: M): Int =
      if (isExistsById(model.id)) data.set(selectRowIdById(model.id).toInt(), model).let { 1 }
      else 0

  override fun delete(model: M): Int =
      if (isExistsById(model.id)) data.remove(model).let { 1 } else 0

  override fun selectAll(): List<M> = data

  override fun selectById(id: Long?): M? = data.find { it.id != null && id != null && it.id == id }

  override fun selectById(ids: List<Long>): List<M> =
      data.filter { it.id != null && ids.contains(it.id) }

  override fun selectByRowId(rowId: Long): M? = data.getOrNull(rowId.toInt())

  override fun selectIdByRowId(rowId: Long): Long = data.getOrNull(rowId.toInt())?.id ?: 0L

  override fun selectRowIdById(id: Long?): Long =
      data.indexOfFirst { it.id != null && id != null && it.id == id }.toLong()

  override fun isExistsById(id: Long?): Boolean = selectById(id) != null

  fun assignId(model: M, id: Long): M
}
