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

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.R
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.ui.dashboard.chart.Chart
import com.robifr.ledger.ui.dashboard.chart.ChartWebViewClient
import com.robifr.ledger.ui.dashboard.chart.RevenueChartModel
import com.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

class DashboardRevenue(private val _fragment: DashboardFragment) : View.OnClickListener {
  private val _date: DashboardDate =
      DashboardDate(
          dateChip = { _fragment.fragmentBinding.revenue.dateChip },
          fragment = _fragment,
          _selectedDateRange = {
            _fragment.dashboardViewModel.revenueView.uiState.safeValue.date.range
          },
          _onDateChanged = _fragment.dashboardViewModel.revenueView::onDateChanged)
  private val _chart: Chart =
      Chart(
          ChartWebViewClient(_fragment.requireContext()) {
            _fragment.dashboardViewModel.revenueView.onWebViewLoaded(_fragment.requireContext())
          },
          _fragment.requireContext(),
          _fragment.fragmentBinding.revenue.chart)

  init {
    _fragment.fragmentBinding.revenue.apply {
      receivedIncomeCardView.setOnClickListener(this@DashboardRevenue)
      receivedIncomeCard.icon.setImageResource(R.drawable.icon_paid)
      receivedIncomeCard.legendColor.setCardBackgroundColor(
          _fragment.requireContext().getColor(OverviewType.RECEIVED_INCOME.selectedColorRes))
      receivedIncomeCard.title.setText(R.string.dashboard_receivedIncome)
      receivedIncomeCard.description.setText(R.string.dashboard_receivedIncome_description)
      projectedIncomeCardView.setOnClickListener(this@DashboardRevenue)
      projectedIncomeCard.icon.setImageResource(R.drawable.icon_trending_up)
      projectedIncomeCard.legendColor.setCardBackgroundColor(
          _fragment.requireContext().getColor(OverviewType.PROJECTED_INCOME.selectedColorRes))
      projectedIncomeCard.title.setText(R.string.dashboard_projectedIncome)
      projectedIncomeCard.description.setText(R.string.dashboard_projectedIncome_description)
    }
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.receivedIncomeCardView,
      R.id.projectedIncomeCardView ->
          _fragment.dashboardViewModel.revenueView.onDisplayedChartChanged(
              OverviewType.valueOf(view.tag.toString()))
    }
  }

  fun setDate(date: QueueDate, @StringRes dateFormat: Int) {
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(_fragment.getString(dateFormat))
    _fragment.fragmentBinding.revenue.dateChip.text =
        if (date.range == QueueDate.Range.CUSTOM) {
          _fragment.getString(
              date.range.stringRes,
              date.dateStart.format(dateFormat),
              date.dateEnd.format(dateFormat))
        } else {
          _fragment.getString(date.range.stringRes)
        }
  }

  fun loadChart() {
    _chart.load()
  }

  fun selectCard(overviewType: OverviewType) {
    // There should be only one card getting selected.
    _fragment.fragmentBinding.revenue.receivedIncomeCardView.isSelected =
        overviewType == OverviewType.RECEIVED_INCOME
    _fragment.fragmentBinding.revenue.projectedIncomeCardView.isSelected =
        overviewType == OverviewType.PROJECTED_INCOME
  }

  fun setTotalProjectedIncome(amount: BigDecimal) {
    _fragment.fragmentBinding.revenue.projectedIncomeCard.amount.text =
        CurrencyFormat.formatCents(
            amount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
  }

  fun setTotalReceivedIncome(amount: BigDecimal) {
    _fragment.fragmentBinding.revenue.receivedIncomeCard.amount.text =
        CurrencyFormat.formatCents(
            amount, AppCompatDelegate.getApplicationLocales().toLanguageTags())
  }

  fun displayRevenueChart(model: RevenueChartModel) {
    _chart.displayStackedBarChartWithLargeValue(
        xAxisDomain = model.xAxisDomain,
        yAxisDomain = model.yAxisDomain,
        data = model.data,
        colors = model.colors.map { _fragment.requireContext().getColor(it) },
        groupInOrder = model.groupInOrder.map { it.toString() }.toSet())
  }

  enum class OverviewType(
      @ColorRes val selectedColorRes: Int,
      @ColorRes val unselectedColorRes: Int
  ) {
    PROJECTED_INCOME(R.color.secondary, R.color.secondary_disabled),
    RECEIVED_INCOME(R.color.primary, R.color.secondary)
  }
}
