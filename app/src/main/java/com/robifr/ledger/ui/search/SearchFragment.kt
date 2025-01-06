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
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.robifr.ledger.R
import com.robifr.ledger.databinding.SearchFragmentBinding
import com.robifr.ledger.ui.FragmentAdapter
import com.robifr.ledger.ui.search.viewmodel.SearchState
import com.robifr.ledger.ui.search.viewmodel.SearchViewModel
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import com.robifr.ledger.ui.searchproduct.SearchProductFragment
import com.robifr.ledger.util.hideKeyboard
import com.robifr.ledger.util.hideTooltipText
import com.robifr.ledger.util.showKeyboard
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(), SearchView.OnQueryTextListener {
  private var _fragmentBinding: SearchFragmentBinding? = null
  val fragmentBinding: SearchFragmentBinding
    get() = _fragmentBinding!!

  private val _searchViewModel: SearchViewModel by viewModels()
  private val _searchCustomerFragment: SearchCustomerFragment =
      SearchCustomerFragment().apply {
        arguments =
            Bundle().apply {
              putBoolean(SearchCustomerFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
            }
      }
  private val _searchProductFragment: SearchProductFragment =
      SearchProductFragment().apply {
        arguments =
            Bundle().apply {
              putBoolean(SearchProductFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
            }
      }
  private lateinit var _adapter: FragmentAdapter
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SearchFragmentBinding.inflate(inflater, container, false)
    _adapter = FragmentAdapter(this)
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
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.searchView.queryHint = getString(R.string.searchCustomersAndProducts)
    fragmentBinding.searchView.setOnQueryTextListener(this)
    fragmentBinding.viewPager.adapter = _adapter
    fragmentBinding.noResultsImage.title.setText(R.string.searchCustomersAndProducts_noResultsFound)
    fragmentBinding.noResultsImage.description.setText(
        R.string.searchCustomersAndProducts_noResultsFound_description)
    _searchViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)

    TabLayoutMediator(fragmentBinding.tabLayout, fragmentBinding.viewPager) { tab, position ->
          tab.view.hideTooltipText()
          tab.text =
              when (_adapter.fragmentTabs[position]) {
                is SearchCustomerFragment ->
                    getString(R.string.searchCustomersAndProducts_customers)
                is SearchProductFragment -> getString(R.string.searchCustomersAndProducts_products)
                else -> null
              }
        }
        .attach()
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
    fragmentBinding.tabLayout.isVisible = state.isTabLayoutVisible
    fragmentBinding.noResultsImage.root.isVisible = state.isNoResultFoundIllustrationVisible
    fragmentBinding.viewPager.isVisible = state.isViewPagerVisible
    _adapter.setFragmentTabs(
        mutableListOf<Fragment>().apply {
          if (state.shouldSearchCustomerFragmentLoaded) add(_searchCustomerFragment)
          if (state.shouldSearchProductFragmentLoaded) add(_searchProductFragment)
        })
  }
}

private class OnBackPressedHandler(private val _fragment: SearchFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.finish()
  }
}
