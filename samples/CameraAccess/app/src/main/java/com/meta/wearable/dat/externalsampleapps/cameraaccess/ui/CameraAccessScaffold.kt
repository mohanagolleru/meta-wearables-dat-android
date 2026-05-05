/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// CameraAccessScaffold - Drishti Navigation Orchestrator
//
// Two-tier navigation:
// 1. Not registered → full-screen HomeScreen (welcome / registration)
// 2. Registered     → Scaffold with DrishtiNavBar bottom tabs:
//      HOME      – HomeDashboard (care-recipient overview, start-round CTA)
//      ASSISTANT – StreamScreen when streaming, NonStreamScreen otherwise
//      LOG       – placeholder (Care Log, coming soon)
//      PROFILE   – CareRecipientProfile

package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import com.meta.wearable.dat.externalsampleapps.cameraaccess.MainActivity
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel

@Composable
fun CameraAccessScaffold(
    viewModel: WearablesViewModel,
    onRequestWearablesPermission: suspend (Permission) -> PermissionStatus,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Track selected tab
    var selectedTab by remember { mutableStateOf(DrishtiTab.HOME) }

    // Auto-switch to ASSISTANT tab when streaming starts
    LaunchedEffect(uiState.isStreaming) {
        if (uiState.isStreaming) selectedTab = DrishtiTab.ASSISTANT
    }

    // Observe errors and show snackbar
    LaunchedEffect(uiState.recentError) {
        uiState.recentError?.let { errorMessage ->
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearCameraPermissionError()
        }
    }

    if (!uiState.isRegistered) {
        // ── Welcome screen — no nav bar, full screen ──────────────────
        Box(modifier = modifier.fillMaxSize()) {
            HomeScreen(viewModel = viewModel)

            ErrorSnackbar(snackbarHostState)
        }
    } else {
        // ── Main app with bottom nav ──────────────────────────────────
        val activity = LocalActivity.current as MainActivity

        Scaffold(
            modifier = modifier,
            bottomBar = {
                DrishtiNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                )
            },
            containerColor = AppColor.Background,
        ) { innerPadding ->
            Crossfade(
                targetState = selectedTab,
                label = "tabContent",
            ) { tab ->
                when (tab) {
                    DrishtiTab.HOME -> HomeDashboard(
                        onStartRound = {
                            viewModel.navigateToStreaming { permission ->
                                activity.requestWearablesPermission(permission)
                            }
                            selectedTab = DrishtiTab.ASSISTANT
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                    DrishtiTab.ASSISTANT -> {
                        if (uiState.isStreaming) {
                            StreamScreen(
                                wearablesViewModel = viewModel,
                                modifier = Modifier.padding(innerPadding),
                            )
                        } else {
                            NonStreamScreen(
                                viewModel = viewModel,
                                onRequestWearablesPermission = onRequestWearablesPermission,
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                    DrishtiTab.LOG -> CareLogScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                    DrishtiTab.PROFILE -> CareRecipientProfile(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }

            ErrorSnackbar(snackbarHostState)
        }
    }
}

// ── Shared error snackbar ────────────────────────────────────────────
@Composable
private fun ErrorSnackbar(hostState: SnackbarHostState) {
    SnackbarHost(
        hostState = hostState,
        modifier =
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 32.dp),
        snackbar = { data ->
            Snackbar(
                shape = RoundedCornerShape(24.dp),
                containerColor = AppColor.DestructiveBackground,
                contentColor = AppColor.TextPrimary,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = AppColor.Critical,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(data.visuals.message)
                }
            }
        },
    )
}
