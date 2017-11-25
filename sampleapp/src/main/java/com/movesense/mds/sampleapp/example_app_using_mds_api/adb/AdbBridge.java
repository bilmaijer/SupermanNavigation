package com.movesense.mds.sampleapp.example_app_using_mds_api.adb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.sampleapp.BleManager;
import com.movesense.mds.sampleapp.MdsRx;
import com.movesense.mds.sampleapp.RxBle;
import com.movesense.mds.sampleapp.example_app_using_mds_api.FormatHelper;
import com.movesense.mds.sampleapp.example_app_using_mds_api.dfu.DfuUtil;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseConnectedDevices;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseDevice;
import com.movesense.mds.sampleapp.model.MdsConnectedDevice;
import com.movesense.mds.sampleapp.model.MdsDeviceInfoNewSw;
import com.movesense.mds.sampleapp.model.MdsDeviceInfoOldSw;
import com.polidea.rxandroidble.RxBleScanResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class AdbBridge extends BroadcastReceiver {
    public static final String CONNECTED_WITH = "Connected with: ";

    // Connection example
    // We need to grant permission before we can scan BLE devices
    // adb shell pm grant com.movesense.mds.sampleapp android.permission.ACCESS_COARSE_LOCATION
    // adb shell am broadcast -a android.intent.action.MOVESENSE --es type connect --es address "macAddress"

    // Led
    // adb shell am broadcast -a android.intent.action.MOVESENSE --es type put --es path Component/Led --es value '''{\"isOn\":true}'''

    // Linear Acc Subscribe
    // adb shell am broadcast -a android.intent.action.MOVESENSE --es type subscribe --es path "Meas/Acc/26"
    // Linear Acc Unsubscribe
    // adb shell am broadcast -a android.intent.action.MOVESENSE --es type unsubscribe --es path Meas/Acc/26

    // Dfu Update
    // You must be connected before dfu update
    // adb shell am broadcast -a android.intent.action.MOVESENSE --es type dfu_update --es file_path "file_name_in_external_storage_folder"

    private final String LOG_TAG = AdbBridge.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";

    private final CompositeSubscription mCompositeSubscription = new CompositeSubscription();
    private final CompositeSubscription mScanningCompositeSubscription = new CompositeSubscription();

    private static Map<String, MdsSubscription> mSubscriptions = new HashMap<>();
    private String mDevice_name;
    private static String file_path;
    private boolean isConnecting = false;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(LOG_TAG, "onReceive()");
        if (intent.getExtras() == null) {
            Log.i(LOG_TAG, "No extras");
            return;
        }

        //Get Extras
        final String type;
        final String path;
        final String value;
        final String id;
        final String movesense_mac_address;

        try {
            type = intent.getStringExtra("type").toLowerCase();
            path = intent.getStringExtra("path");
            value = intent.getStringExtra("value");
            id = intent.getStringExtra("id");
            movesense_mac_address = intent.getStringExtra("address");
            file_path = intent.getStringExtra("file_path");
        } catch (Exception e) {
            Log.i(LOG_TAG, "Extras error");
            return;
        }

        if (!type.equals("connect")) {
            //No connected device
            if (MovesenseConnectedDevices.getConnectedDevices().size() == 0) {
                Log.i(LOG_TAG, "No devices connected");
                return;
            }
        } else {
            if (movesense_mac_address == null || movesense_mac_address.isEmpty()) {
                Log.i(LOG_TAG, "No address specified for connection");
            }
        }

        final Mds build = Mds.builder().build(context);


        if (type.equals("subscribe")) {
            MdsSubscription mdsSubscription = build.subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(
                            MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                            path),
                    new MdsNotificationListener() {
                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "ID:" + id + " " + path + " OUTPUT: " + data);

                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);
                        }
                    });

            mSubscriptions.put(path, mdsSubscription);
            MdsSubscription sub = mSubscriptions.get(path);
        } else if (type.equals("unsubscribe")) {
            try {
                MdsSubscription sub = mSubscriptions.get(path);
                if (sub != null)
                    sub.unsubscribe();
                mSubscriptions.remove(path);
            } catch (Exception e) {
                Log.e(LOG_TAG, "onError(): ", e);
            }
        } else if (type.equals("get")) {
            build.get(MdsRx.SCHEME_PREFIX + MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + "/" + path,
                    value, new MdsResponseListener() {
                        @Override
                        public void onSuccess(String data) {
                            Log.d(LOG_TAG, "ID:" + id + " " + path + " OUTPUT: " + data);
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError()", error);
                        }
                    });
        } else if (type.equals("put")) {
            build.put(MdsRx.SCHEME_PREFIX + MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + "/" + path,
                    value, new MdsResponseListener() {
                        @Override
                        public void onSuccess(String data) {
                            Log.d(LOG_TAG, "ID:" + id + " " + path + " OUTPUT: " + data);
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError()", error);
                        }
                    });
        } else if (type.equals("post")) {
            build.post(MdsRx.SCHEME_PREFIX + MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + "/" + path,
                    value, new MdsResponseListener() {
                        @Override
                        public void onSuccess(String data) {
                            Log.d(LOG_TAG, "ID:" + id + " " + path + " OUTPUT: " + data);
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError()", error);
                        }
                    });
        } else if (type.equals("connect")) {
            try {
                mScanningCompositeSubscription.add(RxBle.Instance.getClient().scanBleDevices()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<RxBleScanResult>() {
                            @Override
                            public void call(RxBleScanResult scanResult) {
                                Log.d(LOG_TAG, "scanResult: " + scanResult.getBleDevice().getName() + " : " +
                                        scanResult.getBleDevice().getMacAddress());
                                if (!isConnecting && movesense_mac_address.equals(scanResult.getBleDevice().getMacAddress())) {
                                    Log.e(LOG_TAG, "scanResult: FOUND DEVICE FROM INTENT Connecting..." + scanResult.getBleDevice().getName() + " : " +
                                            scanResult.getBleDevice().getMacAddress());
                                    isConnecting = true;
                                    MdsRx.Instance.connect(scanResult.getBleDevice());
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e(LOG_TAG, "BEFORE CONNECT YOU NEED GRANT LOCATION PERMISSION !!!");
                                Log.e(LOG_TAG, "Connect Error: ", throwable);
                            }
                        }));

                mCompositeSubscription.add(MdsRx.Instance.connectedDeviceObservable()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<MdsConnectedDevice>() {
                            @Override
                            public void call(MdsConnectedDevice mdsConnectedDevice) {
                                if (mdsConnectedDevice.getConnection() != null) {

                                    mScanningCompositeSubscription.unsubscribe();

                                    if (mdsConnectedDevice.getDeviceInfo() instanceof MdsDeviceInfoNewSw) {
                                        MdsDeviceInfoNewSw mdsDeviceInfoNewSw = (MdsDeviceInfoNewSw) mdsConnectedDevice.getDeviceInfo();
                                        MovesenseConnectedDevices.addConnectedDevice(new MovesenseDevice(
                                                mdsDeviceInfoNewSw.getDescription(),
                                                mdsDeviceInfoNewSw.getSerial(),
                                                mdsDeviceInfoNewSw.getSw(),
                                                null,
                                                mdsDeviceInfoNewSw.getAddressInfoNew()));

                                        mDevice_name = mdsDeviceInfoNewSw.getDescription();

                                        Log.d(LOG_TAG, CONNECTED_WITH + mdsDeviceInfoNewSw.getSerial() + " : " + movesense_mac_address);

                                    } else if (mdsConnectedDevice.getDeviceInfo() instanceof MdsDeviceInfoOldSw) {
                                        MdsDeviceInfoOldSw mdsDeviceInfoOldSw = (MdsDeviceInfoOldSw) mdsConnectedDevice.getDeviceInfo();
                                        MovesenseConnectedDevices.addConnectedDevice(new MovesenseDevice(
                                                mdsDeviceInfoOldSw.getDescription(),
                                                mdsDeviceInfoOldSw.getSerial(),
                                                mdsDeviceInfoOldSw.getSw(),
                                                mdsDeviceInfoOldSw.getAddressInfoOld(),
                                                null));

                                        mDevice_name = mdsDeviceInfoOldSw.getDescription();

                                        Log.d(LOG_TAG, CONNECTED_WITH + mdsDeviceInfoOldSw.getSerial() + " : " + movesense_mac_address);
                                    }
                                } else {

                                    if (file_path == null || file_path.isEmpty()) {
                                        Log.e(LOG_TAG, "File path error");
                                        return;
                                    }

                                    File dir = Environment.getExternalStorageDirectory();
                                    File yourFile = new File(dir, file_path);

                                    Log.e(LOG_TAG, "DFU: dir: " + dir);
                                    Log.e(LOG_TAG, "DFU: file: " + yourFile);
                                    Log.e(LOG_TAG, "DFU: file: " + yourFile.exists());
                                    Log.e(LOG_TAG, "DFU: path: " + yourFile.getPath());

                                    if (!yourFile.exists()) {
                                        Log.e(LOG_TAG, "File not exists - path: " + yourFile.getPath());
                                        return;
                                    }

                                    DfuUtil.runDfuServiceUpdate(context, DfuUtil.incrementMacAddress(movesense_mac_address), mDevice_name,
                                            Uri.parse("file://" + yourFile.getPath()), null);


                                }
                            }
                        }));

            } catch (Exception e) {
                Log.e(LOG_TAG, "BEFORE CONNECT YOU NEED GRANT LOCATION PERMISSION !!!");
            }

        } else if (type.equals("disconnect")) {
            if (MovesenseConnectedDevices.getConnectedDevices().size() > 0
                    && movesense_mac_address != null && !movesense_mac_address.isEmpty()) {

                if (MovesenseConnectedDevices.getRxMovesenseConnectedDevices().size() > 0) {
                    BleManager.INSTANCE.disconnect(MovesenseConnectedDevices.getConnectedRxDevice(0));
                }
            }
        } else if (type.equals("dfu_update")) {

            DfuUtil.runDfuModeOnConnectedDevice(context, new MdsResponseListener() {
                @Override
                public void onSuccess(String s) {
                    Log.d(LOG_TAG, "DFU onSuccess: ");

                    BleManager.INSTANCE.disconnect(MovesenseConnectedDevices.getConnectedRxDevice(0));
                }

                @Override
                public void onError(MdsException e) {
                    Log.d(LOG_TAG, "DFU onError: ");
                }
            });
        }
    }
}
