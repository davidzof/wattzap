/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tts_files;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public static void main(String[] args) {
        String[] files = new String[]{
            "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Amstel2010.tts",
            "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\AU_Gerlospass08.tts",
            "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\AU_Grossgloc08.tts",
            "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Devils Elbows.tts"
        };

        /*
         try {
         out = new PrintStream("C:\\Users\\jaroslawp\\Desktop\\tts\\all\\all_tts.txt");
         } catch (Exception e) {
         System.err.println("Cannot create file, " + e);
         }
         */
        for (String file : files) {
            if (out != System.out) {
                System.err.println("File " + file);
            }

            out.println("File " + file);
            TtsFile tts = new TtsFile(file);
            tts.printHeaders();
            out.println("");
        }
    }

    private static int[] key = {
        0xD6, 0x9C, 0xD8, 0xBC, 0xDA, 0xA9, 0xDC, 0xB0, 0xDE, 0xB6,
        0xE0, 0x95, 0xE2, 0xC3, 0xE4, 0x97, 0xE6, 0x92, 0xE8, 0x85,
        0xEA, 0x8E, 0xEC, 0x9E, 0xEE, 0x91, 0xF0, 0xB1, 0xF2, 0xD2,
        0xF4, 0xD4, 0xF6, 0xD7, 0xF8, 0xB1, 0xFA, 0x9E, 0xFC, 0xDD,
        0xFE, 0x96, 0x00, 0x72, 0x02, 0x23, 0x04, 0x71, 0x06, 0x6F,
        0x08, 0x6C, 0x0A, 0x2B, 0x0C, 0x7E, 0x0E, 0x4E, 0x10, 0x67,
        0x12, 0x7A, 0x14, 0x7A, 0x16, 0x65, 0x18, 0x39, 0x1A, 0x74,
        0x1C, 0x7B, 0x1E, 0x3F, 0x20, 0x44, 0x22, 0x75, 0x24, 0x40,
        0x26, 0x55, 0x28, 0x50, 0x2A, 0x0B, 0x2C, 0x18, 0x2E, 0x19,
        0x30, 0x08, 0x32, 0x0B, 0x34, 0x02, 0x36, 0x1F, 0x38, 0x10,
        0x3A, 0x1F, 0x3C, 0x17, 0x3E, 0x17, 0x40, 0x62, 0x42, 0x65,
        0x44, 0x65, 0x46, 0x6E, 0x48, 0x69, 0x4A, 0x25, 0x4C, 0x28,
        0x4E, 0x2A, 0x50, 0x35, 0x52, 0x36, 0x54, 0x31, 0x56, 0x77,
        0x58, 0x09, 0x5A, 0x29, 0x5C, 0x6D, 0x5E, 0x2B, 0x60, 0x52,
        0x62, 0x00, 0x64, 0x31, 0x66, 0x2E, 0x68, 0x26, 0x6A, 0x25,
        0x6C, 0x4D, 0x6E, 0x3C, 0x70, 0x28, 0x72, 0x20, 0x74, 0x21,
        0x76, 0x32, 0x78, 0x34, 0x7A, 0x55, 0x7C, 0x37, 0x7E, 0x0A,
        0x80, 0xF2, 0x82, 0xF7, 0x84, 0xC7, 0x86, 0xC2, 0x88, 0xDA,
        0x8A, 0xDE, 0x8C, 0xDF, 0x8E, 0xCA, 0x90, 0xE5, 0x92, 0xFC,
        0x94, 0xB0, 0x96, 0xC9, 0x98, 0xF4, 0x9A, 0xAF, 0x9C, 0xF6,
        0x9E, 0xFA, 0xA0, 0xD5, 0xA2, 0xCB, 0xA4, 0xCC, 0xA6, 0xD4,
        0xA8, 0x83, 0xAA, 0x8F, 0xAC, 0xD9, 0xAE, 0xDD, 0xB0, 0xD8,
        0xB2, 0xDD, 0xB4, 0xD2, 0xB6, 0xDB, 0xB8, 0xE6, 0xBA, 0xE4,
        0xBC, 0xE2, 0xBE, 0xE0, 0xC0, 0xAF, 0xC2, 0xA4, 0xC4, 0xE2,
        0xC6, 0xA9, 0xC8, 0xBC, 0xCA, 0xAD, 0xCC, 0xAB, 0xCE, 0xEE
    };

    private static int uint(byte b) {
        if (b < 0) {
            return (int) b + 256;
        } else {
            return (int) b;
        }
    }

    private static int[] rehashKey(int[] A_0, int A_1) {
        int i;
        char[] chArray1 = new char[A_0.length / 2];
        for (i = 0; i < chArray1.length; i++) {
            chArray1[i] = (char) (A_0[2 * i] + 256 * A_0[2 * i + 1]);
        }

        int num1 = 1000170181 + A_1;
        int num2 = 0;
        int num3 = 1;
        while (num2 < chArray1.length) {
            int index1 = num2;
            char[] chArray2 = chArray1;
            int index2 = index1;
            int num4 = (int) (short) chArray1[index1];
            int num5 = (int) 255;
            int num6 = num4 & num5;
            int num7 = num1;
            int num8 = 1;
            int num9 = num7 + num8;
            byte num10 = (byte) (num6 ^ num7);
            int num11 = 8;
            int num12 = num4 >> num11;
            int num13 = num9;
            int num14 = 1;
            num1 = num13 + num14;
            int num15 = (int) (byte) (num12 ^ num13);
            int num16 = (int) (uint(num10) << 8 | uint((byte) num15)) & 0xffff;
            chArray2[index2] = (char) num16;
            int num17 = 1;
            num2 += num17;
        }

        int[] ret = new int[chArray1.length];
        for (i = 0; i < ret.length; i++) {
            ret[i] = (int) chArray1[i];
        }
        return ret;
    }

    private static int[] encryptHeader(int[] A_0, int[] key2) {
        int[] bytes = key2;
        int[] numArray = new int[bytes.length];
        int index1 = 0;
        int index2 = 0;
        int num = 6;
        while (true) {
            switch (num) {
                case 0:
                    index1 = 0;
                    num = 2;
                    continue;
                case 1:
                    if (index2 < bytes.length) {
                        numArray[index2] = (A_0[index1] ^ bytes[index2]);
                        num = 5;
                        continue;
                    } else {
                        num = 3;
                        continue;
                    }
                case 2:
                    ++index2;
                    num = 4;
                    continue;
                case 3:
                    return numArray;
                case 4:
                case 6:
                    num = 1;
                    continue;
                case 5:
                    if (index1++ >= A_0.length - 1) {
                        num = 0;
                        continue;
                    } else {
                        num = 2;
                        continue;
                    }
                default:
                    throw new IllegalArgumentException("Restart function?");
            }
        }
    }

    private static int[] decryptData(int[] A_0, int[] A_1) {
        int[] numArray = new int[A_0.length];
        int index = 0;
        int num1 = 5;

        int num2 = -1000;
        int e = 0; // set before each block
        while (true) {
            switch (num1) {
                case 0:
                    return numArray;
                case 1:
                    e = 0;
                    num1 = 4;
                    continue;
                case 2:
                    if (index < A_0.length) {
                        numArray[index] = A_1[e] ^ A_0[index];
                        num2 = e++;
                        num1 = 6;
                        continue;
                    } else {
                        num1 = 0;
                        continue;
                    }
                case 3:
                case 5:
                    num1 = 2;
                    continue;
                case 4:
                    ++index;
                    num1 = 3;
                    continue;
                case 6:
                    if (num2 >= A_1.length - 1) {
                        num1 = 1;
                        continue;
                    } else {
                        num1 = 4;
                        continue;
                    }
                default:
                    throw new IllegalArgumentException("Restart function?");
            }
        }
    }

    private static int[] iarr(byte[] a) {
        int[] r = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (int) a[i];
        }
        return r;
    }

    private static byte[] barr(int[] a) {
        byte[] r = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            r[i] = (byte) a[i];
        }
        return r;
    }

    private List<byte[]> content = new ArrayList<>();
    private byte[] pre = new byte[2];

    public TtsFile(String fileName) {
        try {
            parseFile(fileName);
        } catch (IOException ex) {
            out.println("Cannot read " + fileName + "::" + ex.getLocalizedMessage());
        } catch (IllegalArgumentException ex) {
            out.println("Wrong file format " + fileName + "::" + ex.getLocalizedMessage());
        }
    }

    private boolean readData(InputStream is, byte[] buffer, boolean copyPre) throws IOException {
        int first = 0;
        if (copyPre) {
            buffer[0] = pre[0];
            buffer[1] = pre[1];
            first = 2;
        }
        return (is.read(buffer, first, buffer.length - first) == buffer.length - first);
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

    private static String getHex(byte[] buffer, int offset) {
        StringBuilder b = new StringBuilder();
        for (int i = offset; i < buffer.length; i++) {
            b.append(' ');
            b.append(toHex(buffer[i]));
        }
        return b.toString();
    }

    private static boolean isHeader(byte[] buffer) {
        if (buffer.length < 2) {
            return false;
        }
        return getUShort(buffer, 0) <= 20;
    }

    private void parseFile(String fileName) throws FileNotFoundException, IOException, IllegalArgumentException {
        int lastSize = -1;
        InputStream is = new FileInputStream(fileName);
        for (;;) {
            if (!readData(is, pre, false)) {
                break;
            }
            if (isHeader(pre)) {
                byte[] header = new byte[14];
                if (readData(is, header, true)) {
                    content.add(header);
                    lastSize = getUInt(header, 6) * getUInt(header, 10);
                } else {
                    throw new IllegalArgumentException("Cannot read header");
                }

                // one byte data.. unconditionally read as data, noone is able to check it
                if (lastSize < 2) {
                    byte[] data = new byte[lastSize];
                    if (readData(is, data, false)) {
                        content.add(data);
                    } else {
                        throw new IllegalArgumentException("Cannot read " + lastSize + "b data");
                    }
                    lastSize = -1;
                }
            } else {
                if (lastSize < 2) {
                    throw new IllegalArgumentException("Data not allowed, header " + getUShort(pre, 0));
                }
                byte[] data = new byte[lastSize];
                if (readData(is, data, true)) {
                    content.add(data);
                } else {
                    throw new IllegalArgumentException("Cannot read " + lastSize + "b data");
                }
                lastSize = -1;
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

    private static final Map<Integer, String> strings = new HashMap<>();

    static {
        strings.put(1001, "route name");
        strings.put(1002, "route description");
        strings.put(1041, "segment name");
        strings.put(1042, "segment description");
        strings.put(2001, "company");
        strings.put(2004, "serial");
        strings.put(2005, "time");
        strings.put(2007, "link");
        strings.put(5001, "product");
        strings.put(5002, "name");
        strings.put(6001, "infoboxes");
    }

    private interface Formatter {

        String format(int version, byte[] data);
    }
    private static final Map<Integer, Formatter> formatters = new HashMap<>();

    static {
        formatters.put(991032, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < data.length / 6; i++) {
                    b.append("[");
                    b.append(getUInt(data, 6 * i + 2));
                    if (getUShort(data, 6 * i) != 0) {
                        b.append("/");
                        b.append(getUShort(data, 6 * i));
                    }
                    b.append("]");
                }
                return b.toString();
            }
        });

        formatters.put(991010, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                return "[current] " + (getUInt(data, 2) / 100000.0) + "/" + getUShort(data, 0);
            }
        });
        formatters.put(1041, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length != 10) {
                    return null;
                }
                // segment range; 548300 is 5.483km
                return "[segment range] " + (getUInt(data, 2) / 100000.0) + "-" + (getUInt(data, 6) / 100000.0) + "/" + getUShort(data, 0);
            }
        });
        formatters.put(1032, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length % 6 != 0) {
                    return null;
                }
                StringBuilder b = new StringBuilder();
                b.append("[video points]");
                for (int i = 0; i < data.length / 6; i++) {
                    b.append(" [" + i + "="
                            + getUShort(data, i * 6) + "*" + getUInt(data, i * 6 + 2));
                    b.append("]");
                }
                return b.toString();
            }
        });
        formatters.put(1050, new Formatter() {
            @Override
            public String format(int version, byte[] data) {
                if (data.length % 10 != 0) {
                    return null;
                }
                StringBuilder b = new StringBuilder();
                b.append("[segment range]");
                for (int i = 0; i < data.length / 10; i++) {
                    b.append(" [" + i + "="
                            + (getUInt(data, i * 10 + 0) / 100000.0) + "-" + (getUInt(data, i * 10 + 4) / 100000.0));
                    if (getUShort(data, i * 10 + 6) != 0) {
                        b.append("/0x" + Integer.toHexString(getUShort(data, i * 10 + 6)));
                    }
                    b.append("]");
                }
                return b.toString();
            }
        });
    }

    public void printHeaders() {
        int[] key2 = rehashKey(key, 17);
        int[] keyH = null;

        int blockType = -1;
        int stringId = -1;
        int version = -1;

        int fingerprint = 0;
        int bytes = 0;
        for (byte[] data : content) {
            if (isHeader(data)) {
                if (keyH != null) {
                    out.println(":: fingerprint -> " + (bytes + fingerprint));
                }
                String hdr = bytes + " [" + Integer.toHexString(bytes) + "]: "
                        + getUShort(data, 0) + "." + getUShort(data, 2)
                        + " v" + getUShort(data, 4) + " " + getUInt(data, 6) + "x" + getUInt(data, 10);
                out.print(hdr);
                fingerprint = getUInt(data, 6);
                keyH = encryptHeader(iarr(data), key2);

                stringId = -1;
                if (getUShort(data, 2) == 110) {
                    stringId = getUShort(data, 0);
                } else {
                    blockType = getUShort(data, 2);
                    version = getUShort(data, 4);
                }

            } else {

                if (keyH == null) {
                    out.print(bytes + ": duplicated data block");
                }
                int[] decrD = decryptData(iarr(data), keyH);
                keyH = null;
                out.print("::");

                String result = null;
                if (stringId >= 0) {
                    if (strings.containsKey(blockType + stringId)) {
                        out.print("[" + strings.get(blockType + stringId) + "]");
                    } else {
                        out.print("[" + blockType + "." + stringId + "]");
                    }
                    StringBuilder str = new StringBuilder();
                    for (int i = 0; i < decrD.length / 2; i++) {
                        char c = (char) (decrD[2 * i] | (int) decrD[2 * i + 1] << 8);
                        str.append(c);
                    }
                    result = str.toString();
                    stringId = -1;
                } else if (formatters.containsKey(blockType)) {
                    result = formatters.get(blockType).format(version, barr(decrD));
                }
                if (result != null) {
                    out.print(" " + result);
                } else {
                    for (int i = 0; i < decrD.length; i++) {
                        out.print(" " + toHex((byte) decrD[i]));
                    }
                }
                out.println("");
            }
            bytes += data.length;
        }
        out.println(bytes + " [" + Integer.toHexString(bytes) + "]: end of file");
    }
}
