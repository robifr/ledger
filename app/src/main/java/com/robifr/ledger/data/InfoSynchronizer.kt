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

import com.robifr.ledger.data.model.Info
import com.robifr.ledger.data.model.Model

object InfoSynchronizer {
  fun <I : Info, M : Model> addInfo(
      oldInfo: List<I>,
      newModels: List<M>,
      modelToInfo: (M) -> I
  ): List<I> = oldInfo.toMutableList().apply { addAll(newModels.map(modelToInfo)) }

  fun <I : Info, M : Model> updateInfo(
      oldInfo: List<I>,
      newModels: List<M>,
      modelToInfo: (M) -> I
  ): List<I> =
      oldInfo.toMutableList().apply {
        for (newModel in newModels) {
          for ((i, info) in withIndex()) {
            if (info.id != null && newModel.id != null && info.id == newModel.id) {
              set(i, modelToInfo(newModel))
              break
            }
          }
        }
      }

  fun <I : Info, M : Model> deleteInfo(oldInfo: List<I>, newModels: List<M>): List<I> =
      oldInfo.toMutableList().apply {
        for (model in newModels) {
          for ((i, info) in withIndex().reversed()) {
            if (info.id != null && model.id != null && info.id == model.id) {
              removeAt(i)
              break
            }
          }
        }
      }

  fun <I : Info, M : Model> upsertInfo(
      oldInfo: List<I>,
      newModels: List<M>,
      modelToInfo: (M) -> I
  ): List<I> =
      oldInfo.toMutableList().apply {
        for (newModel in newModels) {
          for ((i, info) in withIndex()) {
            // Update when having the same ID.
            if (info.id != null && newModel.id != null && info.id == newModel.id) {
              set(i, modelToInfo(newModel))
              break
            }
            // Add as new when reached the end of array while can't find info with the same ID.
            if (i == size - 1) add(modelToInfo(newModel))
          }
        }
      }
}
