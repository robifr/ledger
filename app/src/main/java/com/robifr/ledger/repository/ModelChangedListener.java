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

package com.robifr.ledger.repository;

import androidx.annotation.NonNull;
import com.robifr.ledger.data.model.Model;
import java.util.List;

public interface ModelChangedListener<M extends Model> {
  public void onModelAdded(@NonNull List<M> models);

  public void onModelUpdated(@NonNull List<M> models);

  public void onModelDeleted(@NonNull List<M> models);

  public void onModelUpserted(@NonNull List<M> models);

  public sealed interface Source<M extends Model>
      permits CustomerRepository, ProductOrderRepository, ProductRepository, QueueRepository {
    public void addModelChangedListener(@NonNull ModelChangedListener<M> listener);

    public void removeModelChangedListener(@NonNull ModelChangedListener<M> listener);

    public void notifyModelAdded(@NonNull List<M> models);

    public void notifyModelUpdated(@NonNull List<M> models);

    public void notifyModelDeleted(@NonNull List<M> models);

    public void notifyModelUpserted(@NonNull List<M> models);
  }
}
