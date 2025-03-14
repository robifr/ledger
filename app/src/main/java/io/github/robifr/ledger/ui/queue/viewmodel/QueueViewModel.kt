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

package io.github.robifr.ledger.ui.queue.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.QueueSortMethod
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.di.IoDispatcher
import io.github.robifr.ledger.repository.CustomerRepository
import io.github.robifr.ledger.repository.ModelSyncListener
import io.github.robifr.ledger.repository.QueueRepository
import io.github.robifr.ledger.ui.common.PluralResource
import io.github.robifr.ledger.ui.common.StringResource
import io.github.robifr.ledger.ui.common.StringResourceType
import io.github.robifr.ledger.ui.common.pagination.PaginationManager
import io.github.robifr.ledger.ui.common.pagination.PaginationState
import io.github.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.github.robifr.ledger.ui.common.state.SafeLiveData
import io.github.robifr.ledger.ui.common.state.SafeMutableLiveData
import io.github.robifr.ledger.ui.common.state.SnackbarState
import io.github.robifr.ledger.ui.common.state.updateEvent
import io.github.robifr.ledger.ui.main.RequiredPermission
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class QueueViewModel(
    maxPaginatedItemPerPage: Int = 20,
    maxPaginatedItemInMemory: Int = maxPaginatedItemPerPage * 3,
    private val _dispatcher: CoroutineDispatcher,
    private val _queueRepository: QueueRepository,
    private val _customerRepository: CustomerRepository,
    private val _permission: RequiredPermission
) : ViewModel() {
  private var _expandedQueueJob: Job? = null
  private val _paginationManager: PaginationManager<QueuePaginatedInfo> =
      PaginationManager(
          state = { _uiState.safeValue.pagination },
          onStateChanged = { _uiState.setValue(_uiState.safeValue.copy(pagination = it)) },
          maxItemPerPage = maxPaginatedItemPerPage,
          maxItemInMemory = maxPaginatedItemInMemory,
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
            _queueRepository.countFilteredQueues(filterView.parseInputtedFilters())
          },
          _selectItemsByPageOffset = { pageNumber, limit ->
            _queueRepository.selectPaginatedInfoByOffset(
                pageNumber = pageNumber,
                itemPerPage = _paginationManager.maxItemPerPage,
                limit = limit,
                sortMethod = _uiState.safeValue.sortMethod,
                filters = filterView.parseInputtedFilters())
          })
  private val _queueChangedListener: ModelSyncListener<QueueModel, Unit> =
      ModelSyncListener(
          onSync = { _, _ ->
            viewModelScope.launch(_dispatcher) {
              val expandedQueue: QueueModel? =
                  _queueRepository.selectById(_uiState.safeValue.expandedQueue?.id)
              onReloadPage(
                  _uiState.safeValue.pagination.firstLoadedPageNumber,
                  _uiState.safeValue.pagination.lastLoadedPageNumber) {
                    // Update current expanded queue in case they're updated.
                    _uiState.setValue(_uiState.safeValue.copy(expandedQueue = expandedQueue))
                    _onPageLoadedReloadExpandedQueue()
                  }
            }
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
                      totalItem = 0L,
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

  val filterView: QueueFilterViewModel by lazy { QueueFilterViewModel(this, _dispatcher) }

  @Inject
  constructor(
      @IoDispatcher dispatcher: CoroutineDispatcher,
      queueRepository: QueueRepository,
      customerRepository: CustomerRepository,
      permission: RequiredPermission
  ) : this(
      _dispatcher = dispatcher,
      _queueRepository = queueRepository,
      _customerRepository = customerRepository,
      _permission = permission)

  init {
    _queueRepository.addModelChangedListener(_queueChangedListener)
    _customerRepository.addModelChangedListener(_customerChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    // Queue fragment is also the first fragment to be loaded during the initial app run.
    // It's essential to ensure that all necessary permissions are granted.
    if (_permission.isStorageAccessGranted()) {
      viewModelScope.launch(_dispatcher) { onReloadPage(1, 1) }
    }
  }

  override fun onCleared() {
    _queueRepository.removeModelChangedListener(_queueChangedListener)
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onLoadPreviousPage() {
    _paginationManager.onLoadPreviousPage { _onPageLoadedReloadExpandedQueue() }
  }

  fun onLoadNextPage() {
    _paginationManager.onLoadNextPage { _onPageLoadedReloadExpandedQueue() }
  }

  fun onRecyclerStateIdle(isIdle: Boolean) {
    _paginationManager.onRecyclerStateIdleNotifyItemRangeChanged(isIdle)
  }

  fun onExpandedQueueIndexChanged(index: Int) {
    _expandedQueueJob?.cancel()
    _expandedQueueJob =
        viewModelScope.launch(_dispatcher) {
          delay(150)
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
    viewModelScope.launch(_dispatcher) { onReloadPage(1, 1) }
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

  suspend fun onReloadPage(
      firstVisiblePageNumber: Int,
      lastVisiblePageNumber: Int,
      onLoad: (List<QueuePaginatedInfo>) -> Unit = {}
  ) {
    val isTableEmpty: Boolean = _queueRepository.isTableEmpty()
    _paginationManager.onReloadPage(firstVisiblePageNumber, lastVisiblePageNumber) {
      _onNoQueuesCreatedIllustrationVisible(isTableEmpty)
      onLoad(it)
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

  private fun _onPageLoadedReloadExpandedQueue() {
    if (_uiState.safeValue.expandedQueue == null) return
    // Re-expand current expanded queue and load full model of it.
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

  private suspend fun _selectQueueForIndex(index: Int): QueueModel? {
    val queueToFetch: QueuePaginatedInfo =
        _uiState.safeValue.pagination.paginatedItems.getOrNull(index) ?: return null
    if (queueToFetch.fullModel != null) return queueToFetch.fullModel
    return _queueRepository.selectById(queueToFetch.id)
  }
}
