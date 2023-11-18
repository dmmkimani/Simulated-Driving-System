package com.dmmk.simulateddrivingsystem;

import com.phidget22.DigitalOutput;
import com.phidget22.PhidgetException;

public class HonkEvent implements Runnable {
    private final DigitalOutput horn;
    private volatile boolean flag = true;

    public HonkEvent(DigitalOutput horn) {
        this.horn = horn;
    }

    private void honk() {
        try {
            horn.setState(true);
            horn.setState(false);
        } catch (PhidgetException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        Thread honkEvent = new Thread(this);
        flag = false;
        honkEvent.start();
    }

    public void stop() {
        flag = true;
    }

    @Override
    public void run() {
        while (!flag)
            honk();
    }
}

