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

package com.robifr.ledger.ui.createcustomer

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.robifr.ledger.R
import com.robifr.ledger.design.LocalColors
import com.robifr.ledger.design.LocalSpaces
import com.robifr.ledger.ui.StringResource
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerState
import com.robifr.ledger.ui.createcustomer.viewmodel.CreateCustomerViewModel
import com.robifr.ledger.util.CurrencyFormat

@Composable
fun CreateCustomerScreen(navigation: NavController, viewModel: CreateCustomerViewModel) {
  CreateCustomerScreen(
      uiState = viewModel.uiState.safeValue,
      onBackPressed = navigation::popBackStack,
      onSave = viewModel::onSave,
      onNameTextChanged = viewModel::onNameTextChanged)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomerScreen(
    uiState: CreateCustomerState =
        CreateCustomerState(
            name = "", nameErrorMessageRes = null, balance = 0L, debt = 0.toBigDecimal()),
    onBackPressed: () -> Unit,
    onSave: () -> Unit,
    onNameTextChanged: (text: String) -> Unit
) {
  var isBackPressed: Boolean by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
        title = { Text(stringResource(R.string.createCustomer)) },
        navigationIcon = {
          IconButton(onClick = { isBackPressed = true }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
          }
        },
        actions = {
          IconButton(onClick = onSave) { Icon(Icons.Filled.Check, contentDescription = null) }
        })

    Column(modifier = Modifier.fillMaxSize().padding(LocalSpaces.current.Medium)) {
      TextInputField(
          modifier = Modifier.padding(bottom = LocalSpaces.current.Medium),
          label = stringResource(R.string.createCustomer_name),
          value = uiState.name,
          isError = true,
          supportingText = uiState.nameErrorMessageRes?.toStringValue(LocalContext.current),
          onValueChange = onNameTextChanged)
      TextInputField(
          modifier = Modifier.padding(bottom = LocalSpaces.current.Medium),
          label = stringResource(R.string.createCustomer_balance),
          value =
              CurrencyFormat.format(
                  uiState.balance.toBigDecimal(),
                  AppCompatDelegate.getApplicationLocales().toLanguageTags()),
          enabled = false)
      TextInputField(
          modifier = Modifier.padding(bottom = LocalSpaces.current.MediumLarge),
          label = stringResource(R.string.createCustomer_debt),
          value =
              CurrencyFormat.format(
                  uiState.debt, AppCompatDelegate.getApplicationLocales().toLanguageTags()),
          enabled = false,
          textStyle = TextStyle.Default.copy(color = uiState.debtColor))

      Button(
          modifier = Modifier.fillMaxWidth(),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = LocalColors.current.Primary(),
                  disabledContainerColor = LocalColors.current.Primary(true),
                  contentColor = LocalColors.current.PrimaryText(),
                  disabledContentColor = LocalColors.current.PrimaryText(true)),
          onClick = {}) {
            Text(stringResource(R.string.createCustomer_balance_add))
          }
      Button(
          modifier = Modifier.fillMaxWidth(),
          colors =
              ButtonDefaults.buttonColors(
                  containerColor = LocalColors.current.Secondary(),
                  disabledContainerColor = LocalColors.current.Secondary(true),
                  contentColor = LocalColors.current.SecondaryText(),
                  disabledContentColor = LocalColors.current.SecondaryText(true)),
          onClick = {}) {
            Text(stringResource(R.string.createCustomer_balance_withdraw))
          }
    }
  }

  BackHandler(true) { isBackPressed = true }

  if (isBackPressed) {
    AlertDialog(
        onDismissRequest = { isBackPressed = false },
        text = { Text(stringResource(R.string.createCustomer_unsavedChangesWarning)) },
        confirmButton = {
          TextButton(
              onClick = {
                isBackPressed = false
                onBackPressed()
              }) {
                Text(stringResource(R.string.action_discardAndLeave))
              }
        },
        dismissButton = {
          TextButton(onClick = { isBackPressed = false }) {
            Text(stringResource(R.string.action_cancel))
          }
        })
  }
}

@Preview(showBackground = true)
@Composable
private fun CreateCustomerScreenPreview() {
  CreateCustomerScreen(
      uiState =
          CreateCustomerState(
              name = "",
              nameErrorMessageRes = StringResource(R.string.createCustomer_name_emptyError),
              balance = 0L,
              debt = 0.toBigDecimal()),
      onBackPressed = {},
      onSave = {},
      onNameTextChanged = {})
}

@Composable
fun TextInputField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    isError: Boolean = false,
    supportingText: String? = null,
    onValueChange: (String) -> Unit = {}
) {
  Box(modifier = modifier) {
    TextField(
        modifier = Modifier.fillMaxWidth().padding(0.dp),
        label = { Text(label) },
        value = value,
        enabled = enabled,
        textStyle = textStyle,
        isError = isError,
        supportingText = {
          if (isError) {
            supportingText?.let {
              Text(text = it, style = LocalTextStyle.current.copy(background = Color.Transparent))
            }
          }
        },
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent),
        onValueChange = onValueChange)
    // Custom indicator.
    Box(
        modifier =
            Modifier.fillMaxWidth(0.92f)
                .height(0.8.dp)
                .offset(y = (-20).dp)
                .background(
                    if (isError) LocalColors.current.Red
                    else if (enabled) LocalColors.current.Black
                    else LocalColors.current.Gray)
                .align(Alignment.BottomCenter))
  }
}
