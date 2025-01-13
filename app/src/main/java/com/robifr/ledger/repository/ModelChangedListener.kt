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

import com.robifr.ledger.data.InfoSynchronizer
import com.robifr.ledger.data.ModelSynchronizer
import com.robifr.ledger.data.model.Info
import com.robifr.ledger.data.model.Model

interface ModelChangedListener<M : Model> {
  fun onModelAdded(models: List<M>)

  fun onModelUpdated(models: List<M>)

  fun onModelDeleted(models: List<M>)

  fun onModelUpserted(models: List<M>)
}

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

class InfoSyncListener<I : Info, M : Model>(
    val currentInfo: () -> List<I>,
    val modelToInfo: (M) -> I,
    val onSyncInfo: (syncedInfo: List<I>) -> Unit
) : ModelChangedListener<M> {
  override fun onModelAdded(models: List<M>) {
    onSyncInfo(InfoSynchronizer.addInfo(currentInfo(), models, modelToInfo))
  }

  override fun onModelUpdated(models: List<M>) {
    onSyncInfo(InfoSynchronizer.updateInfo(currentInfo(), models, modelToInfo))
  }

  override fun onModelDeleted(models: List<M>) {
    onSyncInfo(InfoSynchronizer.deleteInfo(currentInfo(), models))
  }

  override fun onModelUpserted(models: List<M>) {
    onSyncInfo(InfoSynchronizer.upsertInfo(currentInfo(), models, modelToInfo))
  }
}
