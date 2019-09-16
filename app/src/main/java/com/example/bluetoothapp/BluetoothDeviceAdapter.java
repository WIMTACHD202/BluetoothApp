package com.example.bluetoothapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class BluetoothDeviceAdapter extends BaseAdapter {
    private List<BluetoothDevice> list;
    Context context;
    LayoutInflater inflater;

    public BluetoothDeviceAdapter(List<BluetoothDevice> list, Context context) {

        this.list = list;
        this.context = context;
        inflater = inflater.from(context);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.listviewfordevices,null);
        TextView deviceName = view.findViewById(R.id.deviceName);
        deviceName.setText(list.get(position).getName());
        return view;
    }
}
