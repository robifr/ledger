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
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.repository.InfoSyncListener
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardBalanceViewModel(
    private val _viewModel: DashboardViewModel,
    private val _dispatcher: CoroutineDispatcher,
    private val _selectAllCustomersWithBalance: suspend () -> List<CustomerBalanceInfo>,
    private val _selectAllCustomersWithDebt: suspend () -> List<CustomerDebtInfo>
) {
  val _customerBalanceChangedListener: InfoSyncListener<CustomerBalanceInfo, CustomerModel> =
      InfoSyncListener(
          currentInfo = { _uiState.safeValue.customersWithBalance },
          modelToInfo = ::CustomerBalanceInfo,
          onSyncInfo = { syncedInfo ->
            _onCustomersWithBalanceChanged(syncedInfo.filter { it.balance != 0L })
          })
  val _customerDebtChangedListener: InfoSyncListener<CustomerDebtInfo, CustomerModel> =
      InfoSyncListener(
          currentInfo = { uiState.safeValue.customersWithDebt },
          modelToInfo = ::CustomerDebtInfo,
          onSyncInfo = { syncedInfo ->
            _onCustomersWithDebtChanged(
                syncedInfo.filter { it.debt.compareTo(0.toBigDecimal()) != 0 })
          })

  private val _uiState: SafeMutableLiveData<DashboardBalanceState> =
      SafeMutableLiveData(
          DashboardBalanceState(customersWithBalance = listOf(), customersWithDebt = listOf()))
  val uiState: SafeLiveData<DashboardBalanceState>
    get() = _uiState

  fun _loadAllCustomersWithBalance() {
    _viewModel.viewModelScope.launch(_dispatcher) {
      _selectAllCustomersWithBalance().let {
        withContext(Dispatchers.Main) { _onCustomersWithBalanceChanged(it) }
      }
    }
  }

  fun _loadAllCustomersWithDebt() {
    _viewModel.viewModelScope.launch(_dispatcher) {
      _selectAllCustomersWithDebt().let {
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
