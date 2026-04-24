/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// HomeScreen - DAT Registration Entry Point
//
// This screen handles DAT device registration.

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@Composable
fun HomeScreen(
    viewModel: WearablesViewModel,
    modifier: Modifier = Modifier,
) {
  val activity = LocalActivity.current
  val context = LocalContext.current

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(AppColor.Background)
              .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Spacer(modifier = Modifier.weight(1f))

    // Glasses icon — 64dp, coral tint
    Icon(
        painter = painterResource(R.drawable.smart_glasses_icon),
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = AppColor.PrimaryAccent,
    )

    Spacer(modifier = Modifier.height(20.dp))

    // App name
    Text(
        text = "Drishti",
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppColor.TextPrimary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Tagline
    Text(
        text = "Smart caregiving, hands-free",
        fontSize = 15.sp,
        color = AppColor.TextSecondary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.weight(1f))

    // Connect button — coral, rounded, full width
    SwitchButton(
        label = "Connect Your Glasses",
        onClick = {
          activity?.let { viewModel.startRegistration(it) }
              ?: Toast.makeText(context, "Activity not available", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Redirect notice
    Text(
        text = "You'll be redirected to the Meta app\nto confirm your connection.",
        fontSize = 12.sp,
        color = AppColor.TextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp,
    )

    Spacer(modifier = Modifier.height(32.dp))
  }
}
