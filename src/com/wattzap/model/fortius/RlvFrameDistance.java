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
public class RlvFrameDistance implements RlvFileBuilderIntf {

    public static final int RECORD_SIZE = 8;

    private long frameNumber;
    private float distancePerFrame;

    public RlvFrameDistance(long fn, float df) {
        this.frameNumber = fn;
        this.distancePerFrame = df;
    }

    public RlvFrameDistance(TacxStream is) {
        frameNumber = is.readUnsignedInt();
        distancePerFrame = is.readFloat();
        is.checkData(RECORD_SIZE, this);
    }

    @Override
    public String toString() {
        return "[frame #" + frameNumber + ", dist=" + distancePerFrame + "]";
    }

    public void store(TacxStream os) {
        os.writeUnsignedInt(frameNumber);
        os.writeFloat(distancePerFrame);
    }

    public float getDistancePerFrame() {
        return distancePerFrame;
    }

    public void setDistancePerFrame(float df) {
        this.distancePerFrame = df;
    }

    public long getFrameNumber() {
        return frameNumber;
    }

    public void setFrameNumber(long fn) {
        this.frameNumber = fn;
    }
}
