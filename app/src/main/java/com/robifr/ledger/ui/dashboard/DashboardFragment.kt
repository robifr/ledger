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

package com.robifr.ledger.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.DashboardFragmentBinding
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.dashboard.chart.RevenueChartModel
import com.robifr.ledger.ui.dashboard.chart.SummaryChartModel
import com.robifr.ledger.ui.dashboard.chart.TotalQueuesChartModel
import com.robifr.ledger.ui.dashboard.chart.UncompletedQueuesChartModel
import com.robifr.ledger.ui.dashboard.viewmodel.DashboardBalanceState
import com.robifr.ledger.ui.dashboard.viewmodel.DashboardRevenueState
import com.robifr.ledger.ui.dashboard.viewmodel.DashboardSummaryState
import com.robifr.ledger.ui.dashboard.viewmodel.DashboardViewModel
import com.robifr.ledger.util.getColorAttr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardFragment : Fragment() {
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
    requireActivity().window.statusBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().window.navigationBarColor = requireContext().getColor(R.color.surface)
    dashboardViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    dashboardViewModel.summaryView.uiState.observe(viewLifecycleOwner, ::_onSummaryState)
    dashboardViewModel.summaryView.chartModel.observe(viewLifecycleOwner, ::_onSummaryChartModel)
    dashboardViewModel.revenueView.uiState.observe(viewLifecycleOwner, ::_onRevenueState)
    dashboardViewModel.revenueView.chartModel.observe(viewLifecycleOwner, ::_onRevenueChartModel)
    dashboardViewModel.balanceView.uiState.observe(viewLifecycleOwner, ::_onBalanceState)
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onSummaryState(state: DashboardSummaryState) {
    _summaryOverview.setDate(state.date)
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
    _revenueOverview.setDate(state.date)
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
