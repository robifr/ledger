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

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.robifr.ledger.util.ClassPath

/**
 * This interface is useful when transferring data between fragments. By using enum package path,
 * which is unique, to generate either request (required by [FragmentManager]) or result key
 * (required by [Bundle]).
 *
 * ```kt
 * // DataReceiverFragment.kt
 * // Listening request from another fragment.
 * parentFragmentManager.setFragmentResultListener(
 *     DataSenderFragment.Request.REQUEST_ID.key, viewLifecycleOwner) { requestKey, result ->
 *       if (requestKey == DataSenderFragment.Request.REQUEST_ID.key) {
 *         // Data received: 100L
 *         result.getLong(DataSenderFragment.Result.RESULTED_ID_LONG.key)
 *       }
 *     }
 *
 * // DataSenderFragment.kt
 * class DataSenderFragment : Fragment() {
 *    ...
 *
 *    enum class Request : FragmentResultKey {
 *      REQUEST_ID
 *    }
 *
 *    enum class Result : FragmentResultKey {
 *      RESULTED_ID_LONG
 *    }
 * }
 *
 * // Somewhere in DataSenderFragment.kt
 * // Send result.
 * parentFragmentManager.setFragmentResult(
 *    Request.RESULTED_ID.key,
 *    Bundle().apply { putLong(Result.RESULTED_ID.key, 100L) })
 * ```
 */
interface FragmentResultKey {
  /** Generated key from [ClassPath.fullName] to transfer data between fragments. */
  val key: String
    get() =
        if (this is Enum<*>) ClassPath.fullName(this as Enum<*>) else ClassPath.fullName(javaClass)

  // TODO: Remove annotations after Kotlin migration.
  @Deprecated("Use its property access for Kotlin") fun key(): String = key
}
