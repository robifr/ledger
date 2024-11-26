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

import android.webkit.WebView
import androidx.lifecycle.LiveData
import androidx.webkit.WebViewClientCompat
import com.robifr.ledger.assetbinding.chart.ChartData
import com.robifr.ledger.assetbinding.chart.ChartUtil
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMediatorLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.dashboard.DashboardSummary
import com.robifr.ledger.ui.dashboard.chart.SummaryChartModel
import com.robifr.ledger.ui.dashboard.chart.TotalQueuesChartModel
import com.robifr.ledger.ui.dashboard.chart.UncompletedQueuesChartModel
import java.time.ZoneId
import java.time.ZonedDateTime

class DashboardSummaryViewModel(private val _viewModel: DashboardViewModel) {
  private val _uiState: SafeMediatorLiveData<DashboardSummaryState> =
      SafeMediatorLiveData(
              DashboardSummaryState(
                  displayedChart = DashboardSummary.OverviewType.TOTAL_QUEUES,
                  totalQueues = 0,
                  totalUncompletedQueues = 0,
                  totalActiveCustomers = 0,
                  mostActiveCustomers = mapOf(),
                  totalProductsSold = 0.toBigDecimal(),
                  mostProductsSold = mapOf()))
          .apply {
            addSource(_viewModel.uiState.toLiveData()) { state ->
              setValue(
                  safeValue.copy(
                      totalQueues = state.queues.size,
                      totalUncompletedQueues =
                          state.queues.count { it.status != QueueModel.Status.COMPLETED },
                      totalActiveCustomers =
                          state.queues.asSequence().mapNotNull { it.customerId }.distinct().count(),
                      totalProductsSold =
                          state.queues
                              .asSequence()
                              .flatMap { it.productOrders.asSequence() }
                              .sumOf { it.quantity.toBigDecimal() },
                  ))
            }
          }
  val uiState: SafeLiveData<DashboardSummaryState>
    get() = _uiState

  /**
   * Web view can only be refreshed within [WebViewClientCompat.onPageFinished], which is triggered
   * whenever [WebView.loadUrl] is called to refresh the content. This also need to be called when
   * the current fragment is replaced and then revisited.
   */
  private val _chartModel: SingleLiveEvent<SummaryChartModel> = SingleLiveEvent()
  val chartModel: LiveData<SummaryChartModel>
    get() = _chartModel

  fun onDisplayedChartChanged(displayedChart: DashboardSummary.OverviewType) {
    _uiState.setValue(_uiState.safeValue.copy(displayedChart = displayedChart))
    when (displayedChart) {
      DashboardSummary.OverviewType.ACTIVE_CUSTOMERS -> _onDisplayMostActiveCustomers()
      DashboardSummary.OverviewType.PRODUCTS_SOLD -> _onDisplayMostProductsSold()
      // The rest of the enum is a web view, which is handled via `onWebViewLoaded()`.
      else -> Unit
    }
  }

  fun onWebViewLoaded() {
    when (_uiState.safeValue.displayedChart) {
      DashboardSummary.OverviewType.TOTAL_QUEUES -> _onDisplayTotalQueuesChart()
      DashboardSummary.OverviewType.UNCOMPLETED_QUEUES -> _onDisplayUncompletedQueuesChart()
      // The rest of the enum isn't a web view, which is handled via `onDisplayedChartChanged()`.
      else -> Unit
    }
  }

  private fun _onDisplayTotalQueuesChart() {
    // Remove unnecessary dates.
    val dateStart: ZonedDateTime =
        _viewModel.uiState.safeValue.queues
            .takeIf { _viewModel.uiState.safeValue.date.range == QueueDate.Range.ALL_TIME }
            ?.minOfOrNull(QueueModel::date)
            ?.atZone(ZoneId.systemDefault())
            ?: _viewModel.uiState.safeValue.date.dateStart
                .toInstant()
                .atZone(ZoneId.systemDefault())
    val dateEnd: ZonedDateTime = _viewModel.uiState.safeValue.date.dateEnd

    // The key is a formatted date.
    val rawDataSummed: LinkedHashMap<String, Int> = linkedMapOf()
    val yAxisTicks: Int = 6 // Defined in `createPercentageLinearScale()`. It includes zero.
    var maxValue: Int = yAxisTicks - 1
    // Sum the values if the date is equal. The queues also have to be sorted by date
    // because D3.js draws everything in order.
    for (queue in _viewModel.uiState.safeValue.queues.sortedBy(QueueModel::date)) {
      rawDataSummed
          .merge(
              ChartUtil.toDateTime(queue.date.atZone(ZoneId.systemDefault()), dateStart to dateEnd),
              1,
              Int::plus)
          ?.let { maxValue = maxOf(maxValue, it) }
    }
    _chartModel.setValue(
        TotalQueuesChartModel(
            // Both domains must be the same as the formatted ones.
            // X-axis for the data's key, y-axis for the data's value.
            xAxisDomain = ChartUtil.toDateTimeDomain(dateStart to dateEnd),
            yAxisDomain =
                listOf(
                    0.0,
                    ChartUtil.calculateNiceScale(0.0, maxValue.toDouble(), yAxisTicks.toDouble())[
                            1]),
            data = rawDataSummed.map { (key, value) -> ChartData.Single(key, value) }))
  }

  private fun _onDisplayUncompletedQueuesChart() {
    val rawDataSummed: LinkedHashMap<Int, Int> =
        linkedMapOf<Int, Int>().apply {
          // The order is important to ensure that `UncompletedQueuesChartModel.colors` matches.
          set(QueueModel.Status.IN_QUEUE.stringRes, 0)
          set(QueueModel.Status.IN_PROCESS.stringRes, 0)
          set(QueueModel.Status.UNPAID.stringRes, 0)
        }
    var oldestDate: ZonedDateTime? = null
    for (queue in _viewModel.uiState.safeValue.queues) {
      // Merge the data if the status is uncompleted.
      if (rawDataSummed.containsKey(queue.status.stringRes)) {
        rawDataSummed.merge(queue.status.stringRes, 1, Int::plus)
        if (oldestDate == null || queue.date.isBefore(oldestDate.toInstant())) {
          oldestDate = queue.date.atZone(ZoneId.systemDefault())
        }
      }
    }
    _chartModel.setValue(
        UncompletedQueuesChartModel(
            data = rawDataSummed.map { (key, value) -> ChartData.Single(key, value) },
            colors =
                listOf(
                    QueueModel.Status.IN_QUEUE.backgroundColorRes,
                    QueueModel.Status.IN_PROCESS.backgroundColorRes,
                    QueueModel.Status.UNPAID.backgroundColorRes),
            oldestDate = oldestDate))
  }

  private fun _onDisplayMostActiveCustomers() {
    _uiState.setValue(
        _uiState.safeValue.copy(
            mostActiveCustomers =
                _viewModel.uiState.safeValue.queues
                    .asSequence()
                    .mapNotNull { queue -> queue.customer?.let { it to 1 } }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(4)
                    .toMap()))
  }

  private fun _onDisplayMostProductsSold() {
    _uiState.setValue(
        _uiState.safeValue.copy(
            mostProductsSold =
                _viewModel.uiState.safeValue.queues
                    .asSequence()
                    .flatMap { it.productOrders }
                    .mapNotNull { productOrder ->
                      productOrder.referencedProduct()?.let {
                        it to productOrder.quantity.toBigDecimal()
                      }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { entry -> entry.value.sumOf { it } }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(4)
                    .toMap()))
  }
}
