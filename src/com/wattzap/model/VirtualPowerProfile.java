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
package com.wattzap.model;

/**
 * Class to handle power profile
 * TODO it shall be an enum value (or shall be created by annotations). Base
 * class must be created with all registrations, etc.
 * @author Jarek
 */
public abstract class VirtualPowerProfile extends TelemetryHandler {
    @Override
    public SourceDataHandlerIntf initialize() {
        super.initialize();

        // config changed is called before handler registration to initialize
        // all properties.. so it must be called once again to proper activate
        // this handler...
        configChanged(UserPreferences.VIRTUAL_POWER);
        return this;
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        // activate/deactivate on virtual power setting
        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.VIRTUAL_POWER)) {
            setActive(prefs.getVirtualPower().findActiveHandler() == this);
        }
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            // main target
            case POWER:
                return true;

            default:
                return false;
        }
    }
}
