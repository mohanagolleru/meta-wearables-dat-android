# Drishti Streaming Pipeline — Architecture Reference

**Purpose:** Debug-oriented map of the complete streaming pipeline. Written after resolving a "video stuck at STARTING" bug caused by a corrupted BTC link. Read this before touching any streaming code.

**Repo root for this document:** `samples/CameraAccess/` inside the `drishti` branch of `mohanagolleru/meta-wearables-dat-android`.

---

## Critical Port Fact — Read This First

The Android app (`WsBinarySender.kt:68`) dials `ws://127.0.0.1:8765`. The Mac server (`medspect_assistant.py`) listens on port **18790**. These are connected by `adb reverse tcp:8765 tcp:18790`, which forwards the phone's local port 8765 to the Mac's port 18790.

**Consequence:** If you grep the Android codebase for "18790", you will find nothing. The Android code only knows about 8765. The Mac code only knows about 18790. The adb tunnel is the bridge. If the tunnel is down, the WS silently fails to connect and you get no frames and no audio, but no crash.

Verify the tunnel is alive before debugging anything else:

```
adb reverse --list
# should show: (tcp) 8765 (tcp) 18790
```

If the entry is missing, re-run: `adb reverse tcp:8765 tcp:18790`

---

## 1. Trigger Path: User Tap to startStream()

There are two entry points that call `navigateToStreaming()`. Both ultimately reach the same `startStream()` call.

**Entry point A — NonStreamScreen (primary)**

The user lands on the ASSISTANT tab when not streaming. `NonStreamScreen.kt:169-173` renders a "Start" button whose `onClick` is:

```
viewModel.navigateToStreaming(onRequestWearablesPermission)
```

The button is only `enabled` when `uiState.hasActiveDevice` is true (glasses detected via the DAT SDK). If the button is greyed out, the SDK has not seen the glasses yet — this is a BT pairing problem, not a streaming problem.

**Entry point B — HomeDashboard**

`CameraAccessScaffold.kt:106-113` renders `HomeDashboard` on the HOME tab with an `onStartRound` lambda that also calls `viewModel.navigateToStreaming(...)` and then switches to the ASSISTANT tab.

**Shared path from both entry points:**

`WearablesViewModel.navigateToStreaming()` (`WearablesViewModel.kt:127-155`):
1. Checks `Permission.CAMERA` via `Wearables.checkPermissionStatus()` — this is the Stella/WDAT camera permission, separate from Android's CAMERA permission.
2. If not granted, calls `onRequestWearablesPermission(permission)` which launches `Wearables.RequestPermissionContract()` via the Meta AI app (the glasses companion app).
3. On grant (or if already granted), sets `_uiState.isStreaming = true`.

`CameraAccessScaffold.kt:67-69`: A `LaunchedEffect(uiState.isStreaming)` detects the change and switches `selectedTab` to `DrishtiTab.ASSISTANT`.

`CameraAccessScaffold.kt:114-120`: The ASSISTANT tab branch now renders `StreamScreen` because `uiState.isStreaming` is true.

`StreamScreen.kt:82`: The very first composable effect fires:
```kotlin
LaunchedEffect(Unit) { streamViewModel.startStream() }
```

**Key subtlety — the re-entry guard:** `StreamViewModel.kt:107-110` checks `if (streamSession != null) return`. If `LaunchedEffect` fires twice (recomposition, config change), the second call is silently dropped. If you see logcat line `"startStream: session already active, ignoring duplicate call"`, the session from the first call is still alive — look at its state, not why the second was dropped.

---

## 2. WDAT SDK Boundary: What startStreamSession() Returns

`StreamViewModel.kt:150-158` calls:

```kotlin
Wearables.startStreamSession(
    getApplication(),
    deviceSelector,          // AutoDeviceSelector — picks first paired glasses
    StreamConfiguration(videoQuality = VideoQuality.LOW, 10),  // 10 = frame rate hint
)
```

This call crosses into the `mwdat-camera` AAR (version 0.4.0, shipped as a prebuilt). Source is not available. The call is synchronous but may throw if the SDK cannot set up the session (e.g., device not connected, permissions missing). The `try/catch` at line 160 handles this.

The returned `StreamSession` exposes:
- `session.videoStream: Flow<VideoFrame>` — emits raw I420 frames from the glasses camera
- `session.state: StateFlow<StreamSessionState>` — the session lifecycle state

**The 6 SDK states** (from `com.meta.wearable.dat.camera.types.StreamSessionState`):

| State | What it means in practice |
|---|---|
| `STARTING` | SDK sent session-start to glasses over BTC; waiting for acknowledge. UI shows spinner. This is where stucks happen. |
| `STARTED` | Acknowledge received; camera pipeline negotiating. Not yet emitting frames. |
| `STREAMING` | Glasses camera is live. `videoStream` Flow is emitting. `capturePhoto()` becomes available. UI shows green "Live" dot. |
| `STOPPING` | `session.close()` called; SDK tearing down gracefully. |
| `STOPPED` | Session fully torn down. Triggers `navigateToDeviceSelection()` in `stateJob`. |
| `CLOSED` | SDK internal terminal state; not explicitly handled in app code. |

**Observable SDK logcat tag:** `DAT:STREAM:StreamSession`

Known SDK-emitted lines (not controllable from app code):
- `"Starting stream session"` — emitted at STARTING entry
- `"Starting stream stream"` — emitted at STARTED → STREAMING transition
- `"StreamingSession started streaming for device id"` — confirms STREAMING reached
- `"Stream session in unexpected state"` — SDK saw an illegal transition
- `"Device disconnected"` — BTC link dropped during session

The app only reacts to two transitions: `STOPPED` (triggers shutdown) and `STREAMING` (enables photo capture). All intermediate transitions are SDK-internal.

---

## 3. State Machine: Who Moves What

```
                    ┌────────────────────────────────────────────┐
                    │           SDK INTERNAL                     │
                    │                                            │
 startStreamSession()
        │
        ▼
   [STARTING] ─── BTC acknowledge ──► [STARTED] ──► [STREAMING]
        │                                                │
        │ BTC link dead / device not found              │ session.close() called
        │ SDK: "Device disconnected"                    │ OR stateJob sees STOPPED
        ▼                                               ▼
   [STOPPED] ◄────────────────────────────────── [STOPPING]
        │
        ▼
 stateJob fires:
 stopStream() → navigateToDeviceSelection()
```

**STARTING → STOPPED (the bug we just debugged):** The SDK emits "Starting stream session" but never "StreamingSession started streaming for device id". Instead state transitions to STOPPED. This means the BTC (Bluetooth Classic) link between phone and glasses is in a bad state — the glasses are paired and visible to the Android Bluetooth stack, but the Stella data channel is corrupted or held by another app.

**Stuck at STARTING indefinitely:** The state never moves at all (no STOPPED either). This is the more severe form — the glasses are not responding to the session-start command. Fix: hard Bluetooth toggle (see Quick Debug Recipe). The app's silent watchdog (15s timeout in `StreamViewModel.stateJob`) will clean up the stuck session and bounce the user back to the Start button.

**STREAMING → STOPPED mid-session:** The glasses BT link dropped while streaming (e.g., glasses moved out of range, WhatsApp forcefully took the BTC lock). The stateJob observes this and calls `stopStream()`.

---

## 4. Two Parallel Pipelines

### 4A — Video Pipeline (WDAT-dependent)

```
Glasses camera (I420 frames)
    │
    │  BTC channel managed by Stella/WDAT SDK
    │
VideoFrame emitted on session.videoStream Flow
    │
    ▼
StreamViewModel.handleVideoFrame()   [Dispatchers.IO coroutine, videoJob]
    │
    ├── Validate I420 frame size: expected = width * height * 3/2
    │   If too small: drop frame with logcat "I420 frame too small"
    │
    ├── Copy ByteBuffer to ByteArray (buffer.get(byteArray))
    │   Note: buffer.position() is preserved after read
    │
    ├── Convert I420 → NV21 (Y plane identical; U/V planes interleaved in reverse order)
    │   NV21 is required because Android's YuvImage only accepts NV21/NV12
    │
    ├── Encode NV21 → JPEG (compressToJpeg, quality=50, reuses jpegBuffer ByteArrayOutputStream)
    │   If compressToJpeg returns false: drop frame with logcat "compressToJpeg failed"
    │
    ├── Throttle: send only if (now - lastWsSentMs) >= 1000ms  (~1 FPS to Mac)
    │   If sending: wsSender.sendVideoJpeg(jpegBytes) — prepends b'V' byte
    │   If WS send fails: log "WS send failing (attempt N)" every 5 failures
    │
    └── Decode JPEG → Bitmap → update _uiState.videoFrame (for UI preview)
```

**Frame counter:** `frameCounter` increments on every frame received (before throttle). The first frame logs `"videoStream first frame received: WxH NB"`. If this line never appears in logcat, `session.videoStream` never emitted — meaning the SDK never reached STREAMING state. If this line appears but Mac shows `frames=0`, the WS tunnel is down.

**Key file/line:** `StreamViewModel.kt:293-361`

### 4B — Audio Pipeline (BT SCO, independent of WDAT)

**Capture (phone mic → Mac):**
```
Ray-Ban glasses mic
    │  Bluetooth HFP/SCO profile (AudioManager.startBluetoothSco)
    │  Falls back to phone mic while SCO is connecting
    │
AudioRecord (16kHz, mono, PCM16, VOICE_COMMUNICATION source)
    │  captureThread (background thread, "bt-audio-capture")
    │
    ├── Read bufferSize bytes at a time
    ├── Compute RMS → _audioLevel (VoiceOrb visualization)
    ├── Accumulate until >= 3200 bytes (100ms of audio at 16kHz)
    └── Invoke onAudioCaptured(chunk)
            │
            ▼
    wsSender.sendAudio(pcmData) — prepends b'A' byte
            │
            ▼
    Mac receives audio_q_in.put_nowait(payload)
            │
            ▼
    mic_loop(session) → session.send_realtime_input(audio=Blob(pcm, "audio/pcm"))
            │
            ▼
    Gemini Live API
```

**Playback (Mac → glasses speakers):**
```
Gemini Live API emits audio
    │
recv_loop() → audio_q_out.put_nowait(inline.data)
    │
play_loop() → send_audio_to_phone() → WS msg with b'A' prefix
    │
WsBinarySender.onMessage() strips prefix, calls onAudioReceived(audio)
    │
audioBridge.playAudio(data)
    │
AudioTrack (24kHz, mono, PCM16, USAGE_VOICE_COMMUNICATION)
    │  Bluetooth SCO (switches from phone speaker to glasses speaker when SCO connects)
    │
Ray-Ban glasses speakers
```

**Audio format requirement:** Send at 16kHz, receive at 24kHz. These are Gemini Live API requirements. If audio sample rate mismatches, Gemini will accept it but the voice will be pitch-shifted or garbled.

**SCO connection is async:** `startCapture()` calls `startBluetoothSco()` and returns immediately. Audio starts recording from the phone mic. The `BroadcastReceiver` listening for `ACTION_SCO_AUDIO_STATE_UPDATED` logs `"SCO connected — audio now routed through glasses"` when the handoff completes. You can verify SCO state by watching for this logcat line from tag `BtAudioBridge`.

**Key files:** `BluetoothAudioBridge.kt` (entire file), `WsBinarySender.kt:57-66` (receive path)

---

## 5. Failure Modes with Logcat Signatures

### F1 — Session never advances past STARTING (stuck)

**Symptom:** UI spinner shows indefinitely. Status label stays "Connecting...".

**Logcat (StreamViewModel tag):**
```
I StreamViewModel: startStream: entry
I StreamViewModel: startStream: session created, awaiting state
I StreamViewModel: stream state -> STARTING (prev=null)
```

...then nothing for 10-30 seconds. No "stream state -> STARTED" or "stream state -> STREAMING".

**Root cause:** Stella's BTC RFCOMM link is in a corrupted state. Look for `hammerhead|LinkSwitchDelegateV2: ... removing BtcRfcomm ... Switched to BleL2Cap ... btcLeaseCount=0` in system-wide logcat. BLE-only is insufficient bandwidth for video.

**Auto-recovery in app:** After 15 seconds the watchdog at `StreamViewModel.stateJob` fires, silently calls `stopStream()` and `navigateToDeviceSelection()`. The user sees the Start button reappear. By then, BT link state has often self-healed and a re-tap works.

**Fix sequence (manual, if start.sh wasn't run):**
1. `adb shell am force-stop com.whatsapp`
2. `adb shell svc bluetooth disable; sleep 5; adb shell svc bluetooth enable; sleep 12`
3. Tap Start again.

**Better:** Just re-run `./start.sh` — its `_preflight()` block does all of this automatically.

### F2 — Session starts but state transitions to STOPPED immediately

**Symptom:** Spinner appears then disappears. UI flips back to NonStreamScreen.

**Logcat (StreamViewModel tag):**
```
I StreamViewModel: stream state -> STARTING (prev=STOPPED)
I StreamViewModel: stream state -> STOPPED (prev=STARTING)
```

**Root cause:** SDK internal error — device not found at session-start time, or `startStreamSession()` threw (caught at `StreamViewModel.kt:160` with `Log.e(TAG, "Failed to start stream session", t)`).

### F3 — Stella camera permission denied

**Symptom:** `cameraState=CAPTURE_READY, isStreamingActive=false` in Stella logs. App thinks streaming started but no frames flow.

**Root cause:** Per-app camera permission on Stella set to "Don't allow". This is a SEPARATE permission system from Android's CAMERA / RECORD_AUDIO.

**Fix:** When the "Allow Drishti to access the camera on your Meta devices?" dialog appears, choose **"Always allow"**. It persists across Drishti app data clears, but is reset if you clear Stella's app data.

### F4 — WS tunnel down

**Symptom:** `"videoStream first frame received"` appears but Mac shows `frames=0`. Logcat `WsBinarySender: WS failure` repeating.

**Fix:** `adb reverse tcp:8765 tcp:18790`.

### F5 — audio_chunks growing but frames=0 on Mac

Audio path uses BT SCO directly (not WDAT). Video path uses WDAT. They are independent — one can break while the other works.

If audio works but frames=0:
- Check for `"videoStream first frame received"` in Android logcat — if missing, WDAT isn't STREAMING (see F1)
- If present, check `"compressToJpeg failed"` or `"WS send failing"` (b'V' frames specifically)

### F6 — "session already active, ignoring duplicate call"

This is not a bug itself — it's the re-entry guard working correctly. Investigate why startStream was called twice (likely a recomposition or config change).

### F7 — RECORD_AUDIO missing

**Logcat (BtAudioBridge tag):**
```
E BtAudioBridge: Missing RECORD_AUDIO permission — cannot start capture
```

Already fixed: `RECORD_AUDIO` is in `MainActivity.PERMISSIONS` array. If this log appears, the user revoked it post-install. Settings → Apps → Drishti → Permissions.

### F8 — Mac rejects with code 1008

`WS closed code=1008 reason=another phone is already connected`. Previous Mac session didn't clean up `phone_ws`. Restart the Mac server.

---

## 6. Operational Gotchas

| Gotcha | Symptom | Auto-handled? |
|---|---|---|
| WhatsApp BTC lock | Stuck at STARTING | ✅ start.sh `_preflight` force-stops it |
| Stella BTC corruption (post-Meta-AI-update) | Stuck at STARTING | ✅ start.sh toggles BT preemptively |
| AS emulator running | adb "more than one device" | ✅ start.sh detects and aborts |
| Drishti app stale state | Various | ✅ start.sh force-stops it |
| USB disconnect mid-session | WS sends fail | Manual: re-run `adb reverse tcp:8765 tcp:18790` |
| Stella per-device camera "Don't allow" | cameraState=CAPTURE_READY but no frames | Manual: tap "Always allow" once (persists) |

**Project root for AS:** `samples/CameraAccess/` (the standalone Gradle project). Opening the parent multi-project root confuses AS.

---

## 7. Mac Server Side (medspect_assistant.py)

**File:** `/Users/yasodhargolleru/Senior_AI_Project/medspect_phase3/medspect_assistant.py`

**WS server binding:** `websockets.serve(phone_ws_handler, WS_HOST, WS_PORT)` on `127.0.0.1:18790`. Binary messages only.

**Prefix dispatch** (`phone_ws_handler`, lines 265-291):
```
msg[0] == b'V'  → strip prefix, resize JPEG, store as latest_frame_bytes
msg[0] == b'A'  → strip prefix, put PCM into audio_q_in (bounded, maxsize=50)
```

**Duplicate connection rejection** (lines 249-254): If a second phone connects, close it with code 1008.

**Four concurrent tasks per Gemini session:**
- `mic_loop(session)` — drains `audio_q_in`, sends to Gemini. Muted while `model_speaking=True`.
- `video_loop(session)` — polls `latest_frame_bytes` every 1/VIDEO_FPS seconds.
- `play_loop()` — drains `audio_q_out`, sends back to phone with b'A' prefix.
- `recv_loop(session)` — reads Gemini turns, routes audio + handles tool calls.

**Reconnect supervisor:** Any task exception → all tasks cancelled → reconnect after 5s. The phone WS keeps running across Gemini reconnects.

---

## 8. File Map

All paths relative to the repo root `Senior_AI_Project/`.

| File | Role |
|---|---|
| `meta_wdat/.../cameraaccess/MainActivity.kt` | App entry, permission requests, Wearables.initialize() |
| `meta_wdat/.../cameraaccess/ui/CameraAccessScaffold.kt` | Tab navigation, isStreaming state observer |
| `meta_wdat/.../cameraaccess/ui/NonStreamScreen.kt` | Primary "Start" button trigger |
| `meta_wdat/.../cameraaccess/ui/StreamScreen.kt` | LaunchedEffect that fires startStream(); status label |
| `meta_wdat/.../cameraaccess/stream/StreamViewModel.kt` | Central orchestrator: WDAT session, video pipeline, lifecycle, watchdog |
| `meta_wdat/.../cameraaccess/stream/WsBinarySender.kt` | WebSocket client (OkHttp), b'V'/b'A' framing |
| `meta_wdat/.../cameraaccess/stream/BluetoothAudioBridge.kt` | BT SCO audio capture and playback |
| `meta_wdat/.../cameraaccess/wearables/WearablesViewModel.kt` | DAT SDK device discovery, navigateToStreaming() |
| `meta_wdat/.../AndroidManifest.xml` | RECORD_AUDIO + BLUETOOTH_CONNECT + INTERNET permissions |
| `medspect_phase3/start.sh` | Launcher with `_preflight()` prevention block |
| `medspect_phase3/medspect_assistant.py` | Mac WS server, Gemini Live session, prefix dispatch |

---

## Quick Debug Recipe — "frames=0 or stuck at STARTING"

If `start.sh` ran cleanly, you should not need this. But if you do:

**Step 1 — Re-run start.sh.** Its `_preflight()` block does WhatsApp force-stop, BT stack toggle, and Drishti app cleanup. ~17 seconds. Solves 99% of stuck-at-STARTING.

**Step 2 — Verify adb tunnel**
```bash
adb reverse --list
# Expected: (tcp) 8765 (tcp) 18790
```
If missing: `adb reverse tcp:8765 tcp:18790`.

**Step 3 — Check the lifecycle breadcrumbs**
```bash
adb logcat -s StreamViewModel:I
```
Look for the four canonical lines:
1. `startStream: entry` — function called
2. `startStream: session created, awaiting state` — SDK accepted the request
3. `stream state -> STREAMING (prev=STARTED)` — session fully established
4. `videoStream first frame received: WxH NB` — frames flowing

If you only see (1) but not (2): SDK threw — look for `Failed to start stream session`.
If you see (2) but state stuck at STARTING: BTC link issue — re-run `start.sh`.
If you see (4) but Mac shows `frames=0`: WS tunnel issue — re-run `adb reverse`.

**Step 4 — Check Stella's view**
```bash
adb logcat -d 2>/dev/null | grep -E "cameraState=|isStreamingActive|btcLeaseCount"
```
- `cameraState=CAPTURE_READY, isStreamingActive=false` → Stella has glasses but session never went live (BTC issue or permission)
- `btcLeaseCount=0` → BTC RFCOMM not active → BT toggle needed (re-run `start.sh`)

---

**Last verified:** 2026-05-05 — streaming works end-to-end after BT-toggle preflight added to `start.sh` and 15s silent watchdog added to `StreamViewModel`.
