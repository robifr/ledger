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

package io.github.robifr.ledger.ui.customer.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.robifr.ledger.databinding.CustomerCardWideBinding
import io.github.robifr.ledger.databinding.ListableListTextBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder
import io.github.robifr.ledger.ui.customer.CustomerFragment

class CustomerAdapter(private val _fragment: CustomerFragment) :
    RecyclerView.Adapter<RecyclerViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder =
      when (ViewType.entries.find { it.value == viewType }) {
        ViewType.HEADER ->
            CustomerHeaderHolder(
                _textBinding =
                    ListableListTextBinding.inflate(_fragment.layoutInflater, parent, false),
                _totalCustomers = {
                  _fragment.customerViewModel.uiState.safeValue.pagination.totalItem
                })
        else ->
            CustomerListHolder(
                _cardBinding =
                    CustomerCardWideBinding.inflate(_fragment.layoutInflater, parent, false),
                _customers = {
                  _fragment.customerViewModel.uiState.safeValue.pagination.paginatedItems
                },
                _onExpandedCustomerIndexChanged =
                    _fragment.customerViewModel::onExpandedCustomerIndexChanged,
                _expandedCustomer = {
                  _fragment.customerViewModel.uiState.safeValue.expandedCustomer
                },
                _onCustomerMenuDialogShown = _fragment.customerViewModel::onCustomerMenuDialogShown)
      }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is CustomerHeaderHolder -> holder.bind()
      is CustomerListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.customerViewModel.uiState.safeValue.pagination.paginatedItems.size + 1

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
