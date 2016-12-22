package com.hoho.android.usbserial.examples.events;

import android.hardware.usb.UsbDevice;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.events.BusEvent;

/**
 * Created by coreymann on 12/21/16.
 */

public class SerialConnectionEvent implements BusEvent {

    private final UsbSerialPort port;
    private final UsbDevice device;
    private final String status;
    private final boolean successfullyOpened;

    public SerialConnectionEvent(UsbDevice device, String status, boolean successful) {
        this.device = device;
        this.status = status;
        this.successfullyOpened = successful;
        this.port = null;
    }

    public SerialConnectionEvent(UsbDevice device, UsbSerialPort port, String status) {
        this.device = device;
        this.status = status;
        this.successfullyOpened = true;
        this.port = port;
    }

    public boolean isSuccessfullyOpened() {
        return successfullyOpened;
    }

    public String getStatus() {
        return status;
    }

    public UsbDevice getDevice() {
        return device;
    }

    public UsbSerialPort getPort() {
        return port;
    }
}
