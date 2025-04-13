package com.example.speedmonitor;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class SpeedMonitorActivity extends AppCompatActivity {


    private Car car;
    String LOG_TAG = "SpeedMonitorActivity";
    boolean ignitionStatus = false;
    private float currentSpeed;
    private float speedLimit = 60.0f; // Set by the rental company, could be fetched from Firestore/Realtime Database
    private final String[] permissions = new String[]{"android.car.permission.CAR_SPEED", "permission:android.car.permission.CAR_POWERTRAIN"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_monitor);
        initVehicleConnectivity();
        startSpeedMonitoring();
    }

    private void startSpeedMonitoring() {

    }

    private void sendSpeedExceedAlert() {
        // Send notification to rental company
        String message = "Speed limit exceeded by car ID: " + getCarId();
        sendFirebaseNotification(message);
        // Also alert the user about the speed limit breach
        Toast.makeText(this, "Speed limit exceeded!", Toast.LENGTH_SHORT).show();
    }

    private void sendFirebaseNotification(String message) {
        // Send Firebase notification to rental company
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("alerts");
        ref.push().setValue(message);
    }

    private String getCarId() {
        // Retrieve car ID from shared preferences or intent (you would pass this info at the start of the rental)
        return "CAR_RENTAL_123";  // Get the real car ID here.
    }

    private final void watchSpeedSensor(CarSensorManager carSensorManager) throws CarNotConnectedException {

        carSensorManager.registerListener(new CarSensorManager.OnSensorChangedListener() {
                                              @Override
                                              public void onSensorChanged(CarSensorEvent carSensorEvent) {
                                                  currentSpeed = (int) carSensorEvent.floatValues[0];
                                                  if (currentSpeed > speedLimit) {
                                                      sendSpeedExceedAlert();
                                                  }
                                              }
                                          },
                CarSensorManager.SENSOR_TYPE_CAR_SPEED,
                CarSensorManager.SENSOR_RATE_NORMAL);
    }

    private final void watchIgnitionStatus(CarSensorManager carSensorManager) throws CarNotConnectedException {

        carSensorManager.registerListener(new CarSensorManager.OnSensorChangedListener() {
                                              @Override
                                              public void onSensorChanged(CarSensorEvent carSensorEvent) {
                                                  Log.v(LOG_TAG, "Ignition val : " + carSensorEvent.intValues[0]);
                                                  if (carSensorEvent.intValues[0] == CarSensorEvent.IGNITION_STATE_ON) {
                                                      ignitionStatus = true;
                                                  } else {
                                                      ignitionStatus = false;
                                                  }
                                              }
                                          },
                CarSensorManager.SENSOR_TYPE_IGNITION_STATE,
                CarSensorManager.SENSOR_RATE_NORMAL);
    }

    private final void initVehicleConnectivity() {
        if (this.getPackageManager().hasSystemFeature("android.hardware.type.automotive")) {
            if (this.car == null) {
                car = Car.createCar(this, (ServiceConnection) (new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.v(LOG_TAG, "Vehicle Service Connected!");
                        onCarServiceReady();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.v(LOG_TAG, "Vehicle Service Disconnected!");
                    }
                }));
            }
        }
    }

    private final void onCarServiceReady() {
        try {
            CarSensorManager carSensorManager = (CarSensorManager) car.getCarManager(Car.SENSOR_SERVICE);
            //Last state was not
            this.watchSpeedSensor(carSensorManager);
            this.watchIgnitionStatus(carSensorManager);
        } catch (CarNotConnectedException e) {
            Log.v(LOG_TAG, "Vehicle Service NOT Connected!");
            e.printStackTrace();
        }
    }

    public boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (permissions[0] == Car.PERMISSION_SPEED && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (!car.isConnected() && !car.isConnecting()) {
                car.connect();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            if (!car.isConnected() && !car.isConnecting()) {
                car.connect();
            }
        } else {
            requestPermissions(permissions, 0);
        }
    }

    @Override
    protected void onPause() {
        if (car.isConnected()) {
            car.disconnect();
        }
        super.onPause();
    }

}
