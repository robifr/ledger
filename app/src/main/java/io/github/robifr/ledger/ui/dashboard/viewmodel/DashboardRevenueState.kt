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

package io.github.robifr.ledger.ui.dashboard.viewmodel

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import io.github.robifr.ledger.data.display.LanguageOption
import io.github.robifr.ledger.data.display.QueueDate
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.ui.dashboard.DashboardRevenue
import java.math.BigDecimal

data class DashboardRevenueState(
    val isDateDialogShown: Boolean,
    val date: QueueDate,
    val queues: List<QueuePaginatedInfo>,
    val displayedChart: DashboardRevenue.OverviewType
) {
  @StringRes
  fun dateFormat(): Int =
      LanguageOption.entries
          .find { it.languageTag == AppCompatDelegate.getApplicationLocales().toLanguageTags() }
          ?.shortDateFormat ?: LanguageOption.ENGLISH_US.shortDateFormat

  /** Total income from the completed queues only. */
  fun receivedIncome(): BigDecimal =
      queues.filter { it.status == QueueModel.Status.COMPLETED }.sumOf { it.grandTotalPrice }

  /** Total income from any queues. */
  fun projectedIncome(): BigDecimal = queues.sumOf { it.grandTotalPrice }
}
