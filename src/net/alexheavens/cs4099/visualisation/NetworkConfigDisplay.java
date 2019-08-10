package net.alexheavens.cs4099.visualisation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Iterator;

import net.alexheavens.cs4099.network.configuration.LinkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
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
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;

public class NetworkConfigDisplay extends Display {

	private final ForceDirectedLayout fl;
	private static final long serialVersionUID = 1L;
	public final static String GRAPH = "graph";
	public final static String EDGES = "graph.edges";
	public final static String NODES = "graph.nodes";
	public final static String SELECTED = "_selected";
	private final int SELECTED_COLOUR = ColorLib.rgb(80, 220, 80);
	private final static int NODE_LABEL_RADIUS = 4;
	private final Visualization graphVis;
	private final DragControl dragControl;

	/**
	 * Creates a visualisation with a preloaded graph.
	 * 
	 * @param config
	 *            the preloaded network configuration.
	 */
	public NetworkConfigDisplay(Graph networkGraph) {

		setPreferredSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		graphVis = new Visualization();

		// Define renderers.
		LabelRenderer nodeRenderer = new LabelRenderer();
		nodeRenderer.setRoundedCorner(NODE_LABEL_RADIUS, NODE_LABEL_RADIUS);
		DefaultRendererFactory rendererFactory = new DefaultRendererFactory(
				nodeRenderer);
		graphVis.setRendererFactory(rendererFactory);

		// Load data, focus on the first node.
		VisualGraph visualGraph = graphVis.addGraph(GRAPH, networkGraph);
		if (visualGraph.getNodeCount() > 0) {
			VisualItem focusNode = (VisualItem) visualGraph.getNode(0);
			graphVis.getGroup(Visualization.FOCUS_ITEMS).setTuple(focusNode);
		}
		graphVis.addFocusGroup(SELECTED);

		// Create actions to process visual data.
		ColorAction nodeColour = new ColorAction(NODES, VisualItem.FILLCOLOR,
				ColorLib.rgb(200, 200, 255));
		nodeColour.add(new InGroupPredicate(SELECTED), SELECTED_COLOUR);

		ActionList draw = new ActionList();
		draw.add(nodeColour);
		draw.add(new ColorAction(NODES, VisualItem.STROKECOLOR, 0));
		draw.add(new ColorAction(NODES, VisualItem.TEXTCOLOR, ColorLib.rgb(0,
				0, 0)));
		ColorAction edgeColour = new ColorAction(EDGES, VisualItem.STROKECOLOR,
				ColorLib.gray(200));
		edgeColour.add(new InGroupPredicate(SELECTED), SELECTED_COLOUR);
		draw.add(edgeColour);

		ActionList animate = new ActionList(Activity.INFINITY);
		animate.add(nodeColour);
		animate.add(edgeColour);
		animate.add(new RepaintAction());
		fl = new ForceDirectedLayout(GRAPH);
		animate.add(fl);

		// A control adapter to toggle the repulsion of nodes from one another.
		addControlListener(new ControlAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON2) {
					fl.setEnabled(!fl.isEnabled());
				}
			}
		});

		// Schedule the execution of actions.
		graphVis.putAction("draw", draw);
		graphVis.putAction("layout", animate);
		graphVis.runAfter("draw", "layout");
		graphVis.runAfter("layout", "force");

		setVisualization(graphVis);
		setSize(1280, 800);
		pan(350, 350);
		setForeground(Color.GRAY);
		setBackground(Color.WHITE);

		addControlListener(new FocusControl(1));
		dragControl = new DragControl();
		addControlListener(dragControl);
		addControlListener(new PanControl());
		addControlListener(new ZoomControl());
		addControlListener(new WheelZoomControl());
		addControlListener(new ZoomToFitControl());
		addControlListener(new NeighborHighlightControl());

		graphVis.run("draw");
	}

	public NetworkConfig toNetworkConfig() {
		Graph net = (Graph) graphVis.getSourceData(GRAPH);

		// Need to map VisualItem rows to node ids in config in case items have
		// been deleted.
		HashMap<Integer, Integer> nodeIdMap = new HashMap<Integer, Integer>(
				net.getNodeCount());
		Iterator<Node> nodeIt = net.nodes();
		while (nodeIt.hasNext()) {
			nodeIdMap.put(nodeIt.next().getRow(), nodeIdMap.size());
		}

		NetworkConfig config = new NetworkConfig(net.getNodeCount());
		Iterator<Edge> edgeIt = net.edges();
		while (edgeIt.hasNext()) {
			Edge next = edgeIt.next();
			int sourceId = nodeIdMap.get(next.getSourceNode().getRow());
			int targetId = nodeIdMap.get(next.getTargetNode().getRow());
			long latency = next.getLong("latency");
			config.addLink(new LinkConfig(sourceId, targetId, latency));
		}
		return config;
	}

	public void setDragEnabled(boolean enabled) {
		dragControl.setEnabled(enabled);
	}
}
