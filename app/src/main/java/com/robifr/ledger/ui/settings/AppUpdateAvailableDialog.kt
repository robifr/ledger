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

package com.robifr.ledger.ui.settings

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robifr.ledger.R

class AppUpdateAvailableDialog(private val _context: Context) {
  fun openDialog(updateVersion: String, updateDate: String, onUpdate: () -> Unit) {
    MaterialAlertDialogBuilder(_context)
        .setTitle(
            HtmlCompat.fromHtml(
                _context.getString(R.string.main_appUpdate_updateAvailable_x, updateVersion),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setMessage(
            HtmlCompat.fromHtml(
                _context.getString(R.string.main_appUpdate_updateAvailable_description, updateDate),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setNegativeButton(_context.getString(R.string.action_ignore)) { _, _ -> }
        .setPositiveButton(_context.getString(R.string.action_update)) { _, _ -> onUpdate() }
        .show()
        .apply {
          // Enable hyperlink.
          findViewById<TextView>(android.R.id.message)?.movementMethod =
              LinkMovementMethod.getInstance()
        }
  }
}
