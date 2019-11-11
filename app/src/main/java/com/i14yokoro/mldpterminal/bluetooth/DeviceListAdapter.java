package com.i14yokoro.mldpterminal.bluetooth;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.i14yokoro.mldpterminal.R;
import com.i14yokoro.mldpterminal.bluetooth.BleDevice;

import java.util.ArrayList;

/**
 * ViewとBleDeviceのAdapter
 */
class DeviceListAdapter extends ArrayAdapter<BleDevice> {

    private final ArrayList<BleDevice> bleDevices;
    private final int layoutResourceId;
    private final Context context;

    private class ViewHolder{
        TextView textViewAddress;
        TextView textViewName;
    }

    public DeviceListAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        bleDevices = new ArrayList<>();
    }

    public void addDevice(BleDevice device) {
        if(!bleDevices.contains(device)) {
            bleDevices.add(device);
        }
    }

    public BleDevice getDevice(int position) {
        return bleDevices.get(position);
    }

    public void clear() {
        bleDevices.clear();
    }

    @Override
    public int getCount() {
        return bleDevices.size();
    }

    @Override
    public BleDevice getItem(int i) {
        return bleDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parentView) {
        ViewHolder holder;
        BleDevice device = bleDevices.get(position);

        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parentView, false);
            holder = new ViewHolder();
            holder.textViewAddress = convertView.findViewById(R.id.device_address);
            holder.textViewName = convertView.findViewById(R.id.device_name);
            convertView.setTag(holder);

        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textViewAddress.setText(device.getAddress());
        holder.textViewName.setText(device.getName());
        return convertView;
    }
}
