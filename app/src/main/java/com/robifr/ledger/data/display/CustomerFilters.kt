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

package com.robifr.ledger.data.display

import com.robifr.ledger.data.model.CustomerModel
import java.math.BigDecimal

/**
 * @property filteredBalance Filter customer if [CustomerModel.balance] is in-between min (first)
 *   and max (second). Set the pair value as null to represent unbounded number.
 * @property filteredDebt Filter customer if [CustomerModel.debt] is in-between min (first) and max
 *   (second). Set the pair value as null to represent unbounded number.
 */
data class CustomerFilters(
    val filteredBalance: Pair<Long?, Long?>,
    val filteredDebt: Pair<BigDecimal?, BigDecimal?>
)
