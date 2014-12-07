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

import com.wattzap.MsgBundle;
import com.wattzap.model.dto.Telemetry;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jarek
 */
public enum PauseMsgEnum {
    NOT_STARTED(-3, "not_started"),
    STOPPED(-2, "stopped"),
    INITIALIZE(-1, "initialize"),
    // normal training condition
    RUNNING(0, null),
    START(1, "start_training"),
    // speed is zero during normal training
    NO_MOVEMENT(2, "no_movement"),
    // pause button was pressed
    PAUSED(3, "manual_pause"),

    // race preparation, wheelSpeed must be detected
    RACE_PREPARE(10, "race_prepare"),
    // countdown
    RACE_9(20, "9"),
    RACE_8(21, "8"),
    RACE_7(22, "7"),
    RACE_6(23, "6"),
    RACE_5(24, "5"),
    RACE_4(25, "4"),
    RACE_3(26, "3"),
    RACE_2(27, "2"),
    RACE_(28, "1"),
    RACE_START(29, "race_start"),
    // normal race condition, any pause is not allowed
    RACE_RUNNING(30, null),

    // end of training. Set by video handler, cannot be overriden
    END_OF_ROUTE(100, "end_of_route"),

    // selected handler is not created (yet?). Select another options
    // and condition shall be discarded.
    WRONG_SELECTED(300, "check_selected"),
    // some parameters are not set
    NO_FTHR(301, "no_fthr"),
    NO_FTP(302, "no_ftp"),
    // training delivers wrong data (eg. no slope, no video)
    WRONG_TRAINING(303, "wrong_training"),

    // trial expired.. it is not possible to run next trainings..
    TRIAL_EXPIRED(999, "trial_expired");

    private final int val;
    private final String key;
    private String msg;
    private PauseMsgEnum(int val, String key) {
        this.val = val;
        this.key = key;
        this.msg = null;
    }

    public int val() {
        return val;
    }

    public String key() {
        return key;
    }

    // get enum by int
    private static final Map<Integer, PauseMsgEnum> pauseMsgKeys = new HashMap<>();
    static {
        for (PauseMsgEnum en : values()) {
            pauseMsgKeys.put(en.val(), en);
        }
    }
    public static PauseMsgEnum get(int val) {
        return pauseMsgKeys.get(val);
    }

    public static String msg(PauseMsgEnum v) {
        if (v == null) {
            return null;
        }

        String key = v.key();
        if (key == null) {
            return null;
        }
        if (v.msg == null) {
            if (MsgBundle.containsKey(key)) {
                v.msg = MsgBundle.getString(key);
            } else {
                v.msg = key;
            }
        }
        return v.msg;
    }

    public static String msg(Telemetry t) {
        if (t == null) {
            return msg(NOT_STARTED);
        }
        return msg(t.getPause());
    }
}
