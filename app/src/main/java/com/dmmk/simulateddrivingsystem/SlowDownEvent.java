package com.dmmk.simulateddrivingsystem;

import android.util.Log;

import com.phidget22.PhidgetException;
import com.phidget22.RCServo;

import java.util.concurrent.TimeUnit;

public class SlowDownEvent implements Runnable {
    private final RCServo rcServo0;
    private final RCServo rcServo1;
    private int speed;
    private volatile boolean flag = true;

    public SlowDownEvent(RCServo rcServo0, RCServo rcServo1) {
        this.rcServo0 = rcServo0;
        this.rcServo1 = rcServo1;
    }

    public void setSpeed(int speed) { this.speed = speed; }

    public int getSpeed() {
        return speed;
    }

    public boolean isFlagged() {
        return flag;
    }

    private void slowDown() {
        try {
            speed--;
            Log.i("speed", String.valueOf(speed));
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopMotors() {
        try {
            rcServo0.setTargetPosition(0);
            rcServo1.setTargetPosition(0);

            rcServo0.setEngaged(false);
            rcServo1.setEngaged(false);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public void start(int speed) {
        this.speed = speed;
        Thread slowDown = new Thread(this);
        slowDown.start();
        flag = false;
    }

    public void stop() { flag = true; }

    @Override
    public void run() {
        while (!flag)
            if (speed != 0)
                slowDown();
            else
                stopMotors();
    }
}
