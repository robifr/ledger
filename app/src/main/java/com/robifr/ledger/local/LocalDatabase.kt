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

package com.robifr.ledger.local

import android.content.Context
import android.os.Environment
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.robifr.ledger.data.model.CustomerFtsModel
import com.robifr.ledger.data.model.CustomerModel
import com.robifr.ledger.data.model.ProductFtsModel
import com.robifr.ledger.data.model.ProductModel
import com.robifr.ledger.data.model.ProductOrderModel
import com.robifr.ledger.data.model.QueueModel
import com.robifr.ledger.local.access.CustomerDao
import com.robifr.ledger.local.access.ProductDao
import com.robifr.ledger.local.access.ProductOrderDao
import com.robifr.ledger.local.access.QueueDao
import java.io.File

@Database(
    entities =
        [
            QueueModel::class,
            CustomerModel::class,
            CustomerFtsModel::class,
            ProductOrderModel::class,
            ProductModel::class,
            ProductFtsModel::class],
    version = 1)
abstract class LocalDatabase : RoomDatabase() {
  abstract fun queueDao(): QueueDao

  abstract fun customerDao(): CustomerDao

  abstract fun productOrderDao(): ProductOrderDao

  abstract fun productDao(): ProductDao

  companion object {
    @Volatile private var _instance: LocalDatabase? = null

    @Synchronized
    fun instance(context: Context): LocalDatabase =
        _instance
            ?: Room.databaseBuilder(
                    context.applicationContext, LocalDatabase::class.java, _fileName())
                .addCallback(Callback())
                .fallbackToDestructiveMigration()
                .build()
                .apply { _instance = this }

    private fun _fileName(): String = "${_fileDir()}/data.db"

    private fun _fileDir(): String? {
      val directory: String =
          "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .absolutePath}/.ledger"
      val dir: File = File(directory)
      if (!dir.exists() && !dir.mkdirs()) return null
      return directory
    }
  }

  class Callback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
      // Rebuild FTS index, because customer ID is referenced in queue table
      // (`QueueModel.customerId`).
      db.execSQL("INSERT INTO customer_fts(customer_fts) VALUES ('rebuild')")
      super.onCreate(db)
    }
  }
}
