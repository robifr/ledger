/**
 * Copyright 2025 Robi
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

package com.robifr.ledger.ui.createqueue

import androidx.core.widget.doOnTextChanged

class CreateQueueNote(private val _fragment: CreateQueueFragment) {
  init {
    _fragment.fragmentBinding.note.doOnTextChanged { text: CharSequence?, _, _, _ ->
      _fragment.createQueueViewModel.onNoteTextChanged(text.toString())
    }
  }

  fun setInputtedNoteText(note: String) {
    if (_fragment.fragmentBinding.note.text.toString() != note) {
      _fragment.fragmentBinding.note.setText(note)
    }
  }
}
