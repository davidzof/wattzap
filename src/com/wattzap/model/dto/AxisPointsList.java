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

import java.util.ArrayList;
import java.util.Comparator;

/**
 * List which handles some distance-related data
 *
 * @author Jarek
 * @param <P>
 */
public class AxisPointsList<P extends AxisPointIntf> extends ArrayList<P> {
    private final Comparator<P> comp =
            new Comparator<P>() {
                @Override
                public int compare(P o1, P o2) {
                    return Double.compare(o1.getDistance(), o2.getDistance());
                }
            };
    private int current = -1;
    private int last = -1;

    public void addAll(P[] tab) {
        for (P point : tab) {
            add(point);
        }
    }

    /**
     * check whole data collection
     * @return error message
     */
    public String checkData() {
        sort(comp);
        P prev = null;
        int p = 0;
        for (P point : this) {
            p++;
            if (prev == null) {
                prev = point;
            } else {
                String ret = prev.checkData(point);
                if (ret != null) {
                    return ret + " at " + point.getDistance() + " [" + p +
                            " of " + size() + "]";
                }
                prev = point;
            }
        }
        return null;
    }

    public void normalize(double totalDist) {
        // nothing to normalize??
        if (size() < 1) {
            return;
        }
        // last point must represent totalDist point
        double ratio = totalDist / get(size() - 1).getDistance();
        if ((ratio < 0.99) || (ratio > 1.01)) {
            System.err.println(get(0).getClass().getSimpleName() +
                    ":: normalization ratio " + ratio);
        }
        for (P p : this) {
            p.normalize(ratio);
        }
    }

    /**
     * Find point for requested distance
     */
    public P get(double dist) {
        last = current;

        // distance doesn't advance.. it jumped back, so find new position
        if (current < 0) {
            current = 0;
        }
        // no more points
        if (current >= size()) {
            return null;
        }
        // distance is back.. Start looking from 0
        if  (get(current).getDistance() > dist) {
            current = 0;
        }

        // look for point just before requested distance
        while ((current + 1 < size()) && (get(current + 1).getDistance() <= dist)) {
			current++;
		}

        if (current < size()) {
			return get(current);
        } else {
            return null;
        }
    }

    /**
     * Check if current point was changed.
     * @return true if next point was just passed
     */
    public boolean isChanged() {
        return last != current;
    }

    /**
     * gets next value from the list, doesn't change current value.
     * @return null if list is shorter.
     */
    public P getNext() {
        if ((current >= 0) && (current + 1 < size())) {
            return get(current + 1);
        } else {
            return null;
        }
    }
    /**
     * Gets current point
     */
    public P get() {
        if ((current >= 0) && (current < size())) {
            return get(current);
        } else {
            return null;
        }
    }

    /**
     * interpolates value for current distance. It cannot be the last value
     * ("next" object must be available). Current and next points must exist!
     * (and it must be checked before.. to get current/next values.)
     */
    public double interpolate(double dist, double current, double next) {
        double cdist = get().getDistance();
        double ndist = getNext().getDistance();
        return current + (next - current) * (dist - cdist) / (ndist - cdist);
    }
}
