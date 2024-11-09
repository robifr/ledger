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

package com.robifr.ledger.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.ListableFragmentBinding
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.product.filter.ProductFilter
import com.robifr.ledger.ui.product.recycler.ProductAdapter
import com.robifr.ledger.ui.product.recycler.ProductListHolder
import com.robifr.ledger.ui.product.viewmodel.ProductFilterState
import com.robifr.ledger.ui.product.viewmodel.ProductState
import com.robifr.ledger.ui.product.viewmodel.ProductViewModel
import com.robifr.ledger.util.getColorAttr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val productViewModel: ProductViewModel by activityViewModels()
  private lateinit var _sort: ProductSort
  private lateinit var _filter: ProductFilter
  private lateinit var _adapter: ProductAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _sort = ProductSort(this)
    _filter = ProductFilter(this)
    _adapter = ProductAdapter(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity().window.statusBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().window.navigationBarColor = requireContext().getColor(R.color.surface)
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_main)
    fragmentBinding.toolbar.navigationIcon = null
    fragmentBinding.toolbar.setTitle(R.string.appName)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    productViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    productViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    productViewModel.filterView.uiState.observe(viewLifecycleOwner, ::_onFilterState)
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

  private fun _onUiState(state: ProductState) {
    _adapter.notifyDataSetChanged()
    for (view in fragmentBinding.recyclerView.children) {
      val viewHolder: RecyclerView.ViewHolder =
          fragmentBinding.recyclerView.getChildViewHolder(view)
      if (viewHolder is ProductListHolder) {
        viewHolder.setCardExpanded(
            state.expandedProductIndex != -1 &&
                // +1 offset because header holder.
                fragmentBinding.recyclerView.getChildAdapterPosition(view) ==
                    state.expandedProductIndex + 1)
      }
    }
  }

  private fun _onFilterState(state: ProductFilterState) {
    _filter.filterPrice.setFilteredMinPriceText(state.formattedMinPrice)
    _filter.filterPrice.setFilteredMaxPriceText(state.formattedMaxPrice)
  }
}
