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

package com.robifr.ledger.ui.filtercustomer

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
import com.robifr.ledger.R
import com.robifr.ledger.databinding.ListableFragmentBinding
import com.robifr.ledger.ui.FragmentResultKey
import com.robifr.ledger.ui.OnBackPressedHandler
import com.robifr.ledger.ui.RecyclerAdapterState
import com.robifr.ledger.ui.filtercustomer.recycler.FilterCustomerAdapter
import com.robifr.ledger.ui.filtercustomer.viewmodel.FilterCustomerEvent
import com.robifr.ledger.ui.filtercustomer.viewmodel.FilterCustomerResultState
import com.robifr.ledger.ui.filtercustomer.viewmodel.FilterCustomerViewModel
import com.robifr.ledger.ui.searchcustomer.SearchCustomerFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterCustomerFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val filterCustomerViewModel: FilterCustomerViewModel by viewModels()
  private lateinit var _adapter: FilterCustomerAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _adapter = FilterCustomerAdapter(this)
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
        .addCallback(viewLifecycleOwner, OnBackPressedHandler { filterCustomerViewModel.onSave() })
    fragmentBinding.toolbar.setNavigationOnClickListener { filterCustomerViewModel.onSave() }
    fragmentBinding.toolbar.menu.clear()
    fragmentBinding.toolbar.inflateMenu(R.menu.reusable_toolbar_select_multiple)
    fragmentBinding.toolbar.setTitle(R.string.filterCustomer)
    fragmentBinding.toolbar.setOnMenuItemClickListener(this)
    fragmentBinding.horizontalToolbar.isGone = true
    fragmentBinding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    filterCustomerViewModel.uiEvent.observe(viewLifecycleOwner, ::_onUiEvent)
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
                        filterCustomerViewModel.uiState.safeValue.filteredCustomers
                            .mapNotNull { it.id }
                            .toLongArray())
                  })
          true
        }
        R.id.save -> {
          filterCustomerViewModel.onSave()
          true
        }
        else -> false
      }

  fun finish() {
    findNavController().popBackStack()
  }

  private fun _onUiEvent(event: FilterCustomerEvent) {
    event.recyclerAdapter?.let {
      _onRecyclerAdapterState(it.data)
      it.onConsumed()
    }
    event.filterResult?.let {
      _onResultState(it.data)
      it.onConsumed()
    }
  }

  private fun _onResultState(state: FilterCustomerResultState) {
    parentFragmentManager.setFragmentResult(
        Request.FILTER_CUSTOMER.key(),
        Bundle().apply {
          putLongArray(
              Result.FILTERED_CUSTOMER_IDS_LONG_ARRAY.key(),
              state.filteredCustomerIds.toLongArray())
        })
    finish()
  }

  private fun _onRecyclerAdapterState(state: RecyclerAdapterState) {
    when (state) {
      is RecyclerAdapterState.DataSetChanged -> _adapter.notifyDataSetChanged()
      is RecyclerAdapterState.ItemChanged ->
          state.indexes.forEach { _adapter.notifyItemChanged(it) }
    }
  }

  private fun _onSearchCustomerResult(requestKey: String, result: Bundle) {
    when (SearchCustomerFragment.Request.entries.find { it.key() == requestKey }) {
      SearchCustomerFragment.Request.SELECT_CUSTOMER -> {
        val customerId: Long =
            result.getLong(SearchCustomerFragment.Result.SELECTED_CUSTOMER_ID_LONG.key())
        if (customerId != 0L) {
          filterCustomerViewModel.uiState.safeValue.customers
              .find { it.id != null && it.id == customerId }
              ?.let { filterCustomerViewModel.onCustomerCheckedChanged(it) }
        }
      }
      null -> Unit
    }
  }

  enum class Arguments : FragmentResultKey {
    INITIAL_FILTERED_CUSTOMER_IDS_LONG_ARRAY
  }

  enum class Request : FragmentResultKey {
    FILTER_CUSTOMER
  }

  enum class Result : FragmentResultKey {
    FILTERED_CUSTOMER_IDS_LONG_ARRAY
  }
}
