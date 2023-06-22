/*
 * Copyright (C) 2023 ArrowOS
 * 
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arrow.dubaicameraservice;

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Looper;
import android.os.UserHandle;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class DubaiCameraService extends Service {

    private static final boolean DEBUG = false;
    private static final String TAG = "DubaiCameraService";

    private static final String FRONT_CAMERA_ID = "1";
    private static final int OFFENDING_NR_SA_BAND = 78;

    private CameraManager mCameraManager;
    private SubscriptionManager mSubManager;
    private TelephonyManager mTelephonyManager;
    private QcRilMsgUtils mQcRilMsgUtils;

    private boolean mIsFrontCamInUse = false;
    private int[] mActiveSubIds = new int[0];
    private int mDefaultDataSubId = INVALID_SUBSCRIPTION_ID;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Executor mExecutor = new HandlerExecutor(mHandler);

    private final CameraManager.AvailabilityCallback mCameraCallback =
            new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            dlog("onCameraAvailable id:" + cameraId);
            if (cameraId.equals(FRONT_CAMERA_ID)) {
                mIsFrontCamInUse = false;
                update5gState();
            }
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            dlog("onCameraUnavailable id:" + cameraId);
            if (cameraId.equals(FRONT_CAMERA_ID)) {
                mIsFrontCamInUse = true;
                update5gState();
            }
        }
    };

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            dlog("onSubscriptionsChanged");
            final int[] subs = mSubManager.getActiveSubscriptionIdList();
            if (!Arrays.equals(subs, mActiveSubIds)) {
                dlog("active subs changed, was: " + Arrays.toString(mActiveSubIds)
                        + ", now: " + Arrays.toString(subs));
                mActiveSubIds = subs;
                update5gState();
            }
        }
    };

    private class ActiveDataSubIdCallback extends TelephonyCallback implements
            TelephonyCallback.ActiveDataSubscriptionIdListener {
        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            dlog("onActiveDataSubscriptionIdChanged subId:" + subId);
            if (subId != mDefaultDataSubId) {
                mDefaultDataSubId = subId;
                update5gState();
            }
        }
    };

    private final TelephonyCallback mTelephonyCallback = new ActiveDataSubIdCallback();

    @Override
    public void onCreate() {
        dlog("onCreate");
        mQcRilMsgUtils = new QcRilMsgUtils(this);
        mCameraManager = getSystemService(CameraManager.class);
        mSubManager = getSystemService(SubscriptionManager.class);
        mTelephonyManager = getSystemService(TelephonyManager.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dlog("onStartCommand");
        mQcRilMsgUtils.bindService();
        mCameraManager.registerAvailabilityCallback(mCameraCallback, mHandler);
        mTelephonyManager.registerTelephonyCallback(mExecutor, mTelephonyCallback);
        mSubManager.addOnSubscriptionsChangedListener(mExecutor, mSubListener);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        dlog("onDestroy");
        mQcRilMsgUtils.unbindService();
        mCameraManager.unregisterAvailabilityCallback(mCameraCallback);
        mTelephonyManager.unregisterTelephonyCallback(mTelephonyCallback);
        mSubManager.removeOnSubscriptionsChangedListener(mSubListener);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startService(Context context) {
        Log.i(TAG, "Starting service");
        context.startServiceAsUser(new Intent(context, DubaiCameraService.class),
                UserHandle.CURRENT);
    }

    private void update5gState() {
        if (mDefaultDataSubId == INVALID_SUBSCRIPTION_ID
                || mActiveSubIds.length == 0) {
            dlog("update5gState: Invalid subid or no active subs!");
            return;
        }
        if (mQcRilMsgUtils.setNrSaBandEnabled(mSubManager.getPhoneId(mDefaultDataSubId),
                OFFENDING_NR_SA_BAND, !mIsFrontCamInUse)) {
            Log.i(TAG, (mIsFrontCamInUse ? "Disabled" : "Enabled") + " NR SA band "
                    + OFFENDING_NR_SA_BAND + " for subId " + mDefaultDataSubId);
        } else {
            Log.e(TAG, "Failed to " + (mIsFrontCamInUse ? "disable" : "enable") + " NR SA band "
                    + OFFENDING_NR_SA_BAND + " for subId " + mDefaultDataSubId);
        }
    }

    protected static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
