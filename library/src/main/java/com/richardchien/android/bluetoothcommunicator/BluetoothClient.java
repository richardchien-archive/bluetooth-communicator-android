package com.richardchien.android.bluetoothcommunicator;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.richardchien.android.bluetoothcommunicator.listener.OnReceiveListener;
import com.richardchien.android.bluetoothcommunicator.listener.OnConnectionResultListener;
import com.richardchien.android.bluetoothcommunicator.listener.OnLoseConnectionListener;
import com.richardchien.android.bluetoothcommunicator.listener.OnNewDeviceFoundListener;

import java.util.Set;
import java.util.UUID;

/**
 * BluetoothCommunicator
 * Created by richard on 16/1/11.
 */
public class BluetoothClient extends BluetoothCommunicator {
    private BroadcastReceiver mDiscoveryBroadcastReceiver;

    /**
     * BluetoothClient constructor without listeners
     *
     * @param handler Handler on UI thread
     */
    public BluetoothClient(Handler handler) {
        super(handler);
    }

    /**
     * BluetoothClient constructor with listeners
     *
     * @param handler                  Handler on UI thread
     * @param onReceiveListener        Listener for receiving message
     * @param onLoseConnectionListener Listener for losing connection
     */
    public BluetoothClient(Handler handler, OnReceiveListener onReceiveListener, OnLoseConnectionListener onLoseConnectionListener) {
        super(handler, onReceiveListener, onLoseConnectionListener);
    }

    /**
     * Get all paired devices
     *
     * @return Set of paired devices
     */
    public Set<BluetoothDevice> getPairedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    /**
     * Start discovery of new Bluetooth devices
     *
     * @param context  Context
     * @param listener Listener
     */
    public void startDiscovery(Context context, final OnNewDeviceFoundListener listener) {
        mBluetoothAdapter.startDiscovery();

        mDiscoveryBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                listener.onNewDeviceFound(device);
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(mDiscoveryBroadcastReceiver, intentFilter);
    }

    /**
     * Cancel discovery of new Bluetooth devices
     *
     * @param context Context
     */
    public void cancelDiscovery(Context context) {
        mBluetoothAdapter.cancelDiscovery();

        if (mDiscoveryBroadcastReceiver != null) {
            context.unregisterReceiver(mDiscoveryBroadcastReceiver);
            mDiscoveryBroadcastReceiver = null;
        }
    }

    /**
     * Connect to device
     *
     * @param device   Device to connect
     * @param uuid     The app's UUID string, should be the same as the server side
     * @param listener Listener
     */
    public void connectToDevice(BluetoothDevice device, UUID uuid, OnConnectionResultListener listener) {
        if (mConnections.containsKey(device)) {
            listener.onConnectionSucceeded(device);
            return;
        }

        if (mConnections.size() > 0) {
            listener.onConnectionFailed(device);
            return;
        }

        new ConnectThread(device, uuid, listener).start();
    }

    /**
     * Thread to make connection
     */
    private class ConnectThread extends Thread {
        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;
        private OnConnectionResultListener mmListener;

        public ConnectThread(BluetoothDevice device, UUID uuid, OnConnectionResultListener listener) {
            mmDevice = device;
            mmListener = listener;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (Exception e) {
                cancel();
                connectionFailed(mmDevice);
            }
            mmSocket = tmp;
        }

        @Override
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                startNewCommunicateThread(mmSocket);
                connectionSucceeded(mmDevice);
            } catch (Exception e) {
                cancel();
                connectionFailed(mmDevice);
            }
        }

        /**
         * Cancel the thread
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (Exception ignored) {
            }
        }

        /**
         * Call onConnectionSucceeded method on UI thread
         *
         * @param device Device to connect
         */
        private void connectionSucceeded(final BluetoothDevice device) {
            if (mmListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mmListener.onConnectionSucceeded(device);
                    }
                });
            }
        }

        /**
         * Call onConnectionFailed method on UI thread
         *
         * @param device Device to connect
         */
        private void connectionFailed(final BluetoothDevice device) {
            if (mmListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mmListener.onConnectionFailed(device);
                    }
                });
            }
        }
    }
}