/*
 */
package com.wattzap.model.fortius;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jarek
 */
public class PgmfFile {
    private Header header = null;
    private GeneralInfo generalInfo = null;
    private List<ProgramData> programData = new ArrayList<>();

    public PgmfFile(String fileName) {
        read(new TacxStream(fileName, true));
    }

    public final void read(TacxStream is) {
        header = new Header(is);
        if (header.getFileFingerprint() != Header.PGMF_FINGERPRINT) {
            throw new Error("Wrong Tacx file type " + header.getFileFingerprint());
        }
        for (int i = 0; i < header.getBlockCount(); i++) {
            InfoBlock infoBlock = new InfoBlock(is);

            long before = is.getFilePos();
            switch (infoBlock.getBlockFingerprint()) {
                case InfoBlock.GENERAL_INFO:
                    if (infoBlock.getRecordCount() != 1) {
                        throw new Error("Multiple " + infoBlock.getRecordCount() +
                                " generalInfo in the block");
                    }
                    if (generalInfo != null) {
                        throw new Error("GeneralInfo " + generalInfo +
                                " already added");
                    }
                    generalInfo = new GeneralInfo(is);
                    break;
                case InfoBlock.PROGRAM_DETAILS:
                    if (!programData.isEmpty()) {
                        throw new Error("programData already found: " + programData);
                    }
                    for (int j=0; j < infoBlock.getRecordCount(); j++) {
                        programData.add(new ProgramData(is));
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
    public GeneralInfo getInfo() {
        return generalInfo;
    }
    public ProgramData getProgramData(int p) {
        if ((p < 0) || (p >= programData.size())) {
            return null;
        }
        return programData.get(p);
    }

    // test method
    public void print(PrintStream s) {
        s.println(header);
        s.println(generalInfo);
        double dist = 0.0;
        double asc = 0.0;
        double desc = 0.0;
        for (int i = 0; i < programData.size(); i++) {
            ProgramData pd = programData.get(i);
            dist += pd.getDurationDistance();
            if (pd.getPulseSlopeWatts() < 0.0) {
                desc -= pd.getDurationDistance() * pd.getPulseSlopeWatts() / 100.0;
            } else {
                asc += pd.getDurationDistance() * pd.getPulseSlopeWatts() / 100.0;
            }
        }
        s.println("Distance=" + dist + ", asc=" + asc + ", desc=" + desc);
    }
}
