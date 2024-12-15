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

package com.robifr.ledger.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.robifr.ledger.R
import com.robifr.ledger.components.CustomerCardPackedComponent
import com.robifr.ledger.components.ProductCardPackedComponent
import com.robifr.ledger.databinding.CustomerCardPackedBinding
import com.robifr.ledger.databinding.ProductCardPackedBinding
import com.robifr.ledger.databinding.SearchableFragmentBinding
import com.robifr.ledger.databinding.SearchableListHorizontalBinding
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.search.viewmodel.SearchViewModel
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import com.robifr.ledger.ui.searchproduct.SearchProductFragment
import com.robifr.ledger.util.getColorAttr
import com.robifr.ledger.util.hideKeyboard
import com.robifr.ledger.util.showKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(), SearchView.OnQueryTextListener {
  private var _fragmentBinding: SearchableFragmentBinding? = null
  val fragmentBinding: SearchableFragmentBinding
    get() = _fragmentBinding!!

  private val _searchViewModel: SearchViewModel by viewModels()
  private lateinit var _customerListBinding: SearchableListHorizontalBinding
  private lateinit var _productListBinding: SearchableListHorizontalBinding
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SearchableFragmentBinding.inflate(inflater, container, false)
    _customerListBinding = SearchableListHorizontalBinding.inflate(inflater, container, false)
    _productListBinding = SearchableListHorizontalBinding.inflate(inflater, container, false)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity().window.statusBarColor = requireContext().getColor(R.color.surface)
    requireActivity().window.navigationBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.searchView.queryHint = getString(R.string.searchCustomersAndProducts)
    fragmentBinding.searchView.setOnQueryTextListener(this)
    fragmentBinding.noResultsImage.image.setImageResource(R.drawable.image_noresultsfound)
    fragmentBinding.noResultsImage.title.setText(R.string.searchCustomersAndProducts_noResultsFound)
    fragmentBinding.noResultsImage.description.setText(
        R.string.searchCustomersAndProducts_noResultsFound_description)
    fragmentBinding.recyclerView.isGone = true
    fragmentBinding.horizontalListContainer.addView(_customerListBinding.root)
    fragmentBinding.horizontalListContainer.addView(_productListBinding.root)
    _customerListBinding.title.setText(R.string.searchCustomersAndProducts_customersFound)
    _customerListBinding.viewMoreButton.setOnClickListener {
      findNavController()
          .navigate(
              R.id.searchCustomerFragment,
              Bundle().apply {
                putString(
                    SearchCustomerFragment.Arguments.INITIAL_QUERY_STRING.key(),
                    _searchViewModel.uiState.safeValue.query)
              })
    }
    _productListBinding.title.setText(R.string.searchCustomersAndProducts_productsFound)
    _productListBinding.viewMoreButton.setOnClickListener {
      findNavController()
          .navigate(
              R.id.searchProductFragment,
              Bundle().apply {
                putString(
                    SearchProductFragment.Arguments.INITIAL_QUERY_STRING.key(),
                    _searchViewModel.uiState.safeValue.query)
              })
    }
    _searchViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)

    if (_searchViewModel.uiState.safeValue.query.isEmpty()) {
      fragmentBinding.searchView.requestFocus()
      fragmentBinding.searchView.showKeyboard()
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean = false

  override fun onQueryTextChange(newText: String?): Boolean {
    newText?.let { _searchViewModel.onSearch(it) }
    return true
  }

  fun finish() {
    requireView().hideKeyboard()
    findNavController().popBackStack()
  }

  private fun _onUiState(state: SearchState) {
    fragmentBinding.noResultsImage.root.isVisible = state.isNoResultFoundIllustrationVisible

    _customerListBinding.root.isVisible = state.isCustomerListVisible
    _customerListBinding.listContainer.removeAllViews()
    for (customer in state.customers) {
      val cardBinding: CustomerCardPackedBinding =
          CustomerCardPackedBinding.inflate(layoutInflater, _customerListBinding.root, false)
      CustomerCardPackedComponent(requireContext(), cardBinding).setCustomer(customer)
      _customerListBinding.listContainer.addView(cardBinding.root)
    }

    _productListBinding.root.isVisible = state.isProductListVisible
    _productListBinding.listContainer.removeAllViews()
    for (product in state.products) {
      val cardBinding: ProductCardPackedBinding =
          ProductCardPackedBinding.inflate(layoutInflater, _productListBinding.root, false)
      ProductCardPackedComponent(requireContext(), cardBinding).setProduct(product)
      _productListBinding.listContainer.addView(cardBinding.root)
    }
  }
}

private class OnBackPressedHandler(private val _fragment: SearchFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.finish()
  }
}
