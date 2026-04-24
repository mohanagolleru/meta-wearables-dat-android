/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// StreamScreen — Drishti Live Assistant UI
//
// Shows live video from glasses (rounded card), a voice-reactive orb that
// pulses with real audio, and a status label. The existing WebSocket + Gemini
// backend is unchanged — this is a visual-only redesign.

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.camera.types.StreamSessionState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.R
import com.meta.wearable.dat.externalsampleapps.cameraaccess.stream.StreamViewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@Composable
fun StreamScreen(
    wearablesViewModel: WearablesViewModel,
    modifier: Modifier = Modifier,
    streamViewModel: StreamViewModel =
        viewModel(
            factory =
                StreamViewModel.Factory(
                    application = (LocalActivity.current as ComponentActivity).application,
                    wearablesViewModel = wearablesViewModel,
                ),
        ),
) {
  val streamUiState by streamViewModel.uiState.collectAsStateWithLifecycle()
  val audioLevel by streamViewModel.audioLevel.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { streamViewModel.startStream() }

  val statusText = when {
    streamUiState.streamSessionState == StreamSessionState.STARTING -> "Connecting..."
    audioLevel > 0.05f -> "Listening..."
    else -> "Ready"
  }

  Column(
      modifier = modifier
          .fillMaxSize()
          .background(AppColor.Background)
          .statusBarsPadding(),
  ) {

    // ── Top bar ─────────────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
          text = "Drishti",
          fontSize = 17.sp,
          fontWeight = FontWeight.Medium,
          color = AppColor.TextSecondary,
      )
      Spacer(modifier = Modifier.weight(1f))
      if (streamUiState.streamSessionState == StreamSessionState.STREAMING) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AppColor.Safe),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Live",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AppColor.Safe,
        )
      }
    }

    // ── Error banner ────────────────────────────────────────────────
    streamUiState.streamError?.let { error ->
      Card(
          modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = AppColor.Warning),
      ) {
        Text(
            text = error,
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ── Video preview (16:9 rounded card with elevation) ────────────
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(16f / 9f),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A28)),
    ) {
      Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center,
      ) {
        streamUiState.videoFrame?.let { videoFrame ->
          Image(
              bitmap = videoFrame.asImageBitmap(),
              contentDescription = stringResource(R.string.live_stream),
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
          )
        }
        if (streamUiState.streamSessionState == StreamSessionState.STARTING) {
          CircularProgressIndicator(color = AppColor.PrimaryAccent)
        }
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    // ── Voice orb (audio-reactive) ──────────────────────────────────
    VoiceOrb(
        audioLevel = audioLevel,
        modifier = Modifier
            .size(150.dp)
            .align(Alignment.CenterHorizontally),
    )

    Spacer(modifier = Modifier.height(12.dp))

    // ── Status label ────────────────────────────────────────────────
    Text(
        text = statusText,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = AppColor.TextSecondary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )

    Spacer(modifier = Modifier.weight(1f))

    // ── Stop button (small circular) ────────────────────────────────
    IconButton(
        onClick = {
          streamViewModel.stopStream()
          wearablesViewModel.navigateToDeviceSelection()
        },
        modifier = Modifier
            .size(48.dp)
            .align(Alignment.CenterHorizontally)
            .background(
                color = AppColor.Critical.copy(alpha = 0.1f),
                shape = CircleShape,
            )
            .border(
                width = 1.dp,
                color = AppColor.Critical.copy(alpha = 0.3f),
                shape = CircleShape,
            ),
    ) {
      Icon(
          imageVector = Icons.Default.Stop,
          contentDescription = "Stop",
          tint = AppColor.Critical,
      )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Spacer(modifier = Modifier.navigationBarsPadding())
  }
}
