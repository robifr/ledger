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

fun Any.toIndentedString(): String = buildString {
  val unformattedString: String = this@toIndentedString.toString()
  val space: String = "  "
  var indent: Int = 0
  var i: Int = 0
  while (i < unformattedString.length) {
    when (val char: Char = unformattedString[i]) {
      in "([{" -> {
        indent += 1
        appendLine(char).append(space.repeat(indent))
      }
      in ")]}" -> {
        indent -= 1
        appendLine().append(space.repeat(indent)).append(char)
      }
      ',' -> {
        appendLine(char).append(space.repeat(indent))
        i++ // Default `toString()` will always puts a blank space after comma.
      }
      else -> append(char)
    }
    i++
  }
}
