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

package com.robifr.ledger.ui.dashboard.chart

import androidx.annotation.ColorRes
import com.robifr.ledger.assetbinding.chart.ChartBinding
import com.robifr.ledger.assetbinding.chart.ChartData
import com.robifr.ledger.data.model.QueueModel
import java.time.ZonedDateTime

sealed interface SummaryChartModel

/** @see ChartBinding.renderBarChart */
data class TotalQueuesChartModel(
    val xAxisDomain: List<String>,
    val yAxisDomain: List<Double>,
    val data: List<ChartData.Single<String, Int>>
) : SummaryChartModel

/**
 * @param data The [ChartData.Single] contains:
 * - [key][ChartData.Single.key]: [stringRes][QueueModel.Status.stringRes] in [QueueModel.Status].
 * - [value][ChartData.Single.value]: Total count of queues with the corresponding status.
 *
 * @param colors The [backgroundColorRes][QueueModel.Status.backgroundColorRes] in
 *   [QueueModel.Status].
 * @param oldestDate Oldest date for ranged queue to be shown in the center of donut chart.
 * @see ChartBinding.renderDonutChart
 */
data class UncompletedQueuesChartModel(
    val data: List<ChartData.Single<Int, Int>>,
    @ColorRes val colors: List<Int>,
    val oldestDate: ZonedDateTime?
) : SummaryChartModel
