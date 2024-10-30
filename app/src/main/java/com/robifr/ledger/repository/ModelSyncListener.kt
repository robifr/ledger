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

import com.robifr.ledger.data.ModelSynchronizer
import com.robifr.ledger.data.model.Model

class ModelSyncListener<M : Model>(
    val currentModel: () -> List<M>,
    val onSyncModels: (syncedModels: List<M>) -> Unit
) : ModelChangedListener<M> {
  override fun onModelAdded(models: List<M>) {
    onSyncModels(ModelSynchronizer.addModel(currentModel(), models))
  }

  override fun onModelUpdated(models: List<M>) {
    onSyncModels(ModelSynchronizer.updateModel(currentModel(), models))
  }

  override fun onModelDeleted(models: List<M>) {
    onSyncModels(ModelSynchronizer.deleteModel(currentModel(), models))
  }

  override fun onModelUpserted(models: List<M>) {
    onSyncModels(ModelSynchronizer.upsertModel(currentModel(), models))
  }
}
