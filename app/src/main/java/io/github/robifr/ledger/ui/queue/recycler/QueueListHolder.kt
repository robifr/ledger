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

import io.github.robifr.ledger.components.QueueCardWideComponent
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.databinding.QueueCardWideBinding
import io.github.robifr.ledger.ui.common.RecyclerViewHolder

class QueueListHolder(
    private val _cardBinding: QueueCardWideBinding,
    private val _queues: () -> List<QueuePaginatedInfo>,
    private val _onExpandedQueueIndexChanged: (Int) -> Unit,
    private val _expandedQueue: () -> QueueModel?,
    private val _onQueueMenuDialogShown: (selectedQueue: QueuePaginatedInfo) -> Unit
) : RecyclerViewHolder(_cardBinding.root) {
  private var _queueIndex: Int = -1
  private val _card: QueueCardWideComponent = QueueCardWideComponent(itemView.context, _cardBinding)

  init {
    _cardBinding.cardView.setOnClickListener { _onExpandedQueueIndexChanged(_queueIndex) }
    _cardBinding.normalCard.menuButton.setOnClickListener {
      _onQueueMenuDialogShown(_queues()[_queueIndex])
    }
    _cardBinding.expandedCard.menuButton.setOnClickListener {
      _onQueueMenuDialogShown(_queues()[_queueIndex])
    }
  }

  override fun bind(itemIndex: Int) {
    _queueIndex = itemIndex
    _card.reset()
    _card.setNormalCardQueue(_queues()[_queueIndex])
    _setCardExpanded(_queues()[_queueIndex].id == _expandedQueue()?.id)
  }

  private fun _setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded) _queues()[_queueIndex].fullModel?.let { _card.setExpandedCardQueue(it) }
  }
}
