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
package com.wattzap.model.dto;

import com.wattzap.utils.FileName;
import java.io.File;
import javax.swing.ImageIcon;

/**
 *
 * @author Jarek
 */
public class AxisPointInterest extends AxisPoint {
    public static String path;

    private static final int TEXT_WIDTH = 40;

    private String message;
    private String description;
    private String imageName;
    private int imageHeight;
    private int imageWidth;


    public AxisPointInterest(double dist, String message) {
        super(dist);
        this.message = message;
        this.description = null;
        this.imageName = null;
    }
    public AxisPointInterest(double dist) {
        this(dist, null);
        this.message = null;
    }

    public String getMessage() {
        if ((description == null) && (imageName == null)) {
            return message;
        }

        StringBuilder b = new StringBuilder();
        b.append("<html><center>");
        if (message != null) {
            b.append("<font size=\"6\"><b>");
            b.append(message);
            b.append("</b><br /></font>");
        }
        if (imageName != null) {
            b.append("<img src=\"file:///");
            b.append(imageName);
            b.append("\" width=\"" + imageWidth + "\" height=\"" + imageHeight + "\" /><br />");
        }
        if ((description != null) && (!description.isEmpty())) {
            if ((message != null) || (imageName != null)) {
                b.append("<br />");
            }
            b.append("<font size=\"4\" color=\"darkgray\">");
            // split into rows, each ~TEXT_WIDTH.
            String s = description;
            while (s.length() > TEXT_WIDTH) {
                int l;
                for (l = TEXT_WIDTH; l > 0; l--) {
                    if (s.charAt(l - 1) == ' ') {
                        break;
                    }
                }
                int r;
                for (r = TEXT_WIDTH; r <= s.length(); r++) {
                    if (s.charAt(r - 1) == ' ') {
                        break;
                    }
                }
                if ((l == 0) || ((TEXT_WIDTH - l) > (r - TEXT_WIDTH))) {
                    b.append(s.substring(0, r));
                    s = s.substring(r);
                } else {
                    b.append(s.substring(0, l));
                    s = s.substring(l);
                }
                b.append("<br />");
            }
            b.append(s);
            b.append("</font>");
        }
        b.append("</center></html>");
        return b.toString();
    }

    public void setMessage(String name) {
        this.message = name;
    }
    public void setDescription(String descr) {
        if ((descr == null) || (descr.isEmpty())) {
            description = null;
        } else {
            description = descr;
        }
    }
    public void setImage(String name) {
        imageName = null;
        String namePath = FileName.getPath(name);
        if (!namePath.isEmpty()) {
            name = name.substring(namePath.length() + 1);
        }
        String[] directories = new String[] {"/Images/", "/", "/" + namePath + "/"};
        for (String dir : directories) {
            File img = new File(path + dir + name);
            if (img.exists()) {
                imageName = img.getAbsolutePath();
                // check image size.. it must be downscaled often..
                ImageIcon imgIcon = new ImageIcon(imageName);
                imageWidth = imgIcon.getIconWidth();
                imageHeight = imgIcon.getIconHeight();
                if (imageWidth > 350) {
                    imageHeight = (imageHeight * 300) / imageWidth;
                    imageWidth = 300;
                }
                return;
            }
        }
    }

    public void setHtml(String html) {
        int index;
        StringBuilder b = new StringBuilder();
        b.append("<html>");
        while ((index = html.indexOf("<img ")) >= 0) {
            index = html.indexOf("src=\"", index);
            b.append(html.substring(0, index + 5));
            html = html.substring(index + 5);
            index = html.indexOf('\"');
            setImage(html.substring(0, index));
            if (imageName != null) {
                b.append("file:///");
                b.append(imageName);
                imageName = null;
                b.append("\" alt=\"");
            }
        }
        b.append(html);
        b.append("</html>");
        message = b.toString();
    }

    public boolean isUsable() {
        return (message != null) && (!message.isEmpty()) ||
                (imageName != null) || (description != null);
    }

    @Override
    public String toString() {
        return "[Interest(" + getDistance() + ")" +
                (isUsable() ? " name=" + message : "") +
                "]";
    }
}
