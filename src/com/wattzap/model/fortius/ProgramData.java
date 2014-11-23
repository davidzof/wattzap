/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.model.fortius;

/**
 *
 * @author Jarek
 */
public class ProgramData implements RlvFileBuilderIntf {

    private static final int RECORD_SIZE = 12;

    private float durationDistance;
    private float pulseSlopeWatts;
    private float rollingFriction;

    public ProgramData(TacxStream is) {
        durationDistance = is.readFloat();
        pulseSlopeWatts = is.readFloat();
        rollingFriction = is.readFloat();
        is.checkData(RECORD_SIZE, this);
    }

    public void store(TacxStream os) {
        os.writeFloat(durationDistance);
        os.writeFloat(pulseSlopeWatts);
        os.writeFloat(rollingFriction);
    }

    @Override
    public String toString() {
        return "[programData distance=" + durationDistance +
                ", slope=" + pulseSlopeWatts + ", friction=" + rollingFriction + "]";
    }

    // getters/setters
    public float getDurationDistance() {
        return durationDistance;
    }
    public void setDurationDistance(float durationDistance) {
        this.durationDistance = durationDistance;
    }

    public float getPulseSlopeWatts() {
        return pulseSlopeWatts;
    }
    public void setPulseSlopeWatts(float pulseSlopeWatts) {
        this.pulseSlopeWatts = pulseSlopeWatts;
    }

    public float getRollingFriction() {
        return rollingFriction;
    }
    public void setCheckSum(float rollingFriction) {
        this.rollingFriction = rollingFriction;
    }
}
