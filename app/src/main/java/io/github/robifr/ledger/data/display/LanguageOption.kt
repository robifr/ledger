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

package io.github.robifr.ledger.data.display

import androidx.annotation.StringRes
import io.github.robifr.ledger.R

enum class LanguageOption(
    val languageTag: String,
    @StringRes val stringRes: Int,
    @StringRes val shortDateFormat: Int,
    @StringRes val fullDateFormat: Int,
    @StringRes val detailedDateFormat: Int
) {
  ENGLISH_US(
      "en-US",
      R.string.enum_languageOption_englishUs,
      R.string.enum_languageOption_englishUs_shortDateFormat,
      R.string.enum_languageOption_englishUs_fullDateFormat,
      R.string.enum_languageOption_englishUs_detailedDateFormat),
  INDONESIA(
      "id-ID",
      R.string.enum_languageOption_indonesia,
      R.string.enum_languageOption_indonesia_shortDateFormat,
      R.string.enum_languageOption_indonesia_fullDateFormat,
      R.string.enum_languageOption_indonesia_detailedDateFormat)
}
