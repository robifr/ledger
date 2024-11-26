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

import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData

class DashboardBalanceViewModel {
  private val _uiState: SafeMutableLiveData<DashboardBalanceState> =
      SafeMutableLiveData(
          DashboardBalanceState(customersWithBalance = listOf(), customersWithDebt = listOf()))
  val uiState: SafeLiveData<DashboardBalanceState>
    get() = _uiState

  internal fun _onCustomersWithBalanceChanged(balanceInfo: List<CustomerBalanceInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(customersWithBalance = balanceInfo))
  }

  internal fun _onCustomersWithDebtChanged(debtInfo: List<CustomerDebtInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(customersWithDebt = debtInfo))
  }
}
