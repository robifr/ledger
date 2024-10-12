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

package com.robifr.ledger.data.model

sealed interface Model {
  /**
   * Model primary key.
   *
   * Note: The type is nullable instead of its primitive type even though both of them are valid.
   * It's due to when updating a model with foreign-key set to zero — indicating the referenced row
   * was deleted — instead of null, the query will silently fail.
   */
  val id: Long?

  @Deprecated("Replaced with id property") fun modelId(): Long? = id
}
