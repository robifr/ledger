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
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewClientCompat
import com.robifr.ledger.assetbinding.chart.ChartData
import com.robifr.ledger.assetbinding.chart.ChartUtil
import com.robifr.ledger.data.InfoSynchronizer
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.data.model.QueuePaginatedInfo
import com.robifr.ledger.repository.ModelSyncListener
import com.robifr.ledger.ui.common.state.SafeLiveData
import com.robifr.ledger.ui.common.state.SafeMutableLiveData
import com.robifr.ledger.ui.common.state.UiEvent
import com.robifr.ledger.ui.common.state.updateEvent
import com.robifr.ledger.ui.dashboard.DashboardRevenue
import com.robifr.ledger.ui.dashboard.chart.RevenueChartModel
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardRevenueViewModel(
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
          onSync = { _, updatedModels ->
            _onQueuesChanged(
                updatedModels.filterNot {
                  it.date.isBefore(_uiState.safeValue.date.dateStart.toInstant()) ||
                      it.date.isAfter(_uiState.safeValue.date.dateEnd.toInstant())
                })
          })

  private val _uiState: SafeMutableLiveData<DashboardRevenueState> =
      SafeMutableLiveData(
          DashboardRevenueState(
              isDateDialogShown = false,
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
  private val _chartModelEvent: MutableLiveData<UiEvent<RevenueChartModel>> = MutableLiveData()
  val chartModelEvent: LiveData<UiEvent<RevenueChartModel>>
    get() = _chartModelEvent

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
      _viewModel.selectAllQueuesInRange(date.dateStart, date.dateEnd, true).let {
        withContext(Dispatchers.Main) { _onQueuesChanged(it) }
      }
    }
  }

  private fun _onQueuesChanged(queues: List<QueuePaginatedInfo>) {
    _uiState.setValue(_uiState.safeValue.copy(queues = queues))
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
    val dateStart: ZonedDateTime =
        if (_uiState.safeValue.date.range == QueueDate.Range.ALL_TIME) {
          // Use the oldest date in queues, or default to the start of this year if no data.
          _uiState.safeValue.queues.minOfOrNull { it.date }?.atZone(ZoneId.systemDefault())
              ?: QueueDate.Range.THIS_YEAR.dateStart()
        } else {
          _uiState.safeValue.date.dateStart.toInstant().atZone(ZoneId.systemDefault())
        }
    val dateEnd: ZonedDateTime = _uiState.safeValue.date.dateEnd

    val languageTags: String = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val decimalFractionDigits: Int = CurrencyFormat.decimalFractionDigits(languageTags)
    // The key is a formatted date with overview type as a secondary key.
    val rawDataSummed: LinkedHashMap<Pair<String, DashboardRevenue.OverviewType>, BigDecimal> =
        linkedMapOf()
    val yAxisTicks: Int = 6 // Defined in `createPercentageLinearScale()`. It includes zero.
    var maxValue: BigDecimal = (yAxisTicks - 1).toBigDecimal()
    // Sum the values if the date and overview type are equal.
    // The queues also have to be sorted by date because D3.js draws everything in order.
    for (queue in _uiState.safeValue.queues) {
      val formattedDate: String =
          ChartUtil.toDateTime(queue.date.atZone(ZoneId.systemDefault()), dateStart to dateEnd)
      // WARNING! There's a massive performance issue with the cents parser below, due to constant
      //    object recreation for `languageTags` and `decimalFractionDigits`. Noticeable when the
      //    queue is thousands. That's why they're instantiated outside the for-loop.
      val parsedGrandTotalPriceFromCents: BigDecimal =
          CurrencyFormat.fromCents(queue.grandTotalPrice, languageTags, decimalFractionDigits)
      // Received income are from the completed queue only.
      if (queue.status == QueueModel.Status.COMPLETED) {
        rawDataSummed
            .merge(
                formattedDate to DashboardRevenue.OverviewType.RECEIVED_INCOME,
                parsedGrandTotalPriceFromCents,
                BigDecimal::add)
            ?.let { maxValue = maxValue.max(it) }
      }
      rawDataSummed
          .merge(
              formattedDate to DashboardRevenue.OverviewType.PROJECTED_INCOME,
              parsedGrandTotalPriceFromCents,
              BigDecimal::add)
          ?.let { maxValue = maxValue.max(it) }
    }
    _viewModel.viewModelScope.launch {
      _chartModelEvent.updateEvent(
          data =
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
                          DashboardRevenue.OverviewType.RECEIVED_INCOME)),
          onSet = { it },
          onReset = { null })
    }
  }
}
