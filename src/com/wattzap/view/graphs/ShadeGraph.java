/*
 * This file is part of Wattzap Community Edition.
 *
 * Wattzap Community Edtion is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wattzap Community Edition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wattzap.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wattzap.view.graphs;

import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.dto.Telemetry;
import java.awt.Color;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;

/**
 *
 * @author Jarek
 */
public class ShadeGraph {
    private final SourceDataEnum x_val;
    private final double x_res;
    private final SourceDataEnum y_val;
    private final double y_res;

    // computed on telemetry data
    private double min_x;
    private double max_x;
    private double min_y;
    private double max_y;
    private int rows;
    private int cols;
    private double[] table;

    public ShadeGraph(SourceDataEnum x_val, double x_res, SourceDataEnum y_val, double y_res) {
        this.x_val = x_val;
        this.x_res = x_res;
        this.y_val = y_val;
        this.y_res = y_res;
    }

    public void createPixmap(double min_x, double max_x, double min_y, double max_y) {
        this.min_x = min_x;
        this.max_x = max_x;
        this.min_y = min_y;
        this.max_y = max_y;

        // create table enough to keep all the recorded values
        rows = (int) ((max_y - min_y) / y_res) + 1;
        cols = (int) ((max_x - min_x) / x_res) + 1;
        table = new double[rows * cols];
        for (int i = 0; i < rows * cols; i++) {
            table[i] = 0.0;
        }
        System.out.println("Create pixmap " + cols + "x" + rows);
    }

    public void fillWith(List<Telemetry> data, double minTime, double maxTime) {
        minTime *= 1000.0;
        maxTime *= 1000.0;
        double firstTime = 0.0;
        if ((data != null) && (!data.isEmpty())) {
            firstTime = data.get(0).getDouble(SourceDataEnum.TIME);
        }

        if (table == null) {
            boolean first = true;
            for (Telemetry t : data) {
                double time = t.getDouble(SourceDataEnum.TIME) - firstTime;
                if (((minTime > 0) && (time < minTime)) || ((maxTime > 0) && (time > maxTime))) {
                    // not in time range
                } else if (t.isAvailable(x_val) && t.isAvailable(y_val)) {
                    double x = t.getDouble(x_val);
                    double y = t.getDouble(y_val);
                    if ((first) || (min_x > x)) {
                        min_x = x;
                    }
                    if ((first) || (max_x < x)) {
                        max_x = x;
                    }
                    if ((first) || (min_y > y)) {
                        min_y = y;
                    }
                    if ((first) || (max_y < y)) {
                        max_y = y;
                    }
                    first = false;
                }
            }
            createPixmap(min_x, max_x, min_y, max_y);
        }

        // sumarize all times "spent" in points
        double lastTime = -1.0;
        for (Telemetry t : data) {
            double tt = t.getDouble(SourceDataEnum.TIME);
            double dt = (lastTime < 0.0) ? 0.0 : (tt - lastTime);
            lastTime = tt;

            double time = t.getDouble(SourceDataEnum.TIME) - firstTime;
            if (((minTime > 0) && (time < minTime)) || ((maxTime > 0) && (time > maxTime))) {
                // not in a time range
            } else if (t.isAvailable(x_val) && t.isAvailable(y_val)) {
                int row = (int) ((max_y - t.getDouble(y_val)) / y_res);
                int col = (int) ((t.getDouble(x_val) - min_x) / x_res);
                if ((row >= 0) && (row < rows) && (col >= 0) && (col < cols)) {
                    table[row * cols + col] += dt;
                }
            }
        }
    }

    private static void printColor(int col, double bright, PrintStream stream) {
        col = (int) (col * bright);
        if (col < 16) {
            stream.print('0');
        }
        stream.print(Integer.toHexString(col));
    }

    public void createXpm(int numCol, Color color, String file) {
        try {
            createXpm(numCol, color, new PrintStream(file));
        } catch (FileNotFoundException ex) {
            System.err.println("Cannot create " + file);
        }
    }

    public void createXpm(int shades, Color color, PrintStream stream) {
        String subpix = ".,*abcdefghijklmnopqrstuvwxyz@#$%&";
        if (subpix.length() * subpix.length() < shades) {
            throw new IllegalArgumentException("Too many colors requested");
        }
        stream.println("/* XPM */");
        stream.println("static char *shade_xpm[] = {");
        stream.println("/* width height num_colors chars_per_pixel */");
        stream.println("\"" + cols + " " + rows + " " + shades + " 2\",");
        stream.println("/* colors */");

        // create pixel colors tab
        char[] pixels = new char[shades * 2];
        for (int c = 0; c < shades; c++) {
            pixels[2 * c] = subpix.charAt(c / subpix.length());
            pixels[2 * c + 1] = subpix.charAt(c % subpix.length());
            stream.print("\"" + pixels[2 * c] + pixels[2 * c + 1] + " c #");
            printColor(color.getRed(), (double) (c) / (double) shades, stream);
            printColor(color.getGreen(), (double) (c) / (double) shades, stream);
            printColor(color.getBlue(), (double) (c) / (double) shades, stream);
            stream.println("\",");
        }

        double max = 0.0;
        for (int p = 0; p < rows * cols; p++) {
            double val = table[p];
            if (max < val) {
                max = val;
            }
        }
        max = Math.sqrt(max);

        System.out.println("" + x_val + "[" + min_x + ".." + max_x + "]" +
                " " + y_val + "[" + min_y + ".." + max_y + "]" +
                " max=" + max);

        // create all rows
        for (int p = 0; p < rows * cols; p++) {
            if (p % cols == 0) {
                if (p != 0) {
                    stream.println("\",");
                }
                stream.print("\"");
            }
            int c = (int) ((double)(shades - 1) * (Math.sqrt(table[p]) / max));
            stream.print(pixels[2 * c]);
            stream.print(pixels[2 * c + 1]);
        }
        stream.println("\"");
        stream.println("};");
    }
}
