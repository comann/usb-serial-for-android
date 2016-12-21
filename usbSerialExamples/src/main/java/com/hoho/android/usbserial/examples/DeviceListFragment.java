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

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.examples.events.BulkSerialDeviceDiscoveryEvent;
import com.hoho.android.usbserial.util.HexDump;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a {@link ListView} of available USB devices.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class DeviceListFragment extends Fragment {

    private final String TAG = DeviceListFragment.class.getSimpleName();

    //region Instance Creations
    public static Fragment newInstance() {
        return new DeviceListFragment();
    }
    //endregion

    private ListView mListView;
    private TextView mProgressBarTitle;
    private ProgressBar mProgressBar;

    private static final int MESSAGE_REFRESH = 101;
    private static final long REFRESH_TIMEOUT_MILLIS = 5000;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_REFRESH:
                    getService().discoverSerialDevices();
                    mHandler.sendEmptyMessageDelayed(MESSAGE_REFRESH, REFRESH_TIMEOUT_MILLIS);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    private List<UsbDevice> mEntries = new ArrayList<>();
    private ArrayAdapter<UsbDevice> mAdapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        mListView = (ListView) view.findViewById(R.id.deviceList);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBarTitle = (TextView) view.findViewById(R.id.progressBarTitle);

        mAdapter = new ArrayAdapter<UsbDevice>(getContext(), android.R.layout.simple_expandable_list_item_2, mEntries) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TwoLineListItem row;
                if (convertView == null) {
                    row = (TwoLineListItem) LayoutInflater.from(getActivity())
                            .inflate(android.R.layout.simple_list_item_2, null);
                } else {
                    row = (TwoLineListItem) convertView;
                }

                final UsbSerialPort port = getService().getSerialPort(mEntries.get(position));
                final UsbSerialDriver driver = port.getDriver();
                final UsbDevice device = driver.getDevice();

                final String title = String.format("Vendor %s Product %s",
                        HexDump.toHexString((short) device.getVendorId()),
                        HexDump.toHexString((short) device.getProductId()));
                row.getText1().setText(title);

                final String subtitle = driver.getClass().getSimpleName();
                row.getText2().setText(subtitle);

                return row;
            }

        };
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Pressed item " + position);
                if (position >= mEntries.size()) {
                    Log.w(TAG, "Illegal position.");
                    return;
                }

                ((MainActivity) getActivity()).onDeviceSelected(mEntries.get(position));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        mHandler.sendEmptyMessage(MESSAGE_REFRESH);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        mHandler.removeMessages(MESSAGE_REFRESH);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BulkSerialDeviceDiscoveryEvent evt) {
        mEntries.clear();
        mEntries.addAll(evt.getDevices());
        mAdapter.notifyDataSetChanged();
        mProgressBarTitle.setText(String.format("%s device(s) found", Integer.valueOf(mEntries.size())));
        hideProgressBar();
        Log.d(TAG, "Done refreshing, " + mEntries.size() + " entries found.");

    }


    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBarTitle.setText(R.string.refreshing);
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }


    private SerialService getService(){ return ((MainActivity) getActivity()).getSerialService(); }


}
