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
package com.wattzap;

import com.wattzap.view.MainFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author Jarek
 */
public class PopupMessage implements PopupMessageIntf {
    private MainFrame frame;

    public PopupMessage(MainFrame frame) {
        this.frame = frame;
    }

    public void showWarning(String src, String msg) {
        JOptionPane.showMessageDialog(frame, msg, src,
            JOptionPane.WARNING_MESSAGE);
    }

    public boolean confirm(String src, String msg) {
        return JOptionPane.showConfirmDialog(frame, msg, src,
                JOptionPane.YES_NO_OPTION) == 0;
    }

    public void showMessage(String src, String msg) {
        JOptionPane.showMessageDialog(frame, msg, src,
                JOptionPane.INFORMATION_MESSAGE);
    }
}
