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

package io.github.robifr.ledger.ui.editcustomer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.ui.common.navigation.FragmentResultKey
import io.github.robifr.ledger.ui.common.state.UiEvent
import io.github.robifr.ledger.ui.createcustomer.CreateCustomerFragment
import io.github.robifr.ledger.ui.editcustomer.viewmodel.EditCustomerResultState
import io.github.robifr.ledger.ui.editcustomer.viewmodel.EditCustomerViewModel

@AndroidEntryPoint
class EditCustomerFragment : CreateCustomerFragment() {
  override val createCustomerViewModel: EditCustomerViewModel by viewModels()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentBinding.toolbar.setTitle(R.string.editCustomer)
    createCustomerViewModel.editResultEvent.observe(viewLifecycleOwner) {
        event: UiEvent<EditCustomerResultState>? ->
      event?.let {
        _onResultState(it.data)
        it.onConsumed()
      }
    }
  }

  private fun _onResultState(state: EditCustomerResultState) {
    parentFragmentManager.setFragmentResult(
        Request.EDIT_CUSTOMER.key(),
        Bundle().apply {
          state.editedCustomerId?.let { putLong(Result.EDITED_CUSTOMER_ID_LONG.key(), it) }
        })
    finish()
  }

  enum class Arguments : FragmentResultKey {
    INITIAL_CUSTOMER_ID_TO_EDIT_LONG
  }

  enum class Request : FragmentResultKey {
    EDIT_CUSTOMER
  }

  enum class Result : FragmentResultKey {
    EDITED_CUSTOMER_ID_LONG
  }
}
