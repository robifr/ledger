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

package com.robifr.ledger.data.display;

import androidx.annotation.NonNull;
import java.util.Objects;

public record CustomerSortMethod(@NonNull SortBy sortBy, boolean isAscending) {
  public CustomerSortMethod {
    Objects.requireNonNull(sortBy);
  }

  public enum SortBy {
    NAME,
    BALANCE
  }
}
