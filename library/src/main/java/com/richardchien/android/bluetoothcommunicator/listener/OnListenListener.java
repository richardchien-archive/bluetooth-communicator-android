package com.richardchien.android.bluetoothcommunicator.listener;

import android.bluetooth.BluetoothDevice;

/**
 * BluetoothCommunicator
 * Created by richard on 16/1/11.
 */
public interface OnListenListener {
    void onAccept(BluetoothDevice device);

    void onFail();
}
