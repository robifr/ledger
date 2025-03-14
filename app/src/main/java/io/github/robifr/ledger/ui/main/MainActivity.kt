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

package io.github.robifr.ledger.ui.main

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
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.MainActivityBinding
import io.github.robifr.ledger.local.LocalBackup
import io.github.robifr.ledger.ui.settings.AppUpdateAvailable
import io.github.robifr.ledger.ui.settings.viewmodel.SettingsDialogState
import io.github.robifr.ledger.ui.settings.viewmodel.SettingsViewModel
import io.github.robifr.ledger.util.hideTooltipText
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
      navigate(
          item.itemId,
          null,
          navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.findStartDestination().id) { saveState = true }
          })
    }
    return true
  }

  override fun onDestinationChanged(
      controller: NavController,
      destination: NavDestination,
      arguments: Bundle?
  ) {
    val navigation: View = activityBinding.navigation
    if (navigation !is BottomNavigationView && navigation !is NavigationRailView) return
    // Match selected item of bottom navigation with the visible fragment.
    // It doesn't get matched on back pressed.
    navigation.menu.findItem(destination.id)?.isChecked = true
    // Hide views on main activity when user navigating to another fragment other than the one
    // defined as top of the stack — queue, customer, and product — inside bottom navigation.
    navigation.isVisible = destination.parent?.id == R.id.mainGraph
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
    _permissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), this)
    _setupNavigation()
    _applyUserPreferences()
    if (!_permission.isStorageAccessGranted()) {
      _permission.showStorageAccessDialog(
          onDeny = { finish() },
          onGrant = { _permissionLauncher.launch(_permission.storageAccessIntent()) })
    } else {
      _checkForAppUpdate()
      _performBackupTasks()
    }
  }

  override fun onStart() {
    super.onStart()
    findNavController(R.id.fragmentContainer).addOnDestinationChangedListener(this)
  }

  private fun _setupNavigation() {
    val navigation: View = activityBinding.navigation
    if (navigation is BottomNavigationView) {
      navigation.setOnItemSelectedListener(this)
      ViewCompat.setOnApplyWindowInsetsListener(activityBinding.navigation) { view, insets ->
        val navigationBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
        view.updatePadding(bottom = navigationBarInsets.bottom)
        WindowInsetsCompat.CONSUMED
      }
    } else if (navigation is NavigationRailView) {
      navigation.setOnItemSelectedListener(this)
      ViewCompat.setOnApplyWindowInsetsListener(activityBinding.navigation) { view, insets ->
        val statusBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        val windowInsets: Insets =
            insets.getInsets(
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
        view.updatePadding(top = statusBarInsets.top, left = windowInsets.left)
        WindowInsetsCompat.CONSUMED
      }
    }
    listOf(R.id.dashboardFragment, R.id.queueFragment, R.id.customerFragment, R.id.productFragment)
        .forEach { navigation.findViewById<View>(it)?.hideTooltipText() }
  }

  private fun _applyUserPreferences() {
    AppCompatDelegate.setDefaultNightMode(
        _settingsViewModel.uiState.safeValue.appTheme.defaultNightMode)
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(
            _settingsViewModel.uiState.safeValue.languageUsed.languageTag))
  }

  private fun _checkForAppUpdate() {
    // Only automatically check for app update once a day.
    if (_settingsViewModel.uiState.safeValue.isLastCheckedTimeForAppUpdatePastMidNight()) {
      _settingsViewModel.uiEvent.observe(this) { event ->
        event.dialog?.let {
          _onSettingsDialogState(state = it.data, onDismiss = { it.onConsumed() })
        }
      }
      _settingsViewModel.onCheckForAppUpdate(this, false)
    }
  }

  private fun _onSettingsDialogState(state: SettingsDialogState, onDismiss: () -> Unit) {
    when (state) {
      is SettingsDialogState.UpdateAvailable -> {
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
                onUpdate = { _settingsViewModel.onUpdateApp() },
                onDismiss = onDismiss)
      }
      is SettingsDialogState.UnknownSourceInstallation ->
          _permission.showUnknownSourceInstallationDialog(
              onGrant = {
                _permissionLauncher.launch(_permission.unknownSourceInstallationIntent())
              },
              onDismiss = onDismiss)
    }
  }

  private fun _performBackupTasks() {
    lifecycle.coroutineScope.launch(Dispatchers.IO) {
      LocalBackup.backup(overwriteTodayBackup = false)
      LocalBackup.clearOldBackups()
    }
  }
}
