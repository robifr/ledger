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

import android.R
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children

object FindView {
  fun rootView(activity: AppCompatActivity): ViewGroup =
      activity.findViewById<ViewGroup>(R.id.content).getChildAt(0) as ViewGroup

  fun findParentById(view: View, targetId: Int): View? {
    if (view.id == targetId) return view
    return if (view.parent != null) findParentById(view.parent as View, targetId) else null
  }

  fun <T : View?> findParentByType(view: View, cls: Class<T>): T? {
    if (cls.isInstance(view)) return cls.cast(view)
    return if (view.parent != null) findParentByType(view.parent as View, cls) else null
  }

  fun <T : View?> findViewByType(viewGroup: ViewGroup, cls: Class<T>): T? {
    for (view in viewGroup.children) {
      if (cls.isInstance(view)) return cls.cast(view)

      var result: View?
      if (view is ViewGroup) {
        result = findViewByType(view, cls)
        if (result != null) return cls.cast(result)
      }
    }
    return null
  }

  fun findToolbarNavigationButton(toolbar: Toolbar): ImageButton? {
    for (view in toolbar.children) {
      if (view is ImageButton && view.drawable === toolbar.navigationIcon) return view
    }
    return null
  }
}
