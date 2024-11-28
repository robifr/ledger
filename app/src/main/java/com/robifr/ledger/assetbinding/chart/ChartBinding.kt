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
import org.json.JSONArray

object ChartBinding {
  /**
   * @param layoutBinding JavaScript code from [ChartLayoutBinding.init].
   * @param xScaleBinding JavaScript code from [ChartScaleBinding.createBandScale].
   * @param yScaleBinding JavaScript code from [ChartScaleBinding.createLinearScale] or
   *   [ChartScaleBinding.createPercentageLinearScale].
   * @param data List of data to be rendered with.
   * @param color Bar color.
   * @return A valid JavaScript code for this method.
   */
  fun <K, V> renderBarChart(
      layoutBinding: String,
      xScaleBinding: String,
      yScaleBinding: String,
      data: List<ChartData.Single<K, V>>,
      @ColorInt color: Int
  ): String =
      """
      chart.renderBarChart(
        ${layoutBinding},
        ${xScaleBinding},
        ${yScaleBinding},
        ${JSONArray(data.map { it.toJson() })},
        "${JsInterface.argbToRgbaHex(color)}"
      )
      """
          .replace("\n\\s*".toRegex(), "")

  /**
   * @param layoutBinding JavaScript code from [ChartLayoutBinding.init].
   * @param xScaleBinding JavaScript code from [ChartScaleBinding.createBandScale].
   * @param yScaleBinding JavaScript code from [ChartScaleBinding.createLinearScale] or
   *   [ChartScaleBinding.createPercentageLinearScale].
   * @param data List of data to be rendered with.
   * @param colors Ordered bar colors for the `groupInOrder`.
   * @param groupInOrder Ordered groups, indicating which one is drawn first.
   * @return A valid JavaScript code for this method.
   */
  fun <K, V, G> renderStackedBarChart(
      layoutBinding: String,
      xScaleBinding: String,
      yScaleBinding: String,
      data: List<ChartData.Multiple<K, V, G>>,
      @ColorInt colors: List<Int>,
      groupInOrder: Set<String>
  ): String =
      """
      chart.renderStackedBarChart(
        ${layoutBinding},
        ${xScaleBinding},
        ${yScaleBinding},
        ${JSONArray(data.map { it.toJson() })},
        ${JSONArray(colors.map { JsInterface.argbToRgbaHex(it) })},
        ${JSONArray(groupInOrder)}
      )
      """
          .replace("\n\\s*".toRegex(), "")

  /**
   * @param layoutBinding JavaScript code from [ChartLayoutBinding.init].
   * @param data List of ordered data to be rendered with.
   * @param colors Ordered donut slice colors for the data.
   * @param svgTextInCenter Text in SVG format (within `<text>` attribute) positioned at the center
   *   of the donut graph.
   * @return A valid JavaScript code for this method.
   */
  fun <K, V> renderDonutChart(
      layoutBinding: String,
      data: List<ChartData.Single<K, V>>,
      @ColorInt colors: List<Int>,
      svgTextInCenter: String?
  ): String =
      """
      chart.renderDonutChart(
        ${layoutBinding},
        ${JSONArray(data.map { it.toJson() })},
        ${JSONArray(colors.map { JsInterface.argbToRgbaHex(it) })},
        `${svgTextInCenter}`
      )
      """
          .replace("\n\\s*".toRegex(), "")
}
