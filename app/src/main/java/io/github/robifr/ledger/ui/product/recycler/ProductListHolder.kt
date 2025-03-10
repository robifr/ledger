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

package io.github.robifr.ledger.ui.product.recycler

import io.github.robifr.ledger.components.ProductCardWideComponent
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.databinding.ProductCardWideBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder

class ProductListHolder(
    private val _cardBinding: ProductCardWideBinding,
    private val _products: () -> List<ProductPaginatedInfo>,
    private val _onExpandedProductIndexChanged: (Int) -> Unit,
    private val _expandedProduct: () -> ProductModel?,
    private val _onProductMenuDialogShown: (selectedProduct: ProductPaginatedInfo) -> Unit
) : RecyclerViewHolder(_cardBinding.root) {
  private var _productIndex: Int = -1
  private val _card: ProductCardWideComponent =
      ProductCardWideComponent(itemView.context, _cardBinding)

  init {
    _cardBinding.cardView.setOnClickListener { _onExpandedProductIndexChanged(_productIndex) }
    _cardBinding.normalCard.menuButton.setOnClickListener {
      _onProductMenuDialogShown(_products()[_productIndex])
    }
    _cardBinding.expandedCard.menuButton.setOnClickListener {
      _onProductMenuDialogShown(_products()[_productIndex])
    }
  }

  override fun bind(itemIndex: Int) {
    _productIndex = itemIndex
    _card.reset()
    _products()[_productIndex].fullModel?.let { _card.setNormalCardProduct(it) }
    _setCardExpanded(_products()[_productIndex].id == _expandedProduct()?.id)
  }

  private fun _setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded) _products()[_productIndex].fullModel?.let { _card.setExpandedCardProduct(it) }
  }
}
