package com.i14yokoro.mldpterminal

import android.app.Activity
import android.content.Context
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

    override fun getView(position: Int, convertView: View?, parentView: ViewGroup): View {
        var  view = convertView
        val holder: ViewHolder
        device = bleDevices[position]

        if (view == null) {
            val inflater = (context as Activity).layoutInflater
            view = inflater.inflate(layoutResourceId, parentView, false)
            holder = ViewHolder()
            holder.textViewAddress = view!!.findViewById<View>(R.id.device_address) as TextView
            holder.textViewName = view.findViewById<View>(R.id.device_name) as TextView
            view.tag = holder

        } else {
            holder = view.tag as ViewHolder
        }
        holder.textViewAddress!!.text = device!!.address
        holder.textViewName!!.text = device!!.name
        return view
    }
}
