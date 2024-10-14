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

package com.robifr.ledger.ui.createqueue.viewmodel

import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import java.math.BigDecimal

data class MakeProductOrderState(
    val isDialogShown: Boolean,
    val product: ProductModel?,
    val formattedQuantity: String,
    val formattedDiscount: String,
    val totalPrice: BigDecimal,
    /**
     * Reference to the original product order that will be edited. This value shouldn't change
     * during the editing process.
     */
    val productOrderToEdit: ProductOrderModel?
) {
  val isSaveButtonEnabled: Boolean
    get() = product != null
}
