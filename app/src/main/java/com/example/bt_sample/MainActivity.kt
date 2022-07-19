package com.example.bt_sample

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bt_sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ENABLE_BT = 1
        private const val SCAN_PERIOD: Long = 10000
    }


    private lateinit var binding: ActivityMainBinding
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var mScanner: BluetoothLeScanner
    private lateinit var mLeDeviceListAdapter: LeDeviceListAdapter
    private lateinit var mHandler: Handler
    private var mScanning = false


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val isGranted = result.values.find { !it } == null
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }

    fun isBluetoothConnectGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isScanAndConnectPermissionsGranted()
        } else {
            isFineAndCoarseLocationPermissionsGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isScanAndConnectPermissionsGranted(): Boolean {
        val bluetoothScanPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        )
        val bluetoothConnectPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        return bluetoothScanPermission == PackageManager.PERMISSION_GRANTED// && bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun isFineAndCoarseLocationPermissionsGranted(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED || coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionsAndroidS()
        } else {
            requestPermissionsAndroidL()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestPermissionsAndroidS() {
        if (this.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_SCAN) || this.shouldShowRequestPermissionRationale(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) {
//
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    private fun requestPermissionsAndroidL() {
        if ((this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    || this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION))
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        ) {
//
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        mScanner = mBluetoothAdapter.bluetoothLeScanner

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.content.button.setOnClickListener {
            onButtonClick()
        }

        binding.content.list.onItemClickListener =
            AdapterView.OnItemClickListener { listView, view, position, id ->
                onListItemClick(listView, view, position, id)
            }

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        mLeDeviceListAdapter = LeDeviceListAdapter(this)
        val list = binding.content.list
        list.adapter = mLeDeviceListAdapter
        mHandler = Handler()

    }

    private fun onButtonClick() {
        if (mScanning) {
            scanLeDevice(false)
        } else {
            startScan()
        }
    }

    override fun onPause() {
        super.onPause()
        mLeDeviceListAdapter.clear()
        mLeDeviceListAdapter.notifyDataSetChanged()
        scanLeDevice(false)
    }

    private fun startScan() {
        if (!isBluetoothConnectGranted()) {
            askPermission()
            return
        }

        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(
                enableBtIntent,
                REQUEST_ENABLE_BT
            )
            return
        }
        scanLeDevice(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed({
                mScanning = false
                mScanner.stopScan(leScanCallback)
                updateUi()
            }, SCAN_PERIOD)
            mScanning = true
            mScanner.startScan(leScanCallback)
        } else {
            mScanning = false
            mScanner.stopScan(leScanCallback)
        }
        updateUi()
    }

    private fun updateUi() {
        if (mScanning) {
            binding.content.button.setText(R.string.stop_scan)
        } else {
            binding.content.button.setText(R.string.start_scan)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread {
                mLeDeviceListAdapter.addDevice(result.device)
                mLeDeviceListAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun onListItemClick(l: AdapterView<*>?, v: View?, position: Int, id: Long) {
        val device = mLeDeviceListAdapter.getDevice(position) ?: return
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        if (mScanning) {
            mScanner.stopScan(leScanCallback)
            mScanning = false
        }
        startActivity(intent)
    }

}