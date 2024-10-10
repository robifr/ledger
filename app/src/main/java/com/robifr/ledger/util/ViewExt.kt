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

package com.robifr.ledger.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.TooltipCompat

/**
 * Hide annoying tooltip text. Pop-up when you do a long click on menu item (toolbar, bottom
 * navigation, etc). Simply set this method inside [ ][View.setOnLongClickListener].
 */
fun View.hideTooltipText() {
  setOnLongClickListener {
    TooltipCompat.setTooltipText(this, null)
    false
  }
}

fun View.showKeyboard() {
  val inputManager: InputMethodManager =
      context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  post { inputManager.showSoftInput(findFocus(), InputMethodManager.SHOW_IMPLICIT) }
}

fun View.hideKeyboard() {
  val inputManager: InputMethodManager =
      context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  val focusedView: View = rootView.findFocus() ?: this
  focusedView.post { inputManager.hideSoftInputFromWindow(focusedView.windowToken, 0) }
}
