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

package com.robifr.ledger.network

import android.content.Context
import android.net.ConnectivityManager
import java.net.HttpURLConnection
import java.net.URL
import okio.IOException

object NetworkState {
  suspend fun isInternetAvailable(context: Context): Boolean {
    val connectivity: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    connectivity.activeNetwork?.let {
      val url: URL = URL("https://www.google.com/")
      try {
        val urlConnection: HttpURLConnection =
            (it.openConnection(url) as HttpURLConnection).apply {
              setRequestProperty("User-Agent", "test")
              setRequestProperty("Connection", "close")
              connectTimeout = 1000
            }
        urlConnection.connect()
        return urlConnection.responseCode == 200
      } catch (_: IOException) {
        return false
      }
    }
    return false
  }
}
