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

package io.github.robifr.ledger.ui.selectcustomer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.ListableFragmentBinding
import io.github.robifr.ledger.ui.common.navigation.FragmentResultKey
import io.github.robifr.ledger.ui.common.navigation.OnBackPressedHandler
import io.github.robifr.ledger.ui.common.pagination.PaginationScrollListener
import io.github.robifr.ledger.ui.common.state.RecyclerAdapterState
import io.github.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import io.github.robifr.ledger.ui.selectcustomer.recycler.SelectCustomerAdapter
import io.github.robifr.ledger.ui.selectcustomer.viewmodel.SelectCustomerEvent
import io.github.robifr.ledger.ui.selectcustomer.viewmodel.SelectCustomerResultState
import io.github.robifr.ledger.ui.selectcustomer.viewmodel.SelectCustomerViewModel

@AndroidEntryPoint
class SelectCustomerFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val selectCustomerViewModel: SelectCustomerViewModel by viewModels()
  private lateinit var _adapter: SelectCustomerAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _adapter = SelectCustomerAdapter(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(
          top = systemBarInsets.top, left = windowInsets.left, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(
            viewLifecycleOwner,
            OnBackPressedHandler { selectCustomerViewModel.onCustomerSelected(null) })
    fragmentBinding.toolbar.setNavigationOnClickListener {
      selectCustomerViewModel.onCustomerSelected(null)
    }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_select)
    fragmentBinding.toolbar.setTitle(R.string.selectCustomer)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.horizontalToolbar.isGone = true
    val layoutManager: LinearLayoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.layoutManager = layoutManager
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.addOnScrollListener(
        PaginationScrollListener(
            _layoutManager = layoutManager,
            _onLoadPreviousPage = { selectCustomerViewModel.onLoadPreviousPage() },
            _onLoadNextPage = { selectCustomerViewModel.onLoadNextPage() },
            _isLoading = { selectCustomerViewModel.uiState.safeValue.pagination.isLoading },
            _onStateIdle = selectCustomerViewModel::onRecyclerStateIdle,
            _maxItems = {
              selectCustomerViewModel.uiState.safeValue.pagination.paginatedItems.size
            }))
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    selectCustomerViewModel.uiEvent.observe(viewLifecycleOwner, ::_onUiEvent)
  }

  override fun onStart() {
    super.onStart()
    // Result should be called after all the view model state fully observed.
    parentFragmentManager.setFragmentResultListener(
        SearchCustomerFragment.Request.SELECT_CUSTOMER.key(),
        viewLifecycleOwner,
        ::_onSearchCustomerResult)
  }

  override fun onMenuItemClick(item: MenuItem?): Boolean =
      when (item?.itemId) {
        R.id.search -> {
          findNavController()
              .navigate(
                  R.id.searchCustomerFragment,
                  Bundle().apply {
                    putBoolean(
                        SearchCustomerFragment.Arguments.IS_SELECTION_ENABLED_BOOLEAN.key(), true)
                    putLongArray(
                        SearchCustomerFragment.Arguments.INITIAL_SELECTED_CUSTOMER_IDS_LONG_ARRAY
                            .key(),
                        listOfNotNull(
                                selectCustomerViewModel.uiState.safeValue.initialSelectedCustomer
                                    ?.id)
                            .toLongArray())
                  })
          true
        }
        else -> false
      }

  fun finish() {
    findNavController().popBackStack()
  }

  private fun _onUiEvent(event: SelectCustomerEvent) {
    event.recyclerAdapter?.let {
      _onRecyclerAdapterState(it.data)
      it.onConsumed()
    }
    event.selectResult?.let {
      _onResultState(it.data)
      it.onConsumed()
    }
  }

  private fun _onResultState(state: SelectCustomerResultState) {
    parentFragmentManager.setFragmentResult(
        Request.SELECT_CUSTOMER.key(),
        Bundle().apply {
          state.selectedCustomerId?.let { putLong(Result.SELECTED_CUSTOMER_ID_LONG.key(), it) }
        })
    finish()
  }

  private fun _onRecyclerAdapterState(state: RecyclerAdapterState) {
    when (state) {
      is RecyclerAdapterState.DataSetChanged -> _adapter.notifyDataSetChanged()
      is RecyclerAdapterState.ItemChanged ->
          state.indexes.forEach { _adapter.notifyItemChanged(it) }
      is RecyclerAdapterState.ItemRangeChanged ->
          _adapter.notifyItemRangeChanged(state.positionStart, state.itemCount, state.payload)
      is RecyclerAdapterState.ItemRangeInserted ->
          _adapter.notifyItemRangeInserted(state.positionStart, state.itemCount)
      is RecyclerAdapterState.ItemRangeRemoved ->
          _adapter.notifyItemRangeRemoved(state.positionStart, state.itemCount)
    }
  }

  private fun _onSearchCustomerResult(requestKey: String, result: Bundle) {
    when (SearchCustomerFragment.Request.entries.find { it.key() == requestKey }) {
      SearchCustomerFragment.Request.SELECT_CUSTOMER -> {
        val customerId: Long =
            result.getLong(SearchCustomerFragment.Result.SELECTED_CUSTOMER_ID_LONG.key())
        if (customerId != 0L) {
          selectCustomerViewModel.uiState.safeValue.pagination.paginatedItems
              .find { it.id != null && it.id == customerId }
              .let { selectCustomerViewModel.onCustomerSelected(it) }
        }
      }
      null -> Unit
    }
  }

  enum class Arguments : FragmentResultKey {
    INITIAL_SELECTED_CUSTOMER_PARCELABLE
  }

  enum class Request : FragmentResultKey {
    SELECT_CUSTOMER
  }

  enum class Result : FragmentResultKey {
    SELECTED_CUSTOMER_ID_LONG
  }
}
