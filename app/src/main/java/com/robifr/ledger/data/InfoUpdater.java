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
import com.robifr.ledger.data.model.Info;
import com.robifr.ledger.data.model.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Deprecated
public class InfoUpdater {
  private InfoUpdater() {}

  @NonNull
  public static <M extends Model, I extends Info> List<I> addInfo(
      @NonNull List<M> models,
      @NonNull List<I> oldInfo,
      @NonNull Function<M, I> modelToInfoConverter) {
    Objects.requireNonNull(models);
    Objects.requireNonNull(modelToInfoConverter);
    Objects.requireNonNull(oldInfo);

    final ArrayList<I> info = new ArrayList<>(oldInfo);
    final List<I> newInfo = models.stream().map(modelToInfoConverter).collect(Collectors.toList());

    info.addAll(newInfo);
    return info;
  }

  @NonNull
  public static <M extends Model, I extends Info> List<I> updateInfo(
      @NonNull List<M> models,
      @NonNull List<I> oldInfo,
      @NonNull Function<M, I> modelToInfoConverter) {
    Objects.requireNonNull(models);
    Objects.requireNonNull(modelToInfoConverter);
    Objects.requireNonNull(oldInfo);

    final ArrayList<I> info = new ArrayList<>(oldInfo);
    final List<I> newInfo = models.stream().map(modelToInfoConverter).collect(Collectors.toList());
    final HashMap<Long, I> filteredInfo = new HashMap<>();

    info.forEach(i -> filteredInfo.put(i.modelId(), i));
    newInfo.forEach(i -> filteredInfo.put(i.modelId(), i)); // Override duplicate ID.
    return new ArrayList<>(filteredInfo.values());
  }

  @NonNull
  public static <M extends Model, I extends Info> List<I> deleteInfo(
      @NonNull List<M> models,
      @NonNull List<I> oldInfo,
      @NonNull Function<M, I> modelToInfoConverter) {
    Objects.requireNonNull(models);
    Objects.requireNonNull(modelToInfoConverter);
    Objects.requireNonNull(oldInfo);

    final ArrayList<I> info = new ArrayList<>(oldInfo);

    for (M m : models) {
      for (int i = info.size(); i-- > 0; ) {
        if (info.get(i).modelId() != null && info.get(i).modelId().equals(m.modelId())) {
          info.remove(i);
          break;
        }
      }
    }

    return info;
  }
}
