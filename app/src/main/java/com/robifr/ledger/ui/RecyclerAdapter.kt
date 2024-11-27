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

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * @param _itemToCompare The item to be compared in [DiffUtil.Callback.areItemsTheSame].
 * @param _contentToCompare The content to be compared in [DiffUtil.Callback.areContentsTheSame].
 */
abstract class RecyclerAdapter<T, VH : RecyclerView.ViewHolder>(
    private val _itemToCompare: (T) -> Any,
    private val _contentToCompare: (T) -> Any
) : RecyclerView.Adapter<VH>() {
  protected open var _previousItems: List<T> = listOf()

  open fun notifyDiffedItemChanged(items: List<T>) {
    DiffUtil.calculateDiff(
            RecyclerDiffUtil(_previousItems, items, _itemToCompare, _contentToCompare))
        .dispatchUpdatesTo(this)
    _previousItems = items
  }
}

private class RecyclerDiffUtil<T>(
    private val _oldList: List<T>,
    private val _newList: List<T>,
    private val _itemToCompare: (T) -> Any,
    private val _contentToCompare: (T) -> Any
) : DiffUtil.Callback() {
  override fun getOldListSize(): Int = _oldList.size

  override fun getNewListSize(): Int = _newList.size

  override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
      _itemToCompare(_oldList[oldItemPosition]) == _itemToCompare(_newList[newItemPosition])

  override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
      _contentToCompare(_oldList[oldItemPosition]) == _contentToCompare(_newList[newItemPosition])
}
