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
package com.wattzap.model.ant;

import com.wattzap.PopupMessageIntf;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import com.wattzap.model.SubsystemStateEnum;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.NetworkKey;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.MessageCondition;
import org.cowboycoders.ant.events.MessageConditionFactory;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage.Request;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.cowboycoders.ant.messages.responses.ChannelIdResponse;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.SubsystemIntf;
import com.wattzap.model.SubsystemTypeEnum;
import com.wattzap.model.UserPreferences;
import java.util.ArrayList;
import java.util.List;
import org.cowboycoders.ant.interfaces.AntTransceiver;

/**
 * Handles ANT messages and passes them to apropriate sensors
 * @author Jarek
 */
public class AntSubsystem implements MessageCallback, AntSubsystemIntf {
	private static Logger logger = LogManager.getLogger("Ant");
	public static final Level LOG_LEVEL = Level.SEVERE;

	private static final int ANT_SPORT_FREQ = 57; // 2457MHz

    private final List<Channel> channels = new ArrayList<>();
    private SubsystemStateEnum runLevel;

	private Node node = null;
	private NetworkKey networkKey = null;

    private boolean enabled = false;
    private boolean usbM = false;
    private final PopupMessageIntf popup;


	public AntSubsystem(PopupMessageIntf popup) {
        // subsystem must be initialized in order to run properly
        runLevel = SubsystemStateEnum.NOT_INITIALIZED;
        this.popup = popup;
	}

    @Override
	public SubsystemIntf initialize() {
        MessageBus.INSTANCE.register(Messages.START, this);
		MessageBus.INSTANCE.register(Messages.STOP, this);
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);

        // initialize configuration, both this and delivered classes
        callback(Messages.CONFIG_CHANGED, UserPreferences.INSTANCE);

        // notify all componenet about new subsystem (not initialized yet..)
        runLevel = SubsystemStateEnum.NOT_AVAILABLE;
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);

        logger.debug("All SUBSYSTEM listeners notified");

        // optional: enable console logging with Level = LOG_LEVEL
        setupLogging();
        return this;
	}

	public static void setupLogging() {
		// set logging level
        // AntTransceiver.logger = logger;
		AntTransceiver.LOGGER.setLevel(LOG_LEVEL);
		ConsoleHandler handler = new ConsoleHandler();
		// PUBLISH this level
		handler.setLevel(LOG_LEVEL);
		AntTransceiver.LOGGER.addHandler(handler);
	}

    @Override
    public void release() {
        switch (runLevel) {
            case OPENED:
                close();
                /* no break */
            case CLOSED:
                node = null;
                runLevel = SubsystemStateEnum.NOT_AVAILABLE;
                /* no break */
            case NOT_AVAILABLE:
                MessageBus.INSTANCE.unregister(Messages.CONFIG_CHANGED, this);
                MessageBus.INSTANCE.unregister(Messages.START, this);
                MessageBus.INSTANCE.unregister(Messages.STOP, this);

                runLevel = SubsystemStateEnum.NOT_INITIALIZED;
                MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);
                MessageBus.INSTANCE.send(Messages.SUBSYSTEM_REMOVED, this);
                /* no break */
        }
    }


    /*
     * Request current ID of channel (if channel was created with ID=0)
     * WARNING this function is blocking, it cannot be run in USB thread!
     */
	@Override
    public int getChannelId(Channel channel, AntSensorIntf sensor) {
        if (!channels.contains(channel)) {
            logger.error("Channel is not handled by the subsystem");
            return 0;
        }

        logger.debug("Getting channel ID for " + sensor);
		// build request
		ChannelRequestMessage msg = new ChannelRequestMessage(
				channel.getNumber(), Request.CHANNEL_ID);

		// response should be an instance of ChannelIdResponse
		MessageCondition condition =
                MessageConditionFactory.newInstanceOfCondition(ChannelIdResponse.class);

		try {

			// send request (blocks until reply received or timeout expired)
			ChannelIdResponse response = (ChannelIdResponse)
                    channel.sendAndWaitForMessage(msg, condition, 5L, TimeUnit.SECONDS, null);

			/*
			 * System.out.println();
			 * System.out.println("Device configuration: ");
			 * System.out.println("deviceID: " + response.getDeviceNumber());
			 * System.out.println("deviceType: " + response.getDeviceType());
			 * System.out.println("transmissionType: " + response.getTransmissionType());
			 * System.out.println("pairing flag set: " + response.isPairingFlagSet());
             * System.out.println();
			 */

            logger.debug("Received " + response.getTransmissionType() + "." +
                    response.getDeviceNumber() + ", channel for " + sensor);
            return (response.getTransmissionType() << 16) + response.getDeviceNumber();
		} catch (Exception e) {
			logger.error("exception " + e.getLocalizedMessage());
		}

		return 0;
	}

	public void close() {
        if (runLevel != SubsystemStateEnum.OPENED) {
            logger.error("ANT Subsystem not open");
            return;
        }

        // notify all about subsystem stopped, they should close all channels
        runLevel = SubsystemStateEnum.CLOSED;
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);

        // clean up all channels.. if any left
        if (!channels.isEmpty()) {
            logger.error("Not all ANT sensors were stopped, " + channels.size() + " left");
        }
        while (!channels.isEmpty()) {
            closeChannel(channels.get(0));
        }

        // cleans up : gives up control of usb device etc.
        node.stop();
        logger.debug("ANT subsystem stopped");
    }

	@Override
    public void open() {
        if (runLevel == SubsystemStateEnum.NOT_AVAILABLE) {
            if (!enabled) {
                logger.warn("ANT is disabled, cannot open");
                return;
            }

            try {
                /*
                 * Choose driver: AndroidAntTransceiver or AntTransceiver
                 *
                 * AntTransceiver(int deviceNumber) deviceNumber : 0 ... number of usb
                 * sticks plugged in 0: first usb ant-stick
                 */
                AntTransceiver antChip;
                if (usbM) {
                    antChip = new AntTransceiver(0, AntTransceiver.ANTUSBM_ID);
                } else {
                    antChip = new AntTransceiver(0);
                }
                // initialises node with chosen driver
                node = new Node(antChip);

                // ANT+ key
                networkKey = new NetworkKey(0xB9, 0xA5, 0x21, 0xFB, 0xBD, 0x72, 0xC3, 0x45);
                networkKey.setName("N:ANT+");
                runLevel = SubsystemStateEnum.CLOSED;
            } catch (Exception e) {
                logger.error("ANT+ " + e.getMessage());
            }
        }

        if (runLevel != SubsystemStateEnum.CLOSED) {
            logger.error("ANT Subsystem is " + runLevel + ", cannot open");
            return;
        }

		/* must be called before any configuration takes place */
		node.start();

		/* sends reset request : resets channels to default state */
		node.reset();

        // specs say wait 500ms after reset before sending any more host
		// commands
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {

		}
		// sets network key of network zero
		node.setNetworkKey(0, networkKey);

        // notify all handlers about subsystem ready
        logger.debug("ANT subsystem started");
        runLevel = SubsystemStateEnum.OPENED;
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);
    }

    @Override
	public void callback(Messages message, Object o) {
		switch (message) {
            // only if subsystem switched or subsystem mode changed.
            case CONFIG_CHANGED:
                UserPreferences prefs = (UserPreferences) o;
                if ((prefs.isAntEnabled() != enabled) || (prefs.isAntUSBM() != usbM)) {
                    enabled = prefs.isAntEnabled();
                    usbM = prefs.isAntUSBM();
                    boolean reopen = false;
                    if (runLevel == SubsystemStateEnum.OPENED) {
                        reopen = true;
                        close();
                    }
                    if (runLevel == SubsystemStateEnum.CLOSED) {
                        node = null;
                        runLevel = SubsystemStateEnum.NOT_AVAILABLE;
                    }
                    if (reopen) {
                        open();
                    }
                }
                break;

            case START:
                open();
                break;
            case STOP:
                close();
                break;
		}
	}

    @Override
    public SubsystemTypeEnum getType() {
        return SubsystemTypeEnum.ANT;
    }

    @Override
    public boolean isOpen() {
        return (runLevel == SubsystemStateEnum.OPENED);
    }

    @Override
    public Channel createChannel(AntSensor sensor) {
        logger.debug("Create channel " + sensor);
        // subsystem is closed.. cannot create new channel
        if (!isOpen()) {
            logger.error("Cannot create channel, subsystem not opened");
            return null;
        }

        Channel channel = node.getFreeChannel();
		// Arbitrary name : useful for identifying channel
		channel.setName(sensor.getPrettyName());
		// use ant network key "N:ANT+"
		channel.assign("N:ANT+", new SlaveChannelType());
		// registers an instance of our callback with the channel
		channel.registerRxListener(sensor, BroadcastDataMessage.class);
        // set channel configuration
		channel.setPeriod(sensor.getSensorPeriod());

        // some sensors use transmission type to extend id by 4 bits
        int transmissionType = 0;
        int sensorId = sensor.getSensorId();
        if (sensorId != 0) {
            transmissionType = (
                    (sensor.getTransmissionType() & 0x0f) |
                    ((sensorId >> 12) & 0xf0));
            sensorId &= 0xffff;

        }
        // when pairing flag shall be set?
		channel.setId(sensorId, sensor.getSensorType(), transmissionType, false);
        // set default ANT+ frequency
		channel.setFrequency(ANT_SPORT_FREQ);
		// timeout before we give up looking for device
		channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

		// start listening
		channel.open();
        // keep channel for close operation..
        channels.add(channel);

        //logger.debug("Found id = " + getChannelId(channel, sensor));
        return channel;
    }

    @Override
    public void closeChannel(Channel channel) {
        if (!channels.contains(channel)) {
            logger.error("Channel is not handled by subsystem");
            return;
        }

        logger.debug("Close channel " + channel.getName() + " #" + channel.getNumber());
        channel.close();
        channel.unassign();
        channel.removeAllRxListeners();
        node.freeChannel(channel);
        channels.remove(channel);
    }
}
