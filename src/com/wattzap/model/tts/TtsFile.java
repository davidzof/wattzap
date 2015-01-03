/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.model.tts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author JaroslawP
 */
public class TtsFile {

    private static PrintStream out = System.out;
    private static String currentFile;
    private static int imageId;

    public static void main(String[] args) {
        String[] files = new String[]{
            "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Amstel-Gold07.tts"
        };

        if ((files == null) || (files.length == 0)) {
            files = new String[]{};
            try {
                out = new PrintStream("C:\\Users\\jaroslawp\\Desktop\\tts\\all\\all_tts.txt");
            } catch (Exception e) {
                System.err.println("Cannot create file, " + e);
            }
        }
        for (String file : files) {
            currentFile = file;
            imageId = 0;

            if (out != System.out) {
                System.err.println("File " + file);
            }

            out.println("File " + file);
            TtsFile tts = new TtsFile(file);
            tts.printHeaders();
            out.println("");
        }
    }

    // What does it mean? Any ideas? :P
    // Have they heard about any "more" cryptographic way for protection systems?
    private static String key = "Kermit rules~@!! He is the sAvior of eVery 56987()$*(#& ) needed Pr0t3cTION SYSTEM.JustBESUREto%^m4kethis*$tringl____ng'nuff!";

    private static byte[] rehashKey(String key) {
        byte[] ret = new byte[key.length()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) key.charAt(i);
        }
        return ret;
    }

    private static byte[] xorWithHeader(byte[] data, byte[] header) {
        byte[] result = new byte[data.length];

        int index1 = 0;
        int index2 = 0;
        while (index2 < data.length) {
            result[index2] = (byte) (data[index2] ^ header[index1]);
            index1++;
            if (index1 >= header.length) {
                index1 = 0;
            }
            index2++;
        }
        return result;
    }

    private List<byte[]> content = new ArrayList<>();

    public TtsFile(String fileName) {
        try {
            parseFile(fileName);
        } catch (IOException ex) {
            out.println("Cannot read " + fileName + "::" + ex.getLocalizedMessage());
        } catch (IllegalArgumentException ex) {
            out.println("Wrong file format " + fileName + "::" + ex.getLocalizedMessage());
        }
    }

    private boolean readData(InputStream is, byte[] buffer) throws IOException {
        return is.read(buffer, 0, buffer.length) == buffer.length;
    }

    private static int getUByte(byte[] buffer, int offset) {
        int b = buffer[offset];
        if (b < 0) {
            b += 256;
        }
        return b;
    }

    private static int getUShort(byte[] buffer, int offset) {
        return getUByte(buffer, offset) | (getUByte(buffer, offset + 1) << 8);
    }

    private static int getUInt(byte[] buffer, int offset) {
        return getUShort(buffer, offset) | (getUShort(buffer, offset + 2) << 16);
    }

    private static final Map<Integer, String> fingerprints = new HashMap<>();

    static {
        fingerprints.put(120, "embeded image");

        fingerprints.put(1, ".tts file");
        fingerprints.put(1000, "route");
        fingerprints.put(1030, "training");
        fingerprints.put(1040, "segment");
        fingerprints.put(2000, "description");
        fingerprints.put(4000, "session");
        fingerprints.put(5000, "video");
        fingerprints.put(6000, "infoboxes");
        fingerprints.put(7000, "catalyst");
    }

    private void parseFile(String fileName) throws FileNotFoundException, IOException, IllegalArgumentException {
        byte[] key2 = rehashKey(key);

        InputStream is = new FileInputStream(fileName);
        for (;;) {
            byte[] header = new byte[14];
            if (readData(is, header)) {
                content.add(header);
            } else {
                break;
            }

            if (fingerprints.containsKey(getUShort(header, 2))) {
                content.add(null);
            } else {
                int dataSize = getUInt(header, 6) * getUInt(header, 10);
                byte[] data = new byte[dataSize];
                if (readData(is, data)) {
                    byte[] keyH = xorWithHeader(key2, header);
                    byte[] decrD = xorWithHeader(data, keyH);
                    content.add(decrD);
                } else {
                    throw new IllegalArgumentException("Cannot read " + dataSize + "b data");
                }
            }
        }
        is.close();
    }

    private static String toHex(byte bb) {
        int b = bb;
        if (b < 0) {
            b += 256;
        }
        String s = Integer.toHexString(b);
        if (s.length() == 1) {
            return "0" + s;
        } else {
            return s;
        }
    }

    private static String toDuration(int time) {
        time /= 1000;
        String sec = "" + (time % 60);
        if (sec.length() == 1) {
            sec = "0" + sec;
        }
        time /= 60;
        String min = "" + (time % 60);
        if (min.length() == 1) {
            min = "0" + min;
        }
        time /= 60;
        return "" + time + ":" + min + ":" + sec;
    }

    private static String toDate(byte[] data, int pos) {
        String sec = "" + getUByte(data, pos + 6);
        if (sec.length() == 1) {
            sec = "0" + sec;
        }
        String min = "" + getUByte(data, pos + 5);
        if (min.length() == 1) {
            min = "0" + min;
        }
        return getUShort(data, pos) + "." + getUByte(data, pos + 2) + "." + getUByte(data, pos + 3)
                + " " + getUByte(data, pos + 4) + ":" + min + ":" + sec;
    }

    private static final Map<Integer, String> strings = new HashMap<>();

    static {
        strings.put(1001, "route name");
        strings.put(1002, "route description");
        strings.put(1041, "segment name");
        strings.put(1042, "segment description");
        strings.put(5001, "product id");
        strings.put(5002, "video name");
        // these.. can vary somehow
        strings.put(2001, "company");
        strings.put(2004, "serial");
        strings.put(2005, "time");
        strings.put(2007, "link");
    }

    private interface Formatter {

        String format(int version, byte[] data);
    }

    private static final Map<Integer, String> flags = new HashMap<>();

    static {
        // 0x001??
        flags.put(0x002, "distance"); // training type
        flags.put(0x004, "time");
        flags.put(0x008, "slope"); // program type
        flags.put(0x010, "watts");
        flags.put(0x020, "HR");
        flags.put(0x040, "GPS"); // 5050 block
        flags.put(0x080, "video"); // 5000 block
        flags.put(0x100, "resistance"); // 1050 block
        flags.put(0x200, "infobox"); // 6000 block
        // 0x400??
        flags.put(0x800, "catalyst"); // 7000 block
    }

    private static final Map<Integer, Formatter> formatters = new HashMap<>();
    private static double programCorr = 1.0;
    private static double trainCorr = 100000.0;

    static {
        // general route info. Some ints are always 0.. beer for the one who
        // will guess what does it mean :) My gueses are difficulty and level.
        // Cotacol points, uphill, average/max slopes are computed for
        // each segment.
        formatters.put(1020, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                StringBuilder fl = new StringBuilder();
                int flg = getUInt(data, 0);
                for (Integer key : flags.keySet()) {
                    if (key.equals(flg & key)) {
                        flg &= (~key);
                        fl.append(",");
                        fl.append(flags.get(key));
                    }
                }
                if (fl.length() != 0) {
                    fl.delete(0, 1);
                }
                if (flg != 0) {
                    fl.append(",0x");
                    fl.append(Integer.toHexString(flg));
                }

                return "[training info] "
                        + " flags=" + fl.toString()
                        + " date=" + toDate(data, 4)
                        + " total=" + (getUInt(data, 11) / 100000.0)
                        + " duration=" + toDuration(getUInt(data, 15))
                        + " difficulty=" + getUInt(data, 19)
                        + " altitude=" + (getUInt(data, 23) / 100.0)
                        + " climbing=" + (getUInt(data, 27) / 100000.0)
                        + " level=" + getUInt(data, 31);
            }
        });

        // it looks like part of GENERALINFO, definition of type is included:
        // DWORD WattSlopePulse;//0 = Watt program, 1 = Slope program, 2 = Pulse (HR) program
        // DWORD TimeDist;		//0 = Time based program, 1 = distance based program
        // I'm not sure about the order.. but only slope/distance (0/) and
        //  power/time (1/1) pairs are handled..
        formatters.put(1031, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                String programType;
                switch (data[0]) {
                    case 0:
                        programType = "slope";
                        programCorr = 100.0;
                        break;
                    case 1:
                        programType = "watt";
                        programCorr = 1.0;
                        break;
                    case 2:
                        programType = "heartRate";
                        programCorr = 1.0;
                        break;
                    default:
                        programType = "unknown program";
                        break;
                }
                String trainingType;
                switch (data[1]) {
                    case 0:
                        trainingType = "distance";
                        trainCorr = 100000.0;
                        break;
                    case 1:
                        trainingType = "time";
                        trainCorr = 1000.0;
                        break;
                    default:
                        trainingType = "unknown training";
                        break;
                }
                return "[program type] " + programType + "*" + trainingType;
            }
        });

        // it looks like part of PROGRAM data (record 1020)
        // FLOAT DurationDistance;	//Seconds or metres, depending on program type
        // FLOAT PulseSlopeWatts;	//Pulse, slope or watts data, depending on program type
        // FLOAT RollingFriction;	// Usually 4.0
        // Now it is integer (/100=>[m], /100=>[s]) and short (/100=>[%], [W], probably HR as well..)
        // Value selector is in 1031.
        // Slope /100 is [%], while power is [W] without correction
        formatters.put(1032, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length % 6 != 0) {
                    return null;
                }
                StringBuilder b = new StringBuilder();
                b.append("[" + (data.length / 6) + " program points]");
                for (int i = 0; i < data.length / 6; i++) {
                    int slope = getUShort(data, i * 6);
                    if ((slope & 0x8000) != 0) {
                        slope -= 0x10000;
                    }
                    b.append(" " + (slope / programCorr) + "*" + (getUInt(data, i * 6 + 2) / trainCorr));
                }
                return b.toString();
            }
        });

        // segment range. What is short value in "old" files (always 1?)
        formatters.put(1041, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if ((version == 1104) && (data.length == 8)) {
                    return "[segment range] " + (getUInt(data, 0) / 100000.0) + "-" + (getUInt(data, 4) / 100000.0);
                }
                if ((version == 1000) && (data.length == 10)) {
                    return "[segment range] " + (getUInt(data, 2) / 100000.0) + "-" + (getUInt(data, 6) / 100000.0) + "/" + getUShort(data, 0);
                }
                return null;
            }
        });

        // I though that there is put road surface type (like flat asphalt, mud,
        // pavement, wood, etc), but I was wrong. There is a kind of segment
        // identification, where video speed is quite big (downhills). I assume,
        // that this is used for trainers with "motor" brake for resistance correction
        // (to "enable" helping motor, etc).
        // But why they didn't use slope parameter for this purpose?
        formatters.put(1050, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length % 10 != 0) {
                    return null;
                }
                StringBuilder b = new StringBuilder();
                b.append("[resistance correction]");
                for (int i = 0; i < data.length / 10; i++) {
                    b.append(" [");
                    b.append((getUInt(data, i * 10 + 0) / 100000.0) + "-" + (getUInt(data, i * 10 + 4) / 100000.0));
                    // always byte 6
                    b.append(", resistance=" + getUByte(data, i * 10 + 9));
                    b.append("]");
                }
                return b.toString();
            }
        });
        // 2010 block contains some dates.. What is it for??
        formatters.put(2010, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                StringBuilder b = new StringBuilder();
                if (version == 1003) {
                    // short 1
                    // short 2..
                    b.append(toDate(data, 4));
                    b.append("/");
                    b.append(toDate(data, 11));
                    // byte 0
                }
                if (version == 1102) {
                    // short 1
                    // short 2
                    // short 3
                    b.append(toDate(data, 6));
                    b.append("/");
                    b.append(toDate(data, 13));
                    // short 4
                    // byte 0
                }
                return b.toString();
            }
        });

        // 1 for "plain" RLV, 2 for ERGOs
        formatters.put(5010, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (version == 1004) {
                    switch (data[5]) {
                        case 1:
                            return "[video type] RLV";
                        case 2:
                            return "[video type] ERGO";
                    }
                }
                return null;
            }
        });
        // Distance to frame mapping. But where is FPS?
        formatters.put(5020, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length % 8 != 0) {
                    return null;
                }
                StringBuilder b = new StringBuilder();
                b.append("[" + (data.length / 8) + " video points]");
                for (int i = 0; i < data.length / 8; i++) {
                    b.append(" " + (getUInt(data, i * 8) / 100000.0) + "@" + getUInt(data, i * 8 + 4));
                }
                return b.toString();
            }
        });

        // It screams.. "I'm GPS position!". Distance followed by lat, lon, altitude
        // altitude is first computed on slope/distance (block 1032), and if this
        // message is available, it overrides old one.
        formatters.put(5050, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length % 16 != 0) {
                    return null;
                }
                StringBuilder b = new StringBuilder();
                b.append("[" + (data.length / 16) + " gps points]");
                for (int i = 0; i < data.length / 16; i++) {
                    b.append(" " + (getUInt(data, i * 16) / 100000.0) + "="
                            + Float.intBitsToFloat(getUInt(data, i * 16 + 4)) + "/"
                            + Float.intBitsToFloat(getUInt(data, i * 16 + 8)) + "/"
                            + Float.intBitsToFloat(getUInt(data, i * 16 + 12)));
                }
                return b.toString();
            }
        });
        // which string contains infobox xml (I mean.. tml :) ) Other ones are
        // for images (pairs string + image data in consecutive blocks). TML is
        // usually the last one.
        formatters.put(6020, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                return "[infobox tml] " + getUShort(data, 0);
            }
        });
    }

    private enum DataType {

        NONPRINTABLE,
        BLOCK,
        STRING,
        IMAGE,
        CRC
    };

    public void printHeaders() {
        int bytes = 0;
        boolean header = false;
        DataType dataType = DataType.BLOCK;
        int blockType = -1;
        int headerType = -1;
        int version = -1;
        int stringId = -1;
        int fingerprint = 0;

        for (byte[] data : content) {
            header = !header;
            if (header) {
                String hdr = bytes + " [" + Integer.toHexString(bytes) + "]: "
                        + getUShort(data, 0) + "." + getUShort(data, 2)
                        + " v" + getUShort(data, 4) + " " + getUInt(data, 6) + "x" + getUInt(data, 10);
                out.print(hdr);
                headerType = getUShort(data, 2);
                version = getUShort(data, 4);
                fingerprint = getUInt(data, 6);

                dataType = DataType.NONPRINTABLE;
                switch (headerType) {
                    case 10: // crc of the data?
                        // I don't know how to compute it.. and to which data it belongs
                        // (except images.. for sure it is CRC of file in the block).
                        // for sure I'm not going to check these, I assume file is not broken
                        // (why it can be?)
                        dataType = DataType.CRC;
                        break;
                    case 110: // UTF-16 string
                        dataType = DataType.STRING;
                        stringId = getUShort(data, 0);
                        break;
                    case 120: // image fingerprint
                        stringId = getUShort(data, 0);
                        break;
                    case 121: // imageType? always 01, ignore it
                        break;
                    case 122: // image bytes, name is present in previous string from the block
                        dataType = DataType.IMAGE;
                        break;
                    default:
                        dataType = DataType.BLOCK;
                        blockType = headerType;
                        stringId = -1;
                        break;
                }
            } else {
                if (data == null) {
                    out.println(":: " + fingerprints.get(headerType) + " fingerprint -> " + (bytes + fingerprint));
                } else {
                    out.print("::");

                    String result = null;
                    switch (dataType) {
                        case CRC:
                            out.print("[crc]");
                            break;
                        case IMAGE:
                            out.print("[image " + blockType + "." + stringId + "]");
                            try {
                                result = currentFile + "." + (imageId++) + ".png";
                                FileOutputStream file = new FileOutputStream(result);
                                file.write(data);
                                file.close();
                            } catch (IOException e) {
                                result = "cannot create: " + e;
                            }
                            break;
                        case STRING:
                            if (strings.containsKey(blockType + stringId)) {
                                out.print("[" + strings.get(blockType + stringId) + "]");
                            } else {
                                out.print("[" + blockType + "." + stringId + "]");
                            }
                            StringBuilder str = new StringBuilder();
                            for (int i = 0; i < data.length / 2; i++) {
                                char c = (char) (data[2 * i] | ((int) data[2 * i + 1]) << 8);
                                str.append(c);
                            }
                            result = str.toString();
                            break;
                        case BLOCK:
                            if (formatters.containsKey(blockType)) {
                                result = formatters.get(blockType).format(version, data);
                            }
                            break;
                    }
                    if (result != null) {
                        out.print(" " + result);
                    } else {
                        for (int i = 0; i < data.length; i++) {
                            out.print(" " + toHex(data[i]));
                        }
                    }
                    out.println("");
                }
            }
            if (data != null) {
                bytes += data.length;
            }
        }
        out.println(bytes + " [" + Integer.toHexString(bytes) + "]: end of file");
    }
}
