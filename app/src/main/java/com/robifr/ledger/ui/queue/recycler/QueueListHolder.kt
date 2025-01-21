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

package com.robifr.ledger.ui.queue.recycler

import com.robifr.ledger.components.QueueCardWideComponent
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.databinding.QueueCardWideBinding
import com.robifr.ledger.ui.RecyclerViewHolder

class QueueListHolder(
    private val _cardBinding: QueueCardWideBinding,
    private val _queues: () -> List<QueueModel>,
    private val _expandedQueueIndex: () -> Int,
    private val _onExpandedQueueIndexChanged: (Int) -> Unit,
    private val _onQueueMenuDialogShown: (selectedQueue: QueueModel) -> Unit
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
    _setCardExpanded(
        _expandedQueueIndex() != -1 && _queues()[_queueIndex] == _queues()[_expandedQueueIndex()])
  }

  private fun _setCardExpanded(isExpanded: Boolean) {
    _card.setCardExpanded(isExpanded)
    // Only fill the view when it's shown on screen.
    if (isExpanded) _card.setExpandedCardQueue(_queues()[_queueIndex])
  }
}
