package com.dmmk.simulateddrivingsystem;

import android.util.Log;

import com.phidget22.PhidgetException;
import com.phidget22.RCServo;

import java.util.concurrent.TimeUnit;

public class SpeedUpEvent implements Runnable {
    private final RCServo rcServo0;
    private final RCServo rcServo1;
    private int speed;
    private final int TOP_SPEED = 20;
    private volatile boolean flag = true;

    public SpeedUpEvent(RCServo rcServo0, RCServo rcServo1) {
        this.rcServo0 = rcServo0;
        this.rcServo1 = rcServo1;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isFlagged() {
        return flag;
    }

    private void speedUp() {
        try {
            speed++;
            Log.i("speed", String.valueOf(speed));
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void turnMotors() {
        try {
            rcServo0.setTargetPosition(0);
            rcServo1.setTargetPosition(0);

            rcServo0.setEngaged(true);
            rcServo1.setEngaged(true);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public void start(int speed) {
        this.speed = speed;
        Thread speedUp = new Thread(this);
        speedUp.start();
        flag = false;
    }

    public void stop() { flag = true; }

    @Override
    public void run() {
        turnMotors();
        while (!flag)
            if (speed < TOP_SPEED)
                speedUp();
    }
}
