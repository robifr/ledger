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

package com.robifr.ledger.data.display

import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.data.model.QueueModel
import java.text.Collator
import java.util.Locale

class QueueSorter(
    var sortMethod: QueueSortMethod = QueueSortMethod(QueueSortMethod.SortBy.CUSTOMER_NAME, true)
) {
  fun sort(queues: List<QueueModel>): List<QueueModel> =
      when (sortMethod.sortBy) {
        QueueSortMethod.SortBy.CUSTOMER_NAME -> _sortByCustomerName(queues)
        QueueSortMethod.SortBy.DATE -> _sortByDate(queues)
        QueueSortMethod.SortBy.TOTAL_PRICE -> _sortByTotalPrice(queues)
      }

  private fun _sortByCustomerName(queues: List<QueueModel>): List<QueueModel> {
    val collator: Collator =
        Collator.getInstance(
                Locale.forLanguageTag(AppCompatDelegate.getApplicationLocales().toLanguageTags()))
            .apply { strength = Collator.SECONDARY }
    val comparator: Comparator<QueueModel> =
        Comparator.comparing<QueueModel, String?>(
            { it.customer?.name }, Comparator.nullsLast(collator))
    return queues.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }

  private fun _sortByDate(queues: List<QueueModel>): List<QueueModel> {
    val comparator: Comparator<QueueModel> = Comparator.comparing(QueueModel::date)
    return queues.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }

  private fun _sortByTotalPrice(queues: List<QueueModel>): List<QueueModel> {
    val comparator: Comparator<QueueModel> = Comparator.comparing(QueueModel::grandTotalPrice)
    return queues.sortedWith(if (sortMethod.isAscending) comparator else comparator.reversed())
  }
}
