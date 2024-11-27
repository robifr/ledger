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

package com.robifr.ledger.ui.selectproduct.recycler

import android.view.ViewGroup
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.databinding.ListableListSelectedItemBinding
import com.robifr.ledger.databinding.ProductCardWideBinding
import com.robifr.ledger.ui.RecyclerAdapter
import com.robifr.ledger.ui.RecyclerViewHolder
import com.robifr.ledger.ui.selectproduct.SelectProductFragment

class SelectProductAdapter(private val _fragment: SelectProductFragment) :
    RecyclerAdapter<ProductModel, RecyclerViewHolder>(
        _itemToCompare = { ProductModel::id }, _contentToCompare = { ProductModel::hashCode }) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
    return when (ViewType.entries.find { it.value == viewType }) {
      ViewType.HEADER ->
          SelectProductHeaderHolder(
              _selectedItemBinding =
                  ListableListSelectedItemBinding.inflate(_fragment.layoutInflater, parent, false),
              _initialSelectedProduct = {
                _fragment.selectProductViewModel.uiState.safeValue.initialSelectedProduct
              },
              _isSelectedProductPreviewExpanded = {
                _fragment.selectProductViewModel.uiState.safeValue.isSelectedProductPreviewExpanded
              },
              _onSelectedProductPreviewExpanded =
                  _fragment.selectProductViewModel::onSelectedProductPreviewExpanded)
      else ->
          SelectProductListHolder(
              _cardBinding =
                  ProductCardWideBinding.inflate(_fragment.layoutInflater, parent, false),
              _initialSelectedProductIds = {
                listOfNotNull(
                    _fragment.selectProductViewModel.uiState.safeValue.initialSelectedProduct?.id)
              },
              _products = { _fragment.selectProductViewModel.uiState.safeValue.products },
              _onProductSelected = _fragment.selectProductViewModel::onProductSelected,
              _expandedProductIndex = {
                _fragment.selectProductViewModel.uiState.safeValue.expandedProductIndex
              },
              _onExpandedProductIndexChanged =
                  _fragment.selectProductViewModel::onExpandedProductIndexChanged)
    }
  }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is SelectProductHeaderHolder -> holder.bind()
      is SelectProductListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.selectProductViewModel.uiState.safeValue.products.size + 1

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
