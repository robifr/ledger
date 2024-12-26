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

import android.content.Context
import android.webkit.WebView
import androidx.annotation.ColorRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewClientCompat
import com.robifr.ledger.assetbinding.chart.ChartData
import com.robifr.ledger.assetbinding.chart.ChartUtil
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.display.QueueFilterer
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.SafeLiveData
import com.robifr.ledger.ui.SafeMutableLiveData
import com.robifr.ledger.ui.SingleLiveEvent
import com.robifr.ledger.ui.dashboard.DashboardRevenue
import com.robifr.ledger.ui.dashboard.chart.RevenueChartModel
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardRevenueViewModel(
    private val _viewModel: DashboardViewModel,
    private val _dispatcher: CoroutineDispatcher,
    private val _selectAllQueuesInRange:
        suspend (startDate: ZonedDateTime, endDate: ZonedDateTime) -> List<QueueModel>
) {
  val _queueChangedListener: ModelSyncListener<QueueModel> =
      ModelSyncListener(
          currentModel = { _uiState.safeValue.queues },
          onSyncModels = {
            _onQueuesChanged(
                QueueFilterer()
                    .apply { filters = filters.copy(filteredDate = _uiState.safeValue.date) }
                    .filter(it))
          })

  private val _uiState: SafeMutableLiveData<DashboardRevenueState> =
      SafeMutableLiveData(
          DashboardRevenueState(
              date = QueueDate(QueueDate.Range.ALL_TIME),
              queues = listOf(),
              displayedChart = DashboardRevenue.OverviewType.RECEIVED_INCOME))
  val uiState: SafeLiveData<DashboardRevenueState>
    get() = _uiState

  /**
   * Web view can only be refreshed within [WebViewClientCompat.onPageFinished], which is triggered
   * whenever [WebView.loadUrl] is called to refresh the content. This also need to be called when
   * the current fragment is replaced and then revisited.
   */
  private val _chartModel: SingleLiveEvent<RevenueChartModel> = SingleLiveEvent()
  val chartModel: LiveData<RevenueChartModel>
    get() = _chartModel

  fun onDisplayedChartChanged(displayedChart: DashboardRevenue.OverviewType) {
    _uiState.setValue(_uiState.safeValue.copy(displayedChart = displayedChart))
  }

  fun onWebViewLoaded(context: Context) {
    when (_uiState.safeValue.displayedChart) {
      DashboardRevenue.OverviewType.PROJECTED_INCOME -> _onDisplayProjectedIncomeChart(context)
      DashboardRevenue.OverviewType.RECEIVED_INCOME -> _onDisplayReceivedIncomeChart(context)
    }
  }

  fun onDateChanged(date: QueueDate) {
    _uiState.setValue(_uiState.safeValue.copy(date = date))
    _loadAllQueuesInRange(_uiState.safeValue.date)
  }

  fun _onQueuesChanged(queues: List<QueueModel>) {
    _uiState.setValue(_uiState.safeValue.copy(queues = queues))
  }

  fun _loadAllQueuesInRange(date: QueueDate = _uiState.safeValue.date) {
    _viewModel.viewModelScope.launch(_dispatcher) {
      _selectAllQueuesInRange(date.dateStart, date.dateEnd).let {
        withContext(Dispatchers.Main) { _onQueuesChanged(it) }
      }
    }
  }

  private fun _onDisplayReceivedIncomeChart(context: Context) {
    _onDisplayChart(
        context,
        listOf(
            DashboardRevenue.OverviewType.PROJECTED_INCOME.unselectedColorRes,
            DashboardRevenue.OverviewType.RECEIVED_INCOME.selectedColorRes))
  }

  private fun _onDisplayProjectedIncomeChart(context: Context) {
    _onDisplayChart(
        context,
        listOf(
            DashboardRevenue.OverviewType.PROJECTED_INCOME.selectedColorRes,
            DashboardRevenue.OverviewType.RECEIVED_INCOME.unselectedColorRes))
  }

  private fun _onDisplayChart(context: Context, @ColorRes colors: List<Int>) {
    // Remove unnecessary dates.
    val dateStart: ZonedDateTime =
        _uiState.safeValue.queues
            .takeIf { _uiState.safeValue.date.range == QueueDate.Range.ALL_TIME }
            ?.minOfOrNull { it.date }
            ?.atZone(ZoneId.systemDefault())
            ?: _uiState.safeValue.date.dateStart.toInstant().atZone(ZoneId.systemDefault())
    val dateEnd: ZonedDateTime = _uiState.safeValue.date.dateEnd

    // The key is a formatted date with overview type as a secondary key.
    val rawDataSummed: LinkedHashMap<Pair<String, DashboardRevenue.OverviewType>, BigDecimal> =
        linkedMapOf()
    val yAxisTicks: Int = 6 // Defined in `createPercentageLinearScale()`. It includes zero.
    var maxValue: BigDecimal = (yAxisTicks - 1).toBigDecimal()
    // Sum the values if the date and overview type are equal.
    // The queues also have to be sorted by date because D3.js draws everything in order.
    for (queue in _uiState.safeValue.queues.sortedBy { it.date }) {
      val formattedDate: String =
          ChartUtil.toDateTime(queue.date.atZone(ZoneId.systemDefault()), dateStart to dateEnd)
      // Received income are from the completed queue only.
      if (queue.status == QueueModel.Status.COMPLETED) {
        rawDataSummed
            .merge(
                formattedDate to DashboardRevenue.OverviewType.RECEIVED_INCOME,
                queue.grandTotalPrice(),
                BigDecimal::add)
            ?.let { maxValue = maxValue.max(it) }
      }
      rawDataSummed
          .merge(
              formattedDate to DashboardRevenue.OverviewType.PROJECTED_INCOME,
              queue.grandTotalPrice(),
              BigDecimal::add)
          ?.let { maxValue = maxValue.max(it) }
    }
    _chartModel.setValue(
        RevenueChartModel(
            // Both domains must be the same as the formatted ones.
            // X-axis for the data's key, y-axis for the data's value.
            xAxisDomain = ChartUtil.toDateTimeDomain(dateStart to dateEnd),
            yAxisDomain = ChartUtil.toPercentageLinearDomain(context, maxValue, yAxisTicks),
            data =
                rawDataSummed.map { (key, value) ->
                  ChartData.Multiple(
                      key.first,
                      // Convert to percent because D3.js can't handle big decimal.
                      ChartUtil.toPercentageLinear(value, maxValue, yAxisTicks),
                      key.second.toString())
                },
            colors = colors,
            // When viewing the received income, the projected income bar appears behind it.
            // Therefore, projected income is placed before received income.
            groupInOrder =
                setOf(
                    DashboardRevenue.OverviewType.PROJECTED_INCOME,
                    DashboardRevenue.OverviewType.RECEIVED_INCOME)))
  }
}
