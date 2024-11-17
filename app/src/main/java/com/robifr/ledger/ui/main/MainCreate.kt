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

package com.robifr.ledger.ui.main

import androidx.navigation.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.robifr.ledger.R
import com.robifr.ledger.databinding.MainDialogCreateBinding

class MainCreate(private val _activity: MainActivity) {
  private val _dialogBinding: MainDialogCreateBinding =
      MainDialogCreateBinding.inflate(_activity.layoutInflater).apply {
        createQueueButton.setOnClickListener {
          _activity.findNavController(R.id.fragmentContainer).navigate(R.id.createQueueFragment)
          _dialog.dismiss()
        }
        createCustomerButton.setOnClickListener {
          _activity.findNavController(R.id.fragmentContainer).navigate(R.id.createCustomerFragment)
          _dialog.dismiss()
        }
        createProductButton.setOnClickListener {
          _activity.findNavController(R.id.fragmentContainer).navigate(R.id.createProductFragment)
          _dialog.dismiss()
        }
      }
  private val _dialog: BottomSheetDialog =
      BottomSheetDialog(_activity, R.style.BottomSheetDialog).apply {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        setContentView(_dialogBinding.root)
      }

  init {
    _activity.activityBinding.createButton.setOnClickListener { _dialog.show() }
  }
}
