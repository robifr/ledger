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

package com.robifr.ledger.ui.queue.viewmodel

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.robifr.ledger.data.display.LanguageOption
import com.robifr.ledger.data.display.QueueDate
import com.robifr.ledger.data.model.QueueModel

data class QueueFilterState(
    val isDialogShown: Boolean,
    val isNullCustomerShown: Boolean,
    val customerIds: List<Long>,
    val date: QueueDate,
    val status: Set<QueueModel.Status>,
    val formattedMinTotalPrice: String,
    val formattedMaxTotalPrice: String
) {
  @StringRes
  fun dateFormat(): Int =
      LanguageOption.entries
          .find { it.languageTag == AppCompatDelegate.getApplicationLocales().toLanguageTags() }
          ?.shortDateFormat ?: LanguageOption.ENGLISH_US.shortDateFormat
}
