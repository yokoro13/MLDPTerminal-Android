package com.i14yokoro.mldpterminal

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import java.util.ArrayList

/**
 * ViewとBleDeviceのAdapter
 */
internal class DeviceListAdapter(context: Context, private val layoutResourceId: Int) : ArrayAdapter<BleDevice>(context, layoutResourceId) {

    private val bleDevices: ArrayList<BleDevice> = ArrayList()
    private var device: BleDevice? = null

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

    override fun getView(position: Int, view: View?, parentView: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder
        device = bleDevices[position]

        if (convertView == null) {
            val inflater = (context as Activity).layoutInflater
            convertView = inflater.inflate(layoutResourceId, parentView, false)
            holder = ViewHolder()
            holder.textViewAddress = convertView!!.findViewById<View>(R.id.device_address) as TextView
            holder.textViewName = convertView.findViewById<View>(R.id.device_name) as TextView
            convertView.tag = holder

        } else {
            holder = convertView.tag as ViewHolder
        }
        holder.textViewAddress!!.text = device!!.address
        holder.textViewName!!.text = device!!.name
        return convertView
    }
}
