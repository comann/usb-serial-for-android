/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.examples;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.examples.events.SerialConnectionEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;


/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class MainActivity extends AppCompatActivity implements ServiceConnection{

    private final String TAG = MainActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;


    private SerialConsoleFragment mSerialConsoleFragment;
    private SerialService mService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);

        if(savedInstanceState == null || mService == null) {
            Intent intent = new Intent(this, SerialService.class);
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        }

        EventBus.getDefault().register(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if(getSupportFragmentManager().getBackStackEntryCount() > 1){
            getSupportFragmentManager().popBackStackImmediate();
        }else{
            super.onBackPressed();
        }
    }


    //region Service Connection Implementation

    public SerialService getSerialService(){
        return mService;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder iBinder) {
        mService = ((SerialService.LocalBinder) iBinder).getService();

        //The app is now ready to be used.
        getSupportFragmentManager().beginTransaction()
                .add(R.id.status_frame, DeviceListFragment.newInstance())
                .commit();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(BuildConfig.TAG, "onServiceDisconnected: " + name );
    }

    //endregion


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SerialConnectionEvent event) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(event.getStatus());
        }
    }

    //region USB Permissions
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (device != null) {
                        mService.openUsbConnection(device, 115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    }
                }
            }
        }
    };

    public void onDeviceSelected(UsbDevice usbDevice) {
        if(mSerialConsoleFragment == null) {
            mSerialConsoleFragment = SerialConsoleFragment.newInstance(usbDevice);
        }else{
            mSerialConsoleFragment.getArguments().putParcelable("EXTRA_DEVICE", usbDevice);
        }


        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbManager.requestPermission(usbDevice, mPermissionIntent);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.status_frame, mSerialConsoleFragment)
                .commit();
    }

    //endregion
}
