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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.robifr.ledger.databinding.ListableListTextBinding
import com.robifr.ledger.databinding.ProductCardWideBinding
import com.robifr.ledger.ui.RecyclerViewHolder
import com.robifr.ledger.ui.product.ProductFragment

class ProductAdapter(private val _fragment: ProductFragment) :
    RecyclerView.Adapter<RecyclerViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder =
      when (ViewType.entries.find { it.value == viewType }) {
        ViewType.HEADER ->
            ProductHeaderHolder(
                _textBinding =
                    ListableListTextBinding.inflate(_fragment.layoutInflater, parent, false),
                _products = { _fragment.productViewModel.uiState.safeValue.products })
        else ->
            ProductListHolder(
                _cardBinding =
                    ProductCardWideBinding.inflate(_fragment.layoutInflater, parent, false),
                _products = { _fragment.productViewModel.uiState.safeValue.products },
                _expandedProductIndex = {
                  _fragment.productViewModel.uiState.safeValue.expandedProductIndex
                },
                _onExpandedProductIndexChanged =
                    _fragment.productViewModel::onExpandedProductIndexChanged,
                _onProductMenuDialogShown = _fragment.productViewModel::onProductMenuDialogShown)
      }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is ProductHeaderHolder -> holder.bind()
      is ProductListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.productViewModel.uiState.safeValue.products.size + 1

  override fun getItemViewType(position: Int): Int =
      when (position) {
        0 -> ViewType.HEADER.value
        else -> ViewType.LIST.value
      }

  private enum class ViewType(val value: Int) {
    HEADER(0),
    LIST(1)
  }
}
