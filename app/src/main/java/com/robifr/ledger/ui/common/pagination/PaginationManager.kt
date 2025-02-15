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

package com.robifr.ledger.ui.common.pagination

import androidx.recyclerview.widget.RecyclerView
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// If you ever come here wondering why the recycler view's holder doesn't update or seems to do
// nothing. Try adjusting the `threshold` in `PaginationScrollListener`. Sometimes that helps.
class PaginationManager<T>(
    val state: () -> PaginationState<T>,
    val onStateChanged: (PaginationState<T>) -> Unit,
    val maxPaginationLimit: Int = 20,
    val maxItemInMemory: Int = maxPaginationLimit * 3,
    private val _coroutineScope: CoroutineScope,
    private val _dispatcher: CoroutineDispatcher,
    private val _onNotifyRecyclerState: (RecyclerAdapterState) -> Unit,
    private val _countTotalItem: suspend () -> Long,
    private val _selectItemsByPageOffset: suspend (pageNumber: Int, limit: Int) -> List<T>
) {
  /**
   * Used to notify that the old items which are in between removed and added ones have changed.
   * However, calling [notifyItemRangeChanged][RecyclerView.Adapter.notifyItemRangeChanged] is a bit
   * expensive, especially when the user scrolls too quickly. The solution to this issue is to
   * notify changes only when the recycler view's scroll state is
   * [SCROLL_STATE_IDLE][RecyclerView.SCROLL_STATE_IDLE]. Use
   * [onRecyclerStateIdleNotifyItemRangeChanged] whenever the state changes.
   */
  private var _bufferedItemRangeChanged: RecyclerAdapterState.ItemRangeChanged? = null

  fun onRecyclerStateIdleNotifyItemRangeChanged(isIdle: Boolean) {
    onStateChanged(state().copy(isRecyclerStateIdle = isIdle))
    if (isIdle) {
      _bufferedItemRangeChanged?.let {
        _onNotifyRecyclerState(it)
        _bufferedItemRangeChanged = null
      }
    }
  }

  fun onLoadPreviousPage(onLoad: (newItems: List<T>) -> Unit = {}) {
    if (state().isLoading) return
    onStateChanged(state().copy(isLoading = true))
    _coroutineScope.launch(_dispatcher) {
      val firstLoadedPageNumber: Int = max(1, state().firstLoadedPageNumber - 1)
      // Prevent the same paginated items to be loaded.
      if (state().firstLoadedPageNumber == firstLoadedPageNumber) {
        withContext(Dispatchers.Main) { onStateChanged(state().copy(isLoading = false)) }
        return@launch
      }

      val totalItem: Long = _countTotalItem()
      _loadPagedItems(firstLoadedPageNumber, maxPaginationLimit) { newItems ->
        val items: MutableList<T> =
            state().paginatedItems.toMutableList().apply { addAll(0, newItems) }
        // Manage number of item in memory by removing from the last.
        val isItemsInLastPageRemoved: Boolean = items.size > maxItemInMemory
        if (isItemsInLastPageRemoved) items.subList(maxItemInMemory, items.size).clear()
        onStateChanged(
            state()
                .copy(
                    isLoading = false,
                    firstLoadedPageNumber = firstLoadedPageNumber,
                    // The gap between first and last page should only expands whenever a new page
                    // is loaded. The maximum gap can be calculated as `maxItemInMemory` /
                    // `maxPaginationLimit`.
                    lastLoadedPageNumber =
                        if (isItemsInLastPageRemoved) {
                          min(
                              firstLoadedPageNumber + (maxItemInMemory / maxPaginationLimit) - 1,
                              ceil(totalItem.toDouble() / maxPaginationLimit.toDouble()).toInt())
                        } else {
                          state().lastLoadedPageNumber
                        },
                    paginatedItems = items))
        if (isItemsInLastPageRemoved) {
          _onNotifyRecyclerState(
              RecyclerAdapterState.ItemRangeRemoved(items.size - newItems.size, newItems.size))
        }
        _onNotifyRecyclerState(RecyclerAdapterState.ItemRangeInserted(0, newItems.size))
        // It's important to notify that the old items which are in between removed and added
        // ones have changed. Otherwise, there will be a weird bug like when clicking an item to
        // expand and they do nothing. Don't set the `threshold` in `PaginationScrollListener` to
        // zero if you want to make it more noticeable. Scroll the list (the lag happens because
        // of those threshold) and click to expand the last visible item.
        _bufferedItemRangeChanged =
            RecyclerAdapterState.ItemRangeChanged(newItems.size, items.size - newItems.size, Unit)
        onLoad(newItems)
      }
    }
  }

  fun onLoadNextPage(onLoad: (newItems: List<T>) -> Unit = {}) {
    if (state().isLoading) return
    onStateChanged(state().copy(isLoading = true))
    _coroutineScope.launch(_dispatcher) {
      val totalItem: Long = _countTotalItem()
      val lastLoadedPageNumber: Int =
          min(
              state().lastLoadedPageNumber + 1,
              ceil(totalItem.toDouble() / maxPaginationLimit.toDouble()).toInt())
      // Prevent the same paginated items to be loaded.
      if (state().lastLoadedPageNumber == lastLoadedPageNumber) {
        withContext(Dispatchers.Main) { onStateChanged(state().copy(isLoading = false)) }
        return@launch
      }

      _loadPagedItems(lastLoadedPageNumber, maxPaginationLimit) { newItems ->
        val items: MutableList<T> =
            state().paginatedItems.toMutableList().apply { addAll(newItems) }
        // Manage number of item in memory by removing from the first.
        val totalRemoved: Int = items.size - maxItemInMemory
        val isItemsInFirstPageRemoved: Boolean = items.size > maxItemInMemory
        if (isItemsInFirstPageRemoved) items.subList(0, totalRemoved).clear()
        onStateChanged(
            state()
                .copy(
                    isLoading = false,
                    // The gap between first and last page should only expands whenever a new page
                    // is loaded. The maximum gap can be calculated as `maxItemInMemory` /
                    // `maxPaginationLimit`.
                    firstLoadedPageNumber =
                        if (isItemsInFirstPageRemoved) {
                          max(1, lastLoadedPageNumber - (maxItemInMemory / maxPaginationLimit) + 1)
                        } else {
                          state().firstLoadedPageNumber
                        },
                    lastLoadedPageNumber = lastLoadedPageNumber,
                    paginatedItems = items))
        if (isItemsInFirstPageRemoved) {
          _onNotifyRecyclerState(RecyclerAdapterState.ItemRangeRemoved(0, totalRemoved))
          // It's important to notify that the old items which are in between removed and added
          // ones have changed. Otherwise, there will be a weird bug like when clicking an item to
          // expand and they do nothing. It's more noticeable when you set the `threshold` in
          // `PaginationScrollListener` to zero. Scroll the list (the lag happens because of those
          // threshold) and click to expand the last visible item.
          _bufferedItemRangeChanged =
              RecyclerAdapterState.ItemRangeChanged(totalRemoved, totalRemoved, Unit)
        }
        _onNotifyRecyclerState(
            RecyclerAdapterState.ItemRangeInserted(items.size - totalRemoved, newItems.size))
        onLoad(newItems)
      }
    }
  }

  suspend fun onReloadPage(
      firstLoadedPageNumber: Int,
      lastLoadedPageNumber: Int,
      onLoad: (fetchedItems: List<T>) -> Unit
  ) {
    val totalItem: Long = _countTotalItem()
    _loadPagedItems(
        firstLoadedPageNumber,
        // Load items for pages in the inclusive range of first and last page.
        maxPaginationLimit * max(1, lastLoadedPageNumber - firstLoadedPageNumber + 1)) {
          onStateChanged(
              state()
                  .copy(
                      firstLoadedPageNumber = firstLoadedPageNumber,
                      lastLoadedPageNumber = lastLoadedPageNumber,
                      paginatedItems = it,
                      totalItem = totalItem))
          _onNotifyRecyclerState(RecyclerAdapterState.DataSetChanged)
          onLoad(it)
        }
  }

  private suspend fun _loadPagedItems(
      pageNumber: Int,
      limit: Int,
      onLoad: (fetchedItems: List<T>) -> Unit
  ) {
    val items: List<T> = _selectItemsByPageOffset(pageNumber, limit)
    withContext(Dispatchers.Main) { onLoad(items) }
  }
}
