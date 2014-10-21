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

import com.wattzap.model.UserPreferences;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 *
 * @author Jarek
 */
public class MsgBundle {
	private static final Logger logger = LogManager.getLogger("MsgBundle");
	private static ResourceBundle messages = null;

    // TODO add "anonymous" configChanged listener

    public static String getString(String key) {
        if (messages == null) {
    		messages = ResourceBundle.getBundle("MessageBundle",
                    UserPreferences.LANG.getLocale());
        }
        try {
            return messages.getString(key);
        } catch (MissingResourceException mr) {
            logger.error("Cannot find resource for " + key, mr);
            return key;
        }
    }

    public static boolean containsKey(String key) {
        if (messages == null) {
    		messages = ResourceBundle.getBundle("MessageBundle",
                    UserPreferences.LANG.getLocale());
        }
        return messages.containsKey(key);
    }
}
