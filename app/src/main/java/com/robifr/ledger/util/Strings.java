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

import androidx.annotation.NonNull;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class Strings {
  private Strings() {}

  /**
   * @return JSON-like object just the same as when doing {@code console.log()} on NodeJS.
   */
  @NonNull
  public static String classToString(@NonNull Object object) {
    Objects.requireNonNull(object);

    final StringBuilder builder = new StringBuilder();
    builder.append(object.getClass().getSimpleName());
    builder.append(" {\n");

    final Field[] declaredFields = object.getClass().getDeclaredFields();

    // Filling string with class field data value.
    for (int i = 0; i < declaredFields.length; i++) {
      final Field field = declaredFields[i];
      field.setAccessible(true);

      try {
        final String fieldValue = field.get(object) != null ? field.get(object).toString() : "null";
        final String endLine = i < declaredFields.length - 1 ? ",\n" : "\n";

        builder.append(field.getName());
        builder.append(": ");
        builder.append(fieldValue);
        builder.append(endLine);

        // Ignore exception when trying to access private field.
      } catch (IllegalAccessException ignore) {
      }
    }

    builder.append("}");

    final String str = builder.toString();
    final StringBuilder result = new StringBuilder();

    // Indent formatting.
    for (int i = 0, indent = 0; i < str.length(); i++) {
      result.append(str.charAt(i));

      if (str.charAt(i) == '{') indent += 1;
      if (str.charAt(i) == '\n' && str.charAt(i + 1) == '}') indent -= 1;
      if (str.charAt(i) == '\n') result.append("\s\s\s".repeat(indent));
    }

    return result.toString();
  }

  /**
   * @return Number occurrence of string target.
   */
  public static int countOccurrence(@NonNull String source, @NonNull String target) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(target);

    int count = 0;

    for (int i = 0; (i = source.indexOf(target, i)) != -1; i += target.length()) {
      count++;
    }

    return count;
  }

  /**
   * @return Number matched occurrence of string pattern.
   */
  public static int countOccurrenceRegex(@NonNull String source, @NonNull String pattern) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(pattern);

    final Matcher match = Pattern.compile(pattern).matcher(source);
    int count = 0;

    while (match.find()) count++;
    return count;
  }

  /**
   * @return Index of nth matched occurrence string pattern.
   */
  public static int indexOfNthRegex(@NonNull String source, @NonNull String pattern, int n) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(pattern);

    final Matcher match = Pattern.compile(pattern).matcher(source);
    int index = -1;

    while (match.find() && n-- > 0) index = match.start();
    return index;
  }

  /**
   * @return Index of first matched string pattern.
   */
  public static int indexOfRegex(@NonNull String source, @NonNull String pattern) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(pattern);

    final Matcher match = Pattern.compile(pattern).matcher(source);
    return match.find() ? match.start() : -1;
  }

  /**
   * @return Last index of matched string pattern.
   */
  public static int lastIndexOfRegex(@NonNull String source, @NonNull String pattern) {
    Objects.requireNonNull(source);
    Objects.requireNonNull(pattern);

    final Matcher match = Pattern.compile(pattern).matcher(source);
    int index = -1;

    while (match.find()) index = match.start();
    return index;
  }
}
