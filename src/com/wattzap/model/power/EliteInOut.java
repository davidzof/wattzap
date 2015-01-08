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
package com.wattzap.model.power;

/**
 * Elite crono mag elasto-gel InOut
 *
 * @author Jarek
 */
@PowerAnnotation
public class EliteInOut extends Power {
	private final double[] params = {
        /* L1 */ 3.4137, 6.0147,
        /* L2 */ 6.8164, 20.772,
        /* L3 */ 10.100, 36.083,
        /* L4 */ 13.345, 49.634,
        /* L5 */ 16.679, 63.066,
        /* L6 */ 20.704, 88.255
    };

	public int getPower(double speed, int level) {
		double power = params[2 * level - 2] * speed + params[2 * level - 1];
		if (power < 0) {
			return 0;
		}
		return (int) power;
	}

	public double getSpeed(int power, int level) {
		double speed = (power - params[2 * level - 1]) / params[2 * level - 2];
		if (speed < 0) {
			return 0.0;
		}
		return speed;
	}

	@Override
	public int getResitanceLevels() {
		return params.length / 2;
	}

	public String description() {
		return "Elite Crono InOut Elastogel";
	}
}
