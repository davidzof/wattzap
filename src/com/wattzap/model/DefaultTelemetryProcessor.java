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

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.Messages;
import com.wattzap.model.dto.Point;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.power.Power;

/**
 *
 * @author Jarek
 */
public class DefaultTelemetryProcessor extends TelemetryProcessor {
    private double totalWeight = 85.0;
    private int resistance;
    private boolean autoResistance;
    private RouteReader routeData = null;
    private Power power = null;

    @Override
    public String getPrettyName() {
        return "DefaultTelemetryProcessor";
    }

    @Override
    public SourceDataProcessorIntf initialize() {
        super.initialize();
        MessageBus.INSTANCE.register(Messages.GPXLOAD, this);

        // config changed is called before handler registration to initialize
        // all properties.. so it must be called once again to proper activate
        // this handler...
        configChanged(UserPreferences.VIRTUAL_POWER);
        return this;
    }

    @Override
    public void release() {
        MessageBus.INSTANCE.unregister(Messages.GPXLOAD, this);
        super.release();
    }

    @Override
    public void configChanged(UserPreferences prefs) {
        // activate/deactivate on virtual power setting
        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.VIRTUAL_POWER)) {
            setActive(prefs.getVirtualPower().findActiveHandler() == this);
        }
        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.POWER_PROFILE)) {
            power = prefs.getPowerProfile();
        }
        if ((prefs == UserPreferences.INSTANCE) || (prefs == UserPreferences.RESISTANCE)) {
            if (prefs.getResistance() == 0) {
                resistance = 1;
                autoResistance = true;
            } else {
                resistance = prefs.getResistance();
                autoResistance = false;
            }
        }
        // it can be updated every configChanged without checking the property..
        totalWeight = prefs.getTotalWeight();
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        switch (data) {
            // main targets
            case SPEED:
            case POWER:

            // it shall be handled by TrainerHandler!
            case RESISTANCE:

            // these shall be made by RouteHandler!
            case ALTITUDE:
            case SLOPE:
            case LATITUDE:
            case LONGITUDE:
            case PAUSE:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        // We have a time value and rotation value, lets calculate the speed
        int powerWatts = power.getPower(t.getWheelSpeed(), resistance);
        setValue(SourceDataEnum.POWER, powerWatts);

        // if we have GPX Data and Simulspeed is enabled calculate speed
        // based on power and gradient using magic sauce

        // these data shall be moved to routeHandler! Except simulated speed..
        boolean noMoreRoute = true;
        if (routeData != null) {
            // System.out.println("gettng point at distance " + distance);
            Point p = routeData.getPoint(t.getDistance());
            if (p != null) {
                // if slope is known
                if (routeData.routeType() == RouteReader.SLOPE) {
                    double realSpeed = 3.6 * power.getRealSpeed(totalWeight,
                        p.getGradient() / 100, powerWatts);
                    setValue(SourceDataEnum.SPEED, realSpeed);
                    noMoreRoute = false;
                } else {
                    System.out.println("Route type is " + routeData.routeType());
                }

                setValue(SourceDataEnum.ALTITUDE, p.getElevation());
                setValue(SourceDataEnum.SLOPE, p.getGradient());
                setValue(SourceDataEnum.LATITUDE, p.getLatitude());
                setValue(SourceDataEnum.LONGITUDE, p.getLongitude());
            } else {
                System.out.println("No point at " + t.getDistance());
            }
        } else {
            System.out.println("No route data at all!");
        }

        // default resistance taken from preferences
        if (autoResistance) {
            // Best matching (this is wheelSpeed best matches speed) shall be selected
            setValue(SourceDataEnum.RESISTANCE, 1);
        } else {
            // set default resistance..
            setValue(SourceDataEnum.RESISTANCE, resistance);
        }

        // set pause at end of route or when no running, otherwise unpause
        if (noMoreRoute) {
            setValue(SourceDataEnum.PAUSE, 100.0); // end of training
            setValue(SourceDataEnum.SPEED, 0.0);
        } else if (getValue(SourceDataEnum.SPEED) < 0.01) {
            if (t.getTime() == 0) {
                setValue(SourceDataEnum.PAUSE, 1.0); // start training
            } else {
                setValue(SourceDataEnum.PAUSE, 2.0); // keep moving
            }
        } else {
            setValue(SourceDataEnum.PAUSE, 0.0); // running!
        }
    }

    @Override
    public void callback(Messages m, Object o) {
        switch (m) {
            case GPXLOAD:
                routeData = (RouteReader) o;
                /* no break */
            case STARTPOS:
                setValue(SourceDataEnum.PAUSE, 0);
                break;
        }
        super.callback(m, o);
    }
}
