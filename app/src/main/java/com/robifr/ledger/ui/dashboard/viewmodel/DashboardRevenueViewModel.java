/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.ui.dashboard.viewmodel;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.robifr.ledger.assetbinding.chart.ChartUtil;
import com.robifr.ledger.data.display.QueueDate;
import com.robifr.ledger.data.model.ProductOrderModel;
import com.robifr.ledger.data.model.QueueModel;
import com.robifr.ledger.ui.dashboard.DashboardRevenue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DashboardRevenueViewModel {
  @NonNull private final DashboardViewModel _viewModel;

  @NonNull
  private final MutableLiveData<DashboardRevenue.OverviewType> _displayedChart =
      new MutableLiveData<>();

  @NonNull
  private final MutableLiveData<DashboardRevenue.ChartModel> _chartModel = new MutableLiveData<>();

  @NonNull private final MediatorLiveData<BigDecimal> _receivedIncome = new MediatorLiveData<>();
  @NonNull private final MediatorLiveData<BigDecimal> _projectedIncome = new MediatorLiveData<>();

  public DashboardRevenueViewModel(
      @NonNull DashboardViewModel viewModel, @NonNull LiveData<List<QueueModel>> queuesLiveData) {
    Objects.requireNonNull(queuesLiveData);

    this._viewModel = Objects.requireNonNull(viewModel);

    this._receivedIncome.addSource(
        queuesLiveData,
        queues -> {
          if (queues != null) {
            this._receivedIncome.setValue(
                queues.stream()
                    // Received income are from the completed queues.
                    .filter(queue -> queue.status() == QueueModel.Status.COMPLETED)
                    .flatMap(queue -> queue.productOrders().stream())
                    .map(ProductOrderModel::totalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
          }
        });
    this._projectedIncome.addSource(
        queuesLiveData,
        queues -> {
          if (queues != null) {
            this._projectedIncome.setValue(
                queues.stream()
                    .flatMap(queue -> queue.productOrders().stream())
                    .map(ProductOrderModel::totalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
          }
        });
  }

  @NonNull
  public LiveData<DashboardRevenue.OverviewType> displayedChart() {
    return this._displayedChart;
  }

  @NonNull
  public LiveData<DashboardRevenue.ChartModel> chartModel() {
    return this._chartModel;
  }

  @NonNull
  public LiveData<BigDecimal> receivedIncome() {
    return this._receivedIncome;
  }

  @NonNull
  public LiveData<BigDecimal> projectedIncome() {
    return this._projectedIncome;
  }

  public void onDisplayedChartChanged(@NonNull DashboardRevenue.OverviewType overviewType) {
    Objects.requireNonNull(overviewType);

    this._displayedChart.setValue(overviewType);
  }

  public void onDisplayReceivedIncomeChart() {
    final QueueDate date = this._viewModel.date().getValue();
    final List<QueueModel> queues = this._viewModel._queues().getValue();
    if (date == null || queues == null) return;

    final Map<ZonedDateTime, BigDecimal> unformattedQueueDateWithTotalPrice = new LinkedHashMap<>();

    for (QueueModel queue : queues) {
      // Received income are from the completed queues.
      if (queue.status() != QueueModel.Status.COMPLETED) continue;

      for (ProductOrderModel productOrder : queue.productOrders()) {
        unformattedQueueDateWithTotalPrice.merge(
            queue.date().atZone(ZoneId.systemDefault()),
            productOrder.totalPrice(),
            BigDecimal::add);
      }
    }

    final ZonedDateTime startDate =
        date.range() == QueueDate.Range.ALL_TIME
            // Remove unnecessary dates.
            ? queues.stream()
                .map(QueueModel::date)
                .min(Instant::compareTo)
                .orElse(date.dateStart().toInstant())
                .atZone(ZoneId.systemDefault())
            : date.dateStart();
    final Map<String, BigDecimal> queueDateWithTotalPrice =
        ChartUtil.toDateTimeData(
            unformattedQueueDateWithTotalPrice, new Pair<>(startDate, date.dateEnd()));

    final List<String> xAxisDomain = new ArrayList<>(queueDateWithTotalPrice.keySet());
    // Convert to percent because D3.js can't handle big decimal.
    final List<String> yAxisDomain = ChartUtil.toPercentageLinearDomain(queueDateWithTotalPrice);

    this._chartModel.setValue(
        new DashboardRevenue.ChartModel(
            xAxisDomain,
            yAxisDomain,
            ChartUtil.toPercentageData(queueDateWithTotalPrice, LinkedHashMap::new)));
  }

  public void onDisplayProjectedIncomeChart() {
    final QueueDate date = this._viewModel.date().getValue();
    final List<QueueModel> queues = this._viewModel._queues().getValue();
    if (date == null || queues == null) return;

    final Map<ZonedDateTime, BigDecimal> unformattedQueueDateWithTotalPrice = new LinkedHashMap<>();

    for (QueueModel queue : queues) {
      for (ProductOrderModel productOrder : queue.productOrders()) {
        unformattedQueueDateWithTotalPrice.merge(
            queue.date().atZone(ZoneId.systemDefault()),
            productOrder.totalPrice(),
            BigDecimal::add);
      }
    }

    final ZonedDateTime startDate =
        date.range() == QueueDate.Range.ALL_TIME
            // Remove unnecessary dates.
            ? queues.stream()
                .map(QueueModel::date)
                .min(Instant::compareTo)
                .orElse(date.dateStart().toInstant())
                .atZone(ZoneId.systemDefault())
            : date.dateStart();
    final Map<String, BigDecimal> queueDateWithTotalPrice =
        ChartUtil.toDateTimeData(
            unformattedQueueDateWithTotalPrice, new Pair<>(startDate, date.dateEnd()));

    final List<String> xAxisDomain = new ArrayList<>(queueDateWithTotalPrice.keySet());
    // Convert to percent because D3.js can't handle big decimal.
    final List<String> yAxisDomain = ChartUtil.toPercentageLinearDomain(queueDateWithTotalPrice);

    this._chartModel.setValue(
        new DashboardRevenue.ChartModel(
            xAxisDomain,
            yAxisDomain,
            ChartUtil.toPercentageData(queueDateWithTotalPrice, LinkedHashMap::new)));
  }
}
