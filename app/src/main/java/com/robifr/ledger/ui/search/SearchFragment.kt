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
import com.robifr.ledger.ui.OnBackPressedHandler
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
  private lateinit var _adapter: FragmentAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = SearchFragmentBinding.inflate(inflater, container, false)
    _adapter = FragmentAdapter(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { _, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      // When the device is in landscape mode, you’ll notice that the stroke gets cut in tab layout.
      // Unfortunately, there’s no way to avoid applying insets only to the stroke.
      fragmentBinding.appBarLayout.updatePadding(
          top = systemBarInsets.top, left = windowInsets.left, right = windowInsets.right)
      fragmentBinding.noResultsImageContainer.updatePadding(
          left = windowInsets.left, right = windowInsets.right)
      fragmentBinding.viewPager.updatePadding(left = windowInsets.left, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(viewLifecycleOwner, OnBackPressedHandler { finish() })
    fragmentBinding.toolbar.setNavigationOnClickListener { finish() }
    fragmentBinding.searchView.queryHint = getString(R.string.searchCustomersAndProducts)
    fragmentBinding.searchView.setOnQueryTextListener(this)
    fragmentBinding.viewPager.adapter = _adapter
    fragmentBinding.noResultsImage.image.setImageResource(R.drawable.image_search_3d)
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
    fragmentBinding.noResultsImageContainer.isVisible = state.isNoResultFoundIllustrationVisible
    fragmentBinding.viewPager.isVisible = state.isViewPagerVisible
    // Although this code works just fine, there’s something weird with the `ViewPager`. Previously,
    // when using `SingleLiveEvent`, we didn’t even need to recreate the sub fragments (both
    // `_searchCustomerFragment` and `_searchProductFragment` are final properties, not getters nor
    // functions). It didn’t flash/blink, and the data in the sub fragments got refreshed when
    // navigating back from another fragment. For example, within this fragment, if you edit a
    // customer and go back, you’ll notice the differences.
    //
    // Ever since we used events as states, the sub fragment gets recreated, which might seem normal
    // assuming the `ViewPager` has its own fragment management. However, there’s a flash/blink
    // effect, and the data doesn’t update. The problem is resolved by recreating the fragment only
    // if it hasn’t been created before.
    _adapter.setFragmentTabs(
        _adapter.fragmentTabs.toMutableList().apply {
          // We can’t directly compare fragments from `FragmentAdapter.fragmentTabs` with
          // `_searchCustomerFragment` or `_searchProductFragment`. For example,
          // `fragmentTabs[0] == _searchCustomerFragment` will always fail because they're different
          // instances. One key difference is that fragments created by `ViewPager` have tags.
          // Instead of direct comparison, we identify the fragment in the list by checking its
          // instance type.
          _adapter.fragmentTabs
              .find { it is SearchCustomerFragment }
              .let {
                if (state.shouldSearchCustomerFragmentLoaded && it == null) {
                  add(_createSearchCustomerFragment())
                } else if (!state.shouldSearchCustomerFragmentLoaded) {
                  remove(it)
                }
              }
          _adapter.fragmentTabs
              .find { it is SearchProductFragment }
              .let {
                if (state.shouldSearchProductFragmentLoaded && it == null) {
                  add(_createSearchProductFragment())
                } else if (!state.shouldSearchProductFragmentLoaded) {
                  remove(it)
                }
              }
        })
  }

  private fun _createSearchCustomerFragment(): SearchCustomerFragment =
      SearchCustomerFragment().apply {
        arguments =
            Bundle().apply {
              putBoolean(SearchCustomerFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
            }
      }

  private fun _createSearchProductFragment(): SearchProductFragment =
      SearchProductFragment().apply {
        arguments =
            Bundle().apply {
              putBoolean(SearchProductFragment.Arguments.IS_TOOLBAR_VISIBLE_BOOLEAN.key(), false)
            }
      }
}
