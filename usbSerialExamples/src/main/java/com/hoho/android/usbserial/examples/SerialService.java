package com.hoho.android.usbserial.examples;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.events.SerialErrorEvent;
import com.hoho.android.usbserial.examples.BuildConfig;
import com.hoho.android.usbserial.examples.events.BulkSerialDeviceDiscoveryEvent;
import com.hoho.android.usbserial.examples.events.SerialConnectionEvent;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SerialService extends Service {
    public SerialService() {
    }

    private final LocalBinder mBinder = new LocalBinder();
    private Handler mBackgroundHandler;


    private ArrayMap<UsbDevice, UsbSerialPort> mSerialPortCache = new ArrayMap<>();
    private ArrayMap<UsbDevice, SerialInputOutputManager> mSerialManagerCache = new ArrayMap<>();


    //region Lifecycle Methods
    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("SerialService Thread");
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        //TODO: Kill All Connections
    }

    //endregion


    public UsbSerialPort getSerialPort(@NonNull UsbDevice device) {
        return mSerialPortCache.get(device);
    }


    /**
     * Closes USB Connection. <BR />
     * Terminates the Serial IO Manager <BR />
     *
     * @param device - the usb device to remove
     */
    public void closeUsbConnection(@NonNull UsbDevice device) {
        UsbSerialPort port = mSerialPortCache.get(device);
        if (port != null) {
            try {
                port.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        SerialInputOutputManager manager = mSerialManagerCache.get(device);
        if (manager != null) {
            manager.stop();
            mSerialManagerCache.remove(device);
        }
    }


    public void openUsbConnection(@NonNull UsbDevice device, int baudrate, int databits, int stopBits, int parityBits) {
        UsbSerialPort sPort = mSerialPortCache.get(device);


        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
        if (connection == null) {
            //99% of the time this is because permissions were not granted for this specific usb device.
            EventBus.getDefault().post(new SerialConnectionEvent(device, "Failed to Open Communication.", false));
            return;
        }

        try {
            sPort.open(connection);
            sPort.setParameters(baudrate, databits, stopBits, parityBits);

        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            EventBus.getDefault().post(new SerialConnectionEvent(device, "Error opening device: " + device.getDeviceName(), false));
            try {
                sPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            return;
        }


        SerialInputOutputManager manager = new SerialInputOutputManager(sPort);
        mSerialManagerCache.put(device, manager);
        EventBus.getDefault().post(new SerialConnectionEvent(device, sPort, "Serial Device Connected"));
    }


    public void discoverSerialDevices() {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(BuildConfig.TAG, "Refreshing device list ...");
                //This is in here because there is a 5 second refresh timeout in DeviceListFragment.
                //From original example
                SystemClock.sleep(1000);

                UsbManager usbManager = (UsbManager) getSystemService(USB_SERVICE);
                final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

                final List<UsbDevice> result = new ArrayList<>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    Log.d(TAG, String.format("+ %s: %s port%s", driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                    for (UsbSerialPort port : ports) {
                        UsbDevice usbDevice = port.getDriver().getDevice();
                        if (!mSerialManagerCache.containsKey(usbDevice)) {
                            mSerialPortCache.put(usbDevice, port);
                        }

                        result.add(usbDevice);
                    }
                }

                EventBus.getDefault().post(new BulkSerialDeviceDiscoveryEvent(result));
            }
        });
    }

    public void write(UsbDevice usbDevice, String text) {
        mSerialManagerCache.get(usbDevice).writeAsync(text.getBytes(Charset.forName("UTF-8")));
    }
//    public void monitorSerialPort(@NonNull UsbSerialPort sPort) {
//        Log.i(BuildConfig.TAG, "Starting io manager ..");
//
//        //Create the serial io manager.
//        mSerialManager = new SerialInputOutputManager(sPort);
//
//        //Start listening on a background thread. Using the single threaded handler.
//        mBackgroundHandler.post(mSerialManager);
//    }
//    public void stopMonitoringSerialPort(boolean closePort){
//        if(mSerialManager != null){
//            Log.i(BuildConfig.TAG, "Stopping io manager ..");
//
//            //Stop the serial io manager.
//            mSerialManager.stop();
//
//            //Release the reference.
//            mSerialManager = null;
//        }
//
//        if(closePort && mSerialPort != null){
//            try {
//                mSerialPort.close();
//                mSerialPort = null;
//            }catch (IOException ex){
//                Log.e(BuildConfig.TAG, "Error Closing Port", ex);
//            }
//        }
//    }

    public class LocalBinder extends Binder {
        public SerialService getService() {
            return SerialService.this;
        }
    }
}
