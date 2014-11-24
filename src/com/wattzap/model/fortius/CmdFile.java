/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wattzap.model.fortius;

import com.wattzap.utils.FileName;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JaroslawP
 */
public class CmdFile {

    private final String parentDir;
    private final List<String> messages = new ArrayList<>();

    public CmdFile(String rlvName) {
        parentDir = FileName.getPath(rlvName);

        String training = FileName.stripExtension(FileName.getName(rlvName));
        String[] names = new String[]{
            training + ".cmd",
            training + ".txt",
            "CmdList.txt",
            "EN/CmdList.txt"};
        for (String name : names) {
            if (readFile(parentDir + "/" + name)) {
                break;
            }
        }
    }

    private final String getToken(String token, String line) {
        while (!line.isEmpty()) {
            int si = line.indexOf("(\"");
            if (si < 0) {
                return null;
            }
            int ei = line.indexOf("\");");
            if (ei < 0) {
                return null;
            }
            if (token.equals(line.substring(0, si))) {
                return line.substring(si + 2, ei);
            }
            line = line.substring(ei + 3);
        }
        return null;
    }

    private final boolean readFile(String fileName) {
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(fileName), "UTF-8"));
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            return false;
        }

        String line;
        try {
            line = br.readLine();
        } catch (IOException io) {
            return false;
        }

        while (line != null) {
            if ((!line.isEmpty()) && (line.charAt(0) != '*')) {
                messages.add(getToken("TEXT", line));
            }
            try {
                line = br.readLine();
            } catch (IOException io) {
                break;
            }
        }
        return true;
    }

    public String getMessage(int i) {
        if ((i < 0) || (i >= messages.size())) {
            return null;
        }
        return messages.get(i);
    }
}
