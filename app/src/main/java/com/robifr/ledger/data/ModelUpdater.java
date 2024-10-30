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

package com.robifr.ledger.data;

import androidx.annotation.NonNull;
import com.robifr.ledger.data.model.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Deprecated
public class ModelUpdater {
  private ModelUpdater() {}

  @NonNull
  public static <M extends Model> List<M> addModel(
      @NonNull List<M> oldModels, @NonNull List<M> newModels) {
    Objects.requireNonNull(newModels);
    Objects.requireNonNull(oldModels);

    final ArrayList<M> models = new ArrayList<>(oldModels);
    models.addAll(newModels);
    return models;
  }

  @NonNull
  public static <M extends Model> List<M> updateModel(
      @NonNull List<M> oldModels, @NonNull List<M> newModels) {
    Objects.requireNonNull(newModels);
    Objects.requireNonNull(oldModels);

    final ArrayList<M> models = new ArrayList<>(oldModels);

    for (M updatedModel : newModels) {
      for (int i = 0; i < models.size(); i++) {
        final M currentModel = models.get(i);

        if (currentModel.modelId() != null
            && currentModel.modelId().equals(updatedModel.modelId())) {
          models.set(i, updatedModel);
          break;
        }
      }
    }

    return models;
  }

  @NonNull
  public static <M extends Model> List<M> deleteModel(
      @NonNull List<M> oldModels, @NonNull List<M> newModels) {
    Objects.requireNonNull(newModels);
    Objects.requireNonNull(oldModels);

    final ArrayList<M> models = new ArrayList<>(oldModels);

    for (M updatedModel : newModels) {
      for (int i = 0; i < models.size(); i++) {
        final M currentModel = models.get(i);

        if (currentModel.modelId() != null
            && currentModel.modelId().equals(updatedModel.modelId())) {
          models.remove(i);
          break;
        }
      }
    }

    return models;
  }

  @NonNull
  public static <M extends Model> List<M> upsertModel(
      @NonNull List<M> oldModels, @NonNull List<M> newModels) {
    Objects.requireNonNull(newModels);
    Objects.requireNonNull(oldModels);

    final ArrayList<M> models = new ArrayList<>(oldModels);

    for (M updatedModels : newModels) {
      for (int i = 0; i < models.size(); i++) {
        final M currentModel = models.get(i);

        // Update when having the same ID.
        if (currentModel.modelId() != null
            && currentModel.modelId().equals(updatedModels.modelId())) {
          models.set(i, updatedModels);
          break;
        }

        // Add as new when reached the end of array
        // while can't find model with the same ID.
        if (i == models.size() - 1) models.add(updatedModels);
      }
    }

    return models;
  }
}
