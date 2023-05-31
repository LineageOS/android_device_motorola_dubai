/*
 * Copyright (C) 2023 ArrowOS
 * 
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arrow.dubaicameraservice;

import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;
import static android.telephony.TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER;
import static android.telephony.TelephonyManager.NETWORK_TYPE_BITMASK_NR;

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

import java.util.concurrent.Executor;

public class DubaiCameraService extends Service {

    private static final boolean DEBUG = false;
    private static final String TAG = "DubaiCameraService";

    private static final String FRONT_CAMERA_ID = "1";

    private CameraManager mCameraManager;
    private SubscriptionManager mSubManager;
    private TelephonyManager mTelephonyManager;

    private boolean mIsFrontCamInUse = false;
    private int mSubId = INVALID_SUBSCRIPTION_ID;

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
            update5gState();
        }
    };

    private class ActiveDataSubIdCallback extends TelephonyCallback implements
            TelephonyCallback.ActiveDataSubscriptionIdListener {
        @Override
        public void onActiveDataSubscriptionIdChanged(int subId) {
            dlog("onActiveDataSubscriptionIdChanged subId:" + subId);
            mSubId = subId;
            update5gState();
        }
    };

    private final TelephonyCallback mTelephonyCallback = new ActiveDataSubIdCallback();

    @Override
    public void onCreate() {
        dlog("onCreate");
        mCameraManager = getSystemService(CameraManager.class);
        mSubManager = getSystemService(SubscriptionManager.class);
        mTelephonyManager = getSystemService(TelephonyManager.class);

        mCameraManager.registerAvailabilityCallback(mCameraCallback, mHandler);
        mTelephonyManager.registerTelephonyCallback(mExecutor, mTelephonyCallback);
        mSubManager.addOnSubscriptionsChangedListener(mExecutor, mSubListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dlog("onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        dlog("onDestroy");
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
        if (mSubId == INVALID_SUBSCRIPTION_ID
                || mSubManager.getActiveSubscriptionIdList().length <= 0) {
            dlog("update5gState: Invalid subid or no active subs!");
            return;
        }
        final TelephonyManager tm = mTelephonyManager.createForSubscriptionId(mSubId);
        // Arguably we should use ALLOWED_NETWORK_TYPES_REASON_POWER here but that's already
        // used by battery saver, and we are out of other reasons
        long allowedNetworkTypes = tm.getAllowedNetworkTypesForReason(
                ALLOWED_NETWORK_TYPES_REASON_CARRIER);
        final boolean is5gAllowed = (allowedNetworkTypes & NETWORK_TYPE_BITMASK_NR) != 0;
        dlog("update5gState mIsFrontCamInUse:" + mIsFrontCamInUse + " is5gAllowed:" + is5gAllowed);
        if (mIsFrontCamInUse && is5gAllowed) {
            allowedNetworkTypes &= ~NETWORK_TYPE_BITMASK_NR;
        } else if (!mIsFrontCamInUse && !is5gAllowed) {
            allowedNetworkTypes |= NETWORK_TYPE_BITMASK_NR;
        } else {
            return;
        }
        tm.setAllowedNetworkTypesForReason(ALLOWED_NETWORK_TYPES_REASON_CARRIER,
                allowedNetworkTypes);
    }

    private static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, msg);
    }
}
