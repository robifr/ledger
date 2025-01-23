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
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robifr.ledger.R
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.databinding.QueueCardDialogMenuBinding
import com.robifr.ledger.ui.editqueue.EditQueueFragment

class QueueMenu(private val _fragment: Fragment, private val _onDialogClosed: () -> Unit) {
  private val _dialogBinding: QueueCardDialogMenuBinding =
      QueueCardDialogMenuBinding.inflate(LayoutInflater.from(_fragment.requireContext()))
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_fragment.requireContext(), R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
        setOnDismissListener { _onDialogClosed() }
      }

  fun showDialog(selectedQueue: QueueModel, onDeleteQueue: (QueueModel) -> Unit) {
    _dialogBinding.editButton.setOnClickListener {
      val queueId: Long = selectedQueue.id ?: return@setOnClickListener
      _fragment
          .findNavController()
          .navigate(
              R.id.editQueueFragment,
              Bundle().apply {
                putLong(EditQueueFragment.Arguments.INITIAL_QUEUE_ID_TO_EDIT_LONG.key(), queueId)
              })
      _onDialogClosed()
    }
    _dialogBinding.deleteButton.setOnClickListener {
      MaterialAlertDialogBuilder(_fragment.requireContext())
          .setTitle(
              HtmlCompat.fromHtml(
                  _fragment.requireContext().getString(R.string.queue_cardMenu_deleteWarning),
                  HtmlCompat.FROM_HTML_MODE_LEGACY))
          .setMessage(
              HtmlCompat.fromHtml(
                  _fragment
                      .requireContext()
                      .getString(
                          R.string.queue_cardMenu_deleteWarning_description,
                          selectedQueue.id.toString()),
                  HtmlCompat.FROM_HTML_MODE_LEGACY))
          .setNegativeButton(R.string.action_delete) { _, _ ->
            onDeleteQueue(selectedQueue)
            _onDialogClosed()
          }
          .setPositiveButton(R.string.action_cancel) { _, _ -> }
          .show()
    }
    _dialog.show()
  }

  fun dismissDialog() {
    _dialog.dismiss()
  }
}
