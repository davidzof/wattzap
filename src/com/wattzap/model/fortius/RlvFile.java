/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.model.fortius;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jarek
 */
public class RlvFile {

    private Header header = null;
    private RlvInfo fileInfo = null;
    private final List<RlvFrameDistance> distances = new ArrayList<>();
    private final List<RlvInfoBox> infoBoxes = new ArrayList<>();
    private final List<CourseInfo> courseInfos = new ArrayList<>();

    public RlvFile(String fileName) {
        read(new TacxStream(fileName, true));
    }

    public RlvInfo getInfo() {
        return fileInfo;
    }
    public RlvInfoBox getMessage(int p) {
        if ((p < 0) || (p >= infoBoxes.size())) {
            return null;
        }
        return infoBoxes.get(p);
    }

    // longest course, usually called total or so
    public double getCourseDist() {
        double dist = 0.0;
        for (CourseInfo ci : courseInfos) {
            double cd = ci.getEnd() - ci.getStart();
            if (cd > dist) {
                dist = cd;
            }
        }
        return dist;
    }

    public RlvFrameDistance getPoint(int p) {
        if ((p < 0) || (p >= distances.size())) {
            return null;
        }
        return distances.get(p);
    }

    public final void read(TacxStream is) {
        header = new Header(is);
        if (header.getFileFingerprint() != Header.RLV_FINGERPRINT) {
            throw new Error("Wrong Tacx file type " + header.getFileFingerprint());
        }
        for (int i = 0; i < header.getBlockCount(); i++) {
            InfoBlock infoBlock = new InfoBlock(is);

            long before = is.getFilePos();
            switch (infoBlock.getBlockFingerprint()) {
                case InfoBlock.RLV_VIDEO_INFO:
                    if (fileInfo != null) {
                        throw new Error("RlvInfo already found: " + fileInfo);
                    }
                    fileInfo = new RlvInfo(is);
                    break;
                case InfoBlock.RLV_FRAME_DISTANCE_MAPPING:
                    if (!distances.isEmpty()) {
                        throw new Error("RlvFrameDistance already found: " + distances);
                    }
                    for (int j = 0; j < infoBlock.getRecordCount(); j++) {
                        distances.add(new RlvFrameDistance(is));
                    }
                    break;
                case InfoBlock.RLV_INFOBOX:
                    if (!infoBoxes.isEmpty()) {
                        throw new Error("RlvInfoBox already found: " + infoBoxes);
                    }
                    for (int j = 0; j < infoBlock.getRecordCount(); j++) {
                        infoBoxes.add(new RlvInfoBox(is));
                    }
                    break;
                case InfoBlock.COURSE_INFO:
                    if (!courseInfos.isEmpty()) {
                        throw new Error("CourseInfo already found: " + courseInfos);
                    }
                    for (int j = 0; j < infoBlock.getRecordCount(); j++) {
                        courseInfos.add(new CourseInfo(is));
                    }
                    break;
                default:
                    throw new Error(infoBlock + ":: not expected here");
            }
            long read = is.getFilePos() - before;
            if (read != infoBlock.getRecordCount() * infoBlock.getRecordSize()) {
                throw new Error(infoBlock + ":: read " + read + ", while should "
                        + (infoBlock.getRecordCount() * infoBlock.getRecordSize()));
            }
        }
        try {
            is.readByte();
            throw new Error("File contains more data");
        } catch (Error e) {
            // silence.. file is ok
        }
    }

    // test method
    public void print(PrintStream s) {
        int i;
        s.println(header);
        s.println(fileInfo);
        long frame = 0;
        double prev = 0.0;
        double dist = 0.0;
        for (i = 0; i < distances.size(); i++) {
            RlvFrameDistance d = distances.get(i);
            dist += (d.getFrameNumber() - frame) * prev;
            frame = d.getFrameNumber();
            prev = d.getDistancePerFrame();
        }
        s.println("[distances total=" + dist + ", frames=" + frame + "]");
        for (i = 0; i < infoBoxes.size(); i++) {
            s.println("    " + i + ": " + infoBoxes.get(i));
        }
        for (i = 0; i < courseInfos.size(); i++) {
            s.println("    " + i + ": " + courseInfos.get(i));
        }
    }
}
