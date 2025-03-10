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

package io.github.robifr.ledger.ui.queue.recycler

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.robifr.ledger.databinding.ListableListTextBinding
import io.github.robifr.ledger.databinding.QueueCardWideBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder
import io.github.robifr.ledger.ui.queue.QueueFragment

class QueueAdapter(private val _fragment: QueueFragment) :
    RecyclerView.Adapter<RecyclerViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder =
      when (ViewType.entries.find { it.value == viewType }) {
        ViewType.HEADER ->
            QueueHeaderHolder(
                _textBinding =
                    ListableListTextBinding.inflate(_fragment.layoutInflater, parent, false),
                _totalQueues = { _fragment.queueViewModel.uiState.safeValue.pagination.totalItem })
        else ->
            QueueListHolder(
                _cardBinding =
                    QueueCardWideBinding.inflate(_fragment.layoutInflater, parent, false),
                _queues = { _fragment.queueViewModel.uiState.safeValue.pagination.paginatedItems },
                _onExpandedQueueIndexChanged =
                    _fragment.queueViewModel::onExpandedQueueIndexChanged,
                _expandedQueue = { _fragment.queueViewModel.uiState.safeValue.expandedQueue },
                _onQueueMenuDialogShown = _fragment.queueViewModel::onQueueMenuDialogShown)
      }

  override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
    when (holder) {
      is QueueHeaderHolder -> holder.bind()
      is QueueListHolder -> holder.bind(position - 1)
    }
  }

  override fun getItemCount(): Int =
      // +1 offset because header holder.
      _fragment.queueViewModel.uiState.safeValue.pagination.paginatedItems.size + 1

  override fun getItemViewType(position: Int): Int =
      when (position) {
        0 -> ViewType.HEADER.value
        else -> ViewType.LIST.value
      }

  private enum class ViewType(val value: Int) {
    HEADER(0),
    LIST(1)
  }
}
