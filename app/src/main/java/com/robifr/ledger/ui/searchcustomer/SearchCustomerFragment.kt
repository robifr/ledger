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

package com.robifr.ledger.ui.searchcustomer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.SearchableFragmentBinding
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.customer.CustomerMenu
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.search.viewmodel.SearchViewModel
import com.robifr.ledger.ui.searchcustomer.recycler.SearchCustomerAdapter
import com.robifr.ledger.ui.searchcustomer.viewmodel.SearchCustomerResultState
import com.robifr.ledger.ui.searchcustomer.viewmodel.SearchCustomerState
import com.robifr.ledger.ui.searchcustomer.viewmodel.SearchCustomerViewModel
import com.robifr.ledger.util.hideKeyboard
import com.robifr.ledger.util.showKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchCustomerFragment : Fragment(), SearchView.OnQueryTextListener {
  private var _fragmentBinding: SearchableFragmentBinding? = null
  val fragmentBinding: SearchableFragmentBinding
    get() = _fragmentBinding!!

  val searchCustomerViewModel: SearchCustomerViewModel by viewModels()
  private val _searchViewModel: SearchViewModel by
      viewModels({ if (parentFragment !is NavHostFragment) requireParentFragment() else this })
  private lateinit var _customerMenu: CustomerMenu
  private lateinit var _adapter: SearchCustomerAdapter
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SearchableFragmentBinding.inflate(inflater, container, false)
    _customerMenu = CustomerMenu(this, searchCustomerViewModel::onCustomerMenuDialogClosed)
    _adapter = SearchCustomerAdapter(this)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { _, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      fragmentBinding.appBarLayout.updatePadding(
          top = systemBarInsets.top, left = windowInsets.left, right = windowInsets.right)
      fragmentBinding.noResultsImageContainer.updatePadding(
          left = windowInsets.left, right = windowInsets.right)
      fragmentBinding.recyclerView.updatePadding(
          left = windowInsets.left, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.appBarLayout.isVisible =
        searchCustomerViewModel.uiState.safeValue.isToolbarVisible
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.searchView.queryHint = getString(R.string.searchCustomer)
    fragmentBinding.searchView.setOnQueryTextListener(this)
    fragmentBinding.noResultsImage.image.setImageResource(R.drawable.image_search_3d)
    fragmentBinding.noResultsImage.title.setText(R.string.searchCustomer_noResultsFound)
    fragmentBinding.noResultsImage.description.setText(
        R.string.searchCustomer_noResultsFound_description)
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    searchCustomerViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    searchCustomerViewModel.resultState.observe(viewLifecycleOwner, ::_onResultState)
    searchCustomerViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    searchCustomerViewModel.recyclerAdapterState.observe(
        viewLifecycleOwner, ::_onRecyclerAdapterState)
    _searchViewModel.uiState.observe(viewLifecycleOwner, ::_onSearchUiState)

    if (searchCustomerViewModel.uiState.safeValue.isToolbarVisible) {
      if (searchCustomerViewModel.uiState.safeValue.initialQuery.isNotEmpty()) {
        fragmentBinding.searchView.setQuery(
            searchCustomerViewModel.uiState.safeValue.initialQuery, true)
      } else {
        fragmentBinding.searchView.requestFocus()
        fragmentBinding.searchView.showKeyboard()
      }
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean = false

  override fun onQueryTextChange(newText: String?): Boolean {
    newText?.let { searchCustomerViewModel.onSearch(it) }
    return true
  }

  fun finish() {
    requireView().hideKeyboard()
    findNavController().popBackStack()
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onResultState(state: SearchCustomerResultState) {
    if (searchCustomerViewModel.uiState.safeValue.isSelectionEnabled) {
      parentFragmentManager.setFragmentResult(
          Request.SELECT_CUSTOMER.key(),
          Bundle().apply {
            state.selectedCustomerId?.let { putLong(Result.SELECTED_CUSTOMER_ID_LONG.key(), it) }
          })
    }
    finish()
  }

  private fun _onUiState(state: SearchCustomerState) {
    // FIXME: Customer menu dialog doesn't get retained (it close) when configuration changes.
    if (state.isCustomerMenuDialogShown) {
      state.selectedCustomerMenu?.let {
        _customerMenu.showDialog(it, searchCustomerViewModel::onDeleteCustomer)
      }
    } else {
      _customerMenu.dismissDialog()
    }
    fragmentBinding.noResultsImageContainer.isVisible = state.isNoResultFoundIllustrationVisible
    fragmentBinding.recyclerView.isVisible = state.isRecyclerViewVisible
  }

  private fun _onRecyclerAdapterState(state: RecyclerAdapterState) {
    when (state) {
      is RecyclerAdapterState.DataSetChanged -> _adapter.notifyDataSetChanged()
      is RecyclerAdapterState.ItemChanged ->
          state.indexes.forEach { _adapter.notifyItemChanged(it) }
    }
  }

  private fun _onSearchUiState(state: SearchState) {
    searchCustomerViewModel.onSearchUiStateChanged(state)
  }

  enum class Arguments : FragmentResultKey {
    IS_SELECTION_ENABLED_BOOLEAN,
    IS_TOOLBAR_VISIBLE_BOOLEAN,
    INITIAL_QUERY_STRING,
    INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY
  }

  enum class Request : FragmentResultKey {
    SELECT_CUSTOMER
  }

  enum class Result : FragmentResultKey {
    SELECTED_CUSTOMER_ID_LONG
  }
}

private class OnBackPressedHandler(private val _fragment: SearchCustomerFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.searchCustomerViewModel.onCustomerSelected(null)
  }
}
