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
import com.robifr.ledger.data.display.QueueSorter
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.common.PluralResource
import com.robifr.ledger.ui.common.StringResource
import com.robifr.ledger.ui.common.StringResourceType
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.SnackbarState
import com.robifr.ledger.ui.common.state.updateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
  private val _sorter: QueueSorter = QueueSorter()
  private val _queueChangedListener: ModelSyncListener<QueueModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.queues },
          onSyncModels = {
            viewModelScope.launch(_dispatcher) {
              val isTableEmpty: Boolean = _queueRepository.isTableEmpty()
              withContext(Dispatchers.Main) {
                _onNoQueuesCreatedIllustrationVisible(isTableEmpty)
                filterView._onFiltersChanged(queues = it)
              }
            }
          })
  private val _customerChangedListener: CustomerSyncListener =
      CustomerSyncListener(
          currentQueues = { _uiState.safeValue.queues },
          onSyncQueues = { filterView._onFiltersChanged(queues = it) })

  private val _uiEvent: SafeMutableLiveData<QueueEvent> = SafeMutableLiveData(QueueEvent())
  val uiEvent: SafeLiveData<QueueEvent>
    get() = _uiEvent

  private val _uiState: SafeMutableLiveData<QueueState> =
      SafeMutableLiveData(
          QueueState(
              queues = listOf(),
              expandedQueueIndex = -1,
              isQueueMenuDialogShown = false,
              selectedQueueMenu = null,
              isNoQueuesCreatedIllustrationVisible = false,
              sortMethod = _sorter.sortMethod,
              isSortMethodDialogShown = false))
  val uiState: SafeLiveData<QueueState>
    get() = _uiState

  val filterView: QueueFilterViewModel =
      QueueFilterViewModel(
          _viewModel = this, _dispatcher = _dispatcher, _selectAllQueues = { _selectAllQueues() })

  init {
    _queueRepository.addModelChangedListener(_queueChangedListener)
    _customerRepository.addModelChangedListener(_customerChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    _loadAllQueues()
  }

  override fun onCleared() {
    _queueRepository.removeModelChangedListener(_queueChangedListener)
    _customerRepository.removeModelChangedListener(_customerChangedListener)
  }

  fun onQueuesChanged(queues: List<QueueModel>) {
    _uiState.setValue(_uiState.safeValue.copy(queues = _sorter.sort(queues)))
    _onRecyclerAdapterRefreshed(RecyclerAdapterState.DataSetChanged)
  }

  fun onExpandedQueueIndexChanged(index: Int) {
    // Update both previous and current expanded product. +1 offset because header holder.
    _onRecyclerAdapterRefreshed(
        RecyclerAdapterState.ItemChanged(
            listOfNotNull(
                _uiState.safeValue.expandedQueueIndex.takeIf { it != -1 && it != index }?.inc(),
                index + 1)))
    _uiState.setValue(
        _uiState.safeValue.copy(
            expandedQueueIndex = if (_uiState.safeValue.expandedQueueIndex != index) index else -1))
  }

  fun onQueueMenuDialogShown(selectedQueue: QueueModel) {
    _uiState.setValue(
        _uiState.safeValue.copy(isQueueMenuDialogShown = true, selectedQueueMenu = selectedQueue))
  }

  fun onQueueMenuDialogClosed() {
    _uiState.setValue(
        _uiState.safeValue.copy(isQueueMenuDialogShown = false, selectedQueueMenu = null))
  }

  fun onSortMethodChanged(
      sortMethod: QueueSortMethod,
      queues: List<QueueModel> = _uiState.safeValue.queues
  ) {
    _sorter.sortMethod = sortMethod
    _uiState.setValue(_uiState.safeValue.copy(sortMethod = sortMethod))
    onQueuesChanged(queues)
  }

  /**
   * Sort [QueueState.queues] based on specified [QueueSortMethod.SortBy] type. Doing so will
   * reverse the order — Ascending becomes descending and vice versa. Use [onSortMethodChanged] that
   * takes a [QueueSortMethod] if you want to apply the order by yourself.
   */
  fun onSortMethodChanged(
      sortBy: QueueSortMethod.SortBy,
      queues: List<QueueModel> = _uiState.safeValue.queues
  ) {
    onSortMethodChanged(
        QueueSortMethod(
            sortBy,
            // Reverse sort order when selecting same sort option.
            if (_uiState.safeValue.sortMethod.sortBy == sortBy) {
              !_uiState.safeValue.sortMethod.isAscending
            } else {
              _uiState.safeValue.sortMethod.isAscending
            }),
        queues)
  }

  fun onSortMethodDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = true))
  }

  fun onSortMethodDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isSortMethodDialogShown = false))
  }

  fun onDeleteQueue(queue: QueueModel) {
    viewModelScope.launch(_dispatcher) {
      _queueRepository.delete(queue).let { effected ->
        _onSnackbarShown(
            if (effected > 0) {
              PluralResource(R.plurals.queue_deleted_n_queue, effected, effected)
            } else {
              StringResource(R.string.queue_deleteQueueError)
            })
      }
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

  private suspend fun _selectAllQueues(): List<QueueModel> = _queueRepository.selectAll()

  private fun _loadAllQueues() {
    viewModelScope.launch(_dispatcher) {
      // Queue fragment is the first fragment to be loaded during the initial app run.
      // It's essential to ensure that all necessary permissions are granted.
      val queues: List<QueueModel> =
          if (Environment.isExternalStorageManager()) _selectAllQueues() else listOf()
      val isTableEmpty: Boolean = _queueRepository.isTableEmpty()
      withContext(Dispatchers.Main) {
        _onNoQueuesCreatedIllustrationVisible(isTableEmpty)
        filterView._onFiltersChanged(queues = queues)
      }
    }
  }
}
