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

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.model.CustomerNameInfo
import com.robifr.ledger.data.model.ProductNameInfo
import com.robifr.ledger.data.model.ProductOrderProductInfo
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.data.model.QueuePaginatedInfo
import com.robifr.ledger.ui.dashboard.DashboardSummary
import java.math.BigDecimal

data class DashboardSummaryState(
    val isDateDialogShown: Boolean,
    val date: QueueDate,
    val queues: List<QueuePaginatedInfo>,
    val productsSold: List<ProductOrderProductInfo>,
    val displayedChart: DashboardSummary.OverviewType
) {
  @StringRes
  fun dateFormat(): Int =
      LanguageOption.entries
          .find { it.languageTag == AppCompatDelegate.getApplicationLocales().toLanguageTags() }
          ?.shortDateFormat ?: LanguageOption.ENGLISH_US.shortDateFormat

  fun totalUncompletedQueues(): Int = queues.count { it.status != QueueModel.Status.COMPLETED }

  fun totalActiveCustomers(): Int =
      queues.asSequence().mapNotNull { it.customerId }.distinct().count()

  /** @return Map of the most active customers with their queue counts. */
  fun mostActiveCustomers(): Map<CustomerNameInfo, Int> =
      queues
          .asSequence()
          .mapNotNull { queue ->
            queue.customerName?.let { CustomerNameInfo(queue.customerId, it) }
          }
          .groupingBy { it }
          .eachCount()
          .toList()
          .sortedByDescending { it.second }
          .take(4)
          .toMap()

  fun totalProductsSold(): BigDecimal = productsSold.sumOf { it.quantity.toBigDecimal() }

  /** @return Map of the most products sold with their quantity counts. */
  fun mostProductsSold(): Map<ProductNameInfo, BigDecimal> =
      productsSold
          .asSequence()
          .mapNotNull { productOrder ->
            productOrder.productName?.let {
              ProductNameInfo(productOrder.productId, it) to productOrder.quantity.toBigDecimal()
            }
          }
          .groupBy({ it.first }, { it.second })
          .mapValues { entry -> entry.value.sumOf { it } }
          .toList()
          .sortedByDescending { it.second }
          .take(4)
          .toMap()
}
