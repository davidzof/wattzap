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
public class RlvInfo implements RlvFileBuilderIntf {

    private static final int RECORD_SIZE = 534;
    private static final int TEXT_LEN = 260;

    private String videoFileName;//	Location of the RLV AVI file
    private float frameRate; // Video frame rate in fps
    private float orgRunWeight;
    private int frameOffset;

    public RlvInfo(String fn, float fr, float or, int fo) {
        videoFileName = fn;
        frameRate = fr;
        orgRunWeight = or;
        frameOffset = fo;
    }

    public RlvInfo(TacxStream is) {
        videoFileName = is.readString(TEXT_LEN);
        frameRate = is.readFloat();
        orgRunWeight = is.readFloat();
        frameOffset = is.readInt();
        is.checkData(RECORD_SIZE, this);
    }

    public void store(TacxStream os) {
        os.writeString(videoFileName, TEXT_LEN);
        os.writeFloat(frameRate);
        os.writeFloat(orgRunWeight);
        os.writeInt(frameOffset);
    }

    @Override
    public String toString() {
        return "[rlvInfo rate=" + frameRate + ", weight=" + orgRunWeight
                + ", offset=" + frameOffset + "]";
    }

    // getters/setters
    public int getFrameOffset() {
        return frameOffset;
    }

    public void setFrameOffset(int frameOffset) {
        this.frameOffset = frameOffset;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(float frameRate) {
        this.frameRate = frameRate;
    }

    public float getOrgRunWeight() {
        return orgRunWeight;
    }

    public void setOrgRunWeight(float orgRunWeight) {
        this.orgRunWeight = orgRunWeight;
    }

    public String getVideoFileName() {
        return videoFileName;
    }

    public void setVideoFileName(String fn) {
        videoFileName = fn;
    }
}
