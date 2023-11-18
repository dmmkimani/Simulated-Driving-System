package com.dmmk.simulateddrivingsystem;

import com.phidget22.DigitalOutput;
import com.phidget22.PhidgetException;

import java.util.concurrent.TimeUnit;

public class SignalEvent implements Runnable {
    private final DigitalOutput signal;
    private boolean isSignalling;
    private volatile boolean flag = false;

    public SignalEvent(DigitalOutput signal) {
        this.signal = signal;
        this.isSignalling = false;
    }

    public boolean isSignalling() {
        return isSignalling;
    }

    public void toggleSignal() {
        this.isSignalling = !isSignalling;
    }

    public void switchOff() {
        try {
            signal.setState(false);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    private void signal() {
        try {
            try {
                signal.setState(true);
                TimeUnit.MILLISECONDS.sleep(1000);
                signal.setState(false);
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (PhidgetException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        Thread signalEvent = new Thread(this);
        flag = false;
        signalEvent.start();
    }

    public void stop() {
        flag = true;
    }

    @Override
    public void run() {
        while (!flag)
            signal();
    }
}
