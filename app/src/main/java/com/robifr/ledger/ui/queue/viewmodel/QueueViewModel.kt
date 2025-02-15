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

package com.robifr.ledger.ui.queue.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.R
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.data.model.QueuePaginatedInfo
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.common.PluralResource
import com.robifr.ledger.ui.common.StringResource
import com.robifr.ledger.ui.common.StringResourceType
import com.robifr.ledger.ui.common.pagination.PaginationManager
import com.robifr.ledger.ui.common.pagination.PaginationState
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.SnackbarState
import com.robifr.ledger.ui.common.state.updateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class QueueViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _queueRepository: QueueRepository,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  private var _expandedQueueJob: Job? = null
  private val _paginationManager: PaginationManager<QueuePaginatedInfo> =
      PaginationManager(
          state = { _uiState.safeValue.pagination },
          onStateChanged = { _uiState.setValue(_uiState.safeValue.copy(pagination = it)) },
          _coroutineScope = viewModelScope,
          _dispatcher = _dispatcher,
          _onNotifyRecyclerState = {
            _onRecyclerAdapterRefreshed(
                when (it) {
                  // +1 offset because header holder.
                  is RecyclerAdapterState.ItemRangeChanged ->
                      RecyclerAdapterState.ItemRangeChanged(
                          it.positionStart + 1, it.itemCount, it.payload)
                  is RecyclerAdapterState.ItemRangeInserted ->
                      RecyclerAdapterState.ItemRangeInserted(it.positionStart + 1, it.itemCount)
                  is RecyclerAdapterState.ItemRangeRemoved ->
                      RecyclerAdapterState.ItemRangeRemoved(it.positionStart + 1, it.itemCount)
                  else -> it
                })
          },
          _countTotalItem = {
            _queueRepository.countFilteredQueues(filterView._parseInputtedFilters())
          },
          _selectItemsByPageOffset = { pageNumber, limit ->
            // Queue fragment is the first fragment to be loaded during the initial app run.
            // It's essential to ensure that all necessary permissions are granted.
            if (Environment.isExternalStorageManager()) {
              _queueRepository.selectPaginatedInfoByOffset(
                  pageNumber,
                  limit,
                  _uiState.safeValue.sortMethod,
                  filterView._parseInputtedFilters())
            } else {
              listOf()
            }
          })
  private val _queueChangedListener: ModelSyncListener<QueueModel, Unit> =
      ModelSyncListener(
          onSync = { _, _ ->
            _onReloadPage(
                _uiState.safeValue.pagination.firstLoadedPageNumber,
                _uiState.safeValue.pagination.lastLoadedPageNumber)
          })
  private val _customerChangedListener: CustomerSyncListener =
      CustomerSyncListener(
          currentQueues = { _uiState.safeValue.pagination.paginatedItems },
          onSyncQueues = {
            _paginationManager.onStateChanged(
                _uiState.safeValue.pagination.copy(paginatedItems = it))
            _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
          })

  private val _uiEvent: SafeMutableLiveData<QueueEvent> = SafeMutableLiveData(QueueEvent())
  val uiEvent: SafeLiveData<QueueEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<QueueState> =
      SafeMutableLiveData(
          QueueState(
              pagination =
                  PaginationState(
                      isLoading = false,
                      firstLoadedPageNumber = 1,
                      lastLoadedPageNumber = 1,
                      isRecyclerStateIdle = false,
                      paginatedItems = listOf(),
                      totalItem = 0,
                  ),
              expandedQueueIndex = -1,
              expandedQueue = null,
              isQueueMenuDialogShown = false,
              selectedQueueMenu = null,
              isNoQueuesCreatedIllustrationVisible = false,
              sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, false),
              isSortMethodDialogShown = false))
  val uiState: SafeLiveData<QueueState>
    get() = _uiState

  val filterView: QueueFilterViewModel = QueueFilterViewModel { _onReloadPage(1, 1) }

  init {
    _queueRepository.addModelChangedListener(_queueChangedListener)
    _customerRepository.addModelChangedListener(_customerChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _onReloadPage(1, 1)
  }

  override fun onCleared() {
    _queueRepository.removeModelChangedListener(_queueChangedListener)
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onLoadPreviousPage() {
    _paginationManager.onLoadPreviousPage {
      if (_uiState.safeValue.expandedQueue == null) return@onLoadPreviousPage
      // Load full model of current current expanded queue.
      val expandedQueueIndex: Int =
          _uiState.safeValue.pagination.paginatedItems.indexOfFirst {
            it.id == _uiState.safeValue.expandedQueue?.id
          }
      if (expandedQueueIndex != -1) {
        _onLoadFullQueue(expandedQueueIndex, _uiState.safeValue.expandedQueue)
        // +1 offset because header holder.
        _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(expandedQueueIndex + 1))
      }
    }
  }

  fun onLoadNextPage() {
    _paginationManager.onLoadNextPage {
      if (_uiState.safeValue.expandedQueue == null) return@onLoadNextPage
      // Load full model of current current expanded queue.
      val expandedQueueIndex: Int =
          _uiState.safeValue.pagination.paginatedItems.indexOfFirst {
            it.id == _uiState.safeValue.expandedQueue?.id
          }
      if (expandedQueueIndex != -1) {
        _onLoadFullQueue(expandedQueueIndex, _uiState.safeValue.expandedQueue)
        // +1 offset because header holder.
        _onRecyclerAdapterRefreshed(RecyclerAdapterState.ItemChanged(expandedQueueIndex + 1))
      }
    }
  }

  fun onRecyclerStateIdle(isIdle: Boolean) {
    _paginationManager.onRecyclerStateIdleNotifyItemRangeChanged(isIdle)
  }

  fun onExpandedQueueIndexChanged(index: Int) {
    _expandedQueueJob?.cancel()
    _expandedQueueJob =
        viewModelScope.launch(_dispatcher) {
          delay(200)
          val previousExpandedIndex: Int = _uiState.safeValue.expandedQueueIndex
          val shouldExpand: Boolean = previousExpandedIndex != index
          val queue: QueueModel? = if (shouldExpand) _selectQueueForIndex(index) else null
          withContext(Dispatchers.Main) {
            // Load full model for current expanded queue.
            if (shouldExpand) _onLoadFullQueue(index, queue)
            _uiState.setValue(
                _uiState.safeValue.copy(
                    expandedQueueIndex = if (shouldExpand) index else -1, expandedQueue = queue))
            // Update both previous and current expanded queue. +1 offset because header holder.
            _onRecyclerAdapterRefreshed(
                RecyclerAdapterState.ItemChanged(
                    listOfNotNull(
                        previousExpandedIndex.takeIf { it != -1 && it != index }?.inc(),
                        index + 1)))
          }
        }
  }

  fun onQueueMenuDialogShown(selectedQueue: QueuePaginatedInfo) {
    _uiState.setValue(
        _uiState.safeValue.copy(isQueueMenuDialogShown = true, selectedQueueMenu = selectedQueue))
  }

  fun onQueueMenuDialogClosed() {
    _uiState.setValue(
        _uiState.safeValue.copy(isQueueMenuDialogShown = false, selectedQueueMenu = null))
  }

  fun onSortMethodChanged(sortMethod: QueueSortMethod) {
    _uiState.setValue(_uiState.safeValue.copy(sortMethod = sortMethod))
    _onReloadPage(1, 1)
  }

  /**
   * Sort [PaginationState.paginatedItems] based on specified [QueueSortMethod.SortBy] type. Doing
   * so will reverse the order — Ascending becomes descending and vice versa. Use
   * [onSortMethodChanged] that takes a [QueueSortMethod] if you want to apply the order by
   * yourself.
   */
  fun onSortMethodChanged(sortBy: QueueSortMethod.SortBy) {
    onSortMethodChanged(
        QueueSortMethod(
            sortBy,
            // Reverse sort order when selecting same sort option.
            if (_uiState.safeValue.sortMethod.sortBy == sortBy) {
              !_uiState.safeValue.sortMethod.isAscending
            } else {
              _uiState.safeValue.sortMethod.isAscending
            }))
  }

  fun onSortMethodDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = true))
  }

  fun onSortMethodDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = false))
  }

  fun onDeleteQueue(queueId: Long?) {
    viewModelScope.launch(_dispatcher) {
      _queueRepository.delete(queueId).let { effected ->
        _onSnackbarShown(
            if (effected > 0) {
              PluralResource(R.plurals.queue_deleted_n_queue, effected, effected)
            } else {
              StringResource(R.string.queue_deleteQueueError)
            })
      }
    }
  }

  private fun _onLoadFullQueue(index: Int, queue: QueueModel?) {
    val queueToUpdate: QueuePaginatedInfo =
        _uiState.safeValue.pagination.paginatedItems.getOrNull(index) ?: return
    _paginationManager.onStateChanged(
        _uiState.safeValue.pagination.copy(
            paginatedItems =
                _uiState.safeValue.pagination.paginatedItems.toMutableList().apply {
                  set(index, queueToUpdate.copy(fullModel = queue))
                }))
  }

  private fun _onNoQueuesCreatedIllustrationVisible(isVisible: Boolean) {
    _uiState.setValue(_uiState.safeValue.copy(isNoQueuesCreatedIllustrationVisible = isVisible))
  }

  private fun _onSnackbarShown(messageRes: StringResourceType) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = SnackbarState(messageRes),
          onSet = { this?.copy(snackbar = it) },
          onReset = { this?.copy(snackbar = null) })
    }
  }

  private fun _onRecyclerAdapterRefreshed(state: RecyclerAdapterState) {
    viewModelScope.launch {
      _uiEvent.updateEvent(
          data = state,
          onSet = { this?.copy(recyclerAdapter = it) },
          onReset = { this?.copy(recyclerAdapter = null) })
    }
  }

  private fun _onReloadPage(firstVisiblePageNumber: Int, lastVisiblePageNumber: Int) {
    viewModelScope.launch(_dispatcher) {
      val isTableEmpty: Boolean = _queueRepository.isTableEmpty()
      _paginationManager.onReloadPage(firstVisiblePageNumber, lastVisiblePageNumber) {
        _onNoQueuesCreatedIllustrationVisible(isTableEmpty)
      }
    }
  }

  private suspend fun _selectQueueForIndex(index: Int): QueueModel? {
    val queueToFetch: QueuePaginatedInfo =
        _uiState.safeValue.pagination.paginatedItems.getOrNull(index) ?: return null
    if (queueToFetch.fullModel != null) return queueToFetch.fullModel
    return _queueRepository.selectById(queueToFetch.id)
  }
}
