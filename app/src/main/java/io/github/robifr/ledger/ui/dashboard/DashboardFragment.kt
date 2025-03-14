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

package io.github.robifr.ledger.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.DashboardFragmentBinding
import io.github.robifr.ledger.ui.common.state.UiEvent
import io.github.robifr.ledger.ui.dashboard.chart.RevenueChartModel
import io.github.robifr.ledger.ui.dashboard.chart.SummaryChartModel
import io.github.robifr.ledger.ui.dashboard.chart.TotalQueuesChartModel
import io.github.robifr.ledger.ui.dashboard.chart.UncompletedQueuesChartModel
import io.github.robifr.ledger.ui.dashboard.viewmodel.DashboardBalanceState
import io.github.robifr.ledger.ui.dashboard.viewmodel.DashboardRevenueState
import io.github.robifr.ledger.ui.dashboard.viewmodel.DashboardSummaryState
import io.github.robifr.ledger.ui.dashboard.viewmodel.DashboardViewModel

@AndroidEntryPoint
class DashboardFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: DashboardFragmentBinding? = null
  val fragmentBinding: DashboardFragmentBinding
    get() = _fragmentBinding!!

  val dashboardViewModel: DashboardViewModel by activityViewModels()
  private lateinit var _summaryOverview: DashboardSummary
  private lateinit var _revenueOverview: DashboardRevenue
  private lateinit var _balanceOverview: DashboardBalance

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = DashboardFragmentBinding.inflate(inflater, container, false)
    _summaryOverview = DashboardSummary(this)
    _revenueOverview = DashboardRevenue(this)
    _balanceOverview = DashboardBalance(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val statusBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(top = statusBarInsets.top, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    dashboardViewModel.summaryView.uiState.observe(viewLifecycleOwner, ::_onSummaryState)
    dashboardViewModel.summaryView.chartModelEvent.observe(viewLifecycleOwner) {
        event: UiEvent<SummaryChartModel>? ->
      event?.let {
        _onSummaryChartModel(it.data)
        it.onConsumed()
      }
    }
    dashboardViewModel.revenueView.uiState.observe(viewLifecycleOwner, ::_onRevenueState)
    dashboardViewModel.revenueView.chartModelEvent.observe(viewLifecycleOwner) {
        event: UiEvent<RevenueChartModel>? ->
      event?.let {
        _onRevenueChartModel(it.data)
        it.onConsumed()
      }
    }
    dashboardViewModel.balanceView.uiState.observe(viewLifecycleOwner, ::_onBalanceState)
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.settings -> {
          findNavController().navigate(R.id.settingsFragment)
          true
        }
        else -> false
      }

  private fun _onSummaryState(state: DashboardSummaryState) {
    if (state.isDateDialogShown) _summaryOverview.date.showDialog { state.date.range }
    else _summaryOverview.date.dismissDialog()
    _summaryOverview.setDate(state.date, state.dateFormat())
    _summaryOverview.selectCard(state.displayedChart)
    _summaryOverview.setTotalQueues(state.queues.size)
    _summaryOverview.setTotalUncompletedQueues(state.totalUncompletedQueues())
    _summaryOverview.setTotalActiveCustomers(state.totalActiveCustomers())
    _summaryOverview.setTotalProductsSold(state.totalProductsSold())
    when (state.displayedChart) {
      DashboardSummary.OverviewType.ACTIVE_CUSTOMERS ->
          _summaryOverview.displayMostActiveCustomersList(state.mostActiveCustomers())
      DashboardSummary.OverviewType.PRODUCTS_SOLD ->
          _summaryOverview.displayMostProductsSoldList(state.mostProductsSold())
      // The rest of the enum is a web view, which is handled via `_onSummaryChartModel()`.
      else -> _summaryOverview.loadChart()
    }
  }

  private fun _onSummaryChartModel(model: SummaryChartModel) {
    when (model) {
      is TotalQueuesChartModel -> _summaryOverview.displayTotalQueuesChart(model)
      is UncompletedQueuesChartModel -> _summaryOverview.displayUncompletedQueuesChart(model)
    }
  }

  private fun _onRevenueState(state: DashboardRevenueState) {
    if (state.isDateDialogShown) _revenueOverview.date.showDialog { state.date.range }
    else _revenueOverview.date.dismissDialog()
    _revenueOverview.setDate(state.date, state.dateFormat())
    _revenueOverview.selectCard(state.displayedChart)
    _revenueOverview.setTotalReceivedIncome(state.receivedIncome())
    _revenueOverview.setTotalProjectedIncome(state.projectedIncome())
    _revenueOverview.loadChart()
  }

  private fun _onRevenueChartModel(model: RevenueChartModel) {
    _revenueOverview.displayRevenueChart(model)
  }

  private fun _onBalanceState(state: DashboardBalanceState) {
    _balanceOverview.setTotalBalance(state.totalBalance(), state.customersWithBalance.size)
    _balanceOverview.setTotalDebt(
        state.totalDebt(), state.totalDebtColorRes(), state.customersWithDebt.size)
  }
}
