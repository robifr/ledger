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

package io.github.robifr.ledger.ui.queue.viewmodel

import io.github.robifr.ledger.data.display.QueueSortMethod
import io.github.robifr.ledger.data.model.QueueModel
import io.github.robifr.ledger.data.model.QueuePaginatedInfo
import io.github.robifr.ledger.ui.common.pagination.PaginationState

/**
 * @property expandedQueueIndex Currently expanded queue index from
 *   [PaginationState.paginatedItems]. -1 to represent none being expanded.
 * @property expandedQueue Currently expanded queue full model which is used to fill the
 *   [QueuePaginatedInfo.fullModel] after being loaded.
 */
data class QueueState(
    val pagination: PaginationState<QueuePaginatedInfo>,
    val expandedQueueIndex: Int,
    val expandedQueue: QueueModel?,
    val isQueueMenuDialogShown: Boolean,
    val selectedQueueMenu: QueuePaginatedInfo?,
    val isNoQueuesCreatedIllustrationVisible: Boolean,
    val sortMethod: QueueSortMethod,
    val isSortMethodDialogShown: Boolean
)
