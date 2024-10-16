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

package com.robifr.ledger.design

import androidx.compose.ui.graphics.Color

object Colors {
  val BlackLight: Color = Color(0xFF212121)
  val Black: Color = Color(0xFF000000)
  val Black10: Color = Color(0x1A000000)

  val White: Color = Color(0xFFFFFFFF)
  val White10: Color = Color(0x1AFFFFFF)
  val WhiteDark: Color = Color(0xFFF4F5F8)
  val WhiteDarker: Color = Color(0xFFE0E2E5)

  val RedLight: Color = Color(0xFFF58989)
  val RedLight15: Color = Color(0x26F58989)
  val Red: Color = Color(0xFFFF200C)
  val RedDark: Color = Color(0xFF4D0000)

  val YellowLight: Color = Color(0xFFFFE968)
  val YellowDark: Color = Color(0xFF986A00)

  val BlueLight: Color = Color(0xFF95CEFF)
  val BlueLight15: Color = Color(0x2695CEFF)
  val Blue: Color = Color(0xFF3696EE)
  val Blue20: Color = Color(0xFF333696EE)
  val BlueDark: Color = Color(0xFF2764BB)

  val GrayLight: Color = Color(0xFFB7BBBC)
  val Gray: Color = Color(0xFF969696)
  val GrayDark: Color = Color(0xFF4F4F50)
  val GrayDarker: Color = Color(0xFF2D2D2E)

  fun Primary(isDisabled: Boolean = false, isSelected: Boolean = false): Color =
      if (isDisabled || isSelected) Blue20 else Blue

  fun PrimaryText(isDisabled: Boolean = false): Color = if (isDisabled) GrayDarker else White

  fun PrimaryRipple(): Color = Black10

  fun Secondary(isDisabled: Boolean = false): Color = if (isDisabled) WhiteDarker else GrayDark

  fun SecondaryText(isDisabled: Boolean = false): Color = if (isDisabled) Gray else White

  fun Surface(): Color = WhiteDark

  fun SurfaceText(isDisabled: Boolean = false, isSelected: Boolean = false): Color =
      if (isDisabled) Gray else if (isSelected) Blue else Black

  fun SecondaryRipple(): Color = White10

  fun Image(): Color = WhiteDarker

  fun Stroke(): Color = Gray
}
