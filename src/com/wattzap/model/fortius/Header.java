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
public class Header implements RlvFileBuilderIntf {

    public static final int PGMF_FINGERPRINT = 1000; // Catalyst Program (.pgfm)
    public static final int RLV_FINGERPRINT = 2000;  // RLV (.rlv)
    public static final int CAF_FINGERPRINT = 3000;  // Catalyst Workout (.caf)
    public static final int IMF_FINGERPRINT = 4000;  // VR Workout (.imf)

    private static final int RECORD_SIZE = 8;

    private int fileFingerprint;
    private int fileVersion;
    private int blockCount;

    public Header(int ff, int fv, int bc) {
        fileFingerprint = ff;
        fileVersion = fv;
        blockCount = bc;
    }

    public Header(TacxStream is) {
        fileFingerprint = is.readShort();
        fileVersion = is.readShort();
        blockCount = is.readInt();
        is.checkData(RECORD_SIZE, this);
    }

    @Override
    public void store(TacxStream os) {
        os.writeShort(getFileFingerprint());
        os.writeShort(getFileVersion());
        os.writeInt(getBlockCount());
    }

    private String headerType() {
        switch (fileFingerprint) {
            case PGMF_FINGERPRINT:
                return "pgfm";
            case RLV_FINGERPRINT:
                return "rlv";
            case CAF_FINGERPRINT:
                return "caf";
            case IMF_FINGERPRINT:
                return "imf";
            default:
                return "unknown";
        }
    }

    @Override
    public String toString() {
        return "[Header " + headerType() + ", v=" + fileVersion + ", blocks=" + blockCount + "]";
    }

    public int getFileFingerprint() {
        return fileFingerprint;
    }

    public void setFileFingerprint(int ff) {
        fileFingerprint = ff;
    }

    public int getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(int fv) {
        fileVersion = fv;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int bc) {
        blockCount = bc;
    }
}
