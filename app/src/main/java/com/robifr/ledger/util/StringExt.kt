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

import java.util.regex.Matcher
import java.util.regex.Pattern

/** @return Number occurrence of string target. */
fun String.countOccurrence(target: String): Int {
  var count: Int = 0
  var i: Int = indexOf(target)
  while (i != -1) {
    count++
    i += target.length
    i = indexOf(target, i)
  }
  return count
}

/** @return Number matched occurrence of string pattern. */
fun String.countOccurrenceRegex(pattern: String): Int {
  val match: Matcher = Pattern.compile(pattern).matcher(this)
  var count: Int = 0
  while (match.find()) count++
  return count
}

/** @return Index of nth matched occurrence string pattern. */
fun String.indexOfNthRegex(pattern: String, n: Int): Int {
  val match: Matcher = Pattern.compile(pattern).matcher(this)
  var nth: Int = n
  var i: Int = -1
  while (match.find() && nth-- > 0) i = match.start()
  return i
}

/** @return Index of first matched string pattern. */
fun String.indexOfRegex(pattern: String): Int {
  val match: Matcher = Pattern.compile(pattern).matcher(this)
  return if (match.find()) match.start() else -1
}

/** @return Last index of matched string pattern. */
fun String.lastIndexOfRegex(pattern: String): Int {
  val match: Matcher = Pattern.compile(pattern).matcher(this)
  var i: Int = -1
  while (match.find()) i = match.start()
  return i
}
