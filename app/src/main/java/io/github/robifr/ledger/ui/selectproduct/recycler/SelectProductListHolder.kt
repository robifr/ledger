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

package io.github.robifr.ledger.ui.selectproduct.recycler

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import io.github.robifr.ledger.R
import io.github.robifr.ledger.components.ProductCardWideComponent
import io.github.robifr.ledger.data.model.ProductModel
import io.github.robifr.ledger.data.model.ProductPaginatedInfo
import io.github.robifr.ledger.databinding.ProductCardWideBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder

class SelectProductListHolder(
    private val _cardBinding: ProductCardWideBinding,
    private val _initialSelectedProductIds: () -> List<Long>,
    private val _products: () -> List<ProductPaginatedInfo>,
    private val _onProductSelected: (ProductPaginatedInfo) -> Unit,
    private val _onExpandedProductIndexChanged: (Int) -> Unit,
    private val _expandedProduct: () -> ProductModel?
) : RecyclerViewHolder(_cardBinding.root), View.OnClickListener {
  private var _productIndex: Int = -1
  private val _card: ProductCardWideComponent =
      ProductCardWideComponent(itemView.context, _cardBinding)

  init {
    _cardBinding.cardView.setOnClickListener { _onProductSelected(_products()[_productIndex]) }
    // Don't set menu button to gone as the position will be occupied by expand button.
    _cardBinding.normalCard.menuButton.isInvisible = true
    _cardBinding.normalCard.expandButton.isVisible = true
    _cardBinding.normalCard.expandButton.setOnClickListener(this)
    _cardBinding.expandedCard.menuButton.isInvisible = true
    _cardBinding.expandedCard.expandButton.isVisible = true
    _cardBinding.expandedCard.expandButton.setOnClickListener(this)
  }

  override fun bind(itemIndex: Int) {
    _productIndex = itemIndex
    _card.reset()
    _products()[_productIndex].fullModel?.let { _card.setNormalCardProduct(it) }
    _card.setCardChecked(_initialSelectedProductIds().contains(_products()[_productIndex].id))
    _setCardExpanded(_products()[_productIndex].id == _expandedProduct()?.id)
  }

  override fun onClick(view: View?) {
    when (view?.id) {
      R.id.expandButton -> _onExpandedProductIndexChanged(_productIndex)
    }
  }

  private fun _setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded) _products()[_productIndex].fullModel?.let { _card.setExpandedCardProduct(it) }
  }
}
