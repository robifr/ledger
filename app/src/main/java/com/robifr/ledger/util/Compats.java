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

package com.robifr.ledger.util;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Deprecated
public class Compats {
  private Compats() {}

  /**
   * Hide annoying tooltip text. Pop-up when you do a long click on menu item (toolbar, bottom
   * navigation, etc). Simply set this method inside {@link
   * View#setOnLongClickListener(View.OnLongClickListener)}.
   */
  public static boolean hideTooltipText(@NonNull View view) {
    TooltipCompat.setTooltipText(view, null);
    return false;
  }

  public static void showKeyboard(@NonNull Context context, @NonNull View focusedView) {
    Objects.requireNonNull(context);
    Objects.requireNonNull(focusedView);

    final InputMethodManager inputManager =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

    focusedView.post(
        () ->
            inputManager.showSoftInput(focusedView.findFocus(), InputMethodManager.SHOW_IMPLICIT));
  }

  public static void hideKeyboard(@NonNull Context context, @Nullable View focusedView) {
    Objects.requireNonNull(context);

    final View view = Objects.requireNonNullElse(focusedView, new View(context));
    final InputMethodManager inputManager =
        (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

    view.post(() -> inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0));
  }

  @Nullable
  public static <T extends Parcelable> T parcelableOf(
      @NonNull Bundle bundle, @NonNull String key, @NonNull Class<T> cls) {
    Objects.requireNonNull(bundle);
    Objects.requireNonNull(key);
    Objects.requireNonNull(cls);

    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ? bundle.getParcelable(key, cls)
        : cls.cast(bundle.getParcelable(key));
  }

  @Nullable
  public static List<Long> longArrayListOf(@NonNull Bundle bundle, @NonNull String key) {
    Objects.requireNonNull(bundle);
    Objects.requireNonNull(key);

    final long[] value = bundle.getLongArray(key);
    return value != null ? Arrays.stream(value).boxed().collect(Collectors.toList()) : null;
  }
}
