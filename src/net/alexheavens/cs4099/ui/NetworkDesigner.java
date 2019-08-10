package net.alexheavens.cs4099.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

import net.alexheavens.cs4099.ApplicationController;
import net.alexheavens.cs4099.network.configuration.NetworkConfig;
import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.alexheavens.cs4099.visualisation.NetworkConfigDisplay;
import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONException;

public class NetworkDesigner extends JPanel {

	public static final String DEFAULT_CONFIG_DIR = ".";

	private static final long serialVersionUID = 1L;
	private NetworkConfigDisplay networkVis;
	private static final Color BG_COLOUR = Color.white;
	private final AppGeneralButton createNodeButton, createEdgeButton,
			selectButton, loadButton, saveButton, newButton, deathPanelButton;
	private final JLabel edgeLatLabel;
	private final JSpinner edgeLatField;
	private final JFileChooser saveChooser, loadChooser;
	private final NodeDeathSelector nodeDeathTable;

	private ControlAdapter nodeCreator, select, activeAdapter;
	private EdgeCreator edgeCreator;
	private transient ActionListener nodeListener, edgeListener,
			selectListener, newListener;

	private ChangeListener latencyListener;
	private final ApplicationController controller;

	public NetworkDesigner(ApplicationController controller,
			NetworkConfig config) {

		this.controller = controller;

		setBackground(BG_COLOUR);
		setLayout(new MigLayout("fill, nogrid, hidemode 3"));

		createNodeButton = new AppGeneralButton("Create Nodes");
		createEdgeButton = new AppGeneralButton("Create Edges");
		selectButton = new AppGeneralButton("Select");
		newButton = new AppGeneralButton("New Config");
		saveButton = new AppGeneralButton("Save");
		loadButton = new AppGeneralButton("Load");
		deathPanelButton = new AppGeneralButton("Hide Death Events");
		edgeLatLabel = new JLabel("Latency:");
		edgeLatField = new JSpinner(new SpinnerNumberModel(1, 1,
				Integer.MAX_VALUE, 1));

		nodeDeathTable = new NodeDeathSelector();

		saveChooser = new JFileChooser(DEFAULT_CONFIG_DIR);
		saveChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		saveChooser.setDialogTitle("Save network configuration as...");
		loadChooser = new JFileChooser(DEFAULT_CONFIG_DIR);
		loadChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		loadChooser.setDialogTitle("Open network configuration...");

		loadButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				loadChooser.showOpenDialog(loadButton);
				File file = loadChooser.getSelectedFile();
				if (file != null)
					try {
						final NetworkConfig con = new NetworkConfig(file);
						loadConfig(con);
						nodeDeathTable.setMap(con.getNodeDeathEvents());
					} catch (JSONException e1) {
						NetworkDesigner.this.controller.noteError(e1);
					} catch (NetworkConfigException e1) {
						NetworkDesigner.this.controller.noteError(e1);
					} catch (IOException e1) {
						NetworkDesigner.this.controller.noteError(e1);
					}
			}
		});

		saveButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				saveChooser.showSaveDialog(saveButton);
				File file = saveChooser.getSelectedFile();
				if (file != null)
					try {
						NetworkConfig config = networkVis.toNetworkConfig();
						if (!nodeDeathTable.hasValidIds())
							throw new NetworkConfigException(
									"More than one death event is specified for a single node.");
						config.setNodeDeathEvents(nodeDeathTable.toMap());
						config.writeToFile(file);
					} catch (IOException e1) {
						NetworkDesigner.this.controller.noteError(e1);
					} catch (NetworkConfigException e1) {
						NetworkDesigner.this.controller.noteError(e1);
					}
			}
		});

		deathPanelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean eventsEnabled = !nodeDeathTable.isVisible();
				final String title = (eventsEnabled) ? "Hide Death Events"
						: "Show Death Events";
				deathPanelButton.setText(title);
				nodeDeathTable.setVisible(eventsEnabled);
				revalidate();
			}
		});

		add(newButton);
		add(loadButton);
		add(saveButton);
		add(deathPanelButton);

		add(selectButton, "newline");
		add(createNodeButton);
		add(createEdgeButton);
		add(edgeLatLabel);
		add(edgeLatField);

		loadConfig(config);
	}

	public void loadConfig(NetworkConfig config) {

		if (config == null)
			config = new NetworkConfig(0);

		// Ensure that a previous visualisation is removed if we are reloading
		// one.
		if (networkVis != null) {
			remove(networkVis);
		}

		Graph networkGraph = config.toPrefuseGraph();
		networkVis = new NetworkConfigDisplay(networkGraph);
		networkVis.setFocusable(true);
		networkVis.setBorder(LineBorder.createGrayLineBorder());

		nodeCreator = new NodeCreator(networkVis);
		edgeCreator = new EdgeCreator(networkVis);
		select = new Selector(networkVis);

		if (nodeListener != null) {
			createNodeButton.removeActionListener(nodeListener);
			createEdgeButton.removeActionListener(edgeListener);
			selectButton.removeActionListener(selectListener);
			newButton.removeActionListener(newListener);
		}

		nodeListener = new SelectionListener(nodeCreator);
		edgeListener = new SelectionListener(edgeCreator);
		selectListener = new SelectionListener(select);
		newListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				loadConfig(null);
			}
		};
		latencyListener = new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				edgeCreator.setEdgeLatency((Integer) edgeLatField.getValue());
			}
		};

		createNodeButton.addActionListener(nodeListener);
		createEdgeButton.addActionListener(edgeListener);
		selectButton.addActionListener(selectListener);
		newButton.addActionListener(newListener);
		edgeLatField.addChangeListener(latencyListener);

		networkVis.addControlListener(nodeCreator);
		networkVis.addControlListener(select);
		networkVis.addControlListener(edgeCreator);
		activeAdapter = select;
		setAdapter(select);

		add(networkVis, "newline, grow");
		remove(nodeDeathTable);
		add(nodeDeathTable, "grow, span, top");
		revalidate();
		repaint();
	}

	private void setAdapter(ControlAdapter adapter) {
		activeAdapter.setEnabled(false);
		activeAdapter = adapter;
		activeAdapter.setEnabled(true);
	}

	private class SelectionListener implements ActionListener {

		private final ControlAdapter selection;

		public SelectionListener(ControlAdapter selection) {
			this.selection = selection;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setAdapter(selection);
		}

	}

	private static class NodeCreator extends ControlAdapter {

		private final Display display;
		private final Visualization vis;

		public NodeCreator(Display display) {
			this.display = display;
			vis = display.getVisualization();
			setEnabled(false);
		}

		public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1)
				return;
			Graph graph = (Graph) vis.getSourceData("graph");
			Node node = graph.addNode();
			node.set("label", node.getRow() + "");
			VisualGraph visGraph = (VisualGraph) vis.getGroup("graph");
			NodeItem nodeItem = (NodeItem) visGraph.getNode(node.getRow());
			nodeItem.setX(e.getX() + display.getDisplayX());
			nodeItem.setY(e.getY() + display.getDisplayY());
			vis.run("draw");
		}
	}

	private static class EdgeCreator extends ControlAdapter {

		private final NetworkConfigDisplay dis;
		private final Visualization vis;
		private NodeItem from, to, fakeToVis;
		private Node fakeTo;
		private long edgeLatency;

		public EdgeCreator(NetworkConfigDisplay dis) {
			this.dis = dis;
			this.vis = dis.getVisualization();
			from = null;
			setEnabled(false);
			edgeLatency = 1l;
		}

		public void setEdgeLatency(long latency) {
			if (latency < 1)
				throw new IllegalArgumentException();
			edgeLatency = latency;
		}

		public void itemPressed(VisualItem item, MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1)
				return;

			Graph graph = ((Graph) vis.getSourceData("graph"));
			if (item.isInGroup("graph.nodes")) {
				if (from == null) {
					from = (NodeItem) item;
					VisualGraph visGraph = ((VisualGraph) vis.getGroup("graph"));
					vis.setInteractive("graph.edges", null, false);
					fakeTo = graph.addNode();
					fakeToVis = (NodeItem) visGraph.getNode(fakeTo.getRow());
					fakeToVis.setInteractive(false);
					fakeToVis.setVisible(false);
					fakeToVis.setX(from.getX());
					fakeToVis.setY(from.getY());
					Edge fakeEdge = graph.addEdge(graph.getNode(from.getRow()),
							fakeTo);
					EdgeItem fakeEdgeVis = (EdgeItem) visGraph.getEdge(fakeEdge
							.getRow());
					fakeEdgeVis.setInteractive(false);
				} else if (to == null && item != from) {
					to = (NodeItem) item;
					boolean containsEdge = graph.getEdge(from.getRow(),
							to.getRow()) >= 0;
					containsEdge = containsEdge
							|| graph.getEdge(to.getRow(), from.getRow()) >= 0;
					if (!containsEdge) {
						int edgeId = graph.addEdge(from.getRow(), to.getRow());
						graph.getEdge(edgeId).set("latency", edgeLatency);
					}
					resetFrom();
				}
				vis.run("draw");
				vis.repaint();
			}
		}

		public void mouseReleased(MouseEvent e) {
			resetFrom();
		}

		private void resetFrom() {
			if (from != null) {
				fakeTo.getGraph().removeNode(fakeTo);
				vis.setInteractive("graph.edges", null, true);
				fakeTo = null;
				fakeToVis = null;
				from = null;
				to = null;
			}
			vis.setInteractive("graph.nodes", null, true);
			vis.repaint();
			vis.run("draw");
		}

		public void mouseMoved(MouseEvent e) {
			if (fakeToVis != null) {
				double scale = 1d / dis.getScale();
				fakeToVis.setX((dis.getDisplayX() + (double) e.getX()) * scale);
				fakeToVis.setY((dis.getDisplayY() + (double) e.getY()) * scale);
				vis.run("draw");
			}
		}

		public void setEnabled(boolean enabled) {
			resetFrom();
			dis.setDragEnabled(!enabled);
			super.setEnabled(enabled);
		}

	}

	private static class Selector extends ControlAdapter {

		private static final int DEL_CODE = 127;
		private static final int BS_CODE = 8;

		private final Display dis;
		private final Visualization vis;
		private final TupleSet selected;
		private VisualItem movingItem;
		private double lastX, lastY;

		public Selector(Display dis) {
			this.dis = dis;
			this.vis = dis.getVisualization();
			this.selected = vis.getFocusGroup(NetworkConfigDisplay.SELECTED);
		}

		public void itemPressed(VisualItem item, MouseEvent e) {

			if (e.isControlDown() && selected.containsTuple(item))
				selected.removeTuple(item);
			else
				selected.addTuple(item);
		}

		public void itemDragged(VisualItem item, MouseEvent e) {
			if (movingItem == null) {
				movingItem = item;
			} else {
				double xDiff = movingItem.getX() - lastX;
				double yDiff = movingItem.getY() - lastY;

				Iterator<Tuple> selectedIt = selected.tuples();
				while (selectedIt.hasNext()) {
					VisualItem nextSelected = (VisualItem) selectedIt.next();
					if (nextSelected != movingItem) {
						nextSelected.setX(nextSelected.getX() + xDiff);
						nextSelected.setY(nextSelected.getY() + yDiff);
					}
				}
			}
			lastX = item.getX();
			lastY = item.getY();
		}

		public void itemReleased(VisualItem item, MouseEvent e) {
			movingItem = null;
		}

		public void setEnabled(boolean enabled) {
			selected.clear();
			vis.setInteractive(NetworkConfigDisplay.EDGES, null, enabled);
			super.setEnabled(enabled);
		}

		public void mouseEntered(MouseEvent e) {
			dis.requestFocusInWindow();
		}

		public void mousePressed(MouseEvent e) {
			selected.clear();
		}

		public void itemKeyPressed(VisualItem item, KeyEvent e) {
			if (e.getKeyCode() == BS_CODE || e.getKeyCode() == DEL_CODE) {
				removeSelected();
			}
		}

		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == BS_CODE || e.getKeyCode() == DEL_CODE) {
				removeSelected();
			}
		}

		private void removeSelected() {
			Iterator<Tuple> selectedIt = selected.tuples();
			Graph graph = (Graph) vis.getSourceData(NetworkConfigDisplay.GRAPH);
			while (selectedIt.hasNext()) {
				VisualItem nextSelected = (VisualItem) selectedIt.next();
				if (nextSelected instanceof NodeItem)
					graph.removeNode(nextSelected.getRow());
				else if (nextSelected instanceof EdgeItem
						&& nextSelected.isValid())
					graph.removeEdge(nextSelected.getRow());
			}
			selected.clear();
			vis.repaint();
			vis.run("draw");
		}
	}

}
