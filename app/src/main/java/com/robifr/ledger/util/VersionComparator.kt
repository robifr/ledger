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

object VersionComparator {
  fun isNewVersionNewer(oldVersion: String, newVersion: String): Boolean {
    val oldVersionParts: List<String> = oldVersion.split(".")
    val newVersionParts: List<String> = newVersion.split(".")
    for (i in 0 until maxOf(oldVersionParts.size, newVersionParts.size)) {
      val oldVersionPart = oldVersionParts.getOrElse(i) { "0" }.toInt()
      val newVersionPart = newVersionParts.getOrElse(i) { "0" }.toInt()
      if (oldVersionPart < newVersionPart) return true
      if (oldVersionPart > newVersionPart) return false
    }
    return false
  }
}
