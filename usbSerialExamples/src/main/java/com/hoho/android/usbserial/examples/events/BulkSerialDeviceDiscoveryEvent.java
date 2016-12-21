package com.hoho.android.usbserial.examples.events;

import android.hardware.usb.UsbDevice;

import com.hoho.android.usbserial.events.BusEvent;

import java.util.List;

/**
 * Created by coreymann on 12/21/16.
 */

public class BulkSerialDeviceDiscoveryEvent implements BusEvent {
    private final List<UsbDevice> devices;

    public BulkSerialDeviceDiscoveryEvent(List<UsbDevice> mDevices) {
        this.devices = mDevices;
    }

    public List<UsbDevice> getDevices() {
        return devices;
    }
}
