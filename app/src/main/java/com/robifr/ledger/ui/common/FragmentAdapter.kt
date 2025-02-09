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

package com.robifr.ledger.ui.common

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(
    fragment: Fragment,
    private val _fragmentTabs: MutableList<Fragment> = mutableListOf()
) : FragmentStateAdapter(fragment) {
  val fragmentTabs: List<Fragment>
    get() = _fragmentTabs

  override fun getItemCount(): Int = _fragmentTabs.size

  override fun createFragment(position: Int): Fragment = _fragmentTabs[position]

  override fun getItemId(position: Int): Long =
      // Override to ensure `notifyDataSetChanged()` works.
      _fragmentTabs[position].hashCode().toLong()

  override fun containsItem(itemId: Long): Boolean =
      // Override to ensure `notifyDataSetChanged()` works.
      _fragmentTabs.map { it.hashCode().toLong() }.contains(itemId)

  fun setFragmentTabs(fragments: List<Fragment>) {
    _fragmentTabs.clear()
    _fragmentTabs.addAll(fragments)
    notifyDataSetChanged()
  }
}
