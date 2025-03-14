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

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.HtmlCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.google.android.material.R as MaterialR
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import io.github.robifr.ledger.R
import io.github.robifr.ledger.assetbinding.JsInterface
import io.github.robifr.ledger.assetbinding.chart.ChartData
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.model.CustomerNameInfo
import io.github.robifr.ledger.data.model.ProductNameInfo
import io.github.robifr.ledger.databinding.DashboardCardSummaryListItemBinding
import io.github.robifr.ledger.ui.dashboard.chart.Chart
import io.github.robifr.ledger.ui.dashboard.chart.ChartWebViewClient
import io.github.robifr.ledger.ui.dashboard.chart.TotalQueuesChartModel
import io.github.robifr.ledger.ui.dashboard.chart.UncompletedQueuesChartModel
import io.github.robifr.ledger.ui.dashboard.viewmodel.DashboardSummaryState
import io.github.robifr.ledger.util.CurrencyFormat
import io.github.robifr.ledger.util.getColorAttr
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class DashboardSummary(private val _fragment: DashboardFragment) : View.OnClickListener {
  val date: DashboardDate =
      DashboardDate(
          dateChip = { _fragment.fragmentBinding.summary.dateChip },
          fragment = _fragment,
          onDialogShown = _fragment.dashboardViewModel.summaryView::onDateDialogShown,
          _onDialogClosed = _fragment.dashboardViewModel.summaryView::onDateDialogClosed,
          _onDateChanged = _fragment.dashboardViewModel.summaryView::onDateChanged)
  private val _chart: Chart =
      Chart(
          ChartWebViewClient(_fragment.requireContext()) {
            _fragment.dashboardViewModel.summaryView.onWebViewLoaded()
          },
          _fragment.requireContext(),
          _fragment.fragmentBinding.summary.chart)

  init {
    _fragment.fragmentBinding.summary.apply {
      totalQueuesCardView.setOnClickListener(this@DashboardSummary)
      totalQueuesCard.icon.setImageResource(R.drawable.icon_assignment)
      totalQueuesCard.title.setText(R.string.dashboard_totalQueues)
      uncompletedQueuesCardView.setOnClickListener(this@DashboardSummary)
      uncompletedQueuesCard.icon.setImageResource(R.drawable.icon_assignment_late)
      uncompletedQueuesCard.title.setText(R.string.dashboard_uncompletedQueues)
      activeCustomersCardView.setOnClickListener(this@DashboardSummary)
      activeCustomersCard.icon.setImageResource(R.drawable.icon_person)
      activeCustomersCard.title.setText(R.string.dashboard_activeCustomers)
      productsSoldCardView.setOnClickListener(this@DashboardSummary)
      productsSoldCard.icon.setImageResource(R.drawable.icon_sell)
      productsSoldCard.title.setText(R.string.dashboard_productsSold)
    }
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.totalQueuesCardView,
      R.id.uncompletedQueuesCardView,
      R.id.activeCustomersCardView,
      R.id.productsSoldCardView ->
          _fragment.dashboardViewModel.summaryView.onDisplayedChartChanged(
              OverviewType.valueOf(view.tag.toString()))
    }
  }

  fun setDate(date: QueueDate, @StringRes dateFormat: Int) {
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern(_fragment.getString(dateFormat))
    _fragment.fragmentBinding.summary.dateChip.text =
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
    _fragment.fragmentBinding.summary.totalQueuesCardView.isSelected =
        overviewType == OverviewType.TOTAL_QUEUES
    _fragment.fragmentBinding.summary.uncompletedQueuesCardView.isSelected =
        overviewType == OverviewType.UNCOMPLETED_QUEUES
    _fragment.fragmentBinding.summary.activeCustomersCardView.isSelected =
        overviewType == OverviewType.ACTIVE_CUSTOMERS
    _fragment.fragmentBinding.summary.productsSoldCardView.isSelected =
        overviewType == OverviewType.PRODUCTS_SOLD
  }

  fun setTotalQueues(amount: Int) {
    _fragment.fragmentBinding.summary.totalQueuesCard.amount.text = amount.toString()
  }

  fun displayTotalQueuesChart(model: TotalQueuesChartModel) {
    TransitionManager.endTransitions(_fragment.fragmentBinding.root)
    TransitionManager.beginDelayedTransition(_fragment.fragmentBinding.root, ChangeBounds())
    _fragment.fragmentBinding.summary.listContainer.isGone = true
    _fragment.fragmentBinding.summary.chart.isVisible = true
    _chart.displayBarChart(
        xAxisDomain = model.xAxisDomain,
        yAxisDomain = model.yAxisDomain,
        data = model.data,
        color = _fragment.requireContext().getColorAttr(MaterialR.attr.colorPrimary))
  }

  fun setTotalUncompletedQueues(amount: Int) {
    _fragment.fragmentBinding.summary.uncompletedQueuesCard.amount.text = amount.toString()
  }

  fun displayUncompletedQueuesChart(model: UncompletedQueuesChartModel) {
    TransitionManager.endTransitions(_fragment.fragmentBinding.root)
    TransitionManager.beginDelayedTransition(_fragment.fragmentBinding.root, ChangeBounds())
    _fragment.fragmentBinding.summary.listContainer.isGone = true
    _fragment.fragmentBinding.summary.chart.isVisible = true

    val titleFontSize: Int =
        JsInterface.dpToCssPx(
            _fragment.requireContext(),
            _fragment.resources.getDimensionPixelSize(R.dimen.text_medium).toFloat())
    val oldestDateFontSize: Int =
        JsInterface.dpToCssPx(
            _fragment.requireContext(),
            _fragment.resources.getDimensionPixelSize(R.dimen.text_mediumLarge).toFloat())
    val oldestDate: String? =
        model.oldestDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    _chart.displayDonutChart(
        data = model.data.map { ChartData.Single(_fragment.getString(it.key), it.value) },
        colors = model.colors.map { _fragment.requireContext().getColor(it) },
        svgTextInCenter =
            model.oldestDate?.let {
              _fragment.getString(
                  R.string.dashboard_uncompletedQueues_oldestQueue_x,
                  titleFontSize,
                  oldestDateFontSize,
                  oldestDate)
            })
  }

  fun setTotalActiveCustomers(amount: Int) {
    _fragment.fragmentBinding.summary.activeCustomersCard.amount.text = amount.toString()
  }

  /** @param customers [DashboardSummaryState.mostActiveCustomers] */
  fun displayMostActiveCustomersList(customers: Map<CustomerNameInfo, Int>) {
    TransitionManager.endTransitions(_fragment.fragmentBinding.root)
    TransitionManager.beginDelayedTransition(_fragment.fragmentBinding.root, ChangeBounds())
    _fragment.fragmentBinding.summary.chart.isGone = true
    _fragment.fragmentBinding.summary.listContainer.isVisible = true
    _fragment.fragmentBinding.summary.listContainer.removeAllViews()
    for ((customer, queueCounts) in customers) {
      _fragment.fragmentBinding.summary.listContainer.addView(
          DashboardCardSummaryListItemBinding.inflate(
                  _fragment.layoutInflater, _fragment.fragmentBinding.summary.listContainer, false)
              .apply {
                title.text = customer.name
                description.isGone = true
                amount.text =
                    HtmlCompat.fromHtml(
                        _fragment.resources.getQuantityString(
                            R.plurals.dashboard_activeCustomers_n_queue, queueCounts, queueCounts),
                        HtmlCompat.FROM_HTML_MODE_LEGACY)
                image.shapeableImage.shapeAppearanceModel =
                    ShapeAppearanceModel.builder()
                        .setAllCornerSizes(RelativeCornerSize(0.5f))
                        .build()
                image.text.text = customer.name.take(1)
              }
              .root)
    }
  }

  fun setTotalProductsSold(amount: BigDecimal) {
    _fragment.fragmentBinding.summary.productsSoldCard.amount.text =
        CurrencyFormat.format(
            amount,
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            "",
            CurrencyFormat.countDecimalPlace(amount))
  }

  /** @param products [DashboardSummaryState.mostProductsSold] */
  fun displayMostProductsSoldList(products: Map<ProductNameInfo, BigDecimal>) {
    TransitionManager.endTransitions(_fragment.fragmentBinding.root)
    TransitionManager.beginDelayedTransition(_fragment.fragmentBinding.root, ChangeBounds())
    _fragment.fragmentBinding.summary.chart.isGone = true
    _fragment.fragmentBinding.summary.listContainer.isVisible = true
    _fragment.fragmentBinding.summary.listContainer.removeAllViews()
    for ((product, amountSold) in products) {
      _fragment.fragmentBinding.summary.listContainer.addView(
          DashboardCardSummaryListItemBinding.inflate(
                  _fragment.layoutInflater, _fragment.fragmentBinding.summary.listContainer, false)
              .apply {
                title.text = product.name
                description.isGone = true
                amount.text =
                    HtmlCompat.fromHtml(
                        _fragment.getString(
                            R.string.dashboard_productsSold_n,
                            CurrencyFormat.format(
                                amountSold,
                                AppCompatDelegate.getApplicationLocales().toLanguageTags(),
                                "",
                                CurrencyFormat.countDecimalPlace(amountSold))),
                        HtmlCompat.FROM_HTML_MODE_LEGACY)
                image.shapeableImage.shapeAppearanceModel =
                    ShapeAppearanceModel.builder()
                        .setAllCornerSizes(_fragment.resources.getDimension(R.dimen.corner_small))
                        .build()
                image.text.text = product.name.take(1)
              }
              .root)
    }
  }

  enum class OverviewType {
    TOTAL_QUEUES,
    UNCOMPLETED_QUEUES,
    ACTIVE_CUSTOMERS,
    PRODUCTS_SOLD
  }
}
