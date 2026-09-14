package com.easyconnect.agent.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.easyconnect.agent.interfaces.IPicoConfigurationReader
import com.easyconnect.agent.model.PicoConfigurationJsonData
import kotlinx.coroutines.flow.firstOrNull

private val Context.picoDataStore by preferencesDataStore(
    name = "pico_config"
)
private object PicoKeys {
    val WIFI_SSID = stringPreferencesKey("wifi_ssid")
    val WIFI_PASSWORD = stringPreferencesKey("wifi_password")

    val KEEP_WIFI_ON = booleanPreferencesKey("keep_wifi_on")
    val WIRELESS_DEBUG = booleanPreferencesKey("wireless_debug")
    val USB_DEBUG = booleanPreferencesKey("usb_debug")

    val HOME_GESTURE = booleanPreferencesKey("home_gesture")
    val CALIBRATE_GESTURE = booleanPreferencesKey("calibrate_gesture")
    val HAND_CONTROLLERS = booleanPreferencesKey("hand_and_controllers")

    val MODE_LBE = booleanPreferencesKey("mode_lbe")
    val LONG_HOME_PRESS_BUTTON =
        booleanPreferencesKey("long_home_press_button")

    val TEMPORARY_PLAY_BOUNDARY =
        booleanPreferencesKey("temporary_play_boundary")

    val BOUNDARY_CONFIRMATION_POPUP =
        booleanPreferencesKey("boundary_confirmation_popup")

    val USE_MARKERS_FIRST_POSITION_RECOVERY =
        booleanPreferencesKey("use_markers_first_position_recovery")

    val DISTANCE_SENSITIVITY_PLAY_BOUNDARY =
        intPreferencesKey("distance_sensitivity_play_boundary")

    val COLOR_FENCE_R = intPreferencesKey("color_fence_r")
    val COLOR_FENCE_G = intPreferencesKey("color_fence_g")
    val COLOR_FENCE_B = intPreferencesKey("color_fence_b")
}
class PicoConfigurationDataStore(private val context: Context)
{
    suspend fun savePicoConfig(
        config: IPicoConfigurationReader
    ) {
        context.picoDataStore.edit { preferences ->

            preferences[PicoKeys.HAND_CONTROLLERS] =
                config.handAndControllers

            preferences[PicoKeys.WIFI_SSID] =
                config.wifiSSID

            preferences[PicoKeys.WIFI_PASSWORD] =
                config.wifiPassword

            preferences[PicoKeys.KEEP_WIFI_ON] =
                config.keepWifiOn

            preferences[PicoKeys.WIRELESS_DEBUG] =
                config.wirelessDebug

            preferences[PicoKeys.USB_DEBUG] =
                config.usbDebug

            preferences[PicoKeys.HOME_GESTURE] =
                config.homeGesture

            preferences[PicoKeys.CALIBRATE_GESTURE] =
                config.calibrateGesture

            preferences[PicoKeys.MODE_LBE] =
                config.modeLBE

            preferences[PicoKeys.LONG_HOME_PRESS_BUTTON] =
                config.longHomePressButton

            preferences[PicoKeys.TEMPORARY_PLAY_BOUNDARY] =
                config.temporaryPlayBoundary

            preferences[PicoKeys.BOUNDARY_CONFIRMATION_POPUP] =
                config.boundaryConfirmationPopup

            preferences[PicoKeys.USE_MARKERS_FIRST_POSITION_RECOVERY] =
                config.useMarkersFirstPositionRecovery

            preferences[PicoKeys.DISTANCE_SENSITIVITY_PLAY_BOUNDARY] =
                config.distanceSensitivityPlayBoundary

            preferences[PicoKeys.COLOR_FENCE_R] =
                config.colorFence.first

            preferences[PicoKeys.COLOR_FENCE_G] =
                config.colorFence.second

            preferences[PicoKeys.COLOR_FENCE_B] =
                config.colorFence.third
        }
    }
    suspend fun getPicoConfig(
    ): PicoConfigurationJsonData {

        val preferences =
            context.picoDataStore.data.firstOrNull()

        return PicoConfigurationJsonData(
            wifiSSID =
                preferences?.get(PicoKeys.WIFI_SSID) ?: "NO",

            wifiPassword =
                preferences?.get(PicoKeys.WIFI_PASSWORD) ?: "",

            keepWifiOn =
                preferences?.get(PicoKeys.KEEP_WIFI_ON) ?: false,

            wirelessDebug =
                preferences?.get(PicoKeys.WIRELESS_DEBUG) ?: false,

            usbDebug =
                preferences?.get(PicoKeys.USB_DEBUG) ?: false,

            homeGesture =
                preferences?.get(PicoKeys.HOME_GESTURE) ?: false,

            handAndControllers =
                preferences?.get(PicoKeys.HAND_CONTROLLERS) ?: false,

            calibrateGesture =
                preferences?.get(PicoKeys.CALIBRATE_GESTURE) ?: false,

            modeLBE =
                preferences?.get(PicoKeys.MODE_LBE) ?: false,

            longHomePressButton =
                preferences?.get(PicoKeys.LONG_HOME_PRESS_BUTTON) ?: false,

            temporaryPlayBoundary =
                preferences?.get(PicoKeys.TEMPORARY_PLAY_BOUNDARY) ?: false,

            boundaryConfirmationPopup =
                preferences?.get(PicoKeys.BOUNDARY_CONFIRMATION_POPUP) ?: false,

            useMarkersFirstPositionRecovery =
                preferences?.get(PicoKeys.USE_MARKERS_FIRST_POSITION_RECOVERY)
                    ?: false,

            distanceSensitivityPlayBoundary =
                preferences?.get(PicoKeys.DISTANCE_SENSITIVITY_PLAY_BOUNDARY)
                    ?: 0,

            colorFence = Triple(
                preferences?.get(PicoKeys.COLOR_FENCE_R) ?: 0,
                preferences?.get(PicoKeys.COLOR_FENCE_G) ?: 0,
                preferences?.get(PicoKeys.COLOR_FENCE_B) ?: 0
            )
        )
    }
}