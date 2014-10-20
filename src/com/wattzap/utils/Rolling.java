/* This file is part of Wattzap Community Edition.
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
package com.wattzap.utils;

/**
 * Rolling Average Calculator. Don't cumulate values
 * to avoid rounding errors
 *
 * @author David George
 * @date 11 June 2013
 * @author Jarek
 */
public class Rolling {

    private final int size;
    private final double samples[];

    private int count = 0;
    private int index = 0;

    public Rolling(int size) {
        this.size = size;
        samples = new double[size];
    }

    public void clear() {
        index = 0;
        count = 0;
    }

    public double add(double x) {
        samples[index++] = x;
        if (index == size) {
        	index = 0; // cheaper than modulus
        }
        count++;

        return getAverage();
    }

    private double sum(int n) {
        double total = 0.0;
        while (n > 0) {
            total += samples[--n];
        }
        return total;
    }

    public double getAverage() {
        if (count == 0) {
            return 1.0;
        } else if (count < size) {
    		return sum(count) / count;	// while it is filling up initially
    	} else {
    		return sum(size) / size;
    	}
    }
}