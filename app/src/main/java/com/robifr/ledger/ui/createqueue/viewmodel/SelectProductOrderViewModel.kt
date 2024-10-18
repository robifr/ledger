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

package com.robifr.ledger.ui.createqueue.viewmodel

import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData

class SelectProductOrderViewModel(private val _viewModel: CreateQueueViewModel) {
  private val _uiState: SafeMutableLiveData<SelectProductOrderState> =
      SafeMutableLiveData(SelectProductOrderState(setOf()))
  val uiState: SafeLiveData<SelectProductOrderState>
    get() = _uiState

  fun onProductOrderCheckedChanged(productOrderIndex: Int, isChecked: Boolean) {
    _uiState.setValue(
        _uiState.safeValue.copy(
            selectedIndexes =
                _uiState.safeValue.selectedIndexes.toMutableSet().apply {
                  if (isChecked) add(productOrderIndex) else remove(productOrderIndex)
                }))
  }

  fun onDeleteSelectedProductOrder() {
    _viewModel.onProductOrdersChanged(
        _viewModel.uiState.safeValue.productOrders.toMutableList().apply {
          // Sort the index beforehand, ensuring everything is removed from end to front.
          _uiState.safeValue.selectedIndexes.sortedDescending().forEach { removeAt(it) }
        })
    onDisableContextualMode()
  }

  fun onDisableContextualMode() {
    _uiState.setValue(SelectProductOrderState(setOf()))
  }
}
