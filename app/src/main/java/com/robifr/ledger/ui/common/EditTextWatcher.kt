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

package com.robifr.ledger.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Let's say the full string is `"$123"`. User deleted `"2"`.
 * - [_changedTextBefore] is `"2"`.
 * - [_changedTextAfter] is `""`.
 * - [_unchangedTextLeft] is `"$1"`.
 * - [_unchangedTextRight] is `"3"`.
 *
 * Now combine into a new text:
 * ```
 * _unchangedTextLeft + _changedTextAfter + _unchangedTextRight
 * ```
 *
 * Which becomes `"$13"`.
 */
open class EditTextWatcher(protected val _view: EditText) : TextWatcher {
  protected var _isEditing: Boolean = false
  protected var _isBackspaceClicked: Boolean = false
  protected var _oldCursorPosition: Int = 0
  /** Part of the text before changes applied. */
  protected var _changedTextBefore: String = ""
  /** Part of the text after changes applied. */
  protected var _changedTextAfter: String = ""
  /** Unchanged text which is placed before changed part. */
  protected var _unchangedTextLeft: String = ""
  /** Unchanged text which is placed after changed part. */
  protected var _unchangedTextRight: String = ""

  override fun beforeTextChanged(seq: CharSequence?, start: Int, count: Int, after: Int) {
    _unchangedTextLeft = seq?.subSequence(0, start).toString()
    _changedTextBefore = seq?.subSequence(start, start + count).toString()
    _unchangedTextRight = seq?.subSequence(start + count, seq.length).toString()
    _isBackspaceClicked = after < count
    _oldCursorPosition = _view.selectionStart
  }

  override fun onTextChanged(seq: CharSequence?, start: Int, before: Int, count: Int) {
    _changedTextAfter = seq?.subSequence(start, start + count).toString()
  }

  override fun afterTextChanged(editable: Editable) {}

  fun oldText(): String = _unchangedTextLeft + _changedTextBefore + _unchangedTextRight

  fun newText(): String = _unchangedTextLeft + _changedTextAfter + _unchangedTextRight
}
