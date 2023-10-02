/*
 * Copyright (C) 2023 ArrowOS
 * 
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arrow.dubaicameraservice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.qualcomm.qcrilmsgtunnel.IQcrilMsgTunnel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QcRilMsgUtils {

    private static final String TAG = "DubaiCameraService-QcRil";

    private static final String PACKAGE_NAME = "com.qualcomm.qcrilmsgtunnel";
    private static final String SERVICE_NAME = "com.qualcomm.qcrilmsgtunnel.QcrilMsgTunnelService";

    private static final int OEM_RIL_REQUEST_GET_BAND_PREF = 327723;
    private static final int OEM_RIL_REQUEST_SET_BAND_PREF = 327724;
    private static final int BAND_CONFIG_LENGTH = 168;
    private static final int LTE_CONFIG_LENGTH = 4;
    private static final int NR_CONFIG_LENGTH = 8;

    private IQcrilMsgTunnel mService;
    private QcrilMsgTunnelConnection mServiceConnection;
    private Context mContext;

    public QcRilMsgUtils(Context context) {
        mContext = context;
        mServiceConnection = new QcrilMsgTunnelConnection();
    }

    protected void bindService() {
        dlog("bindService");
        if (!mContext.bindService(new Intent().setClassName(PACKAGE_NAME, SERVICE_NAME),
                mServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Failed to bind to QcrilMsgTunnelService!");
        }
    }

    protected void unbindService() {
        dlog("unbindService");
        mContext.unbindService(mServiceConnection);
        mService = null;
    }

    /* TODO: split this function */
    protected boolean setNrBandEnabled(int phoneId, int band, boolean enabled) {
        if (mService == null) {
            Log.e(TAG, "setNrSaBandEnabled: mService is null!");
            return false;
        }
        dlog("setNrSaBandEnabled: phoneId=" + phoneId + " band=" + band + " enabled=" + enabled);

        // get band config
        byte[] reqData = new byte[8];
        ByteBuffer reqBuf = ByteBuffer.wrap(reqData)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(OEM_RIL_REQUEST_GET_BAND_PREF)
                .putInt(0);
        byte[] resp = new byte[BAND_CONFIG_LENGTH];
        try {
            int ret = mService.sendOemRilRequestRaw(reqData, resp, phoneId);
            if (ret < 0)
                throw new Exception();
        } catch (Exception e) {
            Log.e(TAG, "sendOemRilRequestRaw failed to get band config!", e);
            return false;
        }
        ByteBuffer buf = ByteBuffer.wrap(resp)
                .order(ByteOrder.nativeOrder());
        long nasConfig = buf.getLong();
        long[] lteConfigs = new long[LTE_CONFIG_LENGTH];
        for (int i = 0; i < LTE_CONFIG_LENGTH; i++) {
            lteConfigs[i] = buf.getLong();
        }
        long[] nrSaConfigs = new long[NR_CONFIG_LENGTH];
        for (int i = 0; i < NR_CONFIG_LENGTH; i++) {
            nrSaConfigs[i] = buf.getLong();
        }
        long[] nrNsaConfigs = new long[NR_CONFIG_LENGTH];
        for (int i = 0; i < NR_CONFIG_LENGTH; i++) {
            nrNsaConfigs[i] = buf.getLong();
        }

        // modify band config
        int row = (band - 1) / 64;
        int col = (band - 1) % 64;
        if (enabled) {
            nrSaConfigs[row] |= (1 << col);
            nrNsaConfigs[row] |= (1 << col);
        } else {
            nrSaConfigs[row] &= ~(1 << col);
            nrNsaConfigs[row] &= ~(1 << col);
        }

        // set band config
        byte[] newData = new byte[BAND_CONFIG_LENGTH + 8];
        ByteBuffer newBuf = ByteBuffer.wrap(newData)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(OEM_RIL_REQUEST_SET_BAND_PREF)
                .putInt(BAND_CONFIG_LENGTH)
                .order(ByteOrder.nativeOrder())
                .putLong(nasConfig);
        for (int i = 0; i < LTE_CONFIG_LENGTH; i++) {
            newBuf.putLong(lteConfigs[i]);
        }
        for (int i = 0; i < NR_CONFIG_LENGTH; i++) {
            newBuf.putLong(nrSaConfigs[i]);
        }
        for (int i = 0; i < NR_CONFIG_LENGTH; i++) {
            newBuf.putLong(nrNsaConfigs[i]);
        }
        try {
            int ret = mService.sendOemRilRequestRaw(newData, new byte[1], phoneId);
            if (ret < 0)
                throw new Exception();
        } catch (Exception e) {
            Log.e(TAG, "sendOemRilRequestRaw failed to set band config!", e);
            return false;
        }

        return true;
    }

    private class QcrilMsgTunnelConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IQcrilMsgTunnel.Stub.asInterface(service);
            if (mService == null) {
                Log.e(TAG, "Unable to get IQcrilMsgTunnel!");
                return;
            }
            try {
                service.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Log.e(TAG, "QcrilMsgTunnel service died, trying to bind again");
                        mService = null;
                        QcRilMsgUtils.this.bindService();
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.e(TAG, "linkToDeath failed", e);
            }
            Log.i(TAG, "QcrilMsgTunnel service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "QcrilMsgTunnel service disconnected");
            mService = null;
        }
    }

    private static void dlog(String msg) {
        DubaiCameraService.dlog(msg);
    }
}
