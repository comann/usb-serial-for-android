package com.hoho.android.usbserial.examples;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.events.ReadDataEvent;
import com.hoho.android.usbserial.events.SerialErrorEvent;
import com.hoho.android.usbserial.examples.events.SerialConnectionEvent;
import com.hoho.android.usbserial.util.HexDump;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

/**
 * Created by coreymann on 12/21/16.
 */

public class SerialConsoleFragment extends Fragment {

    public static SerialConsoleFragment newInstance(@NonNull  UsbDevice device){
        SerialConsoleFragment fragment = new SerialConsoleFragment();
        fragment.setArguments(new Bundle());
        fragment.getArguments().putParcelable("EXTRA_DEVICE", device);
        return fragment;
    }

    private TextView mDumpTextView;
    private NestedScrollView mScrollView;
    private CheckBox chkDTR;
    private CheckBox chkRTS;

    private EditText editSendText;
    //region Lifecycle Methods

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        EventBus.getDefault().register(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_serial_console, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDumpTextView = (TextView) view.findViewById(R.id.consoleText);
        mScrollView = (NestedScrollView) view.findViewById(R.id.demoScroller);
        chkDTR = (CheckBox) view.findViewById(R.id.checkBoxDTR);
        chkRTS = (CheckBox) view.findViewById(R.id.checkBoxRTS);

        chkDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                   getService().getSerialPort(getUsbDevice()).setDTR(isChecked);
                }catch (IOException ignored){}
            }
        });

        chkRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    getService().getSerialPort(getUsbDevice()).setRTS(isChecked);
                }catch (IOException ignored){
                    Log.e(BuildConfig.TAG, "Error Opening Port", ignored);
                }
            }
        });

        this.editSendText = ((EditText) view.findViewById(R.id.edit_send));
        view.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = editSendText.getText().toString() + "\r\n";
                getService().write(getUsbDevice(), command);
            }
        });
    }


    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }

    //endregion


    //region Bus Methods
    void showStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SerialConnectionEvent event) {
        if(event.isSuccessfullyOpened()) {
            try {
                UsbSerialPort sPort = event.getPort();
                showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEvent(SerialErrorEvent evt){
        Log.d(BuildConfig.TAG, "Runner stopped.");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ReadDataEvent evt){
        final byte[] data = evt.getData();
        final String message = "RX: "  + new String(data);
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

    //endregion

    private SerialService getService(){
        return ((MainActivity) getActivity()).getSerialService();
    }
    public UsbDevice getUsbDevice() {
        return getArguments().getParcelable("EXTRA_DEVICE");
    }
}
