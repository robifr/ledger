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

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationBarView
import com.robifr.ledger.R
import com.robifr.ledger.databinding.MainActivityBinding
import com.robifr.ledger.repository.SettingsRepository
import com.robifr.ledger.util.hideTooltipText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity :
    AppCompatActivity(),
    NavigationBarView.OnItemSelectedListener,
    NavController.OnDestinationChangedListener,
    ActivityResultCallback<ActivityResult> {
  private var _activityBinding: MainActivityBinding? = null
  val activityBinding: MainActivityBinding
    get() = _activityBinding!!

  @Inject lateinit var _settingsRepository: SettingsRepository
  private lateinit var _permissionLauncher: ActivityResultLauncher<Intent>
  private lateinit var _create: MainCreate

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    findNavController(R.id.fragmentContainer).apply {
      currentDestination?.id?.let {
        navigate(item.itemId, null, NavOptions.Builder().setPopUpTo(it, true).build())
      }
    }
    return true
  }

  override fun onDestinationChanged(
      controller: NavController,
      destination: NavDestination,
      arguments: Bundle?
  ) {
    // Match selected item of bottom navigation with the visible fragment.
    // It doesn't get matched on back pressed.
    activityBinding.bottomNavigation.menu.findItem(destination.id)?.isChecked = true
    // Hide views on main activity when user navigating to another fragment other than the one
    // defined as top of the stack — queue, customer, and product — inside bottom navigation.
    activityBinding.bottomNavigation.isVisible = destination.parent?.id == R.id.mainGraph
    activityBinding.createButton.isVisible =
        destination.parent?.id == R.id.mainGraph && destination.id != R.id.dashboardFragment
  }

  override fun onActivityResult(result: ActivityResult) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Permission granted.
      if (Environment.isExternalStorageManager()) {
        finish()
        startActivity(intent)
        // Denied. Retry to show dialog permission.
      } else {
        _requireStoragePermission()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _activityBinding = MainActivityBinding.inflate(layoutInflater)
    setContentView(activityBinding.root)

    _permissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
    if (!Environment.isExternalStorageManager()) _requireStoragePermission()

    _create = MainCreate(this)
    activityBinding.bottomNavigation.setOnItemSelectedListener(this)
    (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? NavHostFragment)
        ?.navController
        ?.addOnDestinationChangedListener(this)
    onBackPressedDispatcher.addCallback(this, OnBackPressedHandler(this))
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(_settingsRepository.languageUsed().languageTag))
    listOf(R.id.dashboardFragment, R.id.queueFragment, R.id.customerFragment, R.id.productFragment)
        .forEach { activityBinding.bottomNavigation.findViewById<View>(it)?.hideTooltipText() }
  }

  private fun _requireStoragePermission(): Intent {
    val intent: Intent =
        Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.fromParts("package", packageName, null))
    MaterialAlertDialogBuilder(this)
        .setTitle(
            HtmlCompat.fromHtml(
                getString(R.string.main_storageAccessPermission), HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setMessage(
            HtmlCompat.fromHtml(
                getString(R.string.main_storageAccessPermission_description),
                HtmlCompat.FROM_HTML_MODE_LEGACY))
        .setNegativeButton(R.string.action_denyAndQuit) { _, _ -> finish() }
        .setPositiveButton(R.string.action_grant) { _, _ -> _permissionLauncher.launch(intent) }
        .setCancelable(false)
        .show()
    return intent
  }
}

private class OnBackPressedHandler(private val _activity: MainActivity) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _activity.finish()
  }
}
