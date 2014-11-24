/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.model.fortius;

/**
 *
 * @author JaroslawP
 */
public class RlvInfoBox implements RlvFileBuilderIntf {

    public static final int RECORD_SIZE = 8;

    private long frame; // Frame number
    private int cmd;
    private String message;

    public RlvInfoBox(long fr, int cmd) {
        this.frame = fr;
        this.cmd = cmd;
        this.message = null;
    }

    public RlvInfoBox(TacxStream is) {
        frame = is.readUnsignedInt();
        cmd = is.readInt();
        this.message = null;
        is.checkData(RECORD_SIZE, this);
    }

    public void store(TacxStream os) {
        os.writeUnsignedInt(frame);
        os.writeInt(cmd);
    }

    @Override
    public String toString() {
        if (message != null) {
            return "[info #" + frame + ", cmd=" + cmd + ":: " + message + "]";
        } else {
            return "[info #" + frame + ", cmd=" + cmd + "]";
        }
    }

    public long getFrame() {
        return frame;
    }
    public void setFrame(long fr) {
        this.frame = fr;
    }

    public int getCmd() {
        return cmd;
    }
    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String msg) {
        this.message = msg;
    }
}
