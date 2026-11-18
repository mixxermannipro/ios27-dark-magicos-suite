package com.ios27dark.widgets.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PhoneBatteryState(
    val level: Int,
    val isCharging: Boolean
)

object BatteryHelper {

    fun getPhoneBattery(ctx: Context): PhoneBatteryState {
        val intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 100) ?: 100
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val pct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return PhoneBatteryState(pct, charging)
    }

    fun hasBluetoothPermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @Suppress("MissingPermission")
    fun getConnectedBluetoothDevices(ctx: Context): List<BtBatteryDevice> {
        if (!hasBluetoothPermission(ctx)) return PrefsRepository.getCachedBtDevices(ctx)

        val discovered = mutableListOf<BtBatteryDevice>()
        try {
            val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter: BluetoothAdapter = bm?.adapter ?: return PrefsRepository.getCachedBtDevices(ctx)
            if (!adapter.isEnabled) return emptyList()

            val bonded = adapter.bondedDevices ?: emptySet()
            for (device in bonded) {
                val battery = readDeviceBatteryLevel(device)
                val connected = isDeviceConnected(device)
                if (connected && battery in 0..100) {
                    val kind = classifyDevice(device)
                    discovered.add(
                        BtBatteryDevice(
                            name = device.name ?: "Bluetooth",
                            address = device.address ?: "",
                            level = battery,
                            kind = kind
                        )
                    )
                }
            }
            if (discovered.isNotEmpty()) {
                PrefsRepository.saveCachedBtDevices(ctx, discovered)
                return discovered
            }
        } catch (_: Exception) {
        }
        return PrefsRepository.getCachedBtDevices(ctx)
    }

    private fun readDeviceBatteryLevel(device: BluetoothDevice): Int {
        return try {
            val m = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            (m.invoke(device) as? Int) ?: -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val m = BluetoothDevice::class.java.getMethod("isConnected")
            (m.invoke(device) as? Boolean) ?: false
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("MissingPermission")
    private fun classifyDevice(device: BluetoothDevice): String {
        val major = device.bluetoothClass?.majorDeviceClass
        return when (major) {
            BluetoothClass.Device.Major.WEARABLE -> "watch"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "audio"
            else -> {
                val n = device.name?.lowercase() ?: ""
                when {
                    n.contains("watch") || n.contains("band") -> "watch"
                    else -> "audio"
                }
            }
        }
    }
}
