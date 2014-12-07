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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;

import com.wattzap.controller.MessageBus;
import com.wattzap.controller.MessageCallback;
import com.wattzap.controller.Messages;
import com.wattzap.model.PauseMsgEnum;
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.SourceDataHandlerIntf;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.utils.FileName;
import com.wattzap.utils.Rolling;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

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
public class VideoPlayer extends JFrame
        implements MessageCallback, SourceDataHandlerIntf
{
	private static Logger logger = LogManager.getLogger("Video Player");

    private long len = -1;
    private long lastTime = 0;
	private double lastRate = 0.0;
    private final Rolling routeSpeed = new Rolling(12); // smooth within 3 seconds
    private final Rolling bikeSpeed = new Rolling(12); // smooth within 3 seconds
    private final JLayeredPane lpane;

    private final Component odo;
    private EmbeddedMediaPlayer mPlayer;

    private final JLabel pausePanel;
    private String lastPause;

    private final JLabel infoPanel;
    private long hideTime = -1;
    private Telemetry lastTelemetry = null;

    public VideoPlayer(Component odo, JLabel infoPanel, JLabel pausePanel) {
		super();

		this.odo = odo;
        this.pausePanel = pausePanel;
        this.infoPanel = infoPanel;

		setBounds(UserPreferences.INSTANCE.getVideoBounds());

        Container contentPane = getContentPane();
		contentPane.setBackground(Color.black);
		contentPane.setLayout(new BorderLayout());

        // setLayout(new BorderLayout());
		setVisible(false);


        // super-duper JLayer consumes too much processor, cannot be used..
        lpane = new JLayeredPane();
        lpane.setLayout(new PercentLayout());
        lpane.setPreferredSize(new Dimension(640, 480));
        lpane.setMaximumSize(new Dimension(1920, 1200));
        lpane.setBackground(Color.red);
        // video is put on layer #0, default layer
        lpane.setLayer(infoPanel, 1);
        lpane.setLayer(pausePanel, 2);
        contentPane.add(lpane, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                playVideo(null);
            }
		});

        setTitle("Video - www.WattzAp.com");
		ImageIcon img = new ImageIcon("icons/video.jpg");
		setIconImage(img.getImage());
	}

    @Override
    public void release() {
        // don't deal with freeing canvas/labels/layouts..
        // video player is destroyed at exit, so it is not necessary..

        // remove video if any
        playVideo(null);

        // unregister all messages
        MessageBus.INSTANCE.unregister(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.unregister(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.unregister(Messages.CLOSE, this);
		MessageBus.INSTANCE.unregister(Messages.ROUTE_MSG, this);

        // notify about handler removal
        MessageBus.INSTANCE.send(Messages.HANDLER_REMOVED, this);
    }

    @Override
	public SourceDataHandlerIntf initialize() {
        // canvas with video on layer #0
    	Canvas canvas = new Canvas();
		canvas.setBackground(Color.BLACK);
        canvas.setBounds(0, 0, 100, 200);
        lpane.setLayer(canvas, 0);
		lpane.add(canvas);

        // build media player
        MediaPlayerFactory mediaPlayerFactory;
        mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayerFactory.newVideoSurface(canvas);

		FullScreenStrategy fullScreenStrategy = new DefaultFullScreenStrategy(this);
		mPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(fullScreenStrategy);
		mPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(canvas));

		/* Messages we are interested in */
		MessageBus.INSTANCE.register(Messages.TELEMETRY, this);
		MessageBus.INSTANCE.register(Messages.CLOSE, this);
		MessageBus.INSTANCE.register(Messages.GPXLOAD, this);
		MessageBus.INSTANCE.register(Messages.ROUTE_MSG, this);

        MessageBus.INSTANCE.send(Messages.HANDLER, this);
        return this;
    }

    private synchronized void playVideo(String videoFile) {
        // hide video if was loaded
        if (len > 0) {
            mPlayer.stop();
            mPlayer.enableOverlay(false);
            setVisible(false);

            // store current window size
            Rectangle r = getBounds();
			UserPreferences.INSTANCE.setVideoBounds(r);

            // move ODO to mainWindow
            remove(odo);
            UserPreferences.ODO_VISIBLE.setBool(true);

            // move description to mainWindow, hide if was not shown
            lpane.remove(infoPanel);
            UserPreferences.INFO_VISIBLE.setBool(true);
            if (hideTime < 0) {
                infoPanel.setVisible(false);
            }

            // move pause to mainWindow. Show with warning "closed". On next
            // telemetry it would be updated with proper message (or hidden)
            lpane.remove(pausePanel);
            UserPreferences.PAUSE_VISIBLE.setBool(true);
            lastPause = "Closing..";

            // and hide window itself, no video is shown
            len = -1;
        }

        // show window with new file
        if (videoFile != null) {
            // 1:1 video rate is assumed
            lastRate = -1.0;
            routeSpeed.clear();
            bikeSpeed.clear();

            // The video surface component must be displayable
            setVisible(true);
            mPlayer.enableOverlay(true);
            mPlayer.prepareMedia(videoFile);
            mPlayer.start();

            // check the input file and put everything in window
            len = mPlayer.getLength(); // [ms]
            // hide window if empty video
            if (len <= 0) {
                logger.error("Empty video, cannot start it");
                mPlayer.stop();
                mPlayer.enableOverlay(false);
                setVisible(false);
                return;
            }

            double fps = mPlayer.getFps();
            logger.debug("Video initialize: FPS=" + fps + " len=" + len);

            // TODO don't mute video if POWER training and config is set..
            // Usefull for trainings with "sound guided" stuff (sufferfest,
            // spinnerwalls, etc)
            mPlayer.mute(true);

            // messages (with pictures, etc) on layer #1
            UserPreferences.INFO_VISIBLE.setBool(false);
            lpane.add(infoPanel, "east+2/north+1");
            // show info if was previously shown (in mainForm)
            if (hideTime >= 0) {
                infoPanel.setVisible(true);
            }

            // errors, pause msgs on layer #2, no message is shown
            UserPreferences.PAUSE_VISIBLE.setBool(false);
            lpane.add(pausePanel, "35-65/40-60");
            lastPause = null;

            UserPreferences.ODO_VISIBLE.setBool(false);
            add(odo, BorderLayout.SOUTH);
            odo.setVisible(true);

            // revalidate all componenets in the window
            validate();
        }
        setSpeed(lastTelemetry);
    }

	private synchronized void setSpeed(Telemetry t) {
        // handle description, show panel for 10s
        if ((t != null) && (hideTime == 0)) {
            hideTime = t.getTime() + 10000;
        }
        // hide description panel
        if ((t != null) && (hideTime > 0) && (t.getTime() > hideTime)) {
            setDescription(null);
        }

        boolean changePause = false;
        String pauseMsg = PauseMsgEnum.msg(t);
        // compare references, not objects!
        if (lastPause != pauseMsg) {
            changePause = true;
            lastPause = pauseMsg;
        }

        // it handles pauseMessages on panel.. no matter where it is shown..
        if (changePause) {
            if (pauseMsg != null) {
                pausePanel.setText(pauseMsg);
                pausePanel.setVisible(true);
            } else {
                pausePanel.setVisible(false);
            }
        }

        // there is no video loaded
        if (len < 0) {
            return;
        }

        if (changePause) {
            // pause video if necessary. Sometimes it doesn't pause at once..
            // so operation must be checked several times
            while (mPlayer.isPlaying() == (pauseMsg != null)) {
                mPlayer.setPause(pauseMsg != null);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }
        }

		// check position: time for video position and for gpx must be
        // more or less same. Otherwise resync.
        long deltaTime = 0;
        if ((t != null) && (t.isAvailable(SourceDataEnum.ROUTE_TIME))) {
            long videoTime  = (long) (len * mPlayer.getPosition());
            long routeTime = t.getRouteTime();
            deltaTime = videoTime - routeTime;
            if ((deltaTime < -10000) || (10000 < deltaTime)) {
                logger.warn("Setting expected position " + routeTime +
                        ", while current is " + videoTime);
                mPlayer.setPosition((float) routeTime / (float) len);
                deltaTime = 0;
            }
        }

        double rate = -1.0;
        if ((t != null) && (t.isAvailable(SourceDataEnum.SPEED))) {
            // compute ratio for video
            bikeSpeed.add(t.getSpeed());
            // "advertisments" are ignored, usually they are skipped by
            // setPosition(). Don't include them in average route speed
            if (t.getRouteSpeed() >= 1.0) {
                routeSpeed.add(t.getRouteSpeed());
            }
            rate = bikeSpeed.getAverage() / routeSpeed.getAverage();
            // "add" deltaTime correction. deltaTime is -10..10[s]. Don't increase
            // rate too much, full time "recovery" after 30s (up to 33% speedup)
            rate *= (1.0 - deltaTime / 30000.0);
        }

        /*
        if ((t != null) && (logger.isDebugEnabled())) {
            StringBuilder str = new StringBuilder(200);
            str.append("VideoPlayer");
            str.append(" dist=");
            str.append(SourceDataEnum.DISTANCE.format(t.getDistance(), true));
            str.append(" speed=");
            str.append(SourceDataEnum.SPEED.format(t.getSpeed(), true));
            str.append("/");
            str.append(SourceDataEnum.SPEED.format(bikeSpeed.getAverage(), true));
            str.append(" videoSpeed=");
            str.append(SourceDataEnum.SPEED.format(t.getRouteSpeed(), true));
            str.append("/");
            str.append(SourceDataEnum.SPEED.format(routeSpeed.getAverage(), true));
            str.append(" deltaTime=");
            str.append(SourceDataEnum.RESISTANCE.format(deltaTime, true));
            str.append(" rate=");
            str.append(SourceDataEnum.DISTANCE.format(rate, true));
            str.append(" lastRate=");
            str.append(SourceDataEnum.DISTANCE.format(lastRate, true));
            if (pauseMsg != null) {
                str.append(" pause=");
                str.append(t.getPause());
            }
            logger.debug(str);
        }
        */

        // and finally set new rate if changed much. 20% in change is ok?
        // when setRate is called some artefacts are shown on the screen (some
        // frames are lost or what? synchronization issue in vlc?)
        long currentTime = System.currentTimeMillis();
        if ((rate < 0.001) && (lastRate > 0.001) ||
            changedRate(rate, 0.2) ||
            (currentTime - lastTime > 3000) && changedRate(rate, 0.1) ||
            (currentTime - lastTime > 10000) && changedRate(rate, 0.01))
        {
            lastTime = currentTime;
            lastRate = rate;
            if (rate > 0.0) {
                mPlayer.setRate((float) rate);
            } else {
                mPlayer.setRate((float) 1.0);
            }
        }
	}
    private boolean changedRate(double rate, double change) {
        return ((rate < lastRate / (1.0 + change)) ||
                (rate >= lastRate * (1.0 + change)));
    }

    private synchronized void setDescription(String msg) {
        if ((msg == null) || (msg.isEmpty())) {
            if (hideTime >= 0) {
                hideTime = -1;
                infoPanel.setVisible(false);
            }
        } else {
            infoPanel.setText(msg);
            infoPanel.setVisible(true);
            hideTime = 0;
        }
    }

	@Override
	public void callback(Messages message, Object o) {
		switch (message) {
		case TELEMETRY:
            lastTelemetry = (Telemetry) o;
            setSpeed(lastTelemetry);
			break;

        case GPXLOAD:
            // initialize video on new route
            boolean found = false;
			RouteReader routeData = (RouteReader) o;
            if ((routeData != null) && (routeData.getVideoFile() != null)) {
                String path = FileName.getPath(routeData.getVideoFile());
                String name = FileName.getName(routeData.getVideoFile());
                String ext = FileName.getExtension(name);
                name = FileName.stripExtension(name);

                String[] pathArr;
                if (path.isEmpty()) {
                    pathArr = new String[] {path};
                } else {
                    pathArr = new String[] {path, ""};
                }
                String[] extArr = new String[] {ext, "avi", "mp4", "flv"};
                for (String p : pathArr) {
                    if (found) {
                        break;
                    }
                    for (String e : extArr) {
                        File f = new File(routeData.getPath() + "/" +
                                p + "/" + name + "." + e);
                        if (f.exists()) {
                            playVideo(f.getAbsolutePath());
                            found = true;
                            break;
                        }
                    }
                }
            }
            if (!found) {
                playVideo(null);
            }
			break;

        case CLOSE:
            setDescription(null);
            playVideo(null);
			break;

        case ROUTE_MSG:
            setDescription((String) o);
            break;
        }
	}

    // telemetryHandler to get videoSpeed rate
    @Override
    public String getPrettyName() {
        return "videoPlayer";
    }

    @Override
    public void setPrettyName(String name) {
    }

    @Override
    public boolean provides(SourceDataEnum data) {
        return (data == SourceDataEnum.VIDEO_RATE) && (len > 0) && (lastRate >= 0.0);
    }

    @Override
    public double getValue(SourceDataEnum data) {
        if (data == SourceDataEnum.VIDEO_RATE) {
            return lastRate;
        }
        assert false : "Video Player doesn't provide " + data;
        return 0.0;
    }

    @Override
    public boolean checks(SourceDataEnum data) {
        return false;
    }

    @Override
    public long getModificationTime(SourceDataEnum data) {
        return 0;
    }

    @Override
    public long getLastMessageTime() {
        return -1;
    }
}