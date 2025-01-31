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
import com.robifr.ledger.ui.product.filter.ProductFilter
import com.robifr.ledger.ui.product.recycler.ProductAdapter
import com.robifr.ledger.ui.product.viewmodel.ProductEvent
import com.robifr.ledger.ui.product.viewmodel.ProductFilterState
import com.robifr.ledger.ui.product.viewmodel.ProductState
import com.robifr.ledger.ui.product.viewmodel.ProductViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val productViewModel: ProductViewModel by activityViewModels()
  private lateinit var _sort: ProductSort
  private lateinit var _filter: ProductFilter
  private lateinit var _productMenu: ProductMenu
  private lateinit var _adapter: ProductAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _sort = ProductSort(this)
    _filter = ProductFilter(this)
    _productMenu = ProductMenu(this, productViewModel::onProductMenuDialogClosed)
    _adapter = ProductAdapter(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val statusBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(top = statusBarInsets.top, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_main)
    fragmentBinding.toolbar.navigationIcon = null
    fragmentBinding.toolbar.setTitle(R.string.appName)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.noDataCreated.image.setImageResource(R.drawable.image_create_3d)
    fragmentBinding.noDataCreated.title.setText(R.string.product_noProductsAdded)
    fragmentBinding.noDataCreated.description.setText(R.string.product_noProductsAdded_description)
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    productViewModel.uiEvent.observe(viewLifecycleOwner, ::_onUiEvent)
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

  private fun _onUiEvent(event: ProductEvent) {
    event.snackbar?.let {
      _onSnackbarState(it.data)
      it.onConsumed()
    }
    event.recyclerAdapter?.let {
      _onRecyclerAdapterState(it.data)
      it.onConsumed()
    }
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onUiState(state: ProductState) {
    if (state.isSortMethodDialogShown) _sort.showDialog(state.sortMethod) else _sort.dismissDialog()
    if (state.isProductMenuDialogShown) {
      state.selectedProductMenu?.let {
        _productMenu.showDialog(it, productViewModel::onDeleteProduct)
      }
    } else {
      _productMenu.dismissDialog()
    }
    fragmentBinding.noDataCreatedContainer.isVisible = state.isNoProductsAddedIllustrationVisible
    fragmentBinding.recyclerView.isVisible = !state.isNoProductsAddedIllustrationVisible
  }

  private fun _onRecyclerAdapterState(state: RecyclerAdapterState) {
    when (state) {
      is RecyclerAdapterState.DataSetChanged -> _adapter.notifyDataSetChanged()
      is RecyclerAdapterState.ItemChanged ->
          state.indexes.forEach { _adapter.notifyItemChanged(it) }
    }
  }

  private fun _onFilterState(state: ProductFilterState) {
    if (state.isDialogShown) _filter.showDialog() else _filter.dismissDialog()
    _filter.filterPrice.setFilteredMinPriceText(state.formattedMinPrice)
    _filter.filterPrice.setFilteredMaxPriceText(state.formattedMaxPrice)
  }
}
