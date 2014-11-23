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
public class GeneralInfo implements RlvFileBuilderIntf {

    private static final int RECORD_SIZE = 70;
    private static final int COURSE_NAME_LEN = 16;

    private long checkSum;
    private String courseName;
    private int wattSlopePulse;
    private int timeDist;
    private double totalTimeDist;
    private double energyCons;
    private float altitudeStart;
    private int brakeCategory;

    public GeneralInfo(TacxStream is) {
        checkSum = is.readUnsignedInt();
        courseName = is.readString(COURSE_NAME_LEN);
        wattSlopePulse = is.readInt();
        timeDist = is.readInt();
        totalTimeDist = is.readDouble();
        energyCons = is.readDouble();
        altitudeStart = is.readFloat();
        brakeCategory = is.readInt();
        is.checkData(RECORD_SIZE, this);
    }

    public void store(TacxStream os) {
         os.writeUnsignedInt(checkSum);
         os.writeString(courseName, COURSE_NAME_LEN);
         os.writeInt(wattSlopePulse);
         os.writeInt(timeDist);
         os.writeDouble(totalTimeDist);
         os.writeDouble(energyCons);
         os.writeFloat(altitudeStart);
         os.writeInt(brakeCategory);
    }

    public String strWattSlopePulse() {
        switch (wattSlopePulse) {
            case 0: return "watt";
            case 1: return "slope";
            case 2: return "pulse";
            default: return "unknown";
        }
    }
    public String strTimeDist() {
        switch (timeDist) {
            case 0: return "time";
            case 1: return "dist";
            default: return "unknown";
        }
    }

    @Override
    public String toString() {
        return "[generalInfo name=\"" + courseName + "\"" +
                ", type=" + strWattSlopePulse() + "+" + strTimeDist() +
                ", total=" + totalTimeDist + ", energy=" + energyCons +
                ", altitude=" + altitudeStart + ", brake=" + brakeCategory + "]";
    }

    // getters/setters
    public long getCheckSum() {
        return checkSum;
    }
    public void setCheckSum(long checkSum) {
        this.checkSum = checkSum;
    }

    public String getCourseName() {
        return courseName;
    }
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public int getWattSlopePulse() {
        return wattSlopePulse;
    }
    public void setWattSlopePulse(int type) {
        this.wattSlopePulse = type;
    }

    public int getTimeDist() {
        return timeDist;
    }
    public void setTimeDist(int type) {
        this.timeDist = type;
    }

    public double getTotalTimeDist() {
        return totalTimeDist;
    }
    public void setTotalTimeDist(double val) {
        this.totalTimeDist = val;
    }

    public double getEnergyCons() {
        return energyCons;
    }
    public void setEnergyCons(double val) {
        this.energyCons = val;
    }

    public float getAltitudeStart() {
        return altitudeStart;
    }
    public void setAltitudeStart(float altitudeStart) {
        this.altitudeStart = altitudeStart;
    }

    public int getBrakeCategory() {
        return brakeCategory;
    }
    public void setBrakeCategory(int cat) {
        this.brakeCategory = cat;
    }
}
