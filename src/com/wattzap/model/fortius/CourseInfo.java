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
public class CourseInfo implements RlvFileBuilderIntf {

    private static final int RECORD_SIZE = 596;
    private static final int SEGMENTNAME_LEN = 32; // 66
    private static final int TEXTFILE_LEN = 260; // 522

    private float start;
    private float end;
    private String courseSegmentName;
    private String textFile;

    public CourseInfo(float start, float end, String courseSegmentName, String textFile) {
        this.start = start;
        this.end = end;
        this.courseSegmentName = courseSegmentName;
        this.textFile = textFile;
    }

    public CourseInfo(TacxStream is) {
        this.start = is.readFloat();
        this.end = is.readFloat();
        courseSegmentName = is.readString(SEGMENTNAME_LEN);
        textFile = is.readString(TEXTFILE_LEN);
        is.checkData(RECORD_SIZE, this);
    }

    public void store(TacxStream os) {
        os.writeFloat(start);
        os.writeFloat(end);
        os.writeString(courseSegmentName, SEGMENTNAME_LEN);
        os.writeString(textFile, TEXTFILE_LEN);
    }

    @Override
    public String toString() {
        return "[course \"" + courseSegmentName + "\", " + start + ".." + end + ":: "
                + textFile + "]";
    }

    public String getCourseSegmentName() {
        return courseSegmentName;
    }

    public void setCourseSegmentName(String sn) {
        courseSegmentName = sn;
    }

    public float getEnd() {
        return end;
    }

    public void setEnd(float end) {
        this.end = end;
    }

    public float getStart() {
        return start;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public String getTextFile() {
        return textFile;
    }

    public void setTextFile(String tf) {
        textFile = tf;
    }
}
