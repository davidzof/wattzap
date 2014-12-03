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
import com.wattzap.model.PauseMsgEnum;
import com.wattzap.model.RouteReader;
import com.wattzap.model.SourceDataEnum;
import com.wattzap.model.SourceDataHandlerIntf;
import com.wattzap.model.UserPreferences;
import com.wattzap.model.dto.Telemetry;
import com.wattzap.utils.FileName;
import com.wattzap.utils.Rolling;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

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

    private static final int style = Font.BOLD;
    private static final Font pauseFont = new Font("Arial", style, 30);
    private static final Font descrFont = new Font("Arial", style, 20);
	private static final Color skyBlue = new Color(0, 154, 237);
    private static final Border bevelWithSpace = BorderFactory.createCompoundBorder(
            BorderFactory.createBevelBorder(BevelBorder.RAISED),
            BorderFactory.createEmptyBorder(5, 5, 5, 5));

    private long len = -1;
    private long lastTime = 0;
	private double lastRate = 0.0;
    private Rolling routeSpeed = new Rolling(12); // smooth within 3 seconds
    private Rolling bikeSpeed = new Rolling(12); // smooth within 3 seconds

    private final JPanel odo;
    private EmbeddedMediaPlayer mPlayer;

    private JLabel pausePanel;
    private String lastPause;

    private JLabel descrPanel;
    private long hideTime;
    private Telemetry lastTelemetry = null;

    public VideoPlayer(JPanel odo) {
		super();

		this.odo = odo;

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

	public SourceDataHandlerIntf initialize() {
		addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                playVideo(null);
            }
		});

        setLayout(new BorderLayout());
		setBounds(UserPreferences.INSTANCE.getVideoBounds());
		setVisible(false);

        // super-duper JLayer consumes too much processor, cannot be used..
        JLayeredPane lpane = new JLayeredPane();
        lpane.setLayout(new PercentLayout());
        add(lpane, BorderLayout.CENTER);

        // canvas with video on layer #0
    	Canvas canvas = new Canvas();
		canvas.setBackground(Color.BLACK);
        canvas.setBounds(0, 0, 90, 60);
        lpane.setLayer(canvas, 0);
		lpane.add(canvas);

        // messages (with pictures, etc) on layer #1
        descrPanel = new JLabel();
        descrPanel.setVisible(false);
        descrPanel.setBackground(skyBlue);
        descrPanel.setFont(descrFont);
        descrPanel.setOpaque(true);
        descrPanel.setHorizontalAlignment(SwingConstants.CENTER);
        descrPanel.setVerticalAlignment(SwingConstants.CENTER);
        descrPanel.setBorder(bevelWithSpace);
        lpane.setLayer(descrPanel, 1);
        // size is taken from preffered, set by setText(). If doesn't fit
        // min/max sizes it "refit", but with no respect to min/max.
        descrPanel.setMinimumSize(new Dimension(250, 100));
        descrPanel.setMaximumSize(new Dimension(800, 600));
        lpane.add(descrPanel, "east+1/north+2");

        // errors, pause msgs on layer #2
        lastPause = null;
        pausePanel = new JLabel();
        pausePanel.setVisible(false);
        pausePanel.setHorizontalAlignment(SwingConstants.CENTER);
        pausePanel.setVerticalAlignment(SwingConstants.CENTER);
        pausePanel.setFont(pauseFont);
        pausePanel.setOpaque(true);
        pausePanel.setBackground(new Color(120, 0, 0));
        pausePanel.setForeground(Color.WHITE);
        pausePanel.setBorder(bevelWithSpace);
        lpane.setLayer(pausePanel, 2);
        // size is always % of video.. in the middle of the screen
        pausePanel.setMinimumSize(new Dimension(40, 30));
        pausePanel.setMaximumSize(new Dimension(800, 600));
        lpane.add(pausePanel, "35-65/40-60");

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

    private void playVideo(String videoFile) {
        // disable video if enabled
        if (len > 0) {
            mPlayer.stop();
            mPlayer.enableOverlay(false);

            Rectangle r = getBounds();
			UserPreferences.INSTANCE.setVideoBounds(r);
            // remove odo from the window
            odo.setVisible(false);
            remove(odo);
            // and show it in main window back
            UserPreferences.ODO_VISIBLE.setBool(true);
            setVisible(false);
            len = -1;
        }

        // enable new file
        if (videoFile != null) {
            // no pause message shown..
            lastPause = null;
            pausePanel.setVisible(false);

            // show window with odo
            UserPreferences.ODO_VISIBLE.setBool(false);
            add(odo, BorderLayout.SOUTH);
            odo.setVisible(true);
            setVisible(true);
            // what for?
            validate();

            mPlayer.enableOverlay(true);
            mPlayer.prepareMedia(videoFile);
            mPlayer.start();

            // check the input file and put everything in window
            len = mPlayer.getLength(); // [ms]
            // hide window if empty video
            if (len <= 0) {
                logger.error("Empty video, cannot start it");
                // to hide the video.. len must be greater than 0.. otherwise
                // it is "no video visible" indicator
                len = 1;
                playVideo(null);
                return;
            }

            double fps = mPlayer.getFps();
            logger.debug("Video initialize: FPS=" + fps + " len=" + len);


            // TODO don't mute video if POWER training and config is set..
            // Usefull for trainings with "sound guided" stuff (sufferfest,
            // spinnerwalls, etc)
            mPlayer.mute(true);

            lastRate = -1.0;
            setSpeed(lastTelemetry);
        }
    }

	private void setSpeed(Telemetry t) {
        // there is no video loaded
        if (len < 0) {
            return;
        }

        String pauseMsg = PauseMsgEnum.msg(t);

        // compare references, not objects!
        if (lastPause != pauseMsg) {
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

            // and show pause message
            if (pauseMsg != null) {
                pausePanel.setText(pauseMsg);
                pausePanel.setVisible(true);
            } else {
                pausePanel.setVisible(false);
            }
            lastPause = pauseMsg;
        }

        // show panel for 10s
        if ((t != null) && (hideTime == 0)) {
            hideTime = t.getTime() + 10000;
        }
        // hide description panel
        if ((t != null) && (hideTime > 0) && (t.getTime() > hideTime)) {
            descrPanel.setVisible(false);
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

    private void setDescription(String msg) {
        if (len < 0) {
            return;
        }
        if ((msg == null) || (msg.isEmpty())) {
            if (hideTime > 0) {
                hideTime = -1;
                descrPanel.setVisible(false);
            }
            return;
        }
        descrPanel.setText(msg);
        descrPanel.setVisible(true);
        hideTime = 0;
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
            lastTelemetry = null;
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