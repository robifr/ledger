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

package com.robifr.ledger.ui.queue.recycler;

import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import com.robifr.ledger.R;
import com.robifr.ledger.databinding.ListableListTextBinding;
import com.robifr.ledger.ui.RecyclerViewHolder;
import com.robifr.ledger.ui.queue.QueueFragment;
import java.util.Objects;
import java.util.Optional;

public class QueueHeaderHolder extends RecyclerViewHolder<Optional> {
  @NonNull private final QueueFragment _fragment;
  @NonNull private final ListableListTextBinding _textBinding;

  public QueueHeaderHolder(
      @NonNull QueueFragment fragment, @NonNull ListableListTextBinding binding) {
    super(binding.getRoot());
    this._fragment = Objects.requireNonNull(fragment);
    this._textBinding = Objects.requireNonNull(binding);

    this._textBinding.text.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        this._fragment.requireContext().getResources().getDimension(R.dimen.text_small));
  }

  @Override
  public void bind(@NonNull Optional ignore) {
    final int totalQueues = this._fragment.queueViewModel().queues().getValue().size();
    final String text =
        this._fragment
            .getResources()
            .getQuantityString(R.plurals.args_displaying_x_queue, totalQueues, totalQueues);

    this._textBinding.text.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
  }
}
