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

import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.databinding.QueueCardDialogMenuBinding
import com.robifr.ledger.ui.editqueue.EditQueueFragment

class QueueListMenu(private val _holder: QueueListHolder) {
  private val _dialogBinding: QueueCardDialogMenuBinding =
      QueueCardDialogMenuBinding.inflate(LayoutInflater.from(_holder.itemView.context)).apply {
        editButton.setOnClickListener {
          val queueId: Long = _holder._queues()[_holder._queueIndex].id ?: return@setOnClickListener
          _holder.itemView
              .findNavController()
              .navigate(
                  R.id.editQueueFragment,
                  Bundle().apply {
                    putLong(EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key, queueId)
                  })
          _dialog.dismiss()
        }
        deleteButton.setOnClickListener {
          _holder._onDeleteQueue(_holder._queues()[_holder._queueIndex])
          _dialog.dismiss()
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_holder.itemView.context, R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
      }

  fun openDialog() {
    _dialog.show()
  }
}
