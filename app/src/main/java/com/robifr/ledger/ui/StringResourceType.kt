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

package com.robifr.ledger.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

sealed interface StringResourceType {
  fun toStringValue(context: Context): String
}

data class StringResource(@StringRes val resId: Int, val args: List<Any>) : StringResourceType {
  constructor(@StringRes resId: Int, vararg args: Any) : this(resId, args.toList())

  override fun toStringValue(context: Context): String =
      context.getString(resId, *args.toTypedArray())
}

data class PluralResource(@PluralsRes val resId: Int, val quantity: Int, val args: List<Any>) :
    StringResourceType {
  constructor(
      @PluralsRes resId: Int,
      quantity: Int,
      vararg args: Any
  ) : this(resId, quantity, args.toList())

  override fun toStringValue(context: Context): String =
      context.resources.getQuantityString(resId, quantity, *args.toTypedArray())
}
