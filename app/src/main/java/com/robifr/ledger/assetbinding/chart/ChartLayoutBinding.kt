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

package com.robifr.ledger.assetbinding.chart

import androidx.annotation.ColorInt
import com.robifr.ledger.assetbinding.JsInterface

object ChartLayoutBinding {
  /** @return A valid JavaScript code for this method. */
  fun init(
      width: Int,
      height: Int,
      marginTop: Int = 0,
      marginBottom: Int = 0,
      marginLeft: Int = 0,
      marginRight: Int = 0,
      fontSize: Int,
      @ColorInt backgroundColor: Int
  ): String =
      """
      new chart.ChartLayout(
        ${width},
        ${height},
        ${marginTop},
        ${marginBottom},
        ${marginLeft},
        ${marginRight},
        ${fontSize},
        "${JsInterface.argbToRgbaHex(backgroundColor)}"
      )
      """
          .replace("\n\\s*".toRegex(), "")
}
