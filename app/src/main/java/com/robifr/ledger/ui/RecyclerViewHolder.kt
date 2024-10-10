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

package com.robifr.ledger.ui

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @param [I] Item to bind.
 * @param [A] Possible action.
 */
// TODO: Remove annotation after Kotlin migration.
abstract class RecyclerViewHolder<I, A>(
    itemView: View,
    @JvmField @get:JvmName("_action") protected val _action: A
) : RecyclerView.ViewHolder(itemView) {
  @get:JvmName("action")
  val action: A
    get() = _action

  abstract fun bind(item: I)
}
