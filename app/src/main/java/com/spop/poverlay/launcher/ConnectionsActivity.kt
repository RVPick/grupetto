package com.spop.poverlay.launcher

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spop.poverlay.ConfigurationRepository
import com.spop.poverlay.ConfigurationViewModel
import com.spop.poverlay.releases.ReleaseChecker
import com.spop.poverlay.sensor.heartrate.HeartRateManager

/**
 * Pelo-style replacement for Grupetto's settings screen: headphones, heart rate and
 * broadcasting (overlay options live on the Overlay page). Reuses Grupetto's ConfigurationViewModel so the
 * underlying behavior (and upstream merges) stay the same.
 */
class ConnectionsActivity : ComponentActivity() {
    private lateinit var viewModel: ConfigurationViewModel

    private var audioDevices by mutableStateOf<List<AudioDevice>>(emptyList())
    private var a2dp: BluetoothProfile? = null

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            viewModel.onBluetoothPermissionsResult(results.values.all { it })
        }

    // Refresh the headphones list when anything connects, disconnects or pairs.
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshAudioDevices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ConfigurationViewModel(
            application,
            ConfigurationRepository(applicationContext, this),
            ReleaseChecker()
        )
        viewModel.requestBluetoothPermissions.observe(this) { requestBluetoothPermissions.launch(it) }
        viewModel.infoPopup.observe(this) { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }

        BluetoothAdapter.getDefaultAdapter()?.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                a2dp = proxy
                refreshAudioDevices()
            }

            override fun onServiceDisconnected(profile: Int) {
                a2dp = null
            }
        }, BluetoothProfile.A2DP)

        setContent {
            val connected by HeartRateManager.connectedDevice.collectAsState()
            val heartRate by HeartRateManager.heartRate.collectAsState()
            val saved by HeartRateManager.savedDevices.collectAsState()
            val discovered by HeartRateManager.discoveredDevices.collectAsState()
            val scanning by HeartRateManager.isScanning.collectAsState()
            val matchByName by HeartRateManager.matchByName.collectAsState()
            val zones = listOf(
                HeartRateManager.zone12.collectAsState().value,
                HeartRateManager.zone23.collectAsState().value,
                HeartRateManager.zone34.collectAsState().value,
                HeartRateManager.zone45.collectAsState().value,
            )
            val bleEnabled by viewModel.bleTxEnabled.collectAsState()
            val dirConEnabled by viewModel.dirConEnabled.collectAsState()
            val broadcastName by viewModel.bleFtmsDeviceName.collectAsState()

            ConnectionsScreen(
                state = ConnectionsState(
                    audioDevices = audioDevices,
                    heartRateDevice = connected,
                    heartRate = heartRate,
                    savedHeartRateDevices = saved,
                    discoveredHeartRateDevices = discovered.filter { d -> saved.none { it.address == d.address } },
                    scanning = scanning,
                    matchByName = matchByName,
                    zones = zones,
                    bleBroadcast = bleEnabled,
                    wifiBroadcast = dirConEnabled,
                    broadcastName = broadcastName,
                ),
                actions = ConnectionsActions(
                    onClose = ::finish,
                    onOpenBluetoothSettings = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    onConnectHeartRate = viewModel::connectHeartRateDevice,
                    onDisconnectHeartRate = viewModel::disconnectHeartRateDevice,
                    onForgetHeartRate = viewModel::forgetHeartRateDevice,
                    onScan = { if (scanning) viewModel.stopHeartRateDiscovery() else viewModel.startHeartRateDiscovery() },
                    onMatchByName = viewModel::setHrMatchByName,
                    onSaveZones = { z -> HeartRateManager.setHeartRateZones(z[0], z[1], z[2], z[3]) },
                    onBleBroadcast = viewModel::onBleTxEnabledClicked,
                    onWifiBroadcast = viewModel::onDirConEnabledClicked,
                ),
            )
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(bluetoothReceiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        })
        refreshAudioDevices()
        // Same as Grupetto's heart rate dialog: search and watch the connection while this page is
        // open. If Grupetto's auto-reconnect search is already running, it ends that search on its
        // own schedule; the Search button starts a new one.
        viewModel.startHeartRateDiscovery()
        HeartRateManager.setManaging(true)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothReceiver)
        viewModel.stopHeartRateDiscovery()
        HeartRateManager.setManaging(false)
    }

    override fun onDestroy() {
        a2dp?.let { BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        super.onDestroy()
    }

    /** Paired headphones and speakers, with whichever one is playing audio marked connected. */
    private fun refreshAudioDevices() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val connected = a2dp?.connectedDevices.orEmpty().map { it.address }.toSet()
        audioDevices = try {
            adapter.bondedDevices
                .filter { it.bluetoothClass?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO }
                .map { AudioDevice(it.name ?: it.address, it.address in connected) }
                .sortedWith(compareByDescending<AudioDevice> { it.connected }.thenBy { it.name.lowercase() })
        } catch (e: SecurityException) {
            emptyList()
        }
    }
}
