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

package com.robifr.ledger.assetbinding.chart;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public sealed interface ChartData {
  @NonNull
  default JSONObject toJson() {
    final JSONObject json = new JSONObject();
    for (Field field : getClass().getDeclaredFields()) {
      // Although fields in record class are public,
      // it's still required to provide private field to be accessible.
      field.setAccessible(true);
      try {
        final Object fieldValue = field.get(this);
        if (fieldValue == null) {
          json.put(field.getName(), JSONObject.NULL);
        } else if (fieldValue.getClass().isArray()) {
          json.put(field.getName(), new JSONArray(fieldValue));
        } else if (fieldValue instanceof Collection<?> collection) {
          json.put(field.getName(), new JSONArray(collection));
        } else {
          json.put(field.getName(), fieldValue);
        }
      } catch (IllegalAccessException | JSONException e) {
        throw new RuntimeException(e);
      }
    }
    return json;
  }

  record Single<K, V>(@NonNull @Keep K key, @NonNull @Keep V value) implements ChartData {
    public Single {
      Objects.requireNonNull(key);
      Objects.requireNonNull(value);
    }
  }

  record Multiple<K, V, G>(@NonNull @Keep K key, @NonNull @Keep V value, @NonNull @Keep G group)
      implements ChartData {
    public Multiple {
      Objects.requireNonNull(key);
      Objects.requireNonNull(value);
      Objects.requireNonNull(group);
    }
  }
}
