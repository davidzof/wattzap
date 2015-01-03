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

        String[] files = null;
        files = new String[]{ //"C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Amstel-Gold07.tts",
        //"C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Apollo Course.tts"
        };

        if ((files == null) || (files.length == 0)) {
            files = new String[]{
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\2008GaviaDemo.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\2008MSR-Demo.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\3 Times 3 Intervals_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\AlpineClas10.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Amstel-Gold07.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Amstel2010.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Apollo Course(Olympus).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Apollo Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Aries Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Aries Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\AU_Gerlospass08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\AU_Grossgloc08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\B_Flanders2007.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\B_GvA-Tilff08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\B_Houffalize-MTB.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\B_Houffalize.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\B_USA-Training.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\CadelDemo2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\CadelEvans08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Callisto Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Callisto Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Cancer Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Cancer Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Carina Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Carina Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Centaurus Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Centaurus Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\CH_Mendrisio-RR.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\CH_Milram-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\CH_Milram-RLV.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Colombiere.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Columba Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Columba Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\conconi-easy.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\conconi-Hard.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\conconi-med.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\cyclingSettings.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Dam Route(Extreme MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Dam Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Devils Elbows.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\DE_Gondelsheim.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\DE_Roth-Tri-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\DE_Roth-Tri-RLV.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\DK_RRWC_2011.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\DK_Taulov_1.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Dorado Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Dorado Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Dordogne.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Draco Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Draco Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\D_DanubeValley.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\D_Schwarzwald.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Emerald Route(Velodrome).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Emerald Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Astana2011-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Astana2011.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_cruzverde.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Formantor.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Grazalema.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Kozontchuk.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_La_Sierra4.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_MajorcaTour (2).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_MajorcaTour.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Orient.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_PuigMajor.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Quickstep2011-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Quickstep2011.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Rabobank08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_SanSalvador.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_SanSebastian.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_SaxoBank2011-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_SaxoBank2011.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Vall-dEbo.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Xorret del CAti.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ES_Zaragoza.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\E_Rabobank2010-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\E_Rabobank2010-RLV.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\FlecheWalloone.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\free ride atlantis.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\free ride callisto.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\free ride extreme mtb.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\free ride mini mtb.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\free ride olympus.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\free ride velodrome.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\FR_Malaucene-Ventoux.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\FR_PyreneesStage.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\FR_Tourmalet-E.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\FR_Ventoux08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\F_Etape2010.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\F_Roubaix.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Gemini Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Gemini Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Glandon-South.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Going Long_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Grossglockner08-Demo.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Heracles Course(Olympus).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Heracles Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Hill Repeats_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IS_Thingvellir.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IS_West.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Eroica.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Gavia08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Kronplatz.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Lampre09.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Lombardy08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Mortirolo08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_SellaRonda.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Stelvio08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_Stelvio2013.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\IT_TorreChia.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Jade Route(Velodrome).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Jade Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\JP_Aso_San.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\KogelBay.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\LaBerarde.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\lAlpedHuez_12-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Lapis Route(Velodrome).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Lapis Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Leo Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Leo Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\L_Schlecks2009-ERGO.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\L_Schlecks2009-RLV.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Madeleine-North.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Malaucene-Ventoux.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Milan-SanRemo08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Motocross Route(Extreme MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Motocross Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Mt Baw Baw.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Nature ride 2(Mini_MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Nature ride 2.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Nature ride(Mini_MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Nature ride.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\NaturePark Route(Extreme MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\NaturePark Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\NO_Vestkapp.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Paris-CityTrip.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Pisces Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Pisces Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\players.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\PL_Dolny_Slask_1.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\PL_Dolny_Slask_2.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Poseidon Course(Olympus).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Poseidon Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Prologue_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Pyramid interval_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Quarry Route(Extreme MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Quarry Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Rolling Hills_19-12-2008 (2).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Rolling Hills_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Rome2010.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Ruby Route (4)(Velodrome).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Ruby Route (4).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Saw mill medium(Mini_MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Saw mill medium.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Saw mill small(Mini_MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Saw mill small.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Scorpio Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Scorpio Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\settings.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Sirius Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Sirius Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Summit Route(Extreme MTB).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Summit Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Taurus Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Taurus Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\teams.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Tempo-Intervals_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Time Trial 40KM_19-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Topaz Route(Velodrome).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Topaz Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Tour de France 2008 Time trail 1_22-12-2008.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Tucano Route(Atlantis).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Tucano Route.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Village Route .tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Village Route(Extreme MTB) .tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Virgo Course(Callisto).tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Virgo Course.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\wiesbaden90k.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\WorldCupMTB-08.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\Xorret_del_CAti.tts",
                "C:\\Users\\jaroslawp\\Desktop\\tts\\all\\ZA_Argus2010.tts"
            };
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

    private static int[] rehashKey(int[] key, int seed) {
        int i;
        char[] chArray1 = new char[key.length / 2];
        for (i = 0; i < chArray1.length; i++) {
            chArray1[i] = (char) (key[2 * i] + 256 * key[2 * i + 1]);
        }

        int num1 = 1000170181 + seed;
        for (int num2 = 0; num2 < chArray1.length; num2++) {
            int num4 = (int) (short) chArray1[num2];
            int num6 = num4 & 0xff;
            byte num10 = (byte) (num6 ^ num1);
            int num12 = num4 >> 8;
            int num13 = num1 + 1;
            num1 = num13 + 1;
            int num15 = (int) (byte) (num12 ^ num13);
            int num16 = (int) (uint(num10) << 8 | uint((byte) num15)) & 0xffff;
            chArray1[num2] = (char) num16;
        }

        int[] ret = new int[chArray1.length];
        for (i = 0; i < ret.length; i++) {
            ret[i] = (int) chArray1[i];
        }
        return ret;
    }

    private static int[] xorWithHeader(int[] data, int[] header) {
        int[] result = new int[data.length];

        int index1 = 0;
        int index2 = 0;
        while (index2 < data.length) {
            result[index2] = data[index2] ^ header[index1];
            index1++;
            if (index1 >= header.length) {
                index1 = 0;
            }
            index2++;
        }
        return result;
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
        int[] key2 = rehashKey(key, 17);

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
                    int[] keyH = xorWithHeader(key2, iarr(header));
                    int[] decrD = xorWithHeader(iarr(data), keyH);
                    content.add(barr(decrD));
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
