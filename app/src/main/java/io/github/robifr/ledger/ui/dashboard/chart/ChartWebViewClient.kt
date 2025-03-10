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
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class ChartWebViewClient(context: Context, private val _onReload: () -> Unit) :
    WebViewClientCompat() {
  private val _assetLoader: WebViewAssetLoader =
      WebViewAssetLoader.Builder()
          .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
          .build()

  override fun shouldInterceptRequest(
      view: WebView?,
      request: WebResourceRequest?
  ): WebResourceResponse? = request?.url?.let { _assetLoader.shouldInterceptRequest(it) }

  override fun onPageFinished(view: WebView?, url: String?) {
    _onReload()
  }
}
