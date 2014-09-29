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

import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.cowboycoders.ant.Channel;
import org.cowboycoders.ant.NetworkKey;
import org.cowboycoders.ant.Node;
import org.cowboycoders.ant.events.BroadcastListener;
import org.cowboycoders.ant.events.MessageCondition;
import org.cowboycoders.ant.events.MessageConditionFactory;
import org.cowboycoders.ant.interfaces.AntTransceiver;
import org.cowboycoders.ant.messages.ChannelType;
import org.cowboycoders.ant.messages.SlaveChannelType;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage;
import org.cowboycoders.ant.messages.commands.ChannelRequestMessage.Request;
import org.cowboycoders.ant.messages.data.BroadcastDataMessage;
import org.cowboycoders.ant.messages.responses.ChannelIdResponse;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.SensorSubsystemTypeEnum;
import com.wattzap.model.UserPreferences;
import java.util.ArrayList;
import java.util.List;

/**
 * Gets data from Ant device and calculates speed, distance, cadence etc.
 *
 * @author David George
 * @date 30 May 2013
 */
public class Ant implements MessageCallback, AntSensorSubsystem {
    private Channel scChannel = null;
	private Channel hrChannel = null;

	private Node node = null;
	private NetworkKey key = null;

	private BroadcastListener<BroadcastDataMessage> SCListener;
	private BroadcastListener<BroadcastDataMessage> HRListener;

	private UserPreferences userPrefs = UserPreferences.INSTANCE;

	private static final int HRM_CHANNEL_PERIOD = 8070;
	private static final int ANT_SPORT_SPEED_PERIOD = 8086;
	private static final int ANT_SPORT_FREQ = 57; // 0x39

	private static Logger logger = LogManager.getLogger("Ant");

	/*
	 * This should match the device you are connecting with. Some devices are
	 * put into pairing mode (which sets this bit).
	 *
	 * Note: Many ANT+ sport devices do not set this bit (eg. HRM strap).
	 *
	 * See ANT+ docs.
	 */
	private static final boolean PAIRING_FLAG = false;

	/*
	 * Should match device transmission id (0-255). Special rules apply for
	 * shared channels. See ANT+ protocol.
	 *
	 * 0: wildcard, matches any value (slave only)
	 */
	private static final int DEFAULT_TRANSMISSION_TYPE = 1;

	/*
	 * Device type, see ANT+ specs
	 */
	private static final int HRM_DEVICE_TYPE = 120; // 0x78
	private static final int ANT_SPORT_SandC_TYPE = 121; // 0x78

	/*
	 * You should make a note of the device id and use it in preference to the
	 * wild card to pair to a specific device.
	 *
	 * 0: wild card, matches all device ids any other number: match specific
	 * device id
	 */
	// private static final int HRM_DEVICE_ID = 31280;
	// private static final int SC_DEVICE_ID = 26164;

	public static final Level LOG_LEVEL = Level.SEVERE;
    private final List<Channel> channels = new ArrayList<>();
    private boolean running;

	public Ant(BroadcastListener<BroadcastDataMessage> scListener,
			BroadcastListener<BroadcastDataMessage> hrmListener) {
		// optional: enable console logging with Level = LOG_LEVEL
		setupLogging();
		/*
		 * Choose driver: AndroidAntTransceiver or AntTransceiver
		 *
		 * AntTransceiver(int deviceNumber) deviceNumber : 0 ... number of usb
		 * sticks plugged in 0: first usb ant-stick
		 */
		SCListener = scListener;
		HRListener = hrmListener;

        AntTransceiver antchip = null;
        if (userPrefs.isANTUSB()) {
            antchip = new AntTransceiver(0, AntTransceiver.ANTUSBM_ID);
        } else {
            antchip = new AntTransceiver(0);
        }
        // initialises node with chosen driver
        node = new Node(antchip);

        // ANT+ key
        key = new NetworkKey(0xB9, 0xA5, 0x21, 0xFB, 0xBD, 0x72, 0xC3, 0x45);
        key.setName("N:ANT+");

        running = false;

        // new subsystem was added
        MessageBus.INSTANCE.send(Messages.SUBSYSTEM, this);
	}

	public void register() {
        MessageBus.INSTANCE.register(Messages.START, this);
		MessageBus.INSTANCE.register(Messages.STOP, this);
        MessageBus.INSTANCE.register(Messages.CONFIG_CHANGED, this);
	}

	public static void setupLogging() {
		// set logging level
		AntTransceiver.LOGGER.setLevel(LOG_LEVEL);
		ConsoleHandler handler = new ConsoleHandler();
		// PUBLISH this level
		handler.setLevel(LOG_LEVEL);
		AntTransceiver.LOGGER.addHandler(handler);
	}


	public int getHRMChannelId() {
		return getChannelId(hrChannel);
	}

	public int getSCChannelId() {
		return getChannelId(scChannel);
	}

	public int getChannelId(Channel channel) {
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

            return response.getDeviceNumber();
		} catch (Exception e) {
			logger.error(e.getLocalizedMessage());
		}

		return 0;
	}

	public void close() {
        if (!running) {
            return;
        }

        closeChannel(scChannel);
        closeChannel(hrChannel);

        // cleans up : gives up control of usb device etc.
        node.stop();
        running = false;
	}

	public void open(int scID, int hrmID) {
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
		node.setNetworkKey(0, key);



		scChannel = node.getFreeChannel();

		// Arbitrary name : useful for identifying channel
		scChannel.setName("C:SC");

		// choose slave or master type. Constructors exist to set
		// two-way/one-way and shared/non-shared variants.
		ChannelType channelType = new SlaveChannelType();

		// use ant network key "N:ANT+"
		scChannel.assign("N:ANT+", channelType);

		// registers our Heart Rate and Speed and Cadence callbacks with the
		// channel
		scChannel.registerRxListener(SCListener, BroadcastDataMessage.class);

		// ******* start device specific configuration ******
		scChannel.setId(scID, ANT_SPORT_SandC_TYPE, DEFAULT_TRANSMISSION_TYPE,
				PAIRING_FLAG);

		scChannel.setFrequency(ANT_SPORT_FREQ);

		scChannel.setPeriod(ANT_SPORT_SPEED_PERIOD);

		// ******* end device specific configuration ******

		// timeout before we give up looking for device
		scChannel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

		// start listening
		scChannel.open();

		// *** Heart Rate Monitor ***

		hrChannel = node.getFreeChannel();

		// Arbitrary name : useful for identifying channel
		hrChannel.setName("C:HRM");

		ChannelType channelType2 = new SlaveChannelType(); // use ant
															// network key
															// "N:ANT+"
		hrChannel.assign("N:ANT+", channelType2);

		// registers an instance of our callback with the channel
		hrChannel.registerRxListener(HRListener, BroadcastDataMessage.class);

		// start device specific configuration

		hrChannel.setId(hrmID, HRM_DEVICE_TYPE, DEFAULT_TRANSMISSION_TYPE,
				PAIRING_FLAG);

		hrChannel.setFrequency(ANT_SPORT_FREQ);

		hrChannel.setPeriod(HRM_CHANNEL_PERIOD);

		// ******* end device specific configuration

		// timeout before we give up looking for device
		hrChannel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

		// start listening
		hrChannel.open();
        running = true;
	}

    @Override
    public void open() {
        open(userPrefs.getSCId(), userPrefs.getHRMId());
    }

    @Override
	public void callback(Messages message, Object o) {
		switch (message) {
            case CONFIG_CHANGED:
                if (running) {
                    // close if disabled
                }
		case STOP:
			close();
			break;
		case START:
			open(userPrefs.getSCId(), userPrefs.getHRMId());
			break;
		}
	}

    @Override
    public SensorSubsystemTypeEnum getType() {
        return SensorSubsystemTypeEnum.ANT;
    }

    @Override
    public boolean isOpen() {
        return running;
    }

    @Override
    public Channel createChannel(int sensorId, AntSourceDataHandler sensorHandler) {
        // subsystem is closed.. cannot create new channel
        if (node == null) {
            return null;
        }

        Channel channel = node.getFreeChannel();
		// Arbitrary name : useful for identifying channel
		channel.setName(sensorHandler.getSensorName());
		// use ant network key "N:ANT+"
		channel.assign("N:ANT+", new SlaveChannelType());
		// registers an instance of our callback with the channel
		channel.registerRxListener(sensorHandler, BroadcastDataMessage.class);
        // set channel configuration
		channel.setId(sensorId, sensorHandler.getSensorType(), DEFAULT_TRANSMISSION_TYPE, PAIRING_FLAG);
		channel.setFrequency(sensorHandler.getSensorFrequency());
		channel.setPeriod(sensorHandler.getSensorPeriod());
		// timeout before we give up looking for device
		channel.setSearchTimeout(Channel.SEARCH_TIMEOUT_NEVER);

		// start listening
		channel.open();
        // keep channel for close operation..
        channels.add(channel);
        return channel;
    }

    @Override
    public void closeChannel(Channel channel) {
        // if subsystem disabled, all channels were already freed
        if ((node == null) || (channel == null)) {
            return;
        }

        if (channel.isFree()) {
            channel.close();
            channel.unassign();
            node.freeChannel(channel);
        }
        channels.remove(channel);
    }
}
