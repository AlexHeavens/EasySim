package net.alexheavens.cs4099.visualisation;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.alexheavens.cs4099.ui.AppGeneralButton;
import net.alexheavens.cs4099.visualisation.VisualisationClock.State;
import net.miginfocom.swing.MigLayout;

public class VisClockPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	public static final int SLIDER_INTERVAL = 100;

	private final AppGeneralButton playPauseButton;
	private final AppGeneralButton stopButton;
	private final JSlider timeSlider, speedSlider;
	private final JLabel timestepLabel, speedLabel;

	public VisClockPanel(final VisualisationClock clock) {
		super();
		setBackground(Color.WHITE);
		playPauseButton = new AppGeneralButton("Play");
		stopButton = new AppGeneralButton("Stop");
		timeSlider = new JSlider(0, (int) clock.getLength(), 0);
		final int maxMillis = VisualisationClock.MAX_MILLIS_PER_TIMESTEP;
		final int defaultMillis = VisualisationClock.DEFAULT_MILLIS_PER_TIMESTEP;
		speedSlider = new JSlider(-maxMillis, maxMillis, defaultMillis);
		timestepLabel = new JLabel("Timestep: 0");
		speedLabel = new JLabel("Speed: " + clock.getMillisPerTimestep()
				+ " Milliseconds/Timestep");

		clock.addListener(new ClockListener() {
			public void onTimestepEvent(ClockTimestepEvent event) {
				timeSlider.setValue((int) event.getTimestep());
			}

			public void onTickEvent(ClockTickEvent tickEvent) {
			}

			public void onStateChange(ClockStateEvent event) {
				String buttonText = (event.getState() == State.PLAYING) ? "Pause"
						: "Play";
				playPauseButton.setText(buttonText);
			}

			public void onSpeedChange(ClockSpeedEvent event) {
			}
		});

		playPauseButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if (clock.getState() == State.PLAYING) {
					clock.pause();
				} else {
					clock.play();
				}
			}
		});

		stopButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				clock.stop();
			}
		});

		timeSlider.addMouseListener(new MouseAdapter() {

			public void mouseReleased(MouseEvent e) {
				clock.jumpToTimestep(timeSlider.getValue());
			}
		});
		timeSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				timestepLabel.setText("Timestep: " + timeSlider.getValue());
			}
		});
		speedSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int val = speedSlider.getValue() / SLIDER_INTERVAL;
				val = (val == 0) ? SLIDER_INTERVAL : SLIDER_INTERVAL * val;
				clock.setMillisPerTimestep(val);
				speedSlider.setValue(val);
				speedLabel.setText("Speed: " + clock.getMillisPerTimestep()
						+ " Milliseconds/Timestep");
			}
		});

		// Add the buttons to the layout.
		setLayout(new MigLayout("", "[][][]20px[fill,100%][]", ""));
		add(timeSlider, "span, growx, push");
		add(playPauseButton);
		add(stopButton);
		add(timestepLabel);
		add(speedSlider, "growx,push");
		add(speedLabel);
		repaint();
	}
}
