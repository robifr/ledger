/**
 * Copyright 2025 Robi
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

package io.github.robifr.ledger.ui.thirdpartylicenses

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.robifr.ledger.R
import io.github.robifr.ledger.databinding.ThirdPartyLicensesFragmentBinding
import io.github.robifr.ledger.ui.common.navigation.OnBackPressedHandler

@AndroidEntryPoint
class ThirdPartyLicensesFragment : Fragment() {
  private var _fragmentBinding: ThirdPartyLicensesFragmentBinding? = null
  val fragmentBinding: ThirdPartyLicensesFragmentBinding
    get() = _fragmentBinding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = ThirdPartyLicensesFragmentBinding.inflate(inflater, container, false)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val systemBarInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      val windowInsets: Insets =
          insets.getInsets(
              WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(
          top = systemBarInsets.top, left = windowInsets.left, right = windowInsets.right)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity()
        .onBackPressedDispatcher
        .addCallback(viewLifecycleOwner, OnBackPressedHandler { finish() })
    fragmentBinding.toolbar.setNavigationOnClickListener { finish() }
    fragmentBinding.license.text =
        requireContext()
            .resources
            .openRawResource(R.raw.third_party_licenses)
            .bufferedReader()
            .use { it.readText() }
    fragmentBinding.license.movementMethod = LinkMovementMethod.getInstance()
  }

  fun finish() {
    findNavController().popBackStack()
  }
}
