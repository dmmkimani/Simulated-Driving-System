package com.dmmk.simulateddrivingsystem;

import static java.lang.Math.round;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.phidget22.DigitalOutput;
import com.phidget22.Net;
import com.phidget22.PhidgetException;
import com.phidget22.RCServo;
import com.phidget22.ServerType;
import com.phidget22.VoltageRatioInput;
import com.phidget22.VoltageRatioInputVoltageRatioChangeEvent;
import com.phidget22.VoltageRatioInputVoltageRatioChangeListener;

public class DrivingActivity extends Activity implements SensorEventListener {
    private int speed;
    private boolean isBrakeEngaged;
    private boolean isTurning;

    private SensorManager sensorManager;

    private RCServo rcServo0;
    private RCServo rcServo1;
    private RCServo rcServo2;

    private VoltageRatioInput brake;
    private VoltageRatioInput accelerate;

    private DigitalOutput leftSignal;
    private DigitalOutput rightSignal;
    private DigitalOutput horn;

    private SpeedUpEvent speedUp;
    private SlowDownEvent slowDown;
    private SteerEvent steer;
    private SignalEvent signalLeft;
    private SignalEvent signalRight;
    private HonkEvent honk;

    float[] accelerometer = new float[3];
    float[] linearAcceleration = new float[3];
    float[] rotationMatrix = new float[9];
    float[] orientation = new float[3];
    private int targetPosition = 90;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving);

        speed = 0;
        isBrakeEngaged = false;
        isTurning = false;

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        ImageButton leftSignalBtn = findViewById(R.id.btnLeftSignal);
        leftSignalBtn.setOnClickListener(new leftSignalStateChangeListener());

        ImageButton rightSignalBtn = findViewById(R.id.btnRightSignal);
        rightSignalBtn.setOnClickListener(new rightSignalStateChangeListener());

        ImageButton hornBtn = findViewById(R.id.btnHorn);
        hornBtn.setOnTouchListener(new hornStateChangeListener());

        try {
            rcServo0 = new RCServo();
            rcServo1 = new RCServo();
            rcServo2 = new RCServo();

            brake = new VoltageRatioInput();
            accelerate = new VoltageRatioInput();

            leftSignal = new DigitalOutput();
            rightSignal = new DigitalOutput();
            horn = new DigitalOutput();

            String ipAddress = "xxx.xxx.x.x";

            this.getSystemService(Context.NSD_SERVICE);
            Net.enableServerDiscovery(ServerType.DEVICE_REMOTE);
            Net.addServer("", ipAddress, 5661, "", 0);

            rcServo0.setDeviceSerialNumber(20483);
            rcServo0.setChannel(0);
            rcServo0.setIsRemote(true);

            rcServo1.setDeviceSerialNumber(20483);
            rcServo1.setChannel(1);
            rcServo1.setIsRemote(true);

            rcServo2.setDeviceSerialNumber(20483);
            rcServo2.setChannel(2);
            rcServo2.setIsRemote(true);

            brake.setDeviceSerialNumber(620759);
            brake.setIsHubPortDevice(true);
            brake.setHubPort(2);
            brake.setIsRemote(true);
            brake.addVoltageRatioChangeListener(new brakeVoltageRatioChangeListener());

            accelerate.setDeviceSerialNumber(620759);
            accelerate.setIsHubPortDevice(true);
            accelerate.setHubPort(3);
            accelerate.setIsRemote(true);
            accelerate.addVoltageRatioChangeListener(new accelerateVoltageRatioChangeListener());

            leftSignal.setDeviceSerialNumber(30674);
            leftSignal.setChannel(7);
            leftSignal.setIsRemote(true);

            rightSignal.setDeviceSerialNumber(30674);
            rightSignal.setChannel(0);
            rightSignal.setIsRemote(true);

            horn.setDeviceSerialNumber(30674);
            horn.setChannel(3);
            horn.setIsRemote(true);

            rcServo0.open();
            rcServo1.open();
            rcServo2.open();
            brake.open();
            accelerate.open();
            leftSignal.open();
            rightSignal.open();
            horn.open();

            speedUp = new SpeedUpEvent(rcServo0, rcServo1);
            slowDown = new SlowDownEvent(rcServo0, rcServo1);
            steer = new SteerEvent(rcServo2);
            signalLeft = new SignalEvent(leftSignal);
            signalRight = new SignalEvent(rightSignal);
            honk = new HonkEvent(horn);

            steer.start(targetPosition);

        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    private class accelerateVoltageRatioChangeListener implements VoltageRatioInputVoltageRatioChangeListener {
        @Override
        public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent event) {
            if (!isBrakeEngaged) {
                if (event.getVoltageRatio() != 0) {
                    if (!slowDown.isFlagged()) {
                        speed = slowDown.getSpeed();
                        slowDown.stop();
                    }
                    if (speedUp.isFlagged()) {
                        speedUp.start(speed);
                    }
                } else {
                    if (!speedUp.isFlagged()) {
                        speed = speedUp.getSpeed();
                        speedUp.stop();
                        slowDown.start(speed);
                    }
                }
            }
        }
    }

    private class brakeVoltageRatioChangeListener implements VoltageRatioInputVoltageRatioChangeListener {
        @Override
        public void onVoltageRatioChange(VoltageRatioInputVoltageRatioChangeEvent event) {
            if (event.getVoltageRatio() != 0) {
                isBrakeEngaged = true;
                if (!speedUp.isFlagged()) {
                    speed = speedUp.getSpeed();
                    speedUp.stop();
                    slowDown.start(speed);
                }
                if (!slowDown.isFlagged()) {
                    slowDown.setSpeed(0);
                }
            } else {
                isBrakeEngaged = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometer[0] = lowPass(event.values[0], accelerometer[0]);
            accelerometer[1] = lowPass(event.values[1], accelerometer[1]);
            accelerometer[2] = lowPass(event.values[2], accelerometer[2]);

            linearAcceleration[0] = event.values[0] - accelerometer[0];
            linearAcceleration[1] = event.values[1] - accelerometer[1];
            linearAcceleration[2] = event.values[2] - accelerometer[2];

            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometer, linearAcceleration);
            SensorManager.getOrientation(rotationMatrix, orientation);

            float pitch = round(Math.toDegrees(orientation[1]));
            float roll = round(Math.toDegrees(orientation[2]));

            float rotation;

            if (Math.abs(roll) == 90) {
                rotation = pitch * (90 / Math.abs(roll));
            } else {
                rotation = (float) (pitch * ((90 / Math.abs(roll)) * 0.5));
            }

            if (rotation > 90) {
                rotation = 90;
            } else if (rotation < -90) {
                rotation = -90;
            }

            targetPosition = 90 + round(rotation);
            steer.start(targetPosition);

            if (signalLeft.isSignalling()) {
                if (targetPosition > 135) {
                    isTurning = true;
                }
                if (isTurning) {
                    if (targetPosition < 115) {
                        isTurning = false;
                        signalLeft.toggleSignal();
                        signalLeft.switchOff();
                        signalLeft.stop();
                    }
                }
            } else if (signalRight.isSignalling()) {
                if (targetPosition < 45) {
                    isTurning = true;
                }
                if (isTurning) {
                    if (targetPosition > 65) {
                        isTurning = false;
                        signalRight.toggleSignal();
                        signalRight.switchOff();
                        signalRight.stop();
                    }
                }
            }
        }
    }

    private float lowPass(float current, float last) {
        float a = 0.8f;
        return last * (1.0f - a) + current * a;
    }

    private class leftSignalStateChangeListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            signalLeft.toggleSignal();

            if (signalLeft.isSignalling()) {
                if (signalRight.isSignalling()) {
                    signalRight.toggleSignal();
                    signalRight.switchOff();
                    signalRight.stop();
                }
                signalLeft.start();
            } else {
                signalLeft.switchOff();
                signalLeft.stop();
            }
        }
    }

    private class rightSignalStateChangeListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            signalRight.toggleSignal();

            if (signalRight.isSignalling()) {
                if (signalLeft.isSignalling()) {
                    signalLeft.toggleSignal();
                    signalLeft.switchOff();
                    signalLeft.stop();
                }
                signalRight.start();
            } else {
                signalRight.switchOff();
                signalRight.stop();
            }
        }
    }

    private class hornStateChangeListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            try {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    honk.start();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    honk.stop();
                    horn.setState(false);
                }
            } catch (PhidgetException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    @Override
    protected void onResume() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);

        try {
            while (!accelerate.getAttached() || !brake.getAttached()) { }
            accelerate.setDataInterval(20);
            brake.setDataInterval(20);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            rcServo0.close();
            rcServo1.close();
            rcServo2.close();
            brake.close();
            accelerate.close();
            leftSignal.close();
            rightSignal.close();
            horn.close();
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}