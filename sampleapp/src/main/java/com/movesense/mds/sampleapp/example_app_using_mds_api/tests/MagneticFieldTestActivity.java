package com.movesense.mds.sampleapp.example_app_using_mds_api.tests;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.sampleapp.BleManager;
import com.movesense.mds.sampleapp.ConnectionLostDialog;
import com.movesense.mds.sampleapp.MdsRx;
import com.movesense.mds.sampleapp.R;
import com.movesense.mds.sampleapp.example_app_using_mds_api.FormatHelper;
import com.movesense.mds.sampleapp.example_app_using_mds_api.logs.LogsManager;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.InfoResponse;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MagneticField;
import com.movesense.mds.sampleapp.example_app_using_mds_api.model.MovesenseConnectedDevices;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnItemSelected;

public class MagneticFieldTestActivity extends AppCompatActivity implements BleManager.IBleConnectionMonitor {

    @BindView(R.id.switchSubscription) SwitchCompat switchSubscription;
    @BindView(R.id.spinner) Spinner spinner;
    @BindView(R.id.x_axis_textView) TextView xAxisTextView;
    @BindView(R.id.y_axis_textView) TextView yAxisTextView;
    @BindView(R.id.z_axis_textView) TextView zAxisTextView;
    @BindView(R.id.deg_axis_textView) TextView degAxisTextView;
    @BindView(R.id.graphView) GraphView graphView;

    private final String MAGNETIC_FIELD_PATH = "Meas/Magn/";
    private final String ACC_FIELD_PATH = "/Meas/Acc";
    private final String MAGNETIC_FIELD_INFO_PATH = "/Meas/Magn/Info";
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private final String LED_PATH = "/Component/Led";
    private final String LED_PARAMETER = "{\"isOn\":";

    private final String LOG_TAG = MagneticFieldTestActivity.class.getSimpleName();
    @BindView(R.id.connected_device_name_textView) TextView mConnectedDeviceNameTextView;
    @BindView(R.id.connected_device_swVersion_textView) TextView mConnectedDeviceSwVersionTextView;

    private AlertDialog alertDialog;
    private LineGraphSeries seriesX = new LineGraphSeries();
    private LineGraphSeries seriesY = new LineGraphSeries();
    private LineGraphSeries seriesZ = new LineGraphSeries();

    private final List<String> spinnerRates = new ArrayList<>();
    private String rate;
    private MdsSubscription mdsSubscription;
    private MdsSubscription mdsAccSubscription;
    private LogsManager logsManager;
    private GraphView mMagneticGraphView;

    private List<Double> readingsX = new ArrayList<>();
    private List<Double> readingsY = new ArrayList<>();
    private List<Double> readingsZ = new ArrayList<>();

    private Handler handler = new Handler();

    private HashMap<Double, Double> mapIt = new HashMap<>();

    double avgX;
    double avgY;
    double avgZ;
    double deg;
    boolean isWaitingBlinking = false;
    int blinkDelay = 1000;
    boolean ledOn = false;
    int blinkFreq = 2000;
    Vibrator vibrator;
    int vibrateSpeed = 3;
    boolean canVibrate = false;
    double minY = -277;
    double maxY = -229;
    double minX = 37;
    double maxX = 88;
    double balanceZ = -70;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetic_field_test);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Magnetic Field");
        }

        logsManager = new LogsManager(this);

        alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.loading_information)
                .create();

        mConnectedDeviceNameTextView.setText("Serial: " + MovesenseConnectedDevices.getConnectedDevice(0)
                .getSerial());

        mConnectedDeviceSwVersionTextView.setText("Sw version: " + MovesenseConnectedDevices.getConnectedDevice(0)
                .getSwVersion());

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        xAxisTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        yAxisTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        zAxisTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));

        mMagneticGraphView = (GraphView) findViewById(R.id.graphView);
        setUpGraphView();

        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, spinnerRates);

        spinner.setAdapter(spinnerAdapter);

        // Display dialog
        alertDialog.show();

        Mds.builder().build(this).get(MdsRx.SCHEME_PREFIX
                        + MovesenseConnectedDevices.getConnectedDevice(0).getSerial() + MAGNETIC_FIELD_INFO_PATH,
                null, new MdsResponseListener() {
                    @Override
                    public void onSuccess(String data) {
                        Log.d(LOG_TAG, "onSuccess(): " + data);

                        // Hide dialog
                        alertDialog.dismiss();

                        InfoResponse infoResponse = new Gson().fromJson(data, InfoResponse.class);

                        for (Integer inforate : infoResponse.content.sampleRates) {
                            spinnerRates.add(String.valueOf(inforate));

                            // Set first rate as default
                            if (rate == null) {
                                rate = String.valueOf(inforate);
                            }
                        }

                        spinnerAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "onError(): ", error);

                        // Hide dialog
                        alertDialog.dismiss();
                    }
                });

        BleManager.INSTANCE.addBleConnectionMonitorListener(this);

    }

    private double calcDegree(double x, double y) {
        /*double accXnorm = accx/Math.sqrt(accx * accx + accy * accy + accz * accz);
        double accYnorm = accy/Math.sqrt(accx * accx + accy * accy + accz * accz);

        double pitch = Math.asin(accXnorm);
        double roll = -Math.asin(accYnorm / Math.cos(pitch));

        double magXcomp = magx*Math.cos(pitch)+magz*Math.sin(pitch);
        double magYcomp =magx*Math.sin(roll)*Math.sin(pitch)+magy*Math.cos(roll)-magz*Math.sin(roll)*Math.cos(pitch);

        double heading = 180 * Math.atan2(magYcomp, magXcomp);

        if (heading < 0)
            heading += 360;

        return heading;*/

        double xCentre = minX + Math.abs((maxX - minX)/2);
        double yDiff = Math.abs(maxY - minY);
        double yOnCircle = Math.abs(maxY - y);

        double value = x < xCentre ? yOnCircle : yDiff + (yDiff - Math.abs(yOnCircle));


        return (90 / (yDiff / 2)) * value;

    }

    @OnCheckedChanged(R.id.switchSubscription)
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            disableSpinner();

            // Clear Logcat
            logsManager.clearAdbLogcat();
            handler.postDelayed(runnable, 500);

            mdsSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER,
                    FormatHelper.formatContractToJson(MovesenseConnectedDevices.getConnectedDevice(0)
                            .getSerial(), MAGNETIC_FIELD_PATH + rate), new MdsNotificationListener() {
                        @Override
                        public void onNotification(String data) {
                            Log.d(LOG_TAG, "onSuccess(): " + data);

                            MagneticField magneticField = new Gson().fromJson(
                                    data, MagneticField.class);

                            if (magneticField != null) {

                                MagneticField.Array arrayData = magneticField.body.array[0];

                                readingsX.add(arrayData.x);
                                readingsY.add(arrayData.y);
                                readingsZ.add(arrayData.z);

                                try {
                                    seriesX.appendData(
                                            new DataPoint(magneticField.body.timestamp, arrayData.x), false,
                                            200);
                                    seriesY.appendData(
                                            new DataPoint(magneticField.body.timestamp, arrayData.y), true,
                                            200);
                                    seriesZ.appendData(
                                            new DataPoint(magneticField.body.timestamp, arrayData.z), true,
                                            200);
                                } catch (IllegalArgumentException e) {
                                    Log.e(LOG_TAG, "GraphView error ", e);
                                }
                            }
                        }

                        @Override
                        public void onError(MdsException error) {
                            Log.e(LOG_TAG, "onError(): ", error);

                            Toast.makeText(MagneticFieldTestActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();

                            buttonView.setChecked(false);
                        }
                    });
        } else {
            enableSpinner();
            unSubscribe();

            // Save logs
            saveAdbLogsToFile(LOG_TAG);
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
      /* do what you need to do */

            avgZ = calcAvg(readingsZ);
            avgX = calcAvg(readingsX);
            avgY = calcAvg(readingsY);

            if (avgX == -1 || avgY == -1 || avgZ == -1) {
                handler.postDelayed(this, 500);

                return;
            }

            deg = calcDegree(avgX, avgY);

            xAxisTextView.setText(String.format(Locale.getDefault(),
                    "x: %.6f", avgX));
            yAxisTextView.setText(String.format(Locale.getDefault(),
                    "y: %.6f", avgY));
            zAxisTextView.setText(String.format(Locale.getDefault(),
                    "z: %.6f", avgZ));
            degAxisTextView.setText(String.format(Locale.getDefault(),
                    "deg: %.6f", deg));

            readingsX.clear();
            readingsY.clear();
            readingsZ.clear();


            if (avgZ < -60) {
                canVibrate = true;
      /* and here comes the "trick" */

                if (Math.abs(deg - 25) < 9 && vibrateSpeed != 0 && canVibrate) {
                    Log.i("MagneticVibrate", "vibrate 1");
                    vibrateSpeed = 0;
                    long[] pattern = {0, 100, 100};
                    vibrator.vibrate(pattern, 0);
                } else if (Math.abs(deg - 25) < 45 && vibrateSpeed != 1 && canVibrate) {
                    Log.i("MagneticVibrate", "vibrate 2");

                    vibrateSpeed = 1;
                    long[] pattern = {0, 100, (long) Math.abs(deg - 25) * 40};
                    vibrator.vibrate(pattern, 0);
                } else if ( canVibrate && vibrateSpeed != 2) {
                    Log.i("MagneticVibrate", "vibrate 3");

                    vibrateSpeed = 2;
                    long[] pattern = {0, 100, 2000};
                    vibrator.vibrate(pattern, 0);
                }
            } else if (canVibrate){
                Log.i("MagneticVibrate", "vibrate cancel");

                canVibrate = false;
                vibrator.vibrate(100);
                vibrator.cancel();
            }

            handler.postDelayed(this, 500);


        }
    };

    private double calcAvg(List<Double> list) {
        if (list.isEmpty()) {
            return -1;
        }
        double sum = 0;
        double listSize = list.size();


        for (double num : list) {
            sum += num;
        }


        return sum / listSize;
    }

    private void saveAdbLogsToFile(String logTag) {
        if (!logsManager.checkRuntimeWriteExternalStoragePermission(this, this)) {
            return;
        }

        logsManager.saveLogsToSdCard(logTag);
    }

    @OnItemSelected(R.id.spinner)
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        rate = spinnerRates.get(position);
    }

    private void setSeriesColor(@ColorRes int colorRes, LineGraphSeries series) {
        int color = getResources().getColor(colorRes);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(color);
        series.setCustomPaint(paint);
    }

    @Override
    protected void onPause() {
        super.onPause();
        canVibrate = false;
        vibrator.vibrate(100);
        vibrator.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unSubscribe();

        vibrator.vibrate(100);
        vibrator.cancel();
        BleManager.INSTANCE.removeBleConnectionMonitorListener(this);
    }

    private void unSubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription.unsubscribe();
            mdsSubscription = null;
        }
    }

    private void disableSpinner() {
        spinner.setEnabled(false);
    }

    private void enableSpinner() {
        spinner.setEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == LogsManager.REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            // if request is cancelled grantResults array is empty
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {

                    // Save logs
                    saveAdbLogsToFile(LOG_TAG);
                }
            }
        }
    }

    private void setUpGraphView() {
        mMagneticGraphView.addSeries(seriesX);
        mMagneticGraphView.addSeries(seriesY);
        mMagneticGraphView.addSeries(seriesZ);
        seriesX.setDrawAsPath(true);
        seriesY.setDrawAsPath(true);
        seriesZ.setDrawAsPath(true);
        mMagneticGraphView.getViewport().setXAxisBoundsManual(true);
        mMagneticGraphView.getViewport().setMinX(0);
        mMagneticGraphView.getViewport().setMaxX(10000);

        // Disable X axis label
        mMagneticGraphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return "";
                } else {
                    // show currency for y values
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        setSeriesColor(android.R.color.holo_red_dark, seriesX);
        setSeriesColor(android.R.color.holo_green_dark, seriesY);
        setSeriesColor(android.R.color.holo_blue_dark, seriesZ);

    }

    @Override
    public void onDisconnect(RxBleDevice rxBleDevice) {
        Log.e(LOG_TAG, "onDisconnect: " + rxBleDevice.getName() + " " + rxBleDevice.getMacAddress());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setUpGraphView();
                ConnectionLostDialog.INSTANCE.showDialog(MagneticFieldTestActivity.this);
            }
        });
    }

    @Override
    public void onConnect(RxBleDevice rxBleDevice) {
        Log.e(LOG_TAG, "onConnect: " + rxBleDevice.getName() + " " + rxBleDevice.getMacAddress());
        ConnectionLostDialog.INSTANCE.dismissDialog();
    }
}
