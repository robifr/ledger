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

import org.json.JSONArray

object ChartScaleBinding {
  /**
   * @param axisPosition Position for the axis.
   * @param domain List of numbers containing at least two numbers representing the minimum and
   *   maximum values.
   * @return A valid JavaScript code for this method.
   */
  fun createLinearScale(axisPosition: AxisPosition, domain: List<Double>): String =
      "chart.createLinearScale(${axisPosition.value},${JSONArray(domain)})"

  /**
   * Same as a [createLinearScale] but with support for larger numbers by using percentage. Each
   * domain (0-100) will be presented with the provided domain strings.
   *
   * @param axisPosition Position for the axis.
   * @param domain List of 101 strings representing the percentage values.
   * @return A valid JavaScript code for this method.
   */
  fun createPercentageLinearScale(axisPosition: AxisPosition, domain: List<String>): String =
      "chart.createPercentageLinearScale(${axisPosition.value},${JSONArray(domain)})"

  /**
   * @param axisPosition Position for the axis.
   * @param domain List of string for the label.
   * @param isAllLabelVisible Whether label should be visible, even when the domain is large.
   * @return A valid JavaScript code for this method.
   */
  fun createBandScale(
      axisPosition: AxisPosition,
      domain: List<String>,
      isAllLabelVisible: Boolean
  ): String =
      "chart.createBandScale(${axisPosition.value},${JSONArray(domain)},${isAllLabelVisible})"

  enum class AxisPosition(val value: Int) {
    BOTTOM(1),
    LEFT(2)
  }
}
