package com.example.covid;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PandamicActivity extends AppCompatActivity implements OnListItemCLick {

    private static final String TAG = "PANDAMIC_APP";
    SharedPreferences sharedPref;
    Intent mService;

    public ArrayList<BluetoothDevice> mBTDevices;
    public Set<BluetoothDevice> mSelectedBTDevices = new HashSet<>();
    public DeviceListAdapter mDeviceListAdapter;
    RecyclerView mDevicesList;
    TextView mResponse;
    Button btnLogPositive;
    Button btnWantUpdateOthers;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_page);

        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        EventBus.getDefault().register(this);
        Button button = findViewById(R.id.btnOn);
        Button btnOFF = (Button) findViewById(R.id.btnOFF);
        btnLogPositive = findViewById(R.id.btnPositive);
        mResponse = findViewById(R.id.response);
        btnWantUpdateOthers = findViewById(R.id.btnwantupdateothers);
        mBTDevices = new ArrayList<>();
        mDevicesList = (RecyclerView) findViewById(R.id.lvNewDevices);
        mDevicesList.setLayoutManager(new LinearLayoutManager(this));
        mDeviceListAdapter = new DeviceListAdapter(new ArrayList<BluetoothDevice>(mBTDevices), PandamicActivity.this);
        mDevicesList.setAdapter(mDeviceListAdapter);

        if (sharedPref.getInt(getString(R.string.positive), 0) == 0) {
            btnLogPositive.setText(getString(R.string.log_positive));
            mResponse.setText(getString(R.string.stop_spread_msg));
        } else {
            mResponse.setText(getString(R.string.get_well_soon));
            btnLogPositive.setText(getString(R.string.log_negative));
        }
        if (sharedPref.getInt(getString(R.string.wanttoupdate), 0) == 1) {
            btnWantUpdateOthers.setText(getString(R.string.stop_update));
        } else {
            btnWantUpdateOthers.setText(getString(R.string.update_people));
        }

        mService = new Intent(PandamicActivity.this, PandamicService.class);

        if (sharedPref.getInt(getString(R.string.monitor), 0) == 1) {
            startService();
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.monitor), 1);
                editor.commit();
                startService();
                checkLocationPermissions();
            }
        });

        btnOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.monitor), 0);
                editor.commit();
                //stop service and add to preference
                stopService();
            }
        });
        btnLogPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                if (sharedPref.getInt(getString(R.string.positive), 0) == 0) {
                    editor.putInt(getString(R.string.positive), 1);
                    btnLogPositive.setText("Log Negative");
                    mResponse.setText("Get well soon! You have logged Covid-19 positive");
                } else {
                    mResponse.setText("Stop Pandamic Covid-19 together");
                    btnLogPositive.setText("Log Positive");

                    editor.putInt(getString(R.string.positive), 0);
                }
                editor.commit();
                updatePeopleAroundMe();

            }
        });

        btnWantUpdateOthers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPref.edit();
                if (sharedPref.getInt(getString(R.string.wanttoupdate), 0) == 0) {
                    editor.putInt(getString(R.string.wanttoupdate), 1);
                    //btnWantUpdateOthers.setText(getString(R.string.stop_update));
                } else {
                    editor.putInt(getString(R.string.wanttoupdate), 0);
                //    btnWantUpdateOthers.setText(getString(R.string.update_people));
                }
                editor.commit();
                if (sharedPref.getInt(getString(R.string.wanttoupdate), 0) == 1) {
                    updatePeopleAroundMe();
                }
                if (sharedPref.getInt(getString(R.string.wanttoupdate), 0) == 0) {
                    btnWantUpdateOthers.setText(getString(R.string.stop_update));
                } else {
                    btnWantUpdateOthers.setText(getString(R.string.update_people));
                }
            }
        });

    }

    private void updatePeopleAroundMe() {
        EventBus.getDefault().post(mSelectedBTDevices);
    }

    private void startService() {
        startService(mService);
    }

    private void stopService() {
        stopService(mService);
    }

    private void checkLocationPermissions() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        int permissionCheck1 = this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permissionCheck != 0 && permissionCheck1 != 0) {

            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            //btnDiscover();
            startService();

        }
    }

    @Override
    public void onItemSelected(Set<BluetoothDevice> devices) {
        mSelectedBTDevices = devices;
    }


    @Subscribe
    public void updateList(ArrayList<BluetoothDevice> devices) {
        Log.i(TAG, "updateList   " + devices);
        mBTDevices = devices;
        mDeviceListAdapter.setDevices(mBTDevices);
        mDeviceListAdapter.notifyDataSetChanged();
    }


    @Override
    protected void onDestroy() {
        mSelectedBTDevices.clear();
        EventBus.getDefault().post(mSelectedBTDevices);
        super.onDestroy();
        mBTDevices.clear();
        mBTDevices = null;
        mDeviceListAdapter = null;
        mDevicesList = null;
        EventBus.getDefault().unregister(this);
    }
}
