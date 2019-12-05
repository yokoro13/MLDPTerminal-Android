package com.i14yokoro.mldpterminal.bluetooth

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import com.i14yokoro.mldpterminal.R
import com.i14yokoro.mldpterminal.bluetooth.BleDevice

import java.util.ArrayList

/**
 * ViewとBleDeviceのAdapter
 */
internal class DeviceListAdapter(ct: Context, private val layoutResourceId: Int) : ArrayAdapter<BleDevice>(ct, layoutResourceId) {

    private val bleDevices: ArrayList<BleDevice> = ArrayList()

    private inner class ViewHolder {
        internal var textViewAddress: TextView? = null
        internal var textViewName: TextView? = null
    }

    fun addDevice(device: BleDevice) {
        if (!bleDevices.contains(device)) {
            bleDevices.add(device)
        }
    }

    fun getDevice(position: Int): BleDevice {
        return bleDevices[position]
    }

    override fun clear() {
        bleDevices.clear()
    }

    override fun getCount(): Int {
        return bleDevices.size
    }

    override fun getItem(i: Int): BleDevice? {
        return bleDevices[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(position: Int, convertView: View?, parentView: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder
        val device = bleDevices[position]

        if (convertView == null) {
            val inflater = (context as Activity).layoutInflater
            convertView = inflater.inflate(layoutResourceId, parentView, false)
            holder = ViewHolder()
            holder.textViewAddress = convertView!!.findViewById(R.id.device_address)
            holder.textViewName = convertView.findViewById(R.id.device_name)
            convertView.tag = holder

        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.textViewAddress!!.text = device.address
        holder.textViewName!!.text = device.name
        return convertView
    }
}
