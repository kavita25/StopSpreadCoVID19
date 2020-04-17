package com.example.covid;

import android.bluetooth.BluetoothDevice;

import java.util.Set;

public interface OnListItemCLick {
    void onItemSelected(Set<BluetoothDevice> devices);
}
