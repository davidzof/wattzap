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
public class InfoBlock {

    public static final int LAP_DATA = 110;
    public static final int NOTES = 120;
    public static final int UNKNOWN = 130;
    public static final int RIDER_INFO = 210;
    public static final int GENERAL_INFO = 1010;
    public static final int PROGRAM_DETAILS = 1020;
    public static final int RLV_VIDEO_INFO = 2010;
    public static final int RLV_FRAME_DISTANCE_MAPPING = 2020;
    public static final int RLV_INFOBOX = 2030;
    public static final int COURSE_INFO = 2040;
    public static final int RIDE_INFO = 3010;
    public static final int RIDE_DATA = 3020;
    public static final int VR_GENERAL_INFO = 4010;
    public static final int VR_COURSE_DATA = 4020;
    public static final int VR_RIDE_INFO = 4030;
    public static final int VR_RIDE_DATA = 4040;
    public static final int RLV_MULTICOURSE_INFO = 6010;
    public static final int RLV_ITEMMULTISECT = 6020;

    private static final int RECORD_SIZE = 12;
    private int blockFingerprint;//(Unsigned Short) Identifies the data to follow
    private int blockVersion;	 //(Unsigned Short) A 3 digit number indicating the block version, e.g. 100 or 110
    private int recordCount;	 // Number of records in the block
    private int recordSize;		//Size of each record in the block

    public InfoBlock(int bf, int bv, int rc, int s) {
        blockFingerprint = bf;
        blockVersion = bv;
        recordCount = rc;
        recordSize = s;
    }

    public InfoBlock(TacxStream is) {
        blockFingerprint = is.readShort();
        blockVersion = is.readShort();
        recordCount = is.readInt();
        recordSize = is.readInt();
        is.checkData(RECORD_SIZE, this);
    }

    public void persist(TacxStream os) {
        os.writeShort(blockFingerprint);
        os.writeShort(blockVersion);
        os.writeInt(recordCount);
        os.writeInt(recordSize);
    }

    private String blockType() {
        switch (blockFingerprint) {
            case LAP_DATA:
                return "lap_data";
            case NOTES:
                return "notes";
            case UNKNOWN:
                return "unknown";
            case RIDER_INFO:
                return "rider_info";
            case GENERAL_INFO:
                return "general_info_caf";
            case PROGRAM_DETAILS:
                return "program_details";
            case RLV_VIDEO_INFO:
                return "rlv_video_info";
            case RLV_FRAME_DISTANCE_MAPPING:
                return "rlv_frame_distance_mapping";
            case RLV_INFOBOX:
                return "rlv_infobox";
            case COURSE_INFO:
                return "course_info";
            case RIDE_INFO:
                return "ride_info";
            case RIDE_DATA:
                return "ride_data";
            case VR_GENERAL_INFO:
                return "vr_general_info";
            case VR_COURSE_DATA:
                return "vr_course_data";
            case VR_RIDE_INFO:
                return "vr_ride_info";
            case VR_RIDE_DATA:
                return "vr_ride_data";
            case RLV_MULTICOURSE_INFO:
                return "rlv_multicourse_info";
            case RLV_ITEMMULTISECT:
                return "rlv_itemmultisect";
            default:
                return "unknown";
        }
    }

    @Override
    public String toString() {
        return "[InfoBlock " + blockType() + ", version=" + blockVersion
                + ", records=" + recordCount + "x" + recordSize + "]";
    }

    public int getBlockFingerprint() {
        return blockFingerprint;
    }

    public void setBlockFingerprint(int bf) {
        blockFingerprint = bf;
    }

    public int getBlockVersion() {
        return blockVersion;
    }

    public void setBlockVersion(int bv) {
        blockVersion = bv;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int rc) {
        recordCount = rc;
    }

    public int getRecordSize() {
        return recordSize;
    }

    public void setRecordSize(int rs) {
        recordSize = rs;
    }
}
