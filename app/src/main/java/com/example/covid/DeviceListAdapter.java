package com.example.covid;


import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.MyViewHolder> {

    private ArrayList<BluetoothDevice> mDevices;
    private Set<BluetoothDevice> mSelectedList = new HashSet<>();
    OnListItemCLick mListItemClick;

    public DeviceListAdapter(ArrayList<BluetoothDevice> devices, OnListItemCLick listner) {
        this.mDevices = devices;
        mListItemClick = listner;
    }

    public void setDevices(ArrayList<BluetoothDevice> devices) {
        mDevices = devices;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_adapter_view, parent, false);
        return new MyViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        BluetoothDevice device = mDevices.get(position);
        holder.bindData(device);

    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public Set<BluetoothDevice> getSelectedList() {
        return mSelectedList;
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        TextView mDeviceAdress;
        CheckBox mCheckBox;

        public MyViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.tvDeviceName);
            mDeviceAdress = (TextView) itemView.findViewById(R.id.tvDeviceAddress);
            mCheckBox = itemView.findViewById(R.id.checkbox);
        }

        void bindData(final BluetoothDevice bluetoothDevice) {
            //   final BluetoothDevice device = bluetoothDevice;
            if (bluetoothDevice.getName() != null && !bluetoothDevice.getName().isEmpty()) {
                mTitle.setText(bluetoothDevice.getName());
            } else {
                mTitle.setText(R.string.annonymous);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCheckBox.isChecked()) {
                        mSelectedList.remove(bluetoothDevice);
                        mCheckBox.setChecked(false);
                    } else {
                        mSelectedList.add(bluetoothDevice);
                        mCheckBox.setChecked(true);
                    }
                    mListItemClick.onItemSelected(mSelectedList);
                }
            });
        }
    }
}