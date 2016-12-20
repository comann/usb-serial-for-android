package com.hoho.android.usbserial.events;

import com.hoho.android.usbserial.util.SerialInputOutputManager;

/**
 * Created by coreymann on 12/20/16.
 */

public class ReadDataEvent implements BusEvent {
    private final SerialInputOutputManager serialManager;
    private final byte[] data;

    public ReadDataEvent(SerialInputOutputManager serialInputOutputManager, byte[] data) {
        this.data = data;
        this.serialManager = serialInputOutputManager;
    }

    public byte[] getData() {
        return data;
    }

    public SerialInputOutputManager getSerialManager() {
        return serialManager;
    }
}
