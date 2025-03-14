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

package io.github.robifr.ledger.ui.createqueue.viewmodel

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import io.github.robifr.ledger.R
import io.github.robifr.ledger.data.display.LanguageOption
import io.github.robifr.ledger.data.model.CustomerModel
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.data.model.QueueModel
import java.time.ZonedDateTime

/**
 * @property temporalCustomer Current inputted customer with changes to data like balance or debt as
 *   an overview before doing the actual transaction.
 */
data class CreateQueueState(
    val customer: CustomerModel?,
    val temporalCustomer: CustomerModel?,
    val date: ZonedDateTime,
    val status: QueueModel.Status,
    val isStatusDialogShown: Boolean,
    val paymentMethod: QueueModel.PaymentMethod,
    val allowedPaymentMethods: Set<QueueModel.PaymentMethod>,
    val productOrders: List<ProductOrderModel>,
    val note: String
) {
  val isCustomerEndIconVisible: Boolean
    get() = customer != null

  val isTemporalCustomerSummaryVisible: Boolean
    get() = customer != null

  @get:ColorRes
  val customerDebtColorRes: Int
    get() =
        if (temporalCustomer != null && temporalCustomer.debt.compareTo(0.toBigDecimal()) < 0) {
          R.color.red
        } else {
          R.color.text_enabled
        }

  val isPaymentMethodVisible: Boolean
    get() = status == QueueModel.Status.COMPLETED

  @StringRes
  fun dateFormat(): Int =
      LanguageOption.entries
          .find { it.languageTag == AppCompatDelegate.getApplicationLocales().toLanguageTags() }
          ?.fullDateFormat ?: LanguageOption.ENGLISH_US.fullDateFormat
}
