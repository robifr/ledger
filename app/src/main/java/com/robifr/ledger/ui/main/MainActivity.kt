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
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationBarView
import com.robifr.ledger.R
import com.robifr.ledger.databinding.MainActivityBinding
import com.robifr.ledger.ui.settings.AppUpdateAvailable
import com.robifr.ledger.ui.settings.viewmodel.SettingsDialogState
import com.robifr.ledger.ui.settings.viewmodel.SettingsViewModel
import com.robifr.ledger.ui.settings.viewmodel.UnknownSourceInstallationDialogState
import com.robifr.ledger.ui.settings.viewmodel.UpdateAvailableDialogState
import com.robifr.ledger.util.hideTooltipText
import dagger.hilt.android.AndroidEntryPoint
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
open class MainActivity :
    AppCompatActivity(),
    NavigationBarView.OnItemSelectedListener,
    NavController.OnDestinationChangedListener,
    ActivityResultCallback<ActivityResult> {
  private var _activityBinding: MainActivityBinding? = null
  val activityBinding: MainActivityBinding
    get() = _activityBinding!!

  private val _settingsViewModel: SettingsViewModel by viewModels()
  protected lateinit var _permission: RequiredPermission
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
    finish()
    startActivity(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    _activityBinding = MainActivityBinding.inflate(layoutInflater)
    setContentView(activityBinding.root)

    _permission = RequiredPermission(this)
    _create = MainCreate(this)
    activityBinding.bottomNavigation.setOnItemSelectedListener(this)
    ViewCompat.setOnApplyWindowInsetsListener(activityBinding.bottomNavigation) { view, insets ->
      val windowInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(bottom = windowInsets.bottom)
      WindowInsetsCompat.CONSUMED
    }
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(
            _settingsViewModel.uiState.safeValue.languageUsed.languageTag))
    listOf(R.id.dashboardFragment, R.id.queueFragment, R.id.customerFragment, R.id.productFragment)
        .forEach { activityBinding.bottomNavigation.findViewById<View>(it)?.hideTooltipText() }

    _permissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
    if (!_permission.isStorageAccessGranted()) {
      _permission.showStorageAccessDialog(
          onDeny = { finish() },
          onGrant = { _permissionLauncher.launch(_permission.storageAccessIntent()) })
    } else {
      // Only automatically check for app update once a day.
      if (_settingsViewModel.uiState.safeValue.isLastCheckedTimeForAppUpdatePastMidNight()) {
        _settingsViewModel.dialogState.observe(this, ::_onSettingsDialogState)
        _settingsViewModel.onCheckForAppUpdate(this, false)
      }
    }
  }

  override fun onStart() {
    super.onStart()
    findNavController(R.id.fragmentContainer).addOnDestinationChangedListener(this)
  }

  private fun _onSettingsDialogState(state: SettingsDialogState) {
    when (state) {
      is UpdateAvailableDialogState -> {
        val dateFormat: DateTimeFormatter =
            DateTimeFormatter.ofPattern(
                getString(_settingsViewModel.uiState.safeValue.languageUsed.fullDateFormat))
        AppUpdateAvailable(this)
            .showDialog(
                updateVersion = state.githubRelease.tagName,
                updateDate =
                    ZonedDateTime.parse(
                            state.githubRelease.publishedAt, DateTimeFormatter.ISO_DATE_TIME)
                        .format(dateFormat),
                updateSize = state.githubRelease.sizeInMb(),
                onUpdate = { _settingsViewModel.onUpdateApp() })
      }
      is UnknownSourceInstallationDialogState -> {
        _permission.showUnknownSourceInstallationDialog {
          _permissionLauncher.launch(_permission.unknownSourceInstallationIntent())
        }
      }
    }
  }
}
