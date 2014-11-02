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
public class AxisPointsList<P extends AxisPoint> extends ArrayList<P> {
    private final Comparator<P> comp =
            new Comparator<P>() {
                @Override
                public int compare(P o1, P o2) {
                    return Double.compare(o1.getDistance(), o2.getDistance());
                }
            };
    private int current = 0;
    private int last = 0;

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
        for (P point : this) {
            if (prev == null) {
                prev = point;
            } else {
                // points really close.. Too close
                if (point.getDistance() - prev.getDistance() < 0.1) {
                    return "Distance " + (point.getDistance() - prev.getDistance() +
                            " at " + point.getDistance());
                }
                String ret = prev.checkData(point);
                if (ret != null) {
                    return ret + " at " + point.getDistance();
                }
                prev = point;
            }
        }
        return null;
    }

    /**
     * Find point for requested distance
     */
    public P get(double dist) {
        last = current;

        // distance doesn't advance.. it jumped back, so find new position
        if (get(current).getDistance() > dist) {
            current = 0;
        }

        while ((current + 1 < size()) && (get(current + 1).getDistance() < dist)) {
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
        if (current + 1 < size()) {
            return get(current + 1);
        } else {
            return null;
        }
    }
    /**
     * Gets current point
     */
    public P get() {
        if (current < size()) {
            return get(current);
        } else {
            return null;
        }
    }

    /**
     * interpolates value for current distance. It cannot be the last value
     * ("next" object must be available). Current point had to be found!
     */
    public double interpolate(double dist, double current, double next) {
        double cdist = get(this.current).getDistance();
        double ndist = get(this.current + 1).getDistance();
        return current + (next - current) * (dist - cdist) / (ndist - cdist);
    }
}
