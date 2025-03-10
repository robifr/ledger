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

package io.github.robifr.ledger.ui.common.state

sealed interface RecyclerAdapterState {
  data object DataSetChanged : RecyclerAdapterState

  data class ItemChanged(val indexes: List<Int>) : RecyclerAdapterState {
    constructor(vararg indexes: Int) : this(indexes.toList())
  }

  /**
   * @property payload Optional data for partial updates to avoid triggering a full rebind. Pass
   *   [Unit] to disable animation.
   */
  data class ItemRangeChanged(
      val positionStart: Int,
      val itemCount: Int,
      val payload: Any? = null
  ) : RecyclerAdapterState

  data class ItemRangeInserted(val positionStart: Int, val itemCount: Int) : RecyclerAdapterState

  data class ItemRangeRemoved(val positionStart: Int, val itemCount: Int) : RecyclerAdapterState
}
