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

package com.robifr.ledger.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.robifr.ledger.R
import com.robifr.ledger.databinding.AboutFragmentBinding
import com.robifr.ledger.ui.about.viewmodel.AboutState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : Fragment() {
  private var _fragmentBinding: AboutFragmentBinding? = null
  val fragmentBinding: AboutFragmentBinding
    get() = _fragmentBinding!!

  private lateinit var _onBackPressed: OnBackPressedHandler

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _fragmentBinding = AboutFragmentBinding.inflate(inflater, container, false)
    _onBackPressed = OnBackPressedHandler(this)
    return fragmentBinding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    ViewCompat.setOnApplyWindowInsetsListener(fragmentBinding.root) { view, insets ->
      val windowInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      view.updatePadding(top = windowInsets.top)
      WindowInsetsCompat.CONSUMED
    }
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, _onBackPressed)
    fragmentBinding.toolbar.setNavigationOnClickListener { _onBackPressed.handleOnBackPressed() }
    val state: AboutState = AboutState()
    fragmentBinding.info.text =
        HtmlCompat.fromHtml(
            getString(
                R.string.about_info_description,
                state.appVersion(),
                state.lastUpdatedDate(requireContext())),
            HtmlCompat.FROM_HTML_MODE_LEGACY)
    fragmentBinding.license.movementMethod = LinkMovementMethod.getInstance()
    fragmentBinding.thirdPartyLicenses.setOnClickListener {
      findNavController().navigate(R.id.thirdPartyLicensesFragment)
    }
    fragmentBinding.viewSourceCodeButton.setOnClickListener {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/robifr/ledger")))
    }
  }

  fun finish() {
    findNavController().popBackStack()
  }
}

private class OnBackPressedHandler(private val _fragment: AboutFragment) :
    OnBackPressedCallback(true) {
  override fun handleOnBackPressed() {
    _fragment.finish()
  }
}
