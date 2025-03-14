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

import android.webkit.WebView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewClientCompat
import io.github.robifr.ledger.assetbinding.chart.ChartData
import io.github.robifr.ledger.assetbinding.chart.ChartUtil
import io.github.robifr.ledger.data.InfoSynchronizer
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.ProductOrderProductInfo
import io.github.robifr.ledger.data.model.QueueDateInfo
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.repository.ModelSyncListener
import io.github.robifr.ledger.ui.common.state.SafeLiveData
import io.github.robifr.ledger.ui.common.state.SafeMutableLiveData
import io.github.robifr.ledger.ui.common.state.UiEvent
import io.github.robifr.ledger.ui.common.state.updateEvent
import io.github.robifr.ledger.ui.dashboard.DashboardSummary
import io.github.robifr.ledger.ui.dashboard.chart.SummaryChartModel
import io.github.robifr.ledger.ui.dashboard.chart.TotalQueuesChartModel
import io.github.robifr.ledger.ui.dashboard.chart.UncompletedQueuesChartModel
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardSummaryViewModel(
    private val _viewModel: DashboardViewModel,
    private val _dispatcher: CoroutineDispatcher
) {
  val queueChangedListener: ModelSyncListener<QueueModel, QueuePaginatedInfo> =
      ModelSyncListener(
          onAdd = { InfoSynchronizer.addInfo(_uiState.safeValue.queues, it, ::QueuePaginatedInfo) },
          onUpdate = {
            // Use upsert instead of update. When there's a queue (which previously isn't included
            // within the current list) with their `QueueModel.date` being updated to the current
            // selected date range, they wouldn't get included if you do an update. Simply because
            // update will updates if there's a matching queue found.
            InfoSynchronizer.upsertInfo(_uiState.safeValue.queues, it, ::QueuePaginatedInfo)
          },
          onDelete = { InfoSynchronizer.deleteInfo(_uiState.safeValue.queues, it) },
          onUpsert = {
            InfoSynchronizer.upsertInfo(_uiState.safeValue.queues, it, ::QueuePaginatedInfo)
          },
          onSync = { models, updatedModels ->
            _onQueuesChanged(
                updatedModels.filterNot {
                  it.date.isBefore(_uiState.safeValue.date.dateStart.toInstant()) ||
                      it.date.isAfter(_uiState.safeValue.date.dateEnd.toInstant())
                })
          })
  val productOrderChangedListener: ModelSyncListener<ProductOrderModel, ProductOrderProductInfo> =
      ModelSyncListener(
          onAdd = {
            InfoSynchronizer.addInfo(_uiState.safeValue.productsSold, it, ::ProductOrderProductInfo)
          },
          onUpdate = {
            // Use upsert instead of update. When there's a queue (which previously isn't included
            // within the current list) with their `QueueModel.date` being updated to the current
            // selected date range, they wouldn't get included if you do an update. Simply because
            // update will updates if there's a matching queue found.
            InfoSynchronizer.upsertInfo(
                _uiState.safeValue.productsSold, it, ::ProductOrderProductInfo)
          },
          onDelete = { InfoSynchronizer.deleteInfo(_uiState.safeValue.productsSold, it) },
          onUpsert = {
            InfoSynchronizer.upsertInfo(
                _uiState.safeValue.productsSold, it, ::ProductOrderProductInfo)
          },
          onSync = { models, updatedModels ->
            _viewModel.viewModelScope.launch(_dispatcher) {
              // Get list of notified queues whose date isn't in range of current selected date.
              val excludedQueueDateInfo: List<QueueDateInfo> =
                  _viewModel
                      .selectDateInfoById(models.mapNotNull { it.queueId }.distinct())
                      .filterNot {
                        it.date.isBefore(_uiState.safeValue.date.dateStart.toInstant()) ||
                            it.date.isAfter(_uiState.safeValue.date.dateEnd.toInstant())
                      }
              // Then remove every referenced product orders with the same queue ID if they're in
              // the exclusion list.
              val filteredProductsSold: List<ProductOrderProductInfo> =
                  updatedModels.filterNot { productOrder ->
                    excludedQueueDateInfo.find { it.id == productOrder.queueId } != null
                  }
              withContext(Dispatchers.Main) { _onProductsSoldChanged(filteredProductsSold) }
            }
          })

  private val _uiState: SafeMutableLiveData<DashboardSummaryState> =
      SafeMutableLiveData(
          DashboardSummaryState(
              isDateDialogShown = false,
              date = QueueDate(QueueDate.Range.ALL_TIME),
              queues = listOf(),
              productsSold = listOf(),
              displayedChart = DashboardSummary.OverviewType.TOTAL_QUEUES))
  val uiState: SafeLiveData<DashboardSummaryState>
    get() = _uiState

  /**
   * Web view can only be refreshed within [WebViewClientCompat.onPageFinished], which is triggered
   * whenever [WebView.loadUrl] is called to refresh the content. This also need to be called when
   * the current fragment is replaced and then revisited.
   */
  private val _chartModelEvent: MutableLiveData<UiEvent<SummaryChartModel>> = MutableLiveData()
  val chartModelEvent: LiveData<UiEvent<SummaryChartModel>>
    get() = _chartModelEvent

  fun onDisplayedChartChanged(displayedChart: DashboardSummary.OverviewType) {
    _uiState.setValue(_uiState.safeValue.copy(displayedChart = displayedChart))
  }

  fun onWebViewLoaded() {
    // FIXME: Chart doesn't load when returning from another fragment. Steps to reproduce:
    //    1. Select `TOTAL_QUEUES` or `UNCOMPLETED_QUEUES` overview.
    //    2. Switch to `ACTIVE_CUSTOMERS` or `PRODUCTS_SOLD` overview.
    //    3. Navigate to another fragment, such as settings.
    //    4. Press back and reselect the initial overview (`TOTAL_QUEUES` or `UNCOMPLETED_QUEUES`).
    when (_uiState.safeValue.displayedChart) {
      DashboardSummary.OverviewType.TOTAL_QUEUES -> _onDisplayTotalQueuesChart()
      DashboardSummary.OverviewType.UNCOMPLETED_QUEUES -> _onDisplayUncompletedQueuesChart()
      // The rest of the enum isn't a web view, which is handled via `onDisplayedChartChanged()`.
      else -> Unit
    }
  }

  fun onDateChanged(date: QueueDate) {
    _uiState.setValue(_uiState.safeValue.copy(date = date))
    loadAllQueuesInRange(_uiState.safeValue.date)
  }

  fun onDateDialogShown() {
    _uiState.setValue(_uiState.safeValue.copy(isDateDialogShown = true))
  }

  fun onDateDialogClosed() {
    _uiState.setValue(_uiState.safeValue.copy(isDateDialogShown = false))
  }

  fun loadAllQueuesInRange(date: QueueDate = _uiState.safeValue.date) {
    _viewModel.viewModelScope.launch(_dispatcher) {
      val queueInfo: List<QueuePaginatedInfo> =
          _viewModel.selectAllQueuesInRange(date.dateStart, date.dateEnd, false)
      val productOrderInfo: List<ProductOrderProductInfo> =
          _viewModel.selectAllProductsSoldInRange(date.dateStart, date.dateEnd)
      withContext(Dispatchers.Main) {
        _onQueuesChanged(queueInfo)
        _onProductsSoldChanged(productOrderInfo)
      }
    }
  }

  private fun _onQueuesChanged(queueInfo: List<QueuePaginatedInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(queues = queueInfo))
  }

  private fun _onProductsSoldChanged(productOrderInfo: List<ProductOrderProductInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(productsSold = productOrderInfo))
  }

  private fun _onDisplayTotalQueuesChart() {
    val dateStart: ZonedDateTime =
        if (_uiState.safeValue.date.range == QueueDate.Range.ALL_TIME) {
          // Use the oldest date in queues, or default to the start of this year if no data.
          _uiState.safeValue.queues.minOfOrNull { it.date }?.atZone(ZoneId.systemDefault())
              ?: QueueDate.Range.THIS_YEAR.dateStart()
        } else {
          _uiState.safeValue.date.dateStart.toInstant().atZone(ZoneId.systemDefault())
        }
    val dateEnd: ZonedDateTime = _uiState.safeValue.date.dateEnd

    // The key is a formatted date.
    val rawDataSummed: LinkedHashMap<String, Int> = linkedMapOf()
    val yAxisTicks: Int = 6 // Defined in `createPercentageLinearScale()`. It includes zero.
    var maxValue: Int = yAxisTicks - 1
    // Sum the values if the date is equal. The queues also have to be sorted by date
    // because D3.js draws everything in order.
    for (queue in _uiState.safeValue.queues) {
      rawDataSummed
          .merge(
              ChartUtil.toDateTime(queue.date.atZone(ZoneId.systemDefault()), dateStart to dateEnd),
              1,
              Int::plus)
          ?.let { maxValue = maxOf(maxValue, it) }
    }
    _viewModel.viewModelScope.launch {
      _chartModelEvent.updateEvent(
          data =
              TotalQueuesChartModel(
                  // Both domains must be the same as the formatted ones.
                  // X-axis for the data's key, y-axis for the data's value.
                  xAxisDomain = ChartUtil.toDateTimeDomain(dateStart to dateEnd),
                  yAxisDomain =
                      listOf(
                          0.0,
                          ChartUtil.calculateNiceScale(
                                  0.0, maxValue.toDouble(), yAxisTicks.toDouble())[1]),
                  data = rawDataSummed.map { (key, value) -> ChartData.Single(key, value) }),
          onSet = { it },
          onReset = { null })
    }
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
    for (queue in _uiState.safeValue.queues) {
      // Merge the data if the status is uncompleted.
      if (rawDataSummed.containsKey(queue.status.stringRes)) {
        rawDataSummed.merge(queue.status.stringRes, 1, Int::plus)
        if (oldestDate == null || queue.date.isBefore(oldestDate.toInstant())) {
          oldestDate = queue.date.atZone(ZoneId.systemDefault())
        }
      }
    }
    _viewModel.viewModelScope.launch {
      _chartModelEvent.updateEvent(
          data =
              UncompletedQueuesChartModel(
                  data = rawDataSummed.map { (key, value) -> ChartData.Single(key, value) },
                  colors =
                      listOf(
                          QueueModel.Status.IN_QUEUE.backgroundColorRes,
                          QueueModel.Status.IN_PROCESS.backgroundColorRes,
                          QueueModel.Status.UNPAID.backgroundColorRes),
                  oldestDate = oldestDate),
          onSet = { it },
          onReset = { null })
    }
  }
}
