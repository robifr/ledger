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

import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.ui.dashboard.DashboardSummary
import java.math.BigDecimal

/**
 * @param mostActiveCustomers Map of the most active customers with their queue counts.
 * @param mostProductsSold Map of the most products sold with their quantity counts.
 */
data class DashboardSummaryState(
    val displayedChart: DashboardSummary.OverviewType,
    val totalQueues: Int,
    val totalUncompletedQueues: Int,
    val totalActiveCustomers: Int,
    val mostActiveCustomers: Map<CustomerModel, Int>,
    val totalProductsSold: BigDecimal,
    val mostProductsSold: Map<ProductModel, BigDecimal>
)
