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
import android.content.Intent
import androidx.core.content.FileProvider
import com.robifr.ledger.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

class AppUpdater(private val _context: Context, private val _client: OkHttpClient) {
  fun obtainLatestRelease(): GithubReleaseModel? {
    val url: String = "https://api.github.com/repos/robifr/ledger/releases/latest"
    val response: Response = _client.newCall(Request.Builder().url(url).build()).execute()
    val body: String = response.body?.string().takeIf { response.isSuccessful } ?: return null

    val jsonObject: JSONObject = JSONObject(body)
    val assets: JSONArray = jsonObject.getJSONArray("assets")
    for (i in 0 until assets.length()) {
      val asset: JSONObject = assets.getJSONObject(i)
      val downloadUrl: String = asset.getString("browser_download_url")
      if (downloadUrl.endsWith(".apk")) {
        return GithubReleaseModel(
            tagName = jsonObject.getString("tag_name"),
            publishedAt = jsonObject.getString("published_at"),
            browserDownloadUrl = downloadUrl)
      }
    }
    return null
  }

  fun downloadAndInstallApp(appUrl: String) {
    val response: Response = _client.newCall(Request.Builder().url(appUrl).build()).execute()
    val body: ResponseBody = response.body?.takeIf { response.isSuccessful } ?: return
    val apkFile: File = _saveApp(body.byteStream())
    _installApp(apkFile)
  }

  private fun _saveApp(inputStream: InputStream): File {
    val apkFile: File = File(_context.externalCacheDir, "ledger.apk")
    FileOutputStream(apkFile).use { output -> inputStream.copyTo(output) }
    return apkFile
  }

  private fun _installApp(apkFile: File) {
    _context.startActivity(
        Intent(Intent.ACTION_VIEW).apply {
          flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
          setDataAndType(
              FileProvider.getUriForFile(
                  _context, "${BuildConfig.APPLICATION_ID}.fileprovider", apkFile),
              "application/vnd.android.package-archive")
        })
  }
}
