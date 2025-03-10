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

package io.github.robifr.ledger.local

import android.content.Context
import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.robifr.ledger.data.display.LanguageOption
import io.github.robifr.ledger.data.model.ProductOrderModel
import io.github.robifr.ledger.preferences.SettingsPreferences
import io.github.robifr.ledger.util.CurrencyFormat
import java.math.BigDecimal
import kotlin.math.pow

class MigrationProvider(context: Context) {
  private val _settingsPreferences: SettingsPreferences = SettingsPreferences(context)

  /**
   * Add support for fraction digits like cents by multiplying the column value by 10^n, where n is
   * the number of fraction digits based on the currently used language.
   *
   * Updated columns:
   * - `customer` table: `balance`.
   * - `product` table: `price`.
   * - `product_order` table: `product_price`, `discount`, and `total_price`.
   */
  val MIGRATION_1_TO_2 =
      object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
          val languageTagUsed: String =
              _settingsPreferences.sharedPreferences.getString(
                  SettingsPreferences.KEY_LANGUAGE_USED, null)
                  ?: LanguageOption.ENGLISH_US.languageTag
          val fractionDigitsMultiplier: Int =
              10.0.pow(CurrencyFormat.decimalFractionDigits(languageTagUsed)).toInt()

          database.beginTransaction()
          try {
            database.execSQL(
                "UPDATE `customer` SET `balance` = `balance` * ?",
                arrayOf(fractionDigitsMultiplier))
            database.execSQL(
                "UPDATE `product` SET `price` = `price` * ?", arrayOf(fractionDigitsMultiplier))

            // `total_price` in the `product_order` table is stored as a string
            // representation of a big decimal. We have to convert those string firstly in code.
            val cursor: Cursor =
                database.query(
                    "SELECT `id`, `product_price`, `quantity`, `discount` FROM `product_order`")
            while (cursor.moveToNext()) {
              val id: Long = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
              val productPriceIndex: Int = cursor.getColumnIndex("product_price")
              val updatedProductPrice: Long? =
                  if (productPriceIndex >= 0 && !cursor.isNull(productPriceIndex)) {
                    cursor.getLong(productPriceIndex) * fractionDigitsMultiplier
                  } else {
                    null
                  }
              val updatedDiscount: Long =
                  cursor.getLong(cursor.getColumnIndexOrThrow("discount")) *
                      fractionDigitsMultiplier
              val updatedTotalPrice: BigDecimal =
                  ProductOrderModel.calculateTotalPrice(
                      updatedProductPrice,
                      cursor.getDouble(cursor.getColumnIndexOrThrow("quantity")),
                      updatedDiscount)
              database.execSQL(
                  """
                  UPDATE `product_order`
                  SET
                    `product_price` = COALESCE(?, `product_price`),
                    `discount` = ?,
                    `total_price` = ?
                  WHERE `id` = ?
                  """
                      .trimIndent(),
                  arrayOf(
                      updatedProductPrice,
                      updatedDiscount,
                      BigDecimalConverter.fromBigDecimal(updatedTotalPrice),
                      id))
            }
            cursor.close()
            database.setTransactionSuccessful()
          } finally {
            database.endTransaction()
          }
        }
      }

  /** Add `note` column in the `queue` table. */
  val MIGRATION_2_TO_3 =
      object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
          database.execSQL("ALTER TABLE `queue` ADD COLUMN `note` TEXT NOT NULL DEFAULT ''")
        }
      }

  val migrations: List<Migration> = listOf(MIGRATION_1_TO_2, MIGRATION_2_TO_3)

  companion object {
    @Volatile private var _instance: MigrationProvider? = null

    @Synchronized
    fun instance(context: Context): MigrationProvider =
        _instance ?: MigrationProvider(context).apply { _instance = this }
  }
}
