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
package com.wattzap.view.prefs;

import com.wattzap.model.UserPreferences;

/**
 * General interface for single parameter
 * @author Jarek
 */
public interface ConfigFieldIntf {
    // field name, this is key to get from resource bundle
    String getName();

    // value in the field was changed, it must be set in property
    void fieldChanged();

    // property changed, it must be updated (if not "local" change)
    void propertyChanged(UserPreferences prop, String locallyChanged);

    // field removal
    void remove();
}
