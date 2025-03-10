/**
 * Copyright 2025 Robi
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

package io.github.robifr.ledger.ui.common.pagination

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PaginationScrollListener(
    /**
     * The number of remaining items that triggers [_onLoadNextPage] or [_onLoadPreviousPage] during
     * scrolling. Avoid setting this value too close to [PaginationManager.maxItemPerPage] because
     * it may cause unexpected behavior. For example, if both [threshold] and
     * [PaginationManager.maxItemPerPage] is 20, and [PaginationManager.maxItemInMemory] is 60,
     * scrolling far enough to display the item at index 60 may prevent interactions with
     * lower-index items (59, 58, 57, etc.), such as expanding them, from behaving correctly until
     * the item at index 60 is no longer visible.
     */
    val threshold: Int = 10,
    private val _layoutManager: LinearLayoutManager,
    private val _onLoadPreviousPage: () -> Unit,
    private val _onLoadNextPage: () -> Unit,
    private val _isLoading: () -> Boolean,
    private val _onStateIdle: (Boolean) -> Unit,
    private val _maxItems: () -> Int
) : RecyclerView.OnScrollListener() {
  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    super.onScrolled(recyclerView, dx, dy)
    if (!_isLoading() &&
        dy > 0 &&
        _layoutManager.findLastVisibleItemPosition() >= _maxItems() - threshold) {
      _onLoadNextPage()
    } else if (!_isLoading() &&
        dy < 0 &&
        _layoutManager.findFirstVisibleItemPosition() <= threshold) {
      _onLoadPreviousPage()
    }
  }

  override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
    super.onScrollStateChanged(recyclerView, newState)
    _onStateIdle(newState == RecyclerView.SCROLL_STATE_IDLE)
  }
}
