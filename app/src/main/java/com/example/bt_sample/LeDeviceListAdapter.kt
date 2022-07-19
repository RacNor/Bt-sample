package com.example.bt_sample

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class LeDeviceListAdapter(context: Context) : BaseAdapter() {
    private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val mInflator: LayoutInflater

    init {
        mInflator = LayoutInflater.from(context)
    }

    fun addDevice(device: BluetoothDevice) {
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device)
        }
    }

    fun getDevice(position: Int): BluetoothDevice? {
        return mLeDevices[position]
    }

    fun clear() {
        mLeDevices.clear()
    }

    override fun getCount(): Int {
        return mLeDevices.size
    }

    override fun getItem(i: Int): Any {
        return mLeDevices[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View? {
        var view: View? = convertView
        val viewHolder: ViewHolder
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null)
            viewHolder = ViewHolder()
            viewHolder.deviceAddress = view.findViewById<View>(R.id.device_address) as TextView
            viewHolder.deviceName = view.findViewById<View>(R.id.device_name) as TextView
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }
        val device = mLeDevices[i]
        val deviceName = device.name
        if (deviceName != null && deviceName.isNotEmpty()) {
            viewHolder.deviceName?.text = deviceName
        } else {
            viewHolder.deviceName?.setText(
                R.string.unknown_device
            )
        }
        viewHolder.deviceAddress?.text = device.address
        return view
    }
}

internal class ViewHolder {
    var deviceName: TextView? = null
    var deviceAddress: TextView? = null
}