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

package com.robifr.ledger.local

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object LocalBackup {
  fun backup(overwriteTodayBackup: Boolean): Boolean {
    val dbFilePath: String = LocalDatabase.dbFilePath() ?: return false
    val dbFile: File = File(dbFilePath)
    val dbShmFile: File = File("${dbFile}-shm")
    val dbWalFile: File = File("${dbFile}-wal")

    val dateFormat: String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now(ZoneId.systemDefault()))
    val backupDir: File = File(_backupPath(), dateFormat)
    if (!backupDir.exists() && !backupDir.mkdirs()) return false

    return try {
      dbFile.copyTo(
          File(backupDir, "backup-${dateFormat}-${dbFile.name}"), overwrite = overwriteTodayBackup)
      dbShmFile.copyTo(
          File(backupDir, "backup-${dateFormat}-${dbShmFile.name}"),
          overwrite = overwriteTodayBackup)
      dbWalFile.copyTo(
          File(backupDir, "backup-${dateFormat}-${dbWalFile.name}"),
          overwrite = overwriteTodayBackup)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun clearOldBackups() {
    val backupDirPath: String = _backupPath() ?: return
    val backupDir: File = File(backupDirPath)
    val dirs: Array<File> = backupDir.listFiles { file -> file.isDirectory } ?: return
    for (dir in dirs) {
      val dirAge: Long =
          ChronoUnit.DAYS.between(
              LocalDate.parse(dir.name, DateTimeFormatter.ofPattern("yyyy-MM-dd")),
              LocalDate.now(ZoneId.systemDefault()))
      // Delete backup directories older than 30 days.
      if (dirAge > 30) dir.deleteRecursively()
    }
  }

  private fun _backupPath(): String? {
    val path: String = "${LocalDatabase.filePath()}/backup"
    val dir: File = File(path)
    if (!dir.exists() && !dir.mkdirs()) return null
    return path
  }
}
