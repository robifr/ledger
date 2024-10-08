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

package com.robifr.ledger.ui;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Objects;

/**
 * @param <I> Item to bind.
 * @param <A> Possible action.
 */
public abstract class RecyclerViewHolder<I, A> extends RecyclerView.ViewHolder {
  @NonNull protected final A _action;

  public RecyclerViewHolder(@NonNull View itemView, @NonNull A action) {
    super(itemView);
    this._action = Objects.requireNonNull(action);
  }

  @NonNull
  public A action() {
    return this._action;
  }

  public abstract void bind(@NonNull I item);
}
