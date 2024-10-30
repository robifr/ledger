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

package com.robifr.ledger.data

import com.robifr.ledger.data.model.Model

object ModelSynchronizer {
  fun <M : Model> addModel(oldModels: List<M>, newModels: List<M>): List<M> =
      oldModels.toMutableList().apply { addAll(newModels) }

  fun <M : Model> updateModel(oldModels: List<M>, newModels: List<M>): List<M> =
      oldModels.toMutableList().apply {
        for (newModel in newModels) {
          for ((i, oldModel) in withIndex()) {
            if (oldModel.id != null && newModel.id != null && oldModel.id == newModel.id) {
              set(i, newModel)
              break
            }
          }
        }
      }

  fun <M : Model> deleteModel(oldModels: List<M>, newModels: List<M>): List<M> =
      oldModels.toMutableList().apply {
        for (newModel in newModels) {
          for ((i, oldModel) in withIndex().reversed()) {
            if (oldModel.id != null && newModel.id != null && oldModel.id == newModel.id) {
              removeAt(i)
              break
            }
          }
        }
      }

  fun <M : Model> upsertModel(oldModels: List<M>, newModels: List<M>): List<M> =
      oldModels.toMutableList().apply {
        for (newModel in newModels) {
          for ((i, oldModel) in withIndex()) {
            // Update when having the same ID.
            if (oldModel.id != null && newModel.id != null && oldModel.id == newModel.id) {
              set(i, newModel)
              break
            }
            // Add as new when reached the end of array while can't find model with the same ID.
            if (i == size - 1) add(newModel)
          }
        }
      }
}
