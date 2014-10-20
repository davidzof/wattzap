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
package com.wattzap.view;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.RouteReader;
import com.wattzap.model.TelemetryProvider;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.utils.Rolling;
import java.awt.Dimension;
import uk.co.caprica.vlcj.binding.internal.libvlc_marquee_position_e;

/**
 * (c) 2013-2014 David George / TrainingLoops.com
 *
 * Video Player.
 *
 * Synchronizes video playback to road speed. This is done using an SDK to the
 * cross platform VLC player. We can't set the speed frame by frame because it
 * would be too expensive in terms of CPU cycles. What we do is compare the
 * speed from the rider with the speed the video was recorded at and set the
 * playback speed according to this ratio. For example is the rider is doing
 * 10kph and the video was recorded at 20kpg we set the playback speed to 50%.
 *
 * It sounds easy but both the rider speed and video record speed are constantly
 * varying so adjustments have to be made continuously. It is very easy to
 * overshoot hence the different offsets used.
 *
 * @author David George
 * @date 11 June 2013
 *
 * All player activity bases mainly on Telemetry messages. The data is
 * used only for compute current video speed (and location).
 * If route speed is not given, 1:1 ratio is assumed. It is usefull for
 * POWER training, when only RPE/cadence/HR matter.
 *
 * @author Jarek
 */
public class VideoPlayer extends JFrame implements MessageCallback {
	private static Logger logger = LogManager.getLogger("Video Player");

	private final JFrame mainFrame;
	private final JPanel odo;

    private EmbeddedMediaPlayer mPlayer;
	private MediaPlayerFactory mediaPlayerFactory;
	private Canvas canvas;

    private long len = -1;
    private long lastTime = 0;
	private double lastRate = 0.0;
    private Rolling routeSpeed = new Rolling(12); // smooth within 3 seconds
    private Rolling bikeSpeed = new Rolling(12); // smooth within 3 seconds

	public VideoPlayer(JFrame main, JPanel odo) {
		super();

		this.odo = odo;
		this.mainFrame = main;

		setTitle("Video - www.WattzAp.com");
		ImageIcon img = new ImageIcon("icons/video.jpg");
		setIconImage(img.getImage());
	}

	public void init() {
		mediaPlayerFactory = new MediaPlayerFactory();
		canvas = new java.awt.Canvas();
		canvas.setBackground(Color.BLACK);
		this.add(canvas, java.awt.BorderLayout.CENTER);
		mediaPlayerFactory.newVideoSurface(canvas);

		FullScreenStrategy fullScreenStrategy = new DefaultFullScreenStrategy(this);
		mPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(fullScreenStrategy);
		mPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(canvas));

		setBounds(UserPreferences.INSTANCE.getVideoBounds());
		setVisible(false);

		/* Messages we are interested in */
		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.STARTPOS, this);
		MessageBus.INSTANCE.register(Messages.STOP, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
    }

	private void setSpeed(Telemetry t) {
        // there is no video loaded
        if (len < 0) {
            return;
        }

        // initialize on first telemetry
        if (len == 0) {
            lastRate = 1.0;

			mPlayer.enableOverlay(true);
            mPlayer.start();
            mPlayer.setRate((float) lastRate);
            // TODO don't mute video if POWER training and config is set..
            // Usefull for trainings with "sound guided" stuff (sufferfest,
            // spinnerwalls, etc)
			mPlayer.mute();

			len = mPlayer.getLength(); // [ms]
            // if empty video, don't start it again
            if (len == 0) {
                logger.error("Empty video, don't start it");
                len = -1;
                return;
            }

            double fps = mPlayer.getFps();
			logger.debug("Video initialize: FPS=" + fps + " len=" + len);
		}

        // handle pause
        String pauseMsg = TelemetryProvider.INSTANCE.pauseMsg(t);
        if (pauseMsg != null) {
            Dimension size = mPlayer.getVideoDimension();
            if (size != null) {
                mPlayer.setMarqueeSize(size.height / 8);
            } else {
                logger.debug("video has no size");
            }
            mPlayer.setMarqueeText(pauseMsg);
            mPlayer.setMarqueeOpacity(255);
            mPlayer.setMarqueeColour(Color.RED);
            mPlayer.setMarqueePosition(libvlc_marquee_position_e.centre);
            mPlayer.enableMarquee(true);
            mPlayer.setPause(true);
        } else {
            mPlayer.enableMarquee(false);
            mPlayer.setPause(false);
        }

		// check position: time for video position and for gpx must be
        // more or less same. Otherwise resync.
        long videoTime  = (long) (len * mPlayer.getPosition());
        long routeTime = t.getRouteTime();
        long deltaTime = videoTime - routeTime;
        if ((deltaTime < -10000) || (10000 < deltaTime)) {
            logger.warn("Setting expected position " + routeTime +
                    ", while current is " + videoTime);
            mPlayer.setPosition((float) routeTime / (float) len);
            deltaTime = 0;
        }

        // compute ratio for video
        bikeSpeed.add(t.getSpeed());
        // "advertisments" are ignored, usually they are skipped by
        // setPosition(). Don't include them in average route speed
        if (t.getRouteSpeed() >= 1.0) {
            routeSpeed.add(t.getRouteSpeed());
        }
        double rate = bikeSpeed.getAverage() / routeSpeed.getAverage();
        // "add" deltaTime correction. deltaTime is -10..10[s]. Don't increase
        // rate too much, full time "recovery" after 30s (up to 33% speedup)
        rate *= (1.0 - deltaTime / 30000.0);

        logger.debug(String.format(
                "speed=%.1f/%.1f videoSpeed=%.1f/%.1f deltaTime=%d rate=%.3f lastRate=%.3f",
                t.getSpeed(), bikeSpeed.getAverage(),
                t.getRouteSpeed(), routeSpeed.getAverage(),
                deltaTime, rate, lastRate));


        // dont check rate if both last and current are very small..
        // it is only when no movements are detected..
        if ((rate < 0.001) && (lastRate < 0.001)) {
            return;
        }

        // and finally set new rate if changed much. 20% in change is ok?
        // when setRate is called some artefacts are shown on the screen (some
        // frames are lost or what? synchronization issue in vlc?)
        long currentTime = System.currentTimeMillis();
        if (changedRate(rate, 0.2) ||
                (currentTime - lastTime > 5000) && changedRate(rate, 0.1) ||
                (currentTime - lastTime > 10000) && changedRate(rate, 0.05)) {
            logger.debug("Change rate after " + (currentTime - lastTime));
            lastTime = currentTime;
            lastRate = rate;
            mPlayer.setRate((float) rate);
        }
	}
    private boolean changedRate(double rate, double change) {
        return ((rate < lastRate * (1.0 - change)) ||
                (rate >= lastRate * (1.0 + change)));
    }

    private void playVideo(String videoFile) {
        if (videoFile != null) {
            if (!(new File(videoFile)).exists()) {
                videoFile = null;
            }
        }

        // if any video is running..
        if ((len > 0) && (videoFile != null)) {
            playVideo(null);
        }

        // disable video
        if (videoFile == null) {
			Rectangle r = getBounds();
			UserPreferences.INSTANCE.setVideoBounds(r);

            setVisible(false);

            remove(odo);
			revalidate(this);
			mainFrame.add(odo, "cell 0 2, grow");
			revalidate(mainFrame);
            len = -1;
        } else {
            mainFrame.remove(odo);
            mainFrame.repaint();

            add(odo, java.awt.BorderLayout.SOUTH);
            revalidate(this);

            // wait for first telemetry
            mPlayer.enableOverlay(false);
            mPlayer.prepareMedia(videoFile);

            setVisible(true);
            len = 0;
        }
    }

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
            setSpeed((Telemetry) o);
			break;

		case STOP:
			if (len > 0) {
                mPlayer.setPause(true);
            }
			break;

        case GPXLOAD:
            // initialize video on new route
			RouteReader routeData = (RouteReader) o;
            if (routeData != null) {
                playVideo(routeData.getVideoFile());
            }
			break;

        case CLOSE:
            playVideo(null);
			break;
        }
	}

	private void revalidate(JFrame frame) {
		// frame.invalidate();
		frame.validate();
	}
}