package com.movesense.mds.sampleapp.example_app_using_mds_api;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.movesense.mds.sampleapp.BleManager;
import com.movesense.mds.sampleapp.ConnectionLostDialog;
import com.movesense.mds.sampleapp.MainActivity;
import com.movesense.mds.sampleapp.MdsRx;
import com.movesense.mds.sampleapp.R;
import com.movesense.mds.sampleapp.ThrowableToastingAction;
import com.movesense.mds.sampleapp.example_app_using_mds_api.dfu.DfuActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseConnectedDevices;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.TestItemList;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.AngularVelocityActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.DirectionsActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.HeartRateTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.LedTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.LinearAccelerationTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.MagneticFieldTestActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.MapsActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.MultiSubscribeActivity;
import com.movesense.mds.sampleapp.example_app_using_mds_api.tests.TemperatureTestActivity;
import com.movesense.mds.sampleapp.model.MdsConnectedDevice;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;


public class SelectTestActivity extends AppCompatActivity {

    @BindView(R.id.tests_listView) ListView testsListView;

    private ArrayList<TestItemList> testItemListArrayList = new ArrayList<>();
    private AlertDialog alertDialog;
    private final String LOG_TAG = SelectTestActivity.class.getSimpleName();
    private boolean dfuEnabled;
    private AlertDialog connectionStateDialog;
    private CompositeSubscription subscriptions;
    private boolean closeApp = false;
    private boolean disconnect = false;

    private final String TAG = SelectTestActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_test);
        ButterKnife.bind(this);

        subscriptions = new CompositeSubscription();

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.close_app)
                .setMessage(R.string.do_you_want_to_close_application)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        closeApp = true;
                        BleManager.INSTANCE.disconnect(MovesenseConnectedDevices.getConnectedRxDevice(0));
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                })
                .create();


        ArrayAdapter<TestItemList> testsArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, testItemListArrayList);

        testsListView.setAdapter(testsArrayAdapter);

        // Add Tests to List
        testItemListArrayList.add(new TestItemList("Directions"));
        testItemListArrayList.add(new TestItemList(getString(R.string.linear_acceleration_name)));
        testItemListArrayList.add(new TestItemList(getString(R.string.angular_velocity_name)));
        testItemListArrayList.add(new TestItemList(getString(R.string.magnetic_field_name)));
        testItemListArrayList.add(new TestItemList(getString(R.string.heart_rate_name)));
        testItemListArrayList.add(new TestItemList(getString(R.string.temperature_name)));
        testItemListArrayList.add(new TestItemList(getString(R.string.led_name)));
        testItemListArrayList.add(new TestItemList(getString(R.string.multi_subscription_name)));

        testsArrayAdapter.notifyDataSetChanged();

        subscriptions.add(MdsRx.Instance.connectedDeviceObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<MdsConnectedDevice>() {
                    @Override
                    public void call(MdsConnectedDevice mdsConnectedDevice) {
                        // Stop refreshing
                        if (mdsConnectedDevice.getConnection() == null) {
                            Log.e(TAG, "call: Rx Disconnect");
                            if (closeApp) {
                                if (Build.VERSION.SDK_INT >= 21) {
                                    finishAndRemoveTask();
                                } else {
                                    finish();
                                }
                            } else if (disconnect) {
                                startActivity(new Intent(SelectTestActivity.this, MainActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                            } else {
                                ConnectionLostDialog.INSTANCE.showDialog(SelectTestActivity.this);
                            }
                        } else {
                            ConnectionLostDialog.INSTANCE.dismissDialog();
                            Log.e(TAG, "call: Rx Connect");
                        }
                    }
                }, new ThrowableToastingAction(this)));

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnItemClick(R.id.tests_listView)
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (dfuEnabled) {
            Toast.makeText(this, R.string.dfu_mode_is_enabled, Toast.LENGTH_LONG).show();
            return;
        }

        TestItemList manualTestItemListClicked = testItemListArrayList.get(position);
        if ("Directions".equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, MapsActivity.class));
            return;
        }
        else if (getString(R.string.led_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, LedTestActivity.class));
            return;
        } else if (getString(R.string.linear_acceleration_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, LinearAccelerationTestActivity.class));
            return;
        } else if (getString(R.string.temperature_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, TemperatureTestActivity.class));
            return;
        } else if (getString(R.string.angular_velocity_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, AngularVelocityActivity.class));
            return;
        } else if (getString(R.string.magnetic_field_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, MagneticFieldTestActivity.class));
            return;
        } else if (getString(R.string.heart_rate_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, HeartRateTestActivity.class));
            return;
        } else if (getString(R.string.multi_subscription_name).equals(manualTestItemListClicked.getName())) {
            startActivity(new Intent(SelectTestActivity.this, MultiSubscribeActivity.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dfu_mode:
                startActivity(new Intent(SelectTestActivity.this, DfuActivity.class));
                return true;

            case R.id.send_logs_to_google_drive:
                startActivity(new Intent(SelectTestActivity.this, SendLogsToGoogleDriveActivity.class));
                return true;

            case R.id.disconnect:
                BleManager.INSTANCE.disconnect(MovesenseConnectedDevices.getConnectedRxDevice(0));
                disconnect = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        alertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        subscriptions.unsubscribe();
    }
}

