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
import com.robifr.ledger.ui.dashboard.DashboardRevenue

/** @see ChartBinding.renderStackedBarChart */
data class RevenueChartModel(
    val xAxisDomain: List<String>,
    val yAxisDomain: List<String>,
    val data: List<ChartData.Multiple<String, Double, String>>,
    @ColorRes val colors: List<Int>,
    val groupInOrder: Set<DashboardRevenue.OverviewType>
)
