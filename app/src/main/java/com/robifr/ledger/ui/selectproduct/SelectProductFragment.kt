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

package com.robifr.ledger.ui.selectproduct

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.robifr.ledger.R
import com.robifr.ledger.databinding.ListableFragmentBinding
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.SnackbarState
import com.robifr.ledger.ui.product.recycler.ProductListHolder
import com.robifr.ledger.ui.searchproduct.SearchProductFragment
import com.robifr.ledger.ui.selectproduct.recycler.SelectProductAdapter
import com.robifr.ledger.ui.selectproduct.recycler.SelectProductHeaderHolder
import com.robifr.ledger.ui.selectproduct.recycler.SelectProductListHolder
import com.robifr.ledger.ui.selectproduct.viewmodel.SelectProductResultState
import com.robifr.ledger.ui.selectproduct.viewmodel.SelectProductState
import com.robifr.ledger.ui.selectproduct.viewmodel.SelectProductViewModel
import com.robifr.ledger.util.getColorAttr
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectProductFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val selectProductViewModel: SelectProductViewModel by viewModels()
  private lateinit var _adapter: SelectProductAdapter
  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _adapter = SelectProductAdapter(this)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    requireActivity().window.statusBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().window.navigationBarColor =
        requireContext().getColorAttr(android.R.attr.colorBackground)
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_select)
    fragmentBinding.toolbar.setTitle(R.string.selectProduct)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    fragmentBinding.horizontalToolbar.isGone = true
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    selectProductViewModel.snackbarState.observe(viewLifecycleOwner, ::_onSnackbarState)
    selectProductViewModel.resultState.observe(viewLifecycleOwner, ::_onResultState)
    selectProductViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
  }

  override fun onStart() {
    super.onStart()
    // Result should be called after all the view model state fully observed.
    parentFragmentManager.setFragmentResultListener(
        SearchProductFragment.Request.SELECT_PRODUCT.key,
        viewLifecycleOwner,
        ::_onSearchProductResult)
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.search -> {
          findNavController()
              .navigate(
                  R.id.searchProductFragment,
                  Bundle().apply {
                    putBoolean(
                        SearchProductFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key, true)
                    putLongArray(
                        SearchProductFragment.Arguments.INITIAL_SELECTED_PRODUCT_IDS_LONG_ARRAY.key,
                        listOfNotNull(
                                selectProductViewModel.uiState.safeValue.initialSelectedProduct?.id)
                            .toLongArray())
                  })
          true
        }
        else -> false
      }

  fun finish() {
    findNavController().popBackStack()
  }

  private fun _onSnackbarState(state: SnackbarState) {
    Snackbar.make(
            fragmentBinding.root as View,
            state.messageRes.toStringValue(requireContext()),
            Snackbar.LENGTH_LONG)
        .show()
  }

  private fun _onResultState(state: SelectProductResultState) {
    parentFragmentManager.setFragmentResult(
        Request.SELECT_PRODUCT.key,
        Bundle().apply {
          state.selectedProductId?.let { putLong(Result.SELECTED_PRODUCT_ID_LONG.key, it) }
        })
    finish()
  }

  private fun _onUiState(state: SelectProductState) {
    _adapter.notifyDataSetChanged()
    fragmentBinding.recyclerView.findViewHolderForLayoutPosition(0)?.let { viewHolder ->
      if (viewHolder is SelectProductHeaderHolder) {
        viewHolder.setCardExpanded(state.isSelectedProductPreviewExpanded)
        viewHolder.setSelectedItemDescription(
            state.selectedItemDescriptionStringRes?.let { requireContext().getString(it) },
            state.selectedItemDescriptionStringRes != null)
      }
    }

    for (view in fragmentBinding.recyclerView.children) {
      val viewHolder: RecyclerView.ViewHolder =
          fragmentBinding.recyclerView.getChildViewHolder(view)
      val isCardExpanded: Boolean =
          state.expandedProductIndex != -1 &&
              // +1 offset because header holder.
              fragmentBinding.recyclerView.getChildAdapterPosition(view) ==
                  state.expandedProductIndex + 1
      when (viewHolder) {
        is ProductListHolder -> viewHolder.setCardExpanded(isCardExpanded)
        is SelectProductListHolder -> viewHolder.setCardExpanded(isCardExpanded)
      }
    }
  }

  private fun _onSearchProductResult(requestKey: String, result: Bundle) {
    when (SearchProductFragment.Request.entries.find { it.key == requestKey }) {
      SearchProductFragment.Request.SELECT_PRODUCT -> {
        val productId: Long =
            result.getLong(SearchProductFragment.Result.SELECTED_PRODUCT_ID_LONG.key)
        if (productId != 0L) {
          selectProductViewModel.uiState.safeValue.products
              .find { it.id != null && it.id == productId }
              .let { selectProductViewModel.onProductSelected(it) }
        }
      }
      null -> Unit
    }
  }

  enum class Arguments : FragmentResultKey {
    INITIAL_SELECTED_PRODUCT_PARCELABLE
  }

  enum class Request : FragmentResultKey {
    SELECT_PRODUCT
  }

  enum class Result : FragmentResultKey {
    SELECTED_PRODUCT_ID_LONG
  }
}

private class OnBackPressedHandler(private val _fragment: SelectProductFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.selectProductViewModel.onProductSelected(null)
  }
}
