/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// NonStreamScreen - DAT Device Selection and Setup
//
// This screen demonstrates DAT device management and pre-streaming setup. It handles device
// registration status, camera permissions, and stream readiness.

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@Composable
fun NonStreamScreen(
    viewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var dropdownExpanded by remember { mutableStateOf(false) }
  val isDisconnectEnabled = uiState.registrationState is RegistrationState.Registered
  val activity = LocalActivity.current
  val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize().background(AppColor.Background).padding(all = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
      Box(modifier = Modifier.align(Alignment.TopEnd).systemBarsPadding()) {
        IconButton(onClick = { dropdownExpanded = true }) {
          Icon(
              imageVector = Icons.Default.LinkOff,
              contentDescription = "DisconnectIcon",
              tint = AppColor.PrimaryAccent,
              modifier = Modifier.size(28.dp),
          )
        }

        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { dropdownExpanded = false },
        ) {
          DropdownMenuItem(
              text = {
                Text(
                    stringResource(R.string.unregister_button_title),
                    color = if (isDisconnectEnabled) AppColor.Red else Color.Gray,
                )
              },
              enabled = isDisconnectEnabled,
              onClick = {
                activity?.let { viewModel.startUnregistration(it) }
                    ?: Toast.makeText(context, "Activity not available", Toast.LENGTH_SHORT).show()
                dropdownExpanded = false
              },
              modifier = Modifier.height(30.dp),
          )
        }
      }

      Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
            painter = painterResource(id = R.drawable.smart_glasses_icon),
            contentDescription = "Glasses icon",
            tint = AppColor.PrimaryAccent,
            modifier = Modifier.size(48.dp),
        )
        if (uiState.hasActiveDevice) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Canvas(modifier = Modifier.size(10.dp)) {
              drawCircle(color = AppColor.Safe)
            }
            Text(
                text = "Glasses Connected",
                style = MaterialTheme.typography.headlineSmall,
                color = AppColor.TextPrimary,
            )
          }
        } else {
          Text(
              text = "Waiting for glasses...",
              style = MaterialTheme.typography.bodyLarge,
              color = AppColor.TextSecondary,
          )
        }
      }

      Column(
          modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (!uiState.hasActiveDevice) {
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 12.dp),
          ) {
            Icon(
                painter = painterResource(id = R.drawable.hourglass_icon),
                contentDescription = "Waiting for device",
                tint = AppColor.Secondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.waiting_for_active_device),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColor.Secondary,
            )
          }
        }

        // Start Streaming Button
        SwitchButton(
            label = "Start",
            onClick = { viewModel.navigateToStreaming(onRequestWearablesPermission) },
            enabled = uiState.hasActiveDevice,
        )
      }

      // Getting Started Sheet (removed from default view)
    }
}

@Composable
private fun GettingStartedSheetContent(onContinue: () -> Unit, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(24.dp),
  ) {
    Text(
        text = stringResource(R.string.getting_started_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(8.dp).padding(bottom = 16.dp),
    ) {
      TipItem(
          iconResId = R.drawable.video_icon,
          text = stringResource(R.string.getting_started_tip_permission),
      )
      TipItem(
          iconResId = R.drawable.tap_icon,
          text = stringResource(R.string.getting_started_tip_photo),
      )
      TipItem(
          iconResId = R.drawable.smart_glasses_icon,
          text = stringResource(R.string.getting_started_tip_led),
      )
    }

    SwitchButton(
        label = stringResource(R.string.getting_started_continue),
        onClick = onContinue,
        modifier = Modifier.navigationBarsPadding(),
    )
  }
}

@Composable
private fun TipItem(iconResId: Int, text: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth()) {
    Icon(
        painter = painterResource(id = iconResId),
        contentDescription = "Getting started tip icon",
        modifier = Modifier.padding(start = 4.dp, top = 4.dp).width(24.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(text = text)
  }
}
