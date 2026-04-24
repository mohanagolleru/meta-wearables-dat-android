/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwitchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
) {
  Button(
      modifier = modifier.height(52.dp).fillMaxWidth(),
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor =
                  if (isDestructive) AppColor.DestructiveBackground else AppColor.PrimaryAccent,
              disabledContainerColor = AppColor.Secondary.copy(alpha = 0.3f),
              disabledContentColor = AppColor.TextSecondary,
              contentColor = if (isDestructive) AppColor.DestructiveForeground else Color.White,
          ),
      enabled = enabled,
  ) {
    Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
  }
}
