/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.model.fortius;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Jarek
 */
public class TacxStream {

    private final InputStream is;
    private final OutputStream os;
    private int number = 0;
    private long filePos = 0;

    public TacxStream(String fileName, boolean input) {
        try {
            if (input) {
                this.os = null;
                this.is = new FileInputStream(fileName);
            } else {
                this.is = null;
                this.os = new FileOutputStream(fileName);
            }
        } catch (FileNotFoundException ex) {
            throw new Error("File not found", ex);
        }
    }

    public long getFilePos() {
        return filePos;
    }

    public void checkData(int bytes, Object o) {
        if (number != bytes) {
            throw new Error(o.getClass().getSimpleName() + " has " + number
                    + ", while should have " + bytes);
        }
        number = 0;
    }

    public int readByte() {
        int b;
        try {
            b = is.read();
        } catch (IOException ex) {
            throw new Error(ex);
        }
        if (b < 0) {
            throw new Error("End of file reached");
        }
        filePos++;
        number++;
        return b;
    }

    public int readShort() {
        return readByte() + (readByte() << 8);
    }

    public int readInt() {
        return readByte() + (readByte() << 8) + (readByte() << 16) + (readByte() << 24);
    }

    public String readString(int maxLen) {
        // TODO String with UTF-8..
        StringBuilder b = new StringBuilder();
        boolean append = true;
        // maxLen + zeroTerminator
        for (int i = 0; i <= maxLen; i++) {
            // file contains UTF-16..
            char c = (char) readShort();
            if (c == 0) {
                append = false;
            }
            if (append) {
                b.append(c);
            }
        }
        return b.toString();
    }

    public long readUnsignedInt() {
        int b1 = readByte();
        int b2 = readByte();
        int b3 = readByte();
        int b4 = readByte();
        long ret = b4;
        ret = (ret << 8) + b3;
        ret = (ret << 8) + b2;
        ret = (ret << 8) + b1;
        return ret;
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }
    public double readDouble() {
        long val = readUnsignedInt() + readUnsignedInt() << 32;
        return Double.longBitsToDouble(val);
    }

    // NOT IMPLEMENTED YET
    public void writeShort(int val) {
    }

    public void writeInt(int val) {
    }
    public void writeUnsignedInt(long val) {
    }

    public void writeString(String val, int maxLen) {
        for (int i = 0; (i < val.length()) && (i < maxLen); i++) {
            writeShort(val.charAt(i));
        }
        writeShort(0);
    }

    public void writeFloat(float val) {
    }
    public void writeDouble(double val) {
    }
}
