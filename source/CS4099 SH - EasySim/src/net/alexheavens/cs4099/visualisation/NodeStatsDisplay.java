package net.alexheavens.cs4099.visualisation;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

public class NodeStatsDisplay extends JPanel {

	private static final long serialVersionUID = 1L;
	private Dimension SIZE = new Dimension(400, Integer.MAX_VALUE);
	private JTabbedPane tabs = new JTabbedPane();
	private JPanel timeTable, sentTable, receivedTable, readTable;

	public NodeStatsDisplay() {

		setLayout(new MigLayout("fill, wrap 1"));
		setBackground(Color.WHITE);

		timeTable = new JPanel();
		sentTable = new JPanel();
		receivedTable = new JPanel();
		readTable = new JPanel();

		reset();

		tabs.setPreferredSize(SIZE);
		tabs.addTab("Time", timeTable);
		tabs.addTab("Sent", sentTable);
		tabs.addTab("Received", receivedTable);
		tabs.addTab("Read", readTable);
		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		add(tabs);
	}

	public void reset() {
		timeTable.removeAll();
		timeTable.add(new JLabel("Click a node to view statistical data."));
		sentTable.removeAll();
		sentTable.add(new JLabel("Click a node to view statistical data."));
		receivedTable.removeAll();
		receivedTable.add(new JLabel("Click a node to view statistical data."));
		readTable.removeAll();
		readTable.add(new JLabel("Click a node to view statistical data."));

		setPreferredSize(SIZE);
		revalidate();
		repaint();
	}

	public void display(NodeSimulationProfile profile) {

		timeTable.removeAll();
		timeTable.setLayout(new MigLayout("wrap 2"));
		timeTable.add(new JLabel("timestep"));
		timeTable.add(new JLabel("execution time (ns)"));
		for (Entry<Long, Long> timeEntry : profile.getExecutionTimes()) {
			timeTable.add(new JLabel(timeEntry.getKey().toString()));
			timeTable.add(new JLabel(timeEntry.getValue().toString()), "growx");
		}

		sentTable.removeAll();
		sentTable.setLayout(new MigLayout("wrap 2"));
		sentTable.add(new JLabel("timestep"));
		sentTable.add(new JLabel("messages sent"));
		for (Entry<Long, Integer> sentEntry : profile.getMessagesSent()) {
			sentTable.add(new JLabel(sentEntry.getKey().toString()));
			sentTable.add(new JLabel(sentEntry.getValue().toString()));
		}

		receivedTable.removeAll();
		receivedTable.setLayout(new MigLayout("wrap 2"));
		receivedTable.add(new JLabel("timestep"));
		receivedTable.add(new JLabel("messages received"));
		for (Entry<Long, Integer> receivedEntry : profile.getMessagesReceived()) {
			receivedTable.add(new JLabel(receivedEntry.getKey().toString()));
			receivedTable.add(new JLabel(receivedEntry.getValue().toString()));
		}

		readTable.removeAll();
		readTable.setLayout(new MigLayout("wrap 2"));
		readTable.add(new JLabel("timestep"));
		readTable.add(new JLabel("messages read"));
		for (Entry<Long, Integer> readEntry : profile.getMessagesRead()) {
			readTable.add(new JLabel(readEntry.getKey().toString()));
			readTable.add(new JLabel(readEntry.getValue().toString()));
		}

		tabs.setPreferredSize(SIZE);
		revalidate();
		repaint();
	}
}
