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
package com.wattzap.model.ant;

import java.util.LinkedList;

/**
 * Computes current value from event counter over ticks
 * @author Jarek
 */
public class AntCumulativeComp {
    // number of ticks per [s]
    private final int ticksPerS;

    private final int samples;
    private final int ticksLsb;
    private final int ticksBytes;
    private final int ticksAllowed;
    private final int ticksRollover;
    private final int eventsLsb;
    private final int eventsBytes;
    private final int eventsAllowed;
    private final int eventsRollover;
    private final long lostTime;

    private final LinkedList<Integer> foundTicks = new LinkedList<>();
    private final LinkedList<Integer> foundEvents = new LinkedList<>();
    private int lastTicks;
    private int lastEvents;
    private long lastUpdate;
    private boolean lastValid;

    /**
     *
     * @param samples number of samples to get average value
     * @param ticksLsb first byte of ticks value
     * @param ticksBytes number of bytes to compose events number
     * @param ticksAllowed number of ticks allowed between updates of value.
     * It constraints minimal value as well. For speed 1.8km/h=>0.5m/s=>rotation/4s.
     * So ticks allowed shall be 4096 (4s * 1024)
     * @param eventsLsb first byte of events value
     * @param eventsBytes number of bytes to compose events number
     * @param eventsAllowed number of events allowed within one second.
     * It constraints maximal value. For speed 120km/h=>32m/s=>16rotations/s.
     * Value shall be 16 in this example. It might happend that 5 rotations is
     * recorded in single message, and it constrains speed more (here it would
     * be 100km).
     * @param lostTime time between messages when time must be restarted
     */
    public AntCumulativeComp(
            int ticksLsb, int ticksBytes, int ticksAllowed,
            int eventsLsb, int eventsBytes, int eventsAllowed,
            int samples,
            long lostTime) {
        this.ticksPerS = 1024;
        this.ticksLsb = ticksLsb;
        this.ticksBytes = ticksBytes;
        this.ticksAllowed = ticksAllowed;
        this.ticksRollover = (1 << (8 * ticksBytes));
        this.eventsLsb = eventsLsb;
        this.eventsBytes = eventsBytes;
        this.eventsAllowed = eventsAllowed;
        this.eventsRollover = (1 << (8 * eventsBytes));
        this.lostTime = lostTime;
        this.samples = samples;

        assert (ticksAllowed * samples) < ticksRollover :
                "Wrong number of samples versus ticks, longer than rollover";
        assert (eventsAllowed * samples) < eventsRollover :
                "Wrong number of samples versus events, longer than rollover";
        restart();
    }

    // ticksAllowed equals lost time, data taken from the message
    public AntCumulativeComp(
            int ticksLsb, int ticksBytes, int ticksAllowed,
            int eventsLsb, int eventsBytes, int eventsAllowed,
            int samples) {
        this(ticksLsb, ticksBytes, ticksAllowed,
                eventsLsb, eventsBytes, eventsAllowed,
                samples,
                (1000 * ticksAllowed) / 1024);
    }


    public AntCumulativeComp(
            int ticksPerS,
            int ticksBits, int ticksAllowed,
            int eventsBits, int eventsAllowed,
            int samples) {
        this.ticksPerS = ticksPerS;
        this.ticksAllowed = ticksAllowed;
        this.ticksRollover = (1 << ticksBits);
        this.eventsAllowed = eventsAllowed;
        this.eventsRollover = (1 << eventsBits);
        this.lostTime = (1000 * ticksAllowed) / ticksPerS;
        this.samples = samples;

        // not used, method with (ticks, events) is to be called
        this.ticksLsb = 0;
        this.ticksBytes = 0;
        this.eventsLsb = 0;
        this.eventsBytes = 0;

        restart();
    }

    public String getName() {
        return null;
    }

    public final void restart() {
        lastValid = false;
        lastUpdate = -1;
        foundEvents.clear();
        foundTicks.clear();
    }


    /**
     * Compute value from message
     * @param msgTime message receival time, to "discard" lost messages
     * @param data ANT message
     * @return 0 if no value is computed (no rotations?), or -1 when values are
     * wrong, or number events per second.
     */
    public double compute(long msgTime, int[] data) {
        // check current number of ticks
        int i;
        int ticks = 0;
        for (i = 0; i < ticksBytes; i++) {
            ticks |= (data[ticksLsb + i] << (8 * i));
        }
        // check current number of events
        int events = 0;
        for (i = 0; i < eventsBytes; i++) {
            events |= (data[eventsLsb + i] << (8 * i));
        }
        return compute(msgTime, ticks, events);
    }
    /**
     * Compute data from given  values
     * @param msgTime
     * @param ticks
     * @param events
     * @return
     */
    public double compute(long msgTime, int ticks, int events) {
        String err = null;

        // messages not updated for long time
        if (lastValid && (lastUpdate > 0) && (msgTime > lastUpdate + lostTime)) {
            if (getName() != null) {
                err = "not updated for " + (msgTime - lastUpdate);
            }
            lastValid = false;
        }

        // handle rollover condition. In fact should check whether
        // it is rollover (lastEvents + processing time > rollover?)
        if (lastValid && (ticks < lastTicks)) {
            lastTicks -= ticksRollover;
        }
        // This is intended for checking the "jumps" in the value. It happens
        // that value is smaller (or even zero). Such situations are bogus..
        if (lastValid && (lastUpdate > 0) && (ticks - lastTicks > ticksAllowed)) {
            if (getName() != null) {
                err = "wrong ticks number " + (ticks - lastTicks);
            }
            lastValid = false;
        }

        // handle rollover condition..
        if (lastValid && (events < lastEvents)) {
            lastEvents -= eventsRollover;
        }
        // number of events changed.. but time doesn't advance? what?
        if (lastValid && (events != lastEvents) && (lastTicks == ticks)) {
            if (getName() != null) {
                err = "time did not advance";
            }
            lastValid = false;
        }

        // too many events per second? Worst case is 2 events in single message
        // (with 4Hz ratio) while allowed 4 per second. But such situation is
        // possible only when max speed is 14.4km/h
        // This is intended for checking the "jumps" in the value. It happens
        // that value is smaller (or even zero). Such situations are bogus..
        int allowed = 0;
        if (lastValid) {
            allowed = ((ticks - lastTicks) * eventsAllowed) / ticksPerS;
        }
        if (lastValid && (events - lastEvents > allowed)) {
            if (getName() != null) {
                err = "Too many events " + (events - lastEvents) +
                        " in " + (ticks - lastTicks) +
                        ", allowed are " + allowed;
            }
            lastValid = false;
        }

        double ret;
        if (!lastValid) {
            // something is wrong
            if (getName() != null) {
                if (err != null) {
                    System.err.println(getName() + ":: restart when" +
                            " events:" + events + "-" + lastEvents + "=" + (events - lastEvents) +
                            " ticks:" + ticks + "-" + lastTicks + "=" + (ticks - lastTicks) +
                            " error=" + err);
                } else {
                    System.err.println(getName() + ":: Restart when" +
                            " events:" + events + "-" + lastEvents + "=" + (events - lastEvents) +
                            " ticks:" + ticks + "-" + lastTicks + "=" + (ticks - lastTicks));
                }
            }
            restart();
            ret = -1.0;
        } else if ((ticks > lastTicks) && (events > lastEvents)) {
            // if events was not updated for long time.. Just wait for next
            // update, to ignore previous point (it IS wrong..)
            foundTicks.add(ticks);
            foundEvents.add(events);
            int found = foundTicks.size();
            if (found > 1) {
                if (found > samples + 1) {
                    foundTicks.removeFirst();
                    foundEvents.removeFirst();
                }

                int eventsSum = foundEvents.getLast() - foundEvents.getFirst();
                if (eventsSum < 0) {
                    eventsSum += eventsRollover;
                }
                int ticksSum = foundTicks.getLast() - foundTicks.getFirst();
                if (ticksSum < 0) {
                    ticksSum += ticksRollover;
                }
                ret = (double) ticksPerS * (double) eventsSum / (double) ticksSum;

                if (getName() != null) {
                    System.out.println(getName() + ":: Received change" +
                            " events:" + events + "-" + lastEvents + "=" + (events - lastEvents) +
                            " ticks:" + ticks + "-" + lastTicks + "=" + (ticks - lastTicks) +
                            " sum:" + eventsSum + "/" + ticksSum + "=" + ret);
                }
            } else {
                ret = 0.0;
            }
            lastUpdate = msgTime;
        } else {
            // not changed
            ret = 0.0;
        }

        lastEvents = events;
        lastTicks = ticks;
        lastValid = true;
        return ret;
    }
}
