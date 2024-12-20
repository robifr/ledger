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

package com.robifr.ledger.ui.searchproduct.viewmodel

import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.ui.searchproduct.SearchProductFragment

/**
 * @property isSelectionEnabled Whether the fragment should return
 *   [SearchProductFragment.Request.SELECT_PRODUCT] on back navigation.
 * @property expandedProductIndex Currently expanded product index from [products]. -1 to represent
 *   none being expanded.
 */
data class SearchProductState(
    val isSelectionEnabled: Boolean,
    val isToolbarVisible: Boolean,
    val initialQuery: String,
    val query: String,
    val initialSelectedProductIds: List<Long>,
    val products: List<ProductModel>,
    val expandedProductIndex: Int
) {
  val isNoResultFoundIllustrationVisible: Boolean
    get() = query.isNotEmpty() && products.isEmpty()

  val isRecyclerViewVisible: Boolean
    get() = query.isNotEmpty() && products.isNotEmpty()
}
