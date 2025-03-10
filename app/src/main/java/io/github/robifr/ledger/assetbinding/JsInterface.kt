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

package io.github.robifr.ledger.assetbinding

import android.content.Context
import android.util.TypedValue
import android.webkit.JavascriptInterface
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import io.github.robifr.ledger.R
import io.github.robifr.ledger.util.CurrencyFormat

class JsInterface(private val _context: Context) {
  @JavascriptInterface
  fun colorHex(colorName: String): String {
    // Attempt to directly resolve the color.
    var colorRes: Int = _context.resources.getIdentifier(colorName, "color", _context.packageName)
    if (colorRes == 0) {
      // If it fails, resolve it as an attribute.
      val attrRes: Int = _context.resources.getIdentifier(colorName, "attr", _context.packageName)
      val typedValue: TypedValue = TypedValue()
      if (attrRes != 0 && _context.theme.resolveAttribute(attrRes, typedValue, true)) {
        colorRes = typedValue.resourceId
      }
    }
    return argbToRgbaHex(_context.getColor(if (colorRes != 0) colorRes else R.color.black))
  }

  @JavascriptInterface
  fun formatCurrencyWithUnit(amount: Double, languageTag: String, symbol: String): String =
      CurrencyFormat.formatWithUnit(_context, amount.toBigDecimal(), languageTag, symbol)

  @JavascriptInterface
  fun localeLanguageTag(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags()

  companion object {
    const val NAME: String = "Android"

    fun argbToRgbaHex(@ColorInt hexColor: Int): String {
      val a: Int = (hexColor shr 24) and 0xFF
      val r: Int = (hexColor shr 16) and 0xFF
      val g: Int = (hexColor shr 8) and 0xFF
      val b: Int = hexColor and 0xFF
      return "#%02X%02X%02X%02X".format(r, g, b, a)
    }

    fun dpToCssPx(context: Context, dp: Float): Int =
        (dp / context.resources.displayMetrics.density).toInt()
  }
}
