package net.alexheavens.cs4099.visualisation;

import java.awt.Toolkit;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.SizeAction;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;

import net.alexheavens.cs4099.network.configuration.INetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.alexheavens.cs4099.simulation.NodeEvent;
import net.alexheavens.cs4099.simulation.SimEventType;
import net.alexheavens.cs4099.simulation.SimulationEvent;
import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class VisualisationPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final String graph = "graph";
	private static final String nodes = "graph.nodes";
	private static final String edges = "graph.edges";
	public static final String visMessages = "visible_messages";
	public static final String messageIndex = "messageIndex";
	public static final String label = "label";
	public static final String xPos = "xpos";
	public static final String yPos = "ypos";

	private static final int MAX_TAG_LENGTH = 3;

	private VisualTable messageTable;
	private VisualItem selectedItem = null;

	private long lastDrawnTimestep = -1;

	private final MessageMap messages;
	private final Map<Long, HashSet<VisualisationEvent>> events;
	private final Map<Node, NodeSimulationProfile> nodeSimStats;
	private final NodeColourMap nodeColours;

	private NodeStatsDisplay nodeStats;
	private final Graph networkGraph;
	private Visualization vis;
	private final VisualisationClock clock;
	private VisClockPanel clockPanel;
	private ForceDirectedLayout fl;

	public VisualisationPanel(JSONObject results) throws JSONException,
			NetworkConfigException, VisualisationException {
		this(results, new VisualisationClock(results.getLong("length")));
	}

	@SuppressWarnings("unchecked")
	public VisualisationPanel(JSONObject results, VisualisationClock clock)
			throws JSONException, NetworkConfigException,
			VisualisationException {

		this.clock = clock;
		clock.setMillisPerTimestep(1000);

		// Create the visualisation graph from the network.
		JSONObject networkJson = results.getJSONObject("network");
		INetworkConfig network = new NetworkConfig(networkJson);
		networkGraph = network.toPrefuseGraph();

		// Generate mapping of messages.
		JSONArray messageLog = results.getJSONArray("messages");
		if (messageLog == null)
			throw new VisualisationException(
					"No message data attached to simulation log.");

		VisualisationMessage[] messageArray = new VisualisationMessage[messageLog
				.size()];
		int messageCount = 0;
		Iterator<JSONObject> messageIt = messageLog.iterator();
		while (messageIt.hasNext()) {
			messageArray[messageCount++] = new VisualisationMessage(
					messageIt.next(), networkGraph);
		}
		messages = new MessageMap(messageArray);

		// Generate events map.
		JSONArray eventsLog = results.getJSONArray("events");
		events = new HashMap<Long, HashSet<VisualisationEvent>>();
		nodeColours = new NodeColourMap();

		if (eventsLog == null)
			throw new VisualisationException(
					"No event data attached to simulation log.");

		Iterator<JSONObject> eventIt = eventsLog.iterator();
		while (eventIt.hasNext()) {
			JSONObject event = eventIt.next();
			long timestep = event.getLong(SimulationEvent.TIME_TAG);
			if (!events.containsKey(timestep))
				events.put(timestep, new HashSet<VisualisationEvent>());
			events.get(timestep).add(
					VisualisationEvent.generateFromJSON(event, messageArray,
							networkGraph));
			if (event.getString(SimulationEvent.TYPE_TAG).equals(
					SimEventType.COLOUR_CHANGE_EVENT.toString())) {
				nodeColours.addNodeColour(networkGraph.getNode(event
						.getInt(NodeEvent.NODE_ID_TAG)), timestep, event
						.getInt("colour"));
			}
		}

		// Make a second pass to ensure that node deaths are painted.
		eventIt = eventsLog.iterator();
		while (eventIt.hasNext()) {
			JSONObject event = eventIt.next();
			long timestep = event.getLong(SimulationEvent.TIME_TAG);
			if (event.getString(SimulationEvent.TYPE_TAG).equals(
					SimEventType.NODE_REMOTE_KILL.toString())) {
				nodeColours.addNodeColour(networkGraph.getNode(event
						.getInt(NodeEvent.NODE_ID_TAG)), timestep,
						Color.DARK_GRAY.getRGB());
			} else if (event.getString(SimulationEvent.TYPE_TAG).equals(
					SimEventType.NODE_FAILURE.toString())) {
				nodeColours.addNodeColour(networkGraph.getNode(event
						.getInt(NodeEvent.NODE_ID_TAG)), timestep, Color.RED
						.getRGB());
			}
		}

		// Generate statistics.
		final JSONObject timeMapJson = results.getJSONObject("nodeSimTimes");
		if (timeMapJson == null)
			throw new VisualisationException(
					"No record of time data attached to simulation log.");

		final Map<Integer, Map<Long, Long>> timemap = new HashMap<Integer, Map<Long, Long>>(
				timeMapJson.size());
		final Iterator<String> timeIt = timeMapJson.keys();
		while (timeIt.hasNext()) {
			final String nextKey = timeIt.next();
			final Map<Long, Long> nodeTimes = new HashMap<Long, Long>();
			timemap.put(Integer.valueOf(nextKey), nodeTimes);
			final Map<String, Integer> nodeJson = ((HashMap<String, Integer>) JSONObject
					.toBean(timeMapJson.getJSONObject(nextKey), HashMap.class));
			for (String timestep : nodeJson.keySet()) {
				nodeTimes.put(Long.valueOf(timestep), nodeJson.get(timestep)
						.longValue());
			}
		}
		nodeSimStats = generateNodeStatistics(networkGraph, events, timemap);

		initVisualisation();
	}

	private synchronized void initVisualisation() {

		vis = new Visualization();
		messageTable = new VisualTable(vis, visMessages);
		messageTable.addColumn(messageIndex, VisualisationMessage.class);
		messageTable.addColumn(label, String.class);
		vis.addTable(visMessages, messageTable);

		LabelRenderer tr = new LabelRenderer();
		tr.setRoundedCorner(4, 4);
		DefaultRendererFactory rf = new DefaultRendererFactory(tr);
		vis.setRendererFactory(rf);

		VisualGraph vg = vis.addGraph(graph, networkGraph);
		vis.setValue(edges, null, VisualItem.INTERACTIVE, Boolean.FALSE);
		if (vg.getNodeCount() > 0) {
			VisualItem f = (VisualItem) vg.getNode(0);
			vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
		}

		// Create actions to process visual data.
		ColorAction nodeColour = new ColorAction(nodes, VisualItem.FILLCOLOR,
				ColorLib.rgb(200, 200, 255)) {

			public void process(VisualItem item, double frac) {
				final Integer c = nodeColours.getNodeColour(
						networkGraph.getNode(item.getRow()),
						clock.getTimestep());
				if (c == null) {
					super.process(item, frac);
				} else {
					int o = item.getInt(m_colorField);
					item.setInt(m_startField, o);
					item.setInt(m_endField, c);
					item.setInt(m_colorField, c);
				}
			}
		};

		ActionList draw = new ActionList();
		draw.add(nodeColour);
		draw.add(new ColorAction(nodes, VisualItem.STROKECOLOR, 0));
		draw.add(new ColorAction(nodes, VisualItem.TEXTCOLOR, ColorLib.rgb(0,
				0, 0)));
		draw.add(new ColorAction(edges, VisualItem.FILLCOLOR, ColorLib
				.gray(200)));
		draw.add(new ColorAction(edges, VisualItem.STROKECOLOR, ColorLib
				.gray(200)));

		ActionList animate = new ActionList(Activity.INFINITY);
		fl = new ForceDirectedLayout(graph);
		Layout sl = new Layout(visMessages) {

			public void run(double frac) {
				synchronized (clock) {

					@SuppressWarnings("unchecked")
					Iterator<VisualItem> iter = m_vis.items(m_group);
					while (iter.hasNext()) {
						VisualItem item = (VisualItem) iter.next();
						VisualisationMessage message = (VisualisationMessage) item
								.get(VisualisationPanel.messageIndex);
						Node source = message.getSource();
						Node target = message.getTarget();
						NodeItem sourceItem = (NodeItem) item
								.getVisualization().getVisualItem(
										"graph.nodes", source);
						NodeItem targetItem = (NodeItem) item
								.getVisualization().getVisualItem(
										"graph.nodes", target);
						final double ratio = getMessageRatio(message);
						final double xDiff = targetItem.getX()
								- sourceItem.getX();
						final double yDiff = targetItem.getY()
								- sourceItem.getY();
						final double xPos = sourceItem.getX() + (ratio * xDiff);
						final double yPos = sourceItem.getY() + (ratio * yDiff);
						item.setX(xPos);
						item.setY(yPos);
					}
				}
			}
		};

		animate.add(new Action() {
			public void run(double frac) {
				synchronized (clock) {
					if (lastDrawnTimestep != clock.getTimestep()) {
						reloadMessageTable();
						selectedItem = null;
						lastDrawnTimestep = clock.getTimestep();
					}

					if (selectedItem != null) {
						if (selectedItem.isInGroup(visMessages)) {
							VisualisationMessage message = (VisualisationMessage) selectedItem
									.get(messageIndex);
							String labelStr = "Tag: " + message.getTag()
									+ "\nData: " + message.getData()
									+ "\nSent at: " + message.getSentAt();
							selectedItem.set(label, labelStr);
						}
					}
				}
			}
		});
		animate.add(fl);
		animate.add(sl);
		animate.add(new SizeAction(visMessages, 0.5d));
		animate.add(new ColorAction(visMessages, VisualItem.TEXTCOLOR, ColorLib
				.gray(200)));
		animate.add(new ColorAction(visMessages, VisualItem.FILLCOLOR, ColorLib
				.rgb(0, 46, 184)));
		animate.add(new ColorAction(visMessages, VisualItem.STROKECOLOR,
				ColorLib.rgb(200, 200, 200)));
		animate.add(nodeColour);
		animate.add(new RepaintAction());

		// Schedule the execution of our actions.
		vis.putAction("draw", draw);
		vis.putAction("layout", animate);
		vis.runAfter("draw", "layout");

		Display display = new Display(vis);

		display.pan(350, 350);
		display.setForeground(Color.GRAY);
		display.setBackground(Color.WHITE);
		display.setBorder(LineBorder.createGrayLineBorder());
		display.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());

		display.addControlListener(new FocusControl(1));
		display.addControlListener(new DragControl());
		display.addControlListener(new PanControl());
		display.addControlListener(new ZoomControl());
		display.addControlListener(new WheelZoomControl());
		display.addControlListener(new ZoomToFitControl());
		display.addControlListener(new NeighborHighlightControl());

		display.addControlListener(new ControlAdapter() {
			public void itemClicked(VisualItem item, MouseEvent e) {
				if (selectedItem != null) {
					if (selectedItem.isInGroup(visMessages)) {
						VisualisationMessage message = (VisualisationMessage) selectedItem
								.get(messageIndex);
						String labelStr = message.getTag().substring(0,
								MAX_TAG_LENGTH)
								+ " " + message.getData();
						selectedItem.set(label, labelStr);
					}
				}
				selectedItem = item;
			}
		});

		// A control adapter to toggle the repulsion of nodes from one another.
		display.addControlListener(new ControlAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2) {
					fl.setEnabled(!fl.isEnabled());
				}
			}
		});

		nodeStats = new NodeStatsDisplay();
		display.addControlListener(new ControlAdapter() {
			public void itemClicked(VisualItem item, MouseEvent e) {
				if (selectedItem.isInGroup(nodes)) {
					nodeStats.display(nodeSimStats.get(networkGraph
							.getNode(item.getRow())));
					repaint();
				}
			}
		});

		vis.run("draw");

		// Panel layout.
		setLayout(new MigLayout("hidemode 3"));
		add(display, "grow");
		add(nodeStats, "width 200px");
		clockPanel = new VisClockPanel(clock);
		add(clockPanel, "newline, span");
		setBackground(Color.WHITE);

		this.repaint();
	}

	private void reloadMessageTable() {
		synchronized (clock) {

			messageTable.clear();
			final Collection<VisualisationMessage> currentMessage = messages
					.getMessages(clock.getTimestep());
			if (currentMessage != null) {
				for (VisualisationMessage message : currentMessage) {
					int row = messageTable.addRow();
					messageTable.set(row, messageIndex, message);
					String labelStr = message.getTag().substring(0,
							MAX_TAG_LENGTH)
							+ " " + message.getData();
					messageTable.set(row, label, labelStr);
				}
			}
		}
	}

	private Map<Node, NodeSimulationProfile> generateNodeStatistics(
			Graph graph, Map<Long, HashSet<VisualisationEvent>> eventMap,
			Map<Integer, Map<Long, Long>> nodesExec) {
		final Map<Node, NodeSimulationProfile> eventTimeMap = new HashMap<Node, NodeSimulationProfile>(
				networkGraph.getNodeCount());

		// Create a blank simulation profile for each node. Fill the exec times.
		final Iterator<Node> nodeIt = graph.nodes();
		while (nodeIt.hasNext()) {
			final Node nextNode = nodeIt.next();
			eventTimeMap.put(nextNode, new NodeSimulationProfile());
			final Map<Long, Long> nodeExecTimes = nodesExec.get(nextNode
					.getRow());
			final NodeSimulationProfile nodeProfile = eventTimeMap
					.get(nextNode);
			for (Long timestep : nodeExecTimes.keySet()) {
				nodeProfile.setExecTime(timestep, nodeExecTimes.get(timestep));
			}
		}

		// Fill the simulation stats of each node from the events.
		for (Set<VisualisationEvent> stepEvents : eventMap.values()) {
			for (VisualisationEvent event : stepEvents) {
				if (event instanceof VisualisationMessageEvent) {
					final VisualisationMessageEvent messageEvent = (VisualisationMessageEvent) event;
					final VisualisationMessage message = messageEvent
							.getMessage();
					switch (messageEvent.getType()) {
					case MESSAGE_SENT:
						eventTimeMap.get(message.getSource())
								.incrementMessagesSent(message.getSentAt());
						break;
					case MESSAGE_ARRIVAL:
						eventTimeMap.get(message.getTarget())
								.incrementMessagesReceived(
										message.getArrivedAt());
						break;
					case MESSAGE_READ:
						eventTimeMap.get(message.getTarget())
								.incrementMessagesRead(message.getArrivedAt());
						break;
					}
				}
			}
		}

		return eventTimeMap;
	}

	private float getMessageRatio(VisualisationMessage message) {
		long messageStart = message.getSentAt();
		long messageEnd = message.getArrivedAt();
		long messageDuration = messageEnd - messageStart;
		float result = ((float) (((clock.getTimestep() - messageStart) * clock
				.getTicksPerTimestep()) + clock.getTick()))
				/ ((float) (messageDuration * clock.getTicksPerTimestep()));
		return result;
	}

	public void setPlayerEnabled(boolean enabled) {
		clockPanel.setVisible(enabled);
	}

	public VisualisationClock getClock() {
		return clock;
	}
}
