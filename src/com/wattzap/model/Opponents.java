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
import com.wattzap.model.dto.Opponent;
import com.wattzap.model.dto.OpponentData;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.model.dto.TelemetryValidityEnum;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Module that reports wheelSpeed unavailable. It is intended to be used in
 * conjunction with powerSensor only, otherwise training won't run.
 *
 * @author Jarek
 */
@SelectableDataSourceAnnotation
public class Opponents extends TelemetryHandler {
	private static final Logger logger = LogManager.getLogger("Opponents");
    private static Opponents handler = null;

    public static int addOpponent(RouteReader reader) {
        if (handler == null) {
            logger.error("Oponents handler missing");
            return 0;
        }
        return handler.add(reader);
    }
    public static Opponent getOpponent(int id) {
        if (handler == null) {
            logger.error("Oponents handler missing");
            return null;
        }
        return handler.get(id);
    }
    public static boolean removeOpponent(int id) {
        if (handler == null) {
            logger.error("Oponents handler missing");
            return false;
        }
        return handler.remove(id);
    }
    public static boolean removeOpponents() {
        if (handler == null) {
            logger.error("Oponents handler missing");
            return false;
        }
        return handler.removeAll();
    }


    // all opponents: just route readers. Pretty name is uniq identifier
    // of all opponents, simple serial is used here
    private final List<Opponent> opponents = new ArrayList<>();
    private Telemetry telemetry = null;
    // will be used to check if data is available
    private TelemetryValidityEnum validity;
    private OpponentData[] data = null;

    @Override
    public SourceDataHandlerIntf initialize() {
        handler = this;
        telemetry = new Telemetry(PauseMsgEnum.RUNNING);
        telemetry.setSpeed(0.0);
        return super.initialize();
    }

    @Override
    public void release() {
        handler = null;
        super.release();
    }

    @Override
    public String getPrettyName() {
        return "opponents";
    }

   @Override
    public void configChanged(UserPreferences pref) {
        // notify all readers?
    }

    public int add(RouteReader reader) {
        Opponent opponent = new Opponent(reader);
        if (opponent.getDistance(telemetry.getTime()) >= 0.0) {
            opponents.add(opponent);
            sendOpponents();
            return opponent.getId();
        }
        return 0;
    }
    public Opponent get(int id) {
        for (Opponent o : opponents) {
            if (o.getId() == id) {
                return o;
            }
        }
        return null;
    }
    public boolean remove(int id) {
        for (Opponent o : opponents) {
            if (o.getId() == id) {
                if (opponents.remove(o)) {
                    o.setReader(null);
                    sendOpponents();
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }
    public boolean removeAll() {
        if (opponents.isEmpty()) {
            return false;
        }
        for (Opponent opponent : opponents) {
            opponent.setReader(null);
        }
        opponents.clear();
        return true;
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return
                (opponents.size() == 1) &&
                (data == SourceDataEnum.OPPONENT_DIST);
    }

    @Override
    public boolean checks(SourceDataEnum data) {
        return provides(data);
    }

    @Override
    public long getModificationTime(SourceDataEnum data) {
        switch (validity) {
            case NOT_AVAILABLE:
                return -100;
            case TOO_BIG:
                return 1;
            case TOO_SMALL:
                return -1;
            case WRONG:
                return 100;
            default:
                return 0;
        }
    }

    @Override
    public void storeTelemetryData(Telemetry t) {
        telemetry = t;
        sendOpponents();
    }

    private void sendOpponents() {
        validity = TelemetryValidityEnum.NOT_PRESENT;

        // return null data: no opponents to be shown. But what about config?
        // shall they be removed, or kept in the list (without data)?
        if ((!telemetry.isAvailable(SourceDataEnum.SPEED)) && (data != null)) {
            logger.warn("Ridden route without speed, cannot display/check opponents");
            data = null;
            MessageBus.INSTANCE.send(Messages.OPPONENTS, data);
            return;
        }

        List<Opponent> _opponents;
        synchronized(opponents) {
            _opponents = new ArrayList<>(opponents);
        }
        if (!_opponents.isEmpty()) {
            data = new OpponentData[_opponents.size()];
        } else if (data == null) {
            return;
        } else {
            data = null;
        }

        // rider distances, current and after one minute
        double rDist = telemetry.getDistance();
        double mDist = rDist + telemetry.getSpeed() / 60.0;
        for (int op = 0; op < _opponents.size(); op++) {
            Opponent opponent = _opponents.get(op);
            // promote distance.. pause and other values would be calculated
            double oDist = opponent.getDistance(telemetry.getTime());

            // if route finished.. calculate ranking for opponent. This code
            // might give a kindo of race condition for "real" races (via
            // network), so these shall be calculated by server.
            // This is "local" code to compare with ghosts..
            if (opponent.getPause() == PauseMsgEnum.END_OF_ROUTE) {
                // get position in ranking
                int rank = 1;
                for (Opponent opFinish : _opponents) {
                    int r = opFinish.getRank();
                    if ((r != 0) && (r > rank)) {
                        rank = r + 1;
                    }
                }
                opponent.setRank(rank);
            }

            double nDist = oDist + opponent.getSpeed() / 60.0;
            if (opponent.getPause().key() != null) {
                validity = TelemetryValidityEnum.NOT_AVAILABLE;
            } else if ((oDist < rDist) && (nDist > mDist)) {
                validity = TelemetryValidityEnum.TOO_SMALL;
            } else if ((oDist > rDist) && (nDist < mDist)) {
                validity = TelemetryValidityEnum.TOO_BIG;
            } else if (oDist > rDist) {
                validity = TelemetryValidityEnum.WRONG;
            } else  {
                validity = TelemetryValidityEnum.OK;
            }
            data[op] = new OpponentData(opponent, oDist, rDist, validity);
        }
        // if only one opponent.. show "ghost" position in ODO
        if (_opponents.size() == 1) {
            setValue(SourceDataEnum.OPPONENT_DIST, data[0].getDistance());
        }
        MessageBus.INSTANCE.send(Messages.OPPONENTS, data);
    }
}
