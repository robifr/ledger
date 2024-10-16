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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

private val LightTheme: ColorScheme =
    lightColorScheme(
        primary = Colors.Primary(),
        onPrimary = Colors.PrimaryText(),
        onPrimaryContainer = Colors.Primary(),
        secondary = Colors.Secondary(),
        onSecondary = Colors.SecondaryText(),
        surface = Colors.Surface(),
        onSurface = Colors.SurfaceText(),
        background = Colors.White,
        onBackground = Colors.Black)

val LocalSpaces: ProvidableCompositionLocal<Sizes> = compositionLocalOf { Sizes }
val LocalColors: ProvidableCompositionLocal<Colors> = compositionLocalOf { Colors }

@Composable
fun Themes(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = LightTheme) {
    CompositionLocalProvider(LocalSpaces provides Sizes) { content() }
    CompositionLocalProvider(LocalColors provides Colors) { content() }
  }
}
