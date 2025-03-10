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

package io.github.robifr.ledger.ui.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.display.QueueFilters
import io.github.robifr.ledger.data.display.QueueSortMethod
import io.github.robifr.ledger.data.model.CustomerBalanceInfo
import io.github.robifr.ledger.data.model.CustomerDebtInfo
import io.github.robifr.ledger.data.model.ProductOrderProductInfo
import io.github.robifr.ledger.data.model.QueueDateInfo
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.di.IoDispatcher
import io.github.robifr.ledger.repository.CustomerRepository
import io.github.robifr.ledger.repository.ProductOrderRepository
import io.github.robifr.ledger.repository.QueueRepository
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
  val summaryView: DashboardSummaryViewModel = DashboardSummaryViewModel(this, _dispatcher)
  val revenueView: DashboardRevenueViewModel = DashboardRevenueViewModel(this, _dispatcher)
  val balanceView: DashboardBalanceViewModel = DashboardBalanceViewModel(this, _dispatcher)

  init {
    _queueRepository.addModelChangedListener(summaryView.queueChangedListener)
    _queueRepository.addModelChangedListener(revenueView.queueChangedListener)
    _productOrderRepository.addModelChangedListener(summaryView.productOrderChangedListener)
    _customerRepository.addModelChangedListener(balanceView.customerBalanceChangedListener)
    _customerRepository.addModelChangedListener(balanceView.customerDebtChangedListener)
    // Setting up initial values inside a fragment is painful. See commit d5604599.
    summaryView.loadAllQueuesInRange()
    revenueView.loadAllQueuesInRange()
    balanceView.loadAllCustomersWithBalance()
    balanceView.loadAllCustomersWithDebt()
  }

  override fun onCleared() {
    _queueRepository.removeModelChangedListener(summaryView.queueChangedListener)
    _queueRepository.removeModelChangedListener(revenueView.queueChangedListener)
    _productOrderRepository.removeModelChangedListener(summaryView.productOrderChangedListener)
    _customerRepository.removeModelChangedListener(balanceView.customerBalanceChangedListener)
    _customerRepository.removeModelChangedListener(balanceView.customerDebtChangedListener)
  }

  suspend fun selectAllQueuesInRange(
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

  suspend fun selectAllProductsSoldInRange(
      dateStart: ZonedDateTime,
      dateEnd: ZonedDateTime
  ): List<ProductOrderProductInfo> =
      _productOrderRepository.selectAllProductInfoInRange(dateStart, dateEnd)

  suspend fun selectDateInfoById(queueIds: List<Long>): List<QueueDateInfo> =
      _queueRepository.selectDateInfoById(queueIds = queueIds)

  suspend fun selectAllCustomersWithBalance(): List<CustomerBalanceInfo> =
      _customerRepository.selectAllBalanceInfoWithBalance()

  suspend fun selectAllCustomersWithDebt(): List<CustomerDebtInfo> =
      _customerRepository.selectAllDebtInfoWithDebt()
}
