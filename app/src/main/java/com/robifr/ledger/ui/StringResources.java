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

package com.robifr.ledger.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import java.util.Objects;

@Deprecated
public sealed interface StringResources permits StringResources.Strings, StringResources.Plurals {
  @NonNull
  public static String stringOf(@NonNull Context context, @NonNull StringResources resources) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(resources);

    if (resources instanceof Plurals res) {
      return context.getResources().getQuantityString(res.resId, res.quantity, res.args);
    }

    return context.getString(((Strings) resources).resId, ((Strings) resources).args);
  }

  @Deprecated
  public static record Strings(@StringRes int resId, @NonNull Object... args)
      implements StringResources {}

  @Deprecated
  public static record Plurals(@PluralsRes int resId, int quantity, @NonNull Object... args)
      implements StringResources {}
}
