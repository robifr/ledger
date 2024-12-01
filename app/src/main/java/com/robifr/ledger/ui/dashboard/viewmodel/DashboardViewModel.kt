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

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.InfoSyncListener
import com.robifr.ledger.repository.QueueRepository
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.SnackbarState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    @IoDispatcher internal val _dispatcher: CoroutineDispatcher,
    private val _queueRepository: QueueRepository,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  val balanceView: DashboardBalanceViewModel = DashboardBalanceViewModel()
  private val _customerBalanceChangedListener:
      InfoSyncListener<CustomerBalanceInfo, CustomerModel> =
      InfoSyncListener(
          currentInfo = { balanceView.uiState.safeValue.customersWithBalance },
          modelToInfo = ::CustomerBalanceInfo,
          onSyncInfo = { syncedInfo ->
            balanceView._onCustomersWithBalanceChanged(syncedInfo.filter { it.balance != 0L })
          })
  private val _customerDebtChangedListener: InfoSyncListener<CustomerDebtInfo, CustomerModel> =
      InfoSyncListener(
          currentInfo = { balanceView.uiState.safeValue.customersWithDebt },
          modelToInfo = ::CustomerDebtInfo,
          onSyncInfo = { syncedInfo ->
            balanceView._onCustomersWithDebtChanged(
                syncedInfo.filter { it.debt.compareTo(0.toBigDecimal()) != 0 })
          })

  private val _snackbarState: SingleLiveEvent<SnackbarState> = SingleLiveEvent()
  val snackbarState: LiveData<SnackbarState>
    get() = _snackbarState

  val summaryView: DashboardSummaryViewModel =
      DashboardSummaryViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllQueuesInRange = ::_selectAllQueuesInRange)
  val revenueView: DashboardRevenueViewModel =
      DashboardRevenueViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllQueuesInRange = ::_selectAllQueuesInRange)

  init {
    _queueRepository.addModelChangedListener(summaryView._queueChangedListener)
    _queueRepository.addModelChangedListener(revenueView._queueChangedListener)
    _customerRepository.addModelChangedListener(_customerBalanceChangedListener)
    _customerRepository.addModelChangedListener(_customerDebtChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    summaryView._loadAllQueuesInRange()
    revenueView._loadAllQueuesInRange()
    _loadAllCustomersWithBalance()
    _loadAllCustomersWithDebt()
  }

  override fun onCleared() {
    _queueRepository.removeModelChangedListener(summaryView._queueChangedListener)
    _queueRepository.removeModelChangedListener(revenueView._queueChangedListener)
    _customerRepository.removeModelChangedListener(_customerBalanceChangedListener)
    _customerRepository.removeModelChangedListener(_customerDebtChangedListener)
  }

  private suspend fun _selectAllQueuesInRange(
      startDate: ZonedDateTime,
      endDate: ZonedDateTime
  ): List<QueueModel> = _queueRepository.selectAllInRange(startDate, endDate).await()

  private suspend fun _selectAllCustomersWithBalance(): List<CustomerBalanceInfo> =
      _customerRepository.selectAllInfoWithBalance().await()

  private suspend fun _selectAllCustomersWithDebt(): List<CustomerDebtInfo> =
      _customerRepository.selectAllInfoWithDebt().await()

  private fun _loadAllCustomersWithBalance() {
    viewModelScope.launch(_dispatcher) {
      _selectAllCustomersWithBalance().let {
        withContext(Dispatchers.Main) { balanceView._onCustomersWithBalanceChanged(it) }
      }
    }
  }

  private fun _loadAllCustomersWithDebt() {
    viewModelScope.launch(_dispatcher) {
      _selectAllCustomersWithDebt().let {
        withContext(Dispatchers.Main) { balanceView._onCustomersWithDebtChanged(it) }
      }
    }
  }
}
