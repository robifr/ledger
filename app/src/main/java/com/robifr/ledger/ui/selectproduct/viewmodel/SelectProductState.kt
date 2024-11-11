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

package com.robifr.ledger.ui.selectproduct.viewmodel

import androidx.annotation.StringRes
import com.robifr.ledger.R
import com.robifr.ledger.data.model.ProductModel

data class SelectProductState(
    val initialSelectedProduct: ProductModel?,
    val selectedProductOnDatabase: ProductModel?,
    val products: List<ProductModel>,
    /** Currently expanded product index from [products]. -1 to represent none being expanded. */
    val expandedProductIndex: Int,
    val isSelectedProductPreviewExpanded: Boolean
) {
  @get:StringRes
  val selectedItemDescriptionStringRes: Int?
    get() =
        // The original product in the database was deleted.
        if (initialSelectedProduct != null && selectedProductOnDatabase == null) {
          R.string.selectProduct_originalProductDeleted
          // The original product in the database was edited.
        } else if (initialSelectedProduct != null &&
            initialSelectedProduct != selectedProductOnDatabase) {
          R.string.selectProduct_originalProductChanged
          // It’s the same unchanged product.
        } else {
          null
        }
}
