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

package io.github.robifr.ledger.local

import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.math.BigDecimal
import java.time.Instant

object InstantConverter {
  @TypeConverter fun toInstant(date: String): Instant = Instant.parse(date)

  @TypeConverter fun fromInstant(date: Instant): String = date.toString()
}

object BigDecimalConverter {
  @TypeConverter fun toBigDecimal(number: String): BigDecimal = BigDecimal(number)

  @TypeConverter fun fromBigDecimal(number: BigDecimal): String = number.toString()
}

/**
 * This converter can't be used along with [TypeConverters] annotation. You have to manually convert
 * the string before inserting them as FTS row.
 */
object FtsStringConverter {
  /**
   * Add whitespace after character except when the character is whitespace itself. Because FTS will
   * only search by prefix, so that every single character could be a prefix. e.g. Assuming `_` is a
   * whitespace, `"abc_def"` becomes `"a_b_c__d_e_f_"`.
   */
  fun toFtsSpacedString(str: String): String = str.replace("(?<=.)(?<![$\\s])".toRegex(), " ")

  /**
   * Remove whitespace after character except when the character is whitespace itself. e.g. Assuming
   * `_` is a whitespace, `"a_b_c__d_e_f_"` becomes `"abc_def"`.
   */
  fun fromFtsSpacedString(str: String): String = str.replace("\\s(?=\\S|$)".toRegex(), "")
}
