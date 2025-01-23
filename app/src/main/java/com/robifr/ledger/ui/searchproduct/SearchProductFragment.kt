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

package com.robifr.ledger.ui.searchproduct

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
import com.robifr.ledger.ui.product.ProductMenu
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.search.viewmodel.SearchViewModel
import com.robifr.ledger.ui.searchproduct.recycler.SearchProductAdapter
import com.robifr.ledger.ui.searchproduct.viewmodel.SearchProductResultState
import com.robifr.ledger.ui.searchproduct.viewmodel.SearchProductState
import com.robifr.ledger.ui.searchproduct.viewmodel.SearchProductViewModel
import com.robifr.ledger.util.hideKeyboard
import com.robifr.ledger.util.showKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchProductFragment : Fragment(), SearchView.OnQueryTextListener {
  private var _fragmentBinding: SearchableFragmentBinding? = null
  val fragmentBinding: SearchableFragmentBinding
    get() = _fragmentBinding!!

  val searchProductViewModel: SearchProductViewModel by viewModels()
  private val _searchViewModel: SearchViewModel by
      viewModels({ if (parentFragment !is NavHostFragment) requireParentFragment() else this })
  private lateinit var _productMenu: ProductMenu
  private lateinit var _adapter: SearchProductAdapter
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SearchableFragmentBinding.inflate(inflater, container, false)
    _productMenu = ProductMenu(this, searchProductViewModel::onProductMenuDialogClosed)
    _adapter = SearchProductAdapter(this)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.appBarLayout) { view, insets ->
      val windowInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = windowInsets.top)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.appBarLayout.isVisible =
        searchProductViewModel.uiState.safeValue.isToolbarVisible
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.searchView.queryHint = getString(R.string.searchProduct)
    fragmentBinding.searchView.setOnQueryTextListener(this)
    fragmentBinding.noResultsImage.image.setImageResource(R.drawable.image_search_3d)
    fragmentBinding.noResultsImage.title.setText(R.string.searchProduct_noResultsFound)
    fragmentBinding.noResultsImage.description.setText(
        R.string.searchProduct_noResultsFound_description)
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    searchProductViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    searchProductViewModel.resultState.observe(viewLifecycleOwner, ::_onResultState)
    searchProductViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    searchProductViewModel.recyclerAdapterState.observe(
        viewLifecycleOwner, ::_onRecyclerAdapterState)
    _searchViewModel.uiState.observe(viewLifecycleOwner, ::_onSearchUiState)

    if (searchProductViewModel.uiState.safeValue.isToolbarVisible) {
      if (searchProductViewModel.uiState.safeValue.initialQuery.isNotEmpty()) {
        fragmentBinding.searchView.setQuery(
            searchProductViewModel.uiState.safeValue.initialQuery, true)
      } else {
        fragmentBinding.searchView.requestFocus()
        fragmentBinding.searchView.showKeyboard()
      }
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean = false

  override fun onQueryTextChange(newText: String?): Boolean {
    newText?.let { searchProductViewModel.onSearch(it) }
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

  private fun _onResultState(state: SearchProductResultState) {
    if (searchProductViewModel.uiState.safeValue.isSelectionEnabled) {
      parentFragmentManager.setFragmentResult(
          Request.SELECT_PRODUCT.key(),
          Bundle().apply {
            state.selectedProductId?.let { putLong(Result.SELECTED_PRODUCT_ID_LONG.key(), it) }
          })
    }
    finish()
  }

  private fun _onUiState(state: SearchProductState) {
    // FIXME: Product menu dialog doesn't get retained (it close) when configuration changes.
    if (state.isProductMenuDialogShown) {
      state.selectedProductMenu?.let {
        _productMenu.showDialog(it, searchProductViewModel::onDeleteProduct)
      }
    } else {
      _productMenu.dismissDialog()
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
    searchProductViewModel.onSearchUiStateChanged(state)
  }

  enum class Arguments : FragmentResultKey {
    INITIAL_QUERY_STRING,
    IS_TOOLBAR_VISIBLE_BOOLEAN,
    IS_SELECTION_ENABLED_BOOLEAN,
    INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY
  }

  enum class Request : FragmentResultKey {
    SELECT_PRODUCT
  }

  enum class Result : FragmentResultKey {
    SELECTED_PRODUCT_ID_LONG
  }
}

private class OnBackPressedHandler(private val _fragment: SearchProductFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.searchProductViewModel.onProductSelected(null)
  }
}
