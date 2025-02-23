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

package com.robifr.ledger.ui.dashboard.viewmodel

import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.InfoSynchronizer
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardBalanceViewModel(
    private val _viewModel: DashboardViewModel,
    private val _dispatcher: CoroutineDispatcher
) {
  val customerBalanceChangedListener: ModelSyncListener<CustomerModel, CustomerBalanceInfo> =
      ModelSyncListener(
          onAdd = {
            InfoSynchronizer.addInfo(
                _uiState.safeValue.customersWithBalance, it, ::CustomerBalanceInfo)
          },
          onUpdate = {
            InfoSynchronizer.updateInfo(
                _uiState.safeValue.customersWithBalance, it, ::CustomerBalanceInfo)
          },
          onDelete = { InfoSynchronizer.deleteInfo(_uiState.safeValue.customersWithBalance, it) },
          onUpsert = {
            InfoSynchronizer.upsertInfo(
                _uiState.safeValue.customersWithBalance, it, ::CustomerBalanceInfo)
          },
          onSync = { _, updatedModels ->
            _onCustomersWithBalanceChanged(updatedModels.filter { it.balance != 0L })
          })
  val customerDebtChangedListener: ModelSyncListener<CustomerModel, CustomerDebtInfo> =
      ModelSyncListener(
          onAdd = {
            InfoSynchronizer.addInfo(_uiState.safeValue.customersWithDebt, it, ::CustomerDebtInfo)
          },
          onUpdate = {
            InfoSynchronizer.updateInfo(
                _uiState.safeValue.customersWithDebt, it, ::CustomerDebtInfo)
          },
          onDelete = { InfoSynchronizer.deleteInfo(_uiState.safeValue.customersWithDebt, it) },
          onUpsert = {
            InfoSynchronizer.upsertInfo(
                _uiState.safeValue.customersWithDebt, it, ::CustomerDebtInfo)
          },
          onSync = { _, updatedModels ->
            _onCustomersWithDebtChanged(
                updatedModels.filter { it.debt.compareTo(0.toBigDecimal()) != 0 })
          })

  private val _uiState: SafeMutableLiveData<DashboardBalanceState> =
      SafeMutableLiveData(
          DashboardBalanceState(customersWithBalance = listOf(), customersWithDebt = listOf()))
  val uiState: SafeLiveData<DashboardBalanceState>
    get() = _uiState

  fun loadAllCustomersWithBalance() {
    _viewModel.viewModelScope.launch(_dispatcher) {
      _viewModel.selectAllCustomersWithBalance().let {
        withContext(Dispatchers.Main) { _onCustomersWithBalanceChanged(it) }
      }
    }
  }

  fun loadAllCustomersWithDebt() {
    _viewModel.viewModelScope.launch(_dispatcher) {
      _viewModel.selectAllCustomersWithDebt().let {
        withContext(Dispatchers.Main) { _onCustomersWithDebtChanged(it) }
      }
    }
  }

  private fun _onCustomersWithBalanceChanged(balanceInfo: List<CustomerBalanceInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(customersWithBalance = balanceInfo))
  }

  private fun _onCustomersWithDebtChanged(debtInfo: List<CustomerDebtInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(customersWithDebt = debtInfo))
  }
}
