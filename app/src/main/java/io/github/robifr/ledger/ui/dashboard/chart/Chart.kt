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

package io.github.robifr.ledger.ui.dashboard.chart

import android.content.Context
import android.webkit.WebView
import androidx.annotation.ColorInt
import com.google.android.material.R as MaterialR
import io.github.robifr.ledger.R
import io.github.robifr.ledger.assetbinding.JsInterface
import io.github.robifr.ledger.assetbinding.chart.ChartBinding
import io.github.robifr.ledger.assetbinding.chart.ChartData
import io.github.robifr.ledger.assetbinding.chart.ChartLayoutBinding
import io.github.robifr.ledger.assetbinding.chart.ChartScaleBinding
import io.github.robifr.ledger.util.getColorAttr

class Chart(
    webViewClient: ChartWebViewClient,
    private val _context: Context,
    private val _webView: WebView
) {
  init {
    _webView.webViewClient = webViewClient
    _webView.settings.builtInZoomControls = false
    _webView.settings.allowFileAccess = false
    _webView.settings.javaScriptEnabled = true
    _webView.settings.setSupportZoom(false)
    _webView.addJavascriptInterface(JsInterface(_context), JsInterface.NAME)
    // Background color can't be set from xml.
    _webView.setBackgroundColor(_context.getColorAttr(MaterialR.attr.colorSurface))
  }

  fun load() {
    _webView.loadUrl("https://appassets.androidplatform.net/assets/chart/index.html")
  }

  /** @see ChartBinding.renderBarChart */
  fun <K, V> displayBarChart(
      xAxisDomain: List<String>,
      yAxisDomain: List<Double>,
      data: List<ChartData.Single<K, V>>,
      @ColorInt color: Int
  ) {
    val layoutBinding: String =
        ChartLayoutBinding.init(
            width = JsInterface.dpToCssPx(_context, _webView.width.toFloat()),
            height = JsInterface.dpToCssPx(_context, _webView.height.toFloat()),
            fontSize =
                JsInterface.dpToCssPx(
                    _context, _context.resources.getDimension(R.dimen.text_small)),
            backgroundColor = _context.getColorAttr(MaterialR.attr.colorSurface))
    val xScaleBinding: String =
        ChartScaleBinding.createBandScale(ChartScaleBinding.AxisPosition.BOTTOM, xAxisDomain, false)
    val yScaleBinding: String =
        ChartScaleBinding.createLinearScale(ChartScaleBinding.AxisPosition.LEFT, yAxisDomain)
    val chartRender: String =
        ChartBinding.renderBarChart("layoutBinding", "xScaleBinding", "yScaleBinding", data, color)
    _webView.evaluateJavascript(
        """
        (() => { // Wrap in a function to avoid variable redeclaration.
          const layoutBinding = ${layoutBinding};
          const xScaleBinding = ${xScaleBinding};
          const yScaleBinding = ${yScaleBinding};
          ${chartRender};
        })();
        """,
        null)
  }

  /** @see ChartBinding.renderStackedBarChart */
  fun <K, V, G> displayStackedBarChartWithLargeValue(
      xAxisDomain: List<String>,
      yAxisDomain: List<String>,
      data: List<ChartData.Multiple<K, V, G>>,
      @ColorInt colors: List<Int>,
      groupInOrder: Set<String>
  ) {
    val layoutBinding: String =
        ChartLayoutBinding.init(
            width = JsInterface.dpToCssPx(_context, _webView.width.toFloat()),
            height = JsInterface.dpToCssPx(_context, _webView.height.toFloat()),
            fontSize =
                JsInterface.dpToCssPx(
                    _context, _context.resources.getDimension(R.dimen.text_small)),
            backgroundColor = _context.getColorAttr(MaterialR.attr.colorSurface))
    val xScaleBinding: String =
        ChartScaleBinding.createBandScale(ChartScaleBinding.AxisPosition.BOTTOM, xAxisDomain, false)
    val yScaleBinding: String =
        ChartScaleBinding.createPercentageLinearScale(
            ChartScaleBinding.AxisPosition.LEFT, yAxisDomain)
    val chartRender: String =
        ChartBinding.renderStackedBarChart(
            "layoutBinding", "xScaleBinding", "yScaleBinding", data, colors, groupInOrder)
    _webView.evaluateJavascript(
        """
        (() => { // Wrap in a function to avoid variable redeclaration.
          const layoutBinding = ${layoutBinding};
          const xScaleBinding = ${xScaleBinding};
          const yScaleBinding = ${yScaleBinding};
          ${chartRender};
        })();
        """,
        null)
  }

  /** @see ChartBinding.renderDonutChart */
  fun <K, V> displayDonutChart(
      data: List<ChartData.Single<K, V>>,
      @ColorInt colors: List<Int>,
      svgTextInCenter: String?
  ) {
    val layoutBinding: String =
        ChartLayoutBinding.init(
            width = JsInterface.dpToCssPx(_context, _webView.width.toFloat()),
            height = JsInterface.dpToCssPx(_context, _webView.height.toFloat()),
            fontSize =
                JsInterface.dpToCssPx(
                    _context, _context.resources.getDimension(R.dimen.text_small)),
            backgroundColor = _context.getColorAttr(MaterialR.attr.colorSurface))
    val chartRender: String =
        ChartBinding.renderDonutChart("layoutBinding", data, colors, svgTextInCenter)
    _webView.evaluateJavascript(
        """
        (() => { // Wrap in a function to avoid variable redeclaration.
          const layoutBinding = ${layoutBinding};
          ${chartRender};
        })();
        """,
        null)
  }
}
