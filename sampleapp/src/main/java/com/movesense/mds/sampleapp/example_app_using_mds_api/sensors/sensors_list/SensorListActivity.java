package com.movesense.mds.sampleapp.example_app_using_mds_api.sensors.sensors_list;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.sampleapp.BleManager;
import com.movesense.mds.sampleapp.MdsRx;
import com.movesense.mds.sampleapp.R;
import com.movesense.mds.sampleapp.ThrowableToastingAction;
import com.movesense.mds.sampleapp.example_app_using_mds_api.mainView.MainViewActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseConnectedDevices;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.AngularVelocityActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.BatteryActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.EcgActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.HeartRateTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.LedTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.LinearAccelerationTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.MagneticFieldTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.MapsActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.MultiSubscribeActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.TemperatureTestActivity;
import com.movesense.mds.sampleapp.model.MdsConnectedDevice;
import com.movesense.mds.sampleapp.model.MdsInfo;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class SensorListActivity extends AppCompatActivity implements View.OnClickListener {

    @BindView(R.id.sensorList_recyclerView) RecyclerView mSensorListRecyclerView;
    @BindView(R.id.sensorList_connectedSerial_tv) TextView mSensorListConnectedSerialTv;
    @BindView(R.id.sensorList_connectedSwVersion_tv) TextView mSensorListConnectedSwVersionTv;
    private CompositeSubscription subscriptions;

    private final String TAG = SensorListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_list);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sensors List");
        }

        mSensorListConnectedSerialTv.setText("Serial: " + MovesenseConnectedDevices.getConnectedDevice(0).getSerial());
        mSensorListConnectedSwVersionTv.setText("Sw version: " + MovesenseConnectedDevices.getConnectedDevice(0).getSwVersion());


        Mds.builder().build(this).get(MdsRx.SCHEME_PREFIX +
                        MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + "/Info",
                null, new MdsResponseListener() {
                    @Override
                    public void onSuccess(String s) {
                        Log.d(TAG, "Info onSuccess: " + s);
                        MdsInfo mdsInfo = new Gson().fromJson(s, MdsInfo.class);

                        if (mdsInfo.getContent() != null) {
                            MovesenseConnectedDevices.getConnectedDevice(0).setSwVersion(mdsInfo.getContent().getSw());
                        }

                        mSensorListConnectedSerialTv.setText("Serial: " + MovesenseConnectedDevices.getConnectedDevice(0).getSerial());
                        mSensorListConnectedSwVersionTv.setText("Sw version: " + MovesenseConnectedDevices.getConnectedDevice(0).getSwVersion());

                        Log.e(TAG, "Info: " + mdsInfo.getContent());
                    }

                    @Override
                    public void onError(MdsException e) {
                        Log.e(TAG, "Info onError: ", e);

                    }
                });

        subscriptions = new CompositeSubscription();

        ArrayList<SensorListItemModel> sensorListItemModels = new ArrayList<>();

        sensorListItemModels.add(new SensorListItemModel("Map", R.drawable.linear_acc2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.linear_acceleration_name), R.drawable.linear_acc2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.led_name), R.drawable.led2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.temperature_name), R.drawable.temperature2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.heart_rate_name), R.drawable.heart_rate2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.angular_velocity_name), R.drawable.gyro2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.magnetic_field_name), R.drawable.magnetic_field2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.multi_subscription_name), R.drawable.magnetic_field2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.ecg), R.drawable.magnetic_field2));
        sensorListItemModels.add(new SensorListItemModel(getString(R.string.battery_energy), R.drawable.magnetic_field2));

        SensorsListAdapter sensorsListAdapter = new SensorsListAdapter(sensorListItemModels, this);
        mSensorListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSensorListRecyclerView.setAdapter(sensorsListAdapter);

        sensorsListAdapter.notifyDataSetChanged();

        subscriptions.add(MdsRx.Instance.connectedDeviceObservable()
                .subscribe(new Action1<MdsConnectedDevice>() {
                    @Override
                    public void call(MdsConnectedDevice mdsConnectedDevice) {
                        Log.e("SensorListActivity", "call: ");
                        if (mdsConnectedDevice.getConnection() == null) {
                            Log.e("SensorListActivity", "TEST Disconnected");

                            startActivity(new Intent(SensorListActivity.this, MainViewActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        }
                    }
                }, new ThrowableToastingAction(this)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy: ");
        subscriptions.unsubscribe();
    }

    @Override
    public void onClick(View v) {
        String sensorName = (String) v.getTag();

        subscriptions.unsubscribe();

        if ("Map".equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, MapsActivity.class));
            return;
        }
        else if (getString(R.string.led_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, LedTestActivity.class));
            return;
        } else if (getString(R.string.linear_acceleration_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, LinearAccelerationTestActivity.class));
            return;
        } else if (getString(R.string.temperature_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, TemperatureTestActivity.class));
            return;
        } else if (getString(R.string.angular_velocity_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, AngularVelocityActivity.class));
            return;
        } else if (getString(R.string.magnetic_field_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, MagneticFieldTestActivity.class));
            return;
        } else if (getString(R.string.heart_rate_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, HeartRateTestActivity.class));
            return;
        } else if (getString(R.string.multi_subscription_name).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, MultiSubscribeActivity.class));
            return;
        } else if (getString(R.string.ecg).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, EcgActivity.class));
            return;
        } else if (getString(R.string.battery_energy).equals(sensorName)) {
            startActivity(new Intent(SensorListActivity.this, BatteryActivity.class));
            return;
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage(R.string.disconnect_dialog_text)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.e(TAG, "TEST Disconnecting...");

                        BleManager.INSTANCE.disconnect(MovesenseConnectedDevices.getConnectedRxDevice(0));
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }
}
