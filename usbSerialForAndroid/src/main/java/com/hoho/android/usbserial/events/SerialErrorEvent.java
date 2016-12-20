package com.hoho.android.usbserial.events;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

/**
 * Created by coreymann on 12/20/16.
 */

public class SerialErrorEvent implements BusEvent {
    private final Exception exception;
    private final UsbSerialPort driver;
    private final SerialInputOutputManager serialInputOutputManager;

    public SerialErrorEvent(SerialInputOutputManager serialInputOutputManager, UsbSerialPort mDriver, Exception e) {
        this.serialInputOutputManager = serialInputOutputManager;
        this.driver = mDriver;
        this.exception = e;
    }

    public UsbSerialPort getDriver() {
        return driver;
    }

    public Exception getException() {
        return exception;
    }

    public SerialInputOutputManager getSerialInputOutputManager() {
        return serialInputOutputManager;
    }
}
