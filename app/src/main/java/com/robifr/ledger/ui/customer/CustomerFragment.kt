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

package com.robifr.ledger.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.ListableFragmentBinding
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.customer.filter.CustomerFilter
import com.robifr.ledger.ui.customer.recycler.CustomerAdapter
import com.robifr.ledger.ui.customer.viewmodel.CustomerFilterState
import com.robifr.ledger.ui.customer.viewmodel.CustomerState
import com.robifr.ledger.ui.customer.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val customerViewModel: CustomerViewModel by activityViewModels()
  private lateinit var _sort: CustomerSort
  private lateinit var _filter: CustomerFilter
  private lateinit var _customerMenu: CustomerMenu
  private lateinit var _adapter: CustomerAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _sort = CustomerSort(this)
    _filter = CustomerFilter(this)
    _customerMenu = CustomerMenu(this, customerViewModel::onCustomerMenuDialogClosed)
    _adapter = CustomerAdapter(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val cutoutInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
      view.updatePadding(top = systemBarInsets.top, right = cutoutInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_main)
    fragmentBinding.toolbar.navigationIcon = null
    fragmentBinding.toolbar.setTitle(R.string.appName)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.noDataCreated.image.setImageResource(R.drawable.image_create_3d)
    fragmentBinding.noDataCreated.title.setText(R.string.customer_noCustomersAdded)
    fragmentBinding.noDataCreated.description.setText(
        R.string.customer_noCustomersAdded_description)
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    customerViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    customerViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    customerViewModel.recyclerAdapterState.observe(viewLifecycleOwner, ::_onRecyclerAdapterState)
    customerViewModel.filterView.uiState.observe(viewLifecycleOwner, ::_onFilterState)
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.search -> {
          findNavController().navigate(R.id.searchFragment)
          true
        }
        R.id.settings -> {
          findNavController().navigate(R.id.settingsFragment)
          true
        }
        else -> false
      }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: CustomerState) {
    if (state.isSortMethodDialogShown) _sort.showDialog(state.sortMethod) else _sort.dismissDialog()
    if (state.isCustomerMenuDialogShown) {
      state.selectedCustomerMenu?.let {
        _customerMenu.showDialog(it, customerViewModel::onDeleteCustomer)
      }
    } else {
      _customerMenu.dismissDialog()
    }
    fragmentBinding.noDataCreatedContainer.isVisible = state.isNoCustomersAddedIllustrationVisible
    fragmentBinding.recyclerView.isVisible = !state.isNoCustomersAddedIllustrationVisible
  }

  private fun _onRecyclerAdapterState(state: RecyclerAdapterState) {
    when (state) {
      is RecyclerAdapterState.DataSetChanged -> _adapter.notifyDataSetChanged()
      is RecyclerAdapterState.ItemChanged ->
          state.indexes.forEach { _adapter.notifyItemChanged(it) }
    }
  }

  private fun _onFilterState(state: CustomerFilterState) {
    if (state.isDialogShown) _filter.showDialog() else _filter.dismissDialog()
    _filter.filterBalance.setFilteredMinBalanceText(state.formattedMinBalance)
    _filter.filterBalance.setFilteredMaxBalanceText(state.formattedMaxBalance)
    _filter.filterDebt.setFilteredMinDebtText(state.formattedMinDebt)
    _filter.filterDebt.setFilteredMaxDebtText(state.formattedMaxDebt)
  }
}
