/**
 * Copyright 2025 Robi
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

package com.robifr.ledger.local.migration

import android.content.Context
import androidx.room.migration.Migration
import com.robifr.ledger.preferences.SettingsPreferences

class MigrationProvider(context: Context) {
  private val _settingsPreferences: SettingsPreferences = SettingsPreferences(context)
  val migrationList: List<Migration> =
      listOf(V1To2Migration(_settingsPreferences), V2To3Migration())
}
