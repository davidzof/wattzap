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

import com.wattzap.model.dto.AxisPoint;

/**
 *
 * @author Jarek
 */
public class AxisPointInterest extends AxisPoint {
    private String name;

    public AxisPointInterest(double dist) {
        super(dist);
        this.name = null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isUsable() {
        return name != null;
    }

    @Override
    public String toString() {
        return "[Interest(" + getDistance() + ")" +
                (isUsable() ? " name=" + name : "") +
                "]";
    }
}
