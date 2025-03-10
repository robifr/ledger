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

package io.github.robifr.ledger.ui.product.viewmodel

import io.github.robifr.ledger.data.display.ProductSortMethod
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.ui.common.pagination.PaginationState

/**
 * @property expandedProductIndex Currently expanded product index from
 *   [PaginationState.paginatedItems]. -1 to represent none being expanded.
 */
data class ProductState(
    val pagination: PaginationState<ProductPaginatedInfo>,
    val expandedProductIndex: Int,
    val isProductMenuDialogShown: Boolean,
    val selectedProductMenu: ProductPaginatedInfo?,
    val isNoProductsAddedIllustrationVisible: Boolean,
    val sortMethod: ProductSortMethod,
    val isSortMethodDialogShown: Boolean
) {
  val expandedProduct: ProductModel?
    get() =
        // The full model is always loaded by default.
        if (expandedProductIndex != -1) pagination.paginatedItems[expandedProductIndex].fullModel
        else null
}
