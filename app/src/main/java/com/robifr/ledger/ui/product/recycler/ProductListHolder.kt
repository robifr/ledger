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

package com.robifr.ledger.ui.product.recycler

import com.robifr.ledger.components.ProductCardWideComponent
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.databinding.ProductCardWideBinding
import com.robifr.ledger.ui.RecyclerViewHolder

class ProductListHolder(
    private val _cardBinding: ProductCardWideBinding,
    private val _products: () -> List<ProductModel>,
    private val _expandedProductIndex: () -> Int,
    private val _onExpandedProductIndexChanged: (Int) -> Unit,
    private val _onProductMenuDialogShown: (selectedProduct: ProductModel) -> Unit
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
    _card.setNormalCardProduct(_products()[_productIndex])
    _setCardExpanded(
        _expandedProductIndex() != -1 &&
            _products()[_productIndex] == _products()[_expandedProductIndex()])
  }

  private fun _setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded) _card.setExpandedCardProduct(_products()[_productIndex])
  }
}
