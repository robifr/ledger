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

package com.robifr.ledger.components

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.GravityInt
import com.google.android.material.textview.MaterialTextView

class AutoScrollTextView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) :
    MaterialTextView(context, attrs, defStyleAttr) {
  @GravityInt private var _originalGravity: Int

  init {
    _originalGravity = gravity
    maxLines = 1
    isSingleLine = true
    ellipsize = TextUtils.TruncateAt.MARQUEE
    marqueeRepeatLimit = -1
    setHorizontallyScrolling(true)
  }

  override fun setGravity(@GravityInt gravity: Int) {
    // For unknown reason, using property access here will crash the app.
    // Same applies to any methods from Android when overridden.
    @Suppress("UsePropertyAccessSyntax") super.setGravity(gravity)
    _originalGravity = gravity
  }

  override fun onVisibilityChanged(changedView: View, visibility: Int) {
    super.onVisibilityChanged(changedView, visibility)
    if (visibility == VISIBLE) isSelected = true
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)
    // When the text is intended to have gravity set to end/right, the left side
    // of the text may get clipped when the text stops after scroll. This ensures that when
    // the text is long enough, it will use start/left gravity.
    if (width - paddingLeft - paddingRight < paint.measureText(text.toString())) {
      super.gravity = Gravity.START // Don't set `_originalGravity` to the start.
    } else {
      gravity = _originalGravity
    }
  }

  override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    // When an item in recycler view is deleted, the remaining items may inherit incorrect gravity
    // settings. For example, if the first item has short text (gravity set to end/right) and
    // the second item has long text (gravity set to start/left), deleting the second item can
    // cause the first item to incorrectly retain the start/left gravity due to reused item
    // from the deleted one. Setting it to their original gravity in this method ensure that such
    // thing won't happen.
    gravity = _originalGravity
  }
}
