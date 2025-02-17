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

import androidx.lifecycle.ViewModel
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.display.QueueFilters
import com.robifr.ledger.data.display.QueueSortMethod
import com.robifr.ledger.data.model.CustomerBalanceInfo
import com.robifr.ledger.data.model.CustomerDebtInfo
import com.robifr.ledger.data.model.ProductOrderProductInfo
import com.robifr.ledger.data.model.QueueDateInfo
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.data.model.QueuePaginatedInfo
import com.robifr.ledger.di.IoDispatcher
import com.robifr.ledger.repository.CustomerRepository
import com.robifr.ledger.repository.ProductOrderRepository
import com.robifr.ledger.repository.QueueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
    @IoDispatcher private val _dispatcher: CoroutineDispatcher,
    private val _queueRepository: QueueRepository,
    private val _productOrderRepository: ProductOrderRepository,
    private val _customerRepository: CustomerRepository
) : ViewModel() {
  val summaryView: DashboardSummaryViewModel =
      DashboardSummaryViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllQueuesInRange = { startDate, endDate ->
            _selectAllQueuesInRange(startDate, endDate, false)
          },
          _selectAllProductsSoldInRange = ::_selectAllProductsSoldInRange,
          _selectDateInfoById = ::_selectDateInfoById)
  val revenueView: DashboardRevenueViewModel =
      DashboardRevenueViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllQueuesInRange = { startDate, endDate ->
            _selectAllQueuesInRange(startDate, endDate, true)
          })
  val balanceView: DashboardBalanceViewModel =
      DashboardBalanceViewModel(
          _viewModel = this,
          _dispatcher = _dispatcher,
          _selectAllCustomersWithBalance = { _selectAllCustomersWithBalance() },
          _selectAllCustomersWithDebt = { _selectAllCustomersWithDebt() })

  init {
    _queueRepository.addModelChangedListener(summaryView._queueChangedListener)
    _queueRepository.addModelChangedListener(revenueView._queueChangedListener)
    _productOrderRepository.addModelChangedListener(summaryView._productOrderChangedListener)
    _customerRepository.addModelChangedListener(balanceView._customerBalanceChangedListener)
    _customerRepository.addModelChangedListener(balanceView._customerDebtChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    summaryView._loadAllQueuesInRange()
    revenueView._loadAllQueuesInRange()
    balanceView._loadAllCustomersWithBalance()
    balanceView._loadAllCustomersWithDebt()
  }

  override fun onCleared() {
    _queueRepository.removeModelChangedListener(summaryView._queueChangedListener)
    _queueRepository.removeModelChangedListener(revenueView._queueChangedListener)
    _productOrderRepository.removeModelChangedListener(summaryView._productOrderChangedListener)
    _customerRepository.removeModelChangedListener(balanceView._customerBalanceChangedListener)
    _customerRepository.removeModelChangedListener(balanceView._customerDebtChangedListener)
  }

  private suspend fun _selectAllQueuesInRange(
      startDate: ZonedDateTime,
      endDate: ZonedDateTime,
      shouldCalculateGrandTotalPrice: Boolean
  ): List<QueuePaginatedInfo> =
      _queueRepository.selectAllPaginatedInfo(
          sortMethod = QueueSortMethod(QueueSortMethod.SortBy.DATE, true),
          filters =
              QueueFilters(
                  filteredCustomerIds = listOf(),
                  isNullCustomerShown = true,
                  filteredStatus = QueueModel.Status.entries.toSet(),
                  filteredDate = QueueDate(startDate, endDate),
                  filteredTotalPrice = null to null),
          shouldCalculateGrandTotalPrice = shouldCalculateGrandTotalPrice)

  private suspend fun _selectAllProductsSoldInRange(
      dateStart: ZonedDateTime,
      dateEnd: ZonedDateTime
  ): List<ProductOrderProductInfo> =
      _productOrderRepository.selectAllProductInfoInRange(dateStart, dateEnd)

  private suspend fun _selectDateInfoById(queueIds: List<Long>): List<QueueDateInfo> =
      _queueRepository.selectDateInfoById(queueIds = queueIds)

  private suspend fun _selectAllCustomersWithBalance(): List<CustomerBalanceInfo> =
      _customerRepository.selectAllBalanceInfoWithBalance()

  private suspend fun _selectAllCustomersWithDebt(): List<CustomerDebtInfo> =
      _customerRepository.selectAllDebtInfoWithDebt()
}
