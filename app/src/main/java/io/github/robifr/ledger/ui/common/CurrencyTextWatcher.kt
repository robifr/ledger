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

package io.github.robifr.ledger.ui.common

import android.text.Editable
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import io.github.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import java.text.ParseException
import kotlin.math.max
import kotlin.math.min

open class CurrencyTextWatcher(
    view: EditText,
    protected val _maximumAmount: BigDecimal = Int.MAX_VALUE.toBigDecimal(),
    protected val _isSymbolHidden: Boolean = false,
    protected val _fractionDigits: Int? = null
) : EditTextWatcher(view) {
  override fun afterTextChanged(editable: Editable) {
    // WARNING! Never ever mess with this method unless you know what you do.
    //    This method is the center of all edge cases demon.
    super.afterTextChanged(editable)
    if (_isEditing) return // Prevent infinite callback.
    _isEditing = true

    val languageTag: String = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val decimalSeparator: String = CurrencyFormat.decimalSeparator(languageTag)
    val symbol: String = if (_isSymbolHidden) "" else CurrencyFormat.symbol(languageTag)
    val fractionDigits: Int = _fractionDigits ?: CurrencyFormat.decimalFractionDigits(languageTag)
    val parsedAmount: BigDecimal =
        try {
          CurrencyFormat.parse(newText(), languageTag, fractionDigits)
        } catch (_: ParseException) {
          0.toBigDecimal()
        }
    val formattedText: String =
        CurrencyFormat.format(parsedAmount, languageTag, symbol, fractionDigits)

    // Handle any invalid input.
    if (!CurrencyFormat.isValidToParseAndFormat(newText(), languageTag, fractionDigits) ||
        // Or when the parsed amount is more than maximum allowed.
        parsedAmount.compareTo(_maximumAmount) >= 0) {
      // The entire text should be cleared when deleting "1" in "$1|" (the bar is cursor).
      val text: String = if (newText().count { it.isDigit() } == 0) "" else oldText()
      _view.setText(text)
      _view.setSelection(max(0, min(_oldCursorPosition - 1, _view.text.length)))
      _isEditing = false
      return
    }

    val isInputtingTrailingDecimalSeparator: Boolean =
        !_isBackspaceClicked &&
            _changedTextAfter == decimalSeparator &&
            // The best way to check if the input is at the end of the digit is by ensuring there
            // are no digits in the right side (except zero). Digits only, because some languages
            // place their currency symbol as a suffix. Zero is also an exception to handle cases
            // like inputting a decimal separator while digits on the right side are filled with
            // zeroes. e.g. Inputting a separator in "$1|00" will result in "$1" instead of "$1.".
            // Notice that the decimal separator is missing due to formatting by `CurrencyFormat`.
            _unchangedTextRight.count { it.isDigit() && it != '0' } == 0
    if (isInputtingTrailingDecimalSeparator) {
      // Split the formatted text — some language place their currency symbol as a suffix — to
      // put the decimal separator at the end of the last digit
      val lastDigitIndex: Int = formattedText.indexOfLast { it.isDigit() } + 1
      val textWithDecimalSeparator: String =
          formattedText.substring(0, lastDigitIndex) +
              decimalSeparator +
              formattedText.substring(lastDigitIndex)
      _view.setText(textWithDecimalSeparator)
      _view.setSelection(max(0, min(_oldCursorPosition, _view.text.length)))
      _isEditing = false
      return
    }

    // Adding or removing zero in the decimal place is valid input, such as "$1.0|". However,
    // only zeros should be handled. So that something like adding "2" to "$1.0|00" (the bar is
    // cursor) will be properly formatted to "$1.02|". While deleting "0" in "$1.0|00" will
    // leave it unformatted as "$1.|00".
    val isEditingTrailingZeroOnDecimal: Boolean =
        _unchangedTextLeft.contains(decimalSeparator) &&
            (_changedTextBefore.any { it == '0' } || _changedTextAfter.any { it == '0' })
    if (isEditingTrailingZeroOnDecimal) {
      val oldCursorPosition: Int =
          if (_isBackspaceClicked) _oldCursorPosition - 1 else _oldCursorPosition + 1
      _view.setText(newText())
      _view.setSelection(max(0, min(oldCursorPosition, _view.text.length)))
      _isEditing = false
      return
    }

    val cursorPosition: Int = _calculateCursorPosition(formattedText, decimalSeparator, symbol)
    _view.setText(formattedText)
    _view.setSelection(max(0, min(cursorPosition, _view.text.length)))
    _isEditing = false
  }

  protected fun _calculateCursorPosition(
      newFormattedText: String,
      decimalSeparator: String,
      symbol: String
  ): Int {
    // The algorithm works by listing stable characters (such as digits, symbols, negative sign,
    // and decimal separator, which don't change position during formatting, unlike grouping
    // separators) on the left side of the cursor. Then, it iterates through the newly formatted
    // text, removing the first character if it matches and incrementing the cursor position until
    // the list is empty.
    val charsPattern: String = """[\d\-${Regex.escape(decimalSeparator)}${Regex.escape(symbol)}]"""
    val charsOnLeft: ArrayDeque<Char> =
        ArrayDeque(
            (_unchangedTextLeft + _changedTextAfter)
                .asSequence()
                .filter { it.toString().matches(charsPattern.toRegex()) }
                // Negative sign should be placed first in the list to handle cases like adding
                // a negative sign in "$|12" (the bar is cursor), which results in "-$12|".
                .sortedBy { if (it == '-') -1 else 1 }
                .toMutableList()
                .apply {
                  val digits: String = newText().filter { it.isDigit() }
                  val firstDigitIndex: Int = indexOfFirst { it.isDigit() }
                  // There's a unique case when deleting the decimal separator in "$0.|1" (the bar
                  // is cursor) that results in "$1|". Notice how "0" is removed after formatting,
                  // while it remains in the list, causing the loop to continue looking for "0"
                  // until the end. Therefore, it should be removed.
                  if (digits.getOrNull(0) == '0' &&
                      // However, we can't simply remove the first digit found, because it means
                      // when deleting the decimal separator in "$0.|" would result in "$|0", and
                      // when the text is empty, adding "0" would result in "|$0". Therefore, it
                      // should be ensured that no other digit follows the first one.
                      digits.getOrNull(1) != null &&
                      firstDigitIndex != -1) {
                    removeAt(firstDigitIndex)
                  }
                })
    var position: Int = 0
    for (char in newFormattedText) {
      if (charsOnLeft.isEmpty()) break
      if (char == charsOnLeft.first()) charsOnLeft.removeFirst()
      position++
    }
    return position
  }
}
