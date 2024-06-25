/**
 * Copyright (c) 2024 Robi
 *
 * Ledger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package com.robifr.ledger.ui.dashboard.viewmodel;

import androidx.annotation.NonNull;
import com.robifr.ledger.data.model.Info;
import com.robifr.ledger.data.model.Model;
import java.util.List;
import java.util.function.Function;

interface InfoUpdaterFunction<M extends Model, I extends Info> {
  @NonNull
  public List<I> apply(
      @NonNull List<M> models,
      @NonNull List<I> oldInfo,
      @NonNull Function<M, I> modelToInfoConverter);
}
