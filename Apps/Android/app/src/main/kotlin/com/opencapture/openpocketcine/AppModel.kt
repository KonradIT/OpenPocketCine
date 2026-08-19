package com.opencapture.openpocketcine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opencapture.openpocketcine.bridge.SwiftCore
import com.opencapture.openpocketcine.core.ConnectionPhase
import com.opencapture.openpocketcine.pairing.SavedCamera
import com.opencapture.openpocketcine.pairing.SavedCameras
import com.opencapture.openpocketcine.pairing.SharedPreferencesSavedCameraStore
import com.opencapture.openpocketcine.session.PocketCameraSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object OperatorPrefs {
    private const val PREFS = "openpocketcine.operator"
    private const val AWAKE = "keep-screen-awake"

    fun keepScreenAwake(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(AWAKE, true)

    fun setKeepScreenAwake(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(AWAKE, value).apply()
    }
}

class AppModel(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = SharedPreferencesSavedCameraStore(context)
    val session = PocketCameraSession(context)

    var savedCameras by mutableStateOf(store.load())
        private set
    var isPairingNewCamera by mutableStateOf(false)
    var showsLaunchSplash by mutableStateOf(true)
    var coreVersion by mutableStateOf<String?>(null)
    var homePanel by mutableStateOf<AppPanel?>(null)
    var keepScreenAwake by mutableStateOf(OperatorPrefs.keepScreenAwake(appContext))
        private set
    var phoneBatteryPercent by mutableStateOf(-1)
        private set
    var phoneCharging by mutableStateOf(false)
        private set
    var uiLocked by mutableStateOf(false)

    fun updateKeepScreenAwake(value: Boolean) {
        keepScreenAwake = value
        OperatorPrefs.setKeepScreenAwake(appContext, value)
    }

    fun refreshPhoneBattery() {
        val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        phoneBatteryPercent = if (pct in 0..100) pct else -1
        val sticky =
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        phoneCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun pressRecord() = session.pressRecord()

    fun setIsoIndex(index: Int) = session.setIsoIndex(index)

    fun setShutterDenom(denom: Int) = session.setShutterDenom(denom)

    fun setExpoMode(manual: Boolean) = session.setExpoMode(manual)

    fun setWhiteBalanceAuto() = session.setWhiteBalanceAuto()

    fun setWhiteBalance(kelvin: Int, tint: Int) = session.setWhiteBalance(kelvin, tint)

    fun setFocusMode(continuous: Boolean) = session.setFocusMode(continuous)

    fun setColorMode(mode: Int) = session.setColorMode(mode)

    fun setResolutionFps(res: Int, fpsIndex: Int) = session.setResolutionFps(res, fpsIndex)

    fun setAudioChannel(value: Int) = session.setAudioChannel(value)

    fun setVocalBoost(on: Boolean) = session.setVocalBoost(on)

    fun setWindNr(on: Boolean) = session.setWindNr(on)

    fun setDirectionalAudio(mode: Int) = session.setDirectionalAudio(mode)

    fun refreshAudio() = session.refreshAudio()

    fun tapFocus(x: Float, y: Float) = session.tapFocus(x, y)

    fun updateGimbalStick(x: Float, y: Float) {
        if (uiLocked) return
        session.updateGimbalStick(x, y)
    }

    fun endGimbalStick() = session.endGimbalStick()

    val shouldShowWizard: Boolean
        get() = SavedCameras.launchShowsWizard(savedCameras) || isPairingNewCamera

    val isLive: Boolean
        get() = session.phaseFlow.value == ConnectionPhase.LIVE

    val isBusy: Boolean
        get() =
            when (session.phaseFlow.value) {
                ConnectionPhase.IDLE,
                ConnectionPhase.SCANNING,
                ConnectionPhase.FAILED,
                ConnectionPhase.LIVE,
                -> false
                else -> true
            }

    fun prepareStartup() {
        savedCameras = store.load()
        coreVersion =
            if (SwiftCore.isAvailable) runCatching { SwiftCore.coreVersion() }.getOrNull() else null
        isPairingNewCamera = SavedCameras.launchShowsWizard(savedCameras)
        session.startScan()
        scope.launch {
            session.phaseFlow.collect { phase ->
                if (phase == ConnectionPhase.LIVE) persistConnectedCameraIfNeeded()
            }
        }
    }

    fun pairNewCamera() {
        isPairingNewCamera = true
        session.startScan()
    }

    fun cancelPairing() {
        session.disconnect()
        isPairingNewCamera = false
        session.startScan()
    }

    fun reconnect(camera: SavedCamera) {
        session.reconnect(camera.id)
    }

    fun forget(camera: SavedCamera) {
        savedCameras = SavedCameras.removing(camera.id, savedCameras)
        store.save(savedCameras)
        if (savedCameras.isEmpty()) {
            isPairingNewCamera = true
            session.startScan()
        }
    }

    fun rename(camera: SavedCamera, name: String?) {
        savedCameras = SavedCameras.renaming(camera.id, name, savedCameras)
        store.save(savedCameras)
    }

    fun persistConnectedCameraIfNeeded() {
        val found = session.connectedCamera ?: return
        if (session.phaseFlow.value != ConnectionPhase.LIVE) return
        val record =
            SavedCamera(
                id = found.id,
                advertisedName = found.name,
                modelName = found.model.name,
                lastSSID = session.joinedSSID,
                lastConnectedAt = System.currentTimeMillis(),
            )
        savedCameras = SavedCameras.upserting(record, savedCameras)
        store.save(savedCameras)
        isPairingNewCamera = false
    }

    fun disconnect() {
        session.disconnect()
        if (!SavedCameras.launchShowsWizard(savedCameras)) session.startScan()
    }

    fun close() {
        session.close()
    }
}
