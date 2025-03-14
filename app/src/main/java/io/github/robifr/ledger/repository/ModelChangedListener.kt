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

package io.github.robifr.ledger.repository

import io.github.robifr.ledger.data.model.Model

interface ModelChangedListener<M : Model> {
  fun onModelAdded(models: List<M>)

  fun onModelUpdated(models: List<M>)

  fun onModelDeleted(models: List<M>)

  fun onModelUpserted(models: List<M>)
}

/**
 * @param M The incoming or notified model type.
 * @param U The type of the updated or synchronized model after processing the change.
 */
class ModelSyncListener<M : Model, U>(
    val onAdd: (models: List<M>) -> List<U> = { listOf() },
    val onUpdate: (models: List<M>) -> List<U> = { listOf() },
    val onDelete: (models: List<M>) -> List<U> = { listOf() },
    val onUpsert: (models: List<M>) -> List<U> = { listOf() },
    val onSync: (models: List<M>, updatedModels: List<U>) -> Unit = { models, updatedModels -> }
) : ModelChangedListener<M> {
  override fun onModelAdded(models: List<M>) {
    onSync(models, onAdd(models))
  }

  override fun onModelUpdated(models: List<M>) {
    onSync(models, onUpdate(models))
  }

  override fun onModelDeleted(models: List<M>) {
    onSync(models, onDelete(models))
  }

  override fun onModelUpserted(models: List<M>) {
    onSync(models, onUpsert(models))
  }
}
