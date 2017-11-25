package com.movesense.mds.sampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.movesense.mds.sampleapp.example_app_using_mds_api.ConnectingDialog;
import com.movesense.mds.sampleapp.example_app_using_mds_api.SelectTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseConnectedDevices;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseDevice;
import com.movesense.mds.sampleapp.model.MdsConnectedDevice;
import com.movesense.mds.sampleapp.model.MdsDeviceInfoNewSw;
import com.movesense.mds.sampleapp.model.MdsDeviceInfoOldSw;
import com.polidea.rxandroidble.RxBleDevice;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends AppCompatActivity implements ScanFragment.DeviceSelectionListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private CompositeSubscription subscriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        subscriptions = new CompositeSubscription();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content, new ScanFragment(), ScanFragment.class.getSimpleName())
                    .commit();
        }
    }

    @Override
    public void onDeviceSelected(final RxBleDevice device) {
        Log.d(TAG, "onDeviceSelected: " + device.getName() + " (" + device.getMacAddress() + ")");
        MdsRx.Instance.connect(device);

        ConnectingDialog.INSTANCE.showDialog(this, device.getMacAddress());

        // Monitor for connected devices
        subscriptions.add(MdsRx.Instance.connectedDeviceObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MdsConnectedDevice>() {
                    @Override
                    public void call(MdsConnectedDevice mdsConnectedDevice) {
                        // Stop refreshing
                        if (mdsConnectedDevice.getConnection() != null) {
                            ConnectingDialog.INSTANCE.dismissDialog();
                            // Add connected device
                            // Fixme: this should be deleted after 1.0 SW release

                            if (mdsConnectedDevice.getDeviceInfo() instanceof MdsDeviceInfoNewSw) {
                                MdsDeviceInfoNewSw mdsDeviceInfoNewSw = (MdsDeviceInfoNewSw) mdsConnectedDevice.getDeviceInfo();
                                MovesenseConnectedDevices.addConnectedDevice(new MovesenseDevice(
                                        mdsDeviceInfoNewSw.getDescription(),
                                        mdsDeviceInfoNewSw.getSerial(),
                                        mdsDeviceInfoNewSw.getSw(),
                                        null,
                                        mdsDeviceInfoNewSw.getAddressInfoNew()));
                            } else if (mdsConnectedDevice.getDeviceInfo() instanceof MdsDeviceInfoOldSw) {
                                MdsDeviceInfoOldSw mdsDeviceInfoOldSw = (MdsDeviceInfoOldSw) mdsConnectedDevice.getDeviceInfo();
                                MovesenseConnectedDevices.addConnectedDevice(new MovesenseDevice(
                                        mdsDeviceInfoOldSw.getDescription(),
                                        mdsDeviceInfoOldSw.getSerial(),
                                        mdsDeviceInfoOldSw.getSw(),
                                        mdsDeviceInfoOldSw.getAddressInfoOld(),
                                        null));
                            }
                            // We have a new SdsDevice
                            startActivity(new Intent(MainActivity.this, SelectTestActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                    }
                }, new ThrowableToastingAction(this)));
    }
}
