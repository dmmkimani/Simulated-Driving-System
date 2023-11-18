package com.dmmk.simulateddrivingsystem;

import com.phidget22.PhidgetException;
import com.phidget22.RCServo;

public class SteerEvent implements Runnable {
    private final RCServo servo;
    private int targetPosition;

    public SteerEvent(RCServo servo) {
        this.servo = servo;
    }

    public void start(int targetPosition) {
        Thread steerEvent = new Thread(this);
        this.targetPosition = targetPosition;
        steerEvent.start();
    }

    public void stop() {
        try {
            servo.setEngaged(false);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            servo.setTargetPosition(targetPosition);
            servo.setEngaged(true);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }
}
