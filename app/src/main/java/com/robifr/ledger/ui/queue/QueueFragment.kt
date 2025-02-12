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

package com.robifr.ledger.ui.queue

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
import com.robifr.ledger.ui.common.pagination.PaginationScrollListener
import com.robifr.ledger.ui.common.state.RecyclerAdapterState
import com.robifr.ledger.ui.common.state.SnackbarState
import com.robifr.ledger.ui.filtercustomer.FilterCustomerFragment
import com.robifr.ledger.ui.queue.filter.QueueFilter
import com.robifr.ledger.ui.queue.recycler.QueueAdapter
import com.robifr.ledger.ui.queue.viewmodel.QueueEvent
import com.robifr.ledger.ui.queue.viewmodel.QueueFilterState
import com.robifr.ledger.ui.queue.viewmodel.QueueState
import com.robifr.ledger.ui.queue.viewmodel.QueueViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QueueFragment : Fragment(), Toolbar.OnMenuItemClickListener {
  private var _fragmentBinding: ListableFragmentBinding? = null
  val fragmentBinding: ListableFragmentBinding
    get() = _fragmentBinding!!

  val queueViewModel: QueueViewModel by activityViewModels()
  private lateinit var _sort: QueueSort
  private lateinit var _filter: QueueFilter
  private lateinit var _queueMenu: QueueMenu
  private lateinit var _adapter: QueueAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ListableFragmentBinding.inflate(inflater, container, false)
    _sort = QueueSort(this)
    _filter = QueueFilter(this)
    _queueMenu = QueueMenu(this, queueViewModel::onQueueMenuDialogClosed)
    _adapter = QueueAdapter(this)
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
    fragmentBinding.noDataCreated.title.setText(R.string.queue_noQueuesCreated)
    fragmentBinding.noDataCreated.description.setText(R.string.queue_noQueuesCreated_description)
    val layoutManager: LinearLayoutManager = LinearLayoutManager(requireContext())
    fragmentBinding.recyclerView.layoutManager = layoutManager
    fragmentBinding.recyclerView.adapter = _adapter
    fragmentBinding.recyclerView.addOnScrollListener(
        PaginationScrollListener(
            _layoutManager = layoutManager,
            _onLoadPreviousPage = { queueViewModel.onLoadPreviousPage() },
            _onLoadNextPage = { queueViewModel.onLoadNextPage() },
            _isLoading = { queueViewModel.uiState.safeValue.pagination.isLoading },
            _onStateIdle = queueViewModel::onRecyclerStateIdle,
            _maxItems = { queueViewModel.uiState.safeValue.pagination.paginatedItems.size }))
    fragmentBinding.recyclerView.setItemViewCacheSize(0)
    queueViewModel.uiEvent.observe(viewLifecycleOwner, ::_onUiEvent)
    queueViewModel.uiState.observe(viewLifecycleOwner, ::_onUiState)
    queueViewModel.filterView.uiState.observe(viewLifecycleOwner, ::_onFilterState)
  }

  override fun onStart() {
    super.onStart()
    // Result should be called after all the view model state fully observed.
    parentFragmentManager.setFragmentResultListener(
        FilterCustomerFragment.Request.FILTER_CUSTOMER.key(),
        viewLifecycleOwner,
        ::_onFilterCustomerResult)
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

  private fun _onUiEvent(event: QueueEvent) {
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

  private fun _onUiState(state: QueueState) {
    if (state.isSortMethodDialogShown) _sort.showDialog(state.sortMethod) else _sort.dismissDialog()
    if (state.isQueueMenuDialogShown) {
      state.selectedQueueMenu?.let { _queueMenu.showDialog(it, queueViewModel::onDeleteQueue) }
    } else {
      _queueMenu.dismissDialog()
    }
    fragmentBinding.noDataCreatedContainer.isVisible = state.isNoQueuesCreatedIllustrationVisible
    fragmentBinding.recyclerView.isVisible = !state.isNoQueuesCreatedIllustrationVisible
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

  private fun _onFilterState(state: QueueFilterState) {
    if (state.isDialogShown) _filter.showDialog() else _filter.dismissDialog()
    _filter.filterCustomer.setNullCustomerShown(state.isNullCustomerShown)
    _filter.filterStatus.setFilteredStatus(state.status)
    _filter.filterDate.setFilteredDate(state.date, state.dateFormat())
    _filter.filterTotalPrice.setFilteredMinTotalPriceText(state.formattedMinTotalPrice)
    _filter.filterTotalPrice.setFilteredMaxTotalPriceText(state.formattedMaxTotalPrice)
  }

  private fun _onFilterCustomerResult(requestKey: String, result: Bundle) {
    when (FilterCustomerFragment.Request.entries.find { it.key() == requestKey }) {
      FilterCustomerFragment.Request.FILTER_CUSTOMER -> {
        val filteredCustomerIds: LongArray =
            result.getLongArray(
                FilterCustomerFragment.Result.FILTERED_CUSTOMER_IDS_LONG_ARRAY.key())
                ?: longArrayOf()
        queueViewModel.filterView.onCustomerIdsChanged(filteredCustomerIds.toList())
        queueViewModel.filterView.onDialogShown()
      }
      null -> Unit
    }
  }
}
