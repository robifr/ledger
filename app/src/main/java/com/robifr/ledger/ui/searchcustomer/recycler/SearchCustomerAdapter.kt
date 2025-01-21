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

package com.robifr.ledger.ui.searchcustomer.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.robifr.ledger.databinding.CustomerCardWideBinding
import com.robifr.ledger.databinding.ListableListTextBinding
import com.robifr.ledger.ui.RecyclerViewHolder
import com.robifr.ledger.ui.customer.recycler.CustomerListHolder
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import com.robifr.ledger.ui.selectcustomer.recycler.SelectCustomerListHolder

class SearchCustomerAdapter(private val _fragment: SearchCustomerFragment) :
    RecyclerView.Adapter<RecyclerViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder =
      when (ViewType.entries.find { it.value == viewType }) {
        ViewType.HEADER ->
            SearchCustomerHeaderHolder(
                _textBinding =
                    ListableListTextBinding.inflate(_fragment.layoutInflater, parent, false),
                _customers = { _fragment.searchCustomerViewModel.uiState.safeValue.customers })
        else -> {
          val cardBinding: CustomerCardWideBinding =
              CustomerCardWideBinding.inflate(_fragment.layoutInflater, parent, false)
          if (_fragment.searchCustomerViewModel.uiState.safeValue.isSelectionEnabled) {
            SelectCustomerListHolder(
                _cardBinding = cardBinding,
                _initialSelectedCustomerIds = {
                  _fragment.searchCustomerViewModel.uiState.safeValue.initialSelectedCustomerIds
                },
                _customers = { _fragment.searchCustomerViewModel.uiState.safeValue.customers },
                _onCustomerSelected = _fragment.searchCustomerViewModel::onCustomerSelected,
                _expandedCustomerIndex = {
                  _fragment.searchCustomerViewModel.uiState.safeValue.expandedCustomerIndex
                },
                _onExpandedCustomerIndexChanged =
                    _fragment.searchCustomerViewModel::onExpandedCustomerIndexChanged)
          } else {
            CustomerListHolder(
                _cardBinding = cardBinding,
                _customers = { _fragment.searchCustomerViewModel.uiState.safeValue.customers },
                _expandedCustomerIndex = {
                  _fragment.searchCustomerViewModel.uiState.safeValue.expandedCustomerIndex
                },
                _onExpandedCustomerIndexChanged =
                    _fragment.searchCustomerViewModel::onExpandedCustomerIndexChanged,
                _onCustomerMenuDialogShown =
                    _fragment.searchCustomerViewModel::onCustomerMenuDialogShown)
          }
        }
      }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is SearchCustomerHeaderHolder -> holder.bind()
      is CustomerListHolder -> holder.bind(position - 1)
      is SelectCustomerListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.searchCustomerViewModel.uiState.safeValue.customers.size + 1

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
