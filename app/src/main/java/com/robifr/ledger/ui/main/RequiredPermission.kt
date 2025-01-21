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

package com.robifr.ledger.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.robifr.ledger.R

class RequiredPermission(private val _context: Context) {
  fun isStorageAccessGranted(): Boolean = Environment.isExternalStorageManager()

  fun storageAccessIntent(): Intent =
      Intent(
          Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
          Uri.fromParts("package", _context.packageName, null))

  fun showStorageAccessDialog(onDeny: () -> Unit, onGrant: () -> Unit) {
    MaterialAlertDialogBuilder(_context)
        .setTitle(
            HtmlCompat.fromHtml(
                _context.getString(R.string.main_storageAccessPermission),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setMessage(
            HtmlCompat.fromHtml(
                _context.getString(R.string.main_storageAccessPermission_description),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setNegativeButton(R.string.action_denyAndQuit) { _, _ -> onDeny() }
        .setPositiveButton(R.string.action_grant) { _, _ -> onGrant() }
        .setCancelable(false)
        .show()
  }

  fun isUnknownSourceInstallationGranted(): Boolean =
      _context.packageManager.canRequestPackageInstalls()

  fun unknownSourceInstallationIntent(): Intent =
      Intent(
          Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
          Uri.fromParts("package", _context.packageName, null))

  fun showUnknownSourceInstallationDialog(onGrant: () -> Unit) {
    MaterialAlertDialogBuilder(_context)
        .setTitle(
            HtmlCompat.fromHtml(
                _context.getString(R.string.main_unknownSourceInstallationPermission),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setMessage(
            _context.getString(R.string.main_unknownSourceInstallationPermission_description))
        .setNegativeButton(R.string.action_deny) { _, _ -> }
        .setPositiveButton(R.string.action_grant) { _, _ -> onGrant() }
        .show()
  }
}
