package net.alexheavens.cs4099.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.alexheavens.cs4099.ApplicationController;
import net.alexheavens.cs4099.ApplicationModel;
import net.miginfocom.swing.MigLayout;

public class MainView extends JFrame implements Observer, SelectionListener {

	private static final long serialVersionUID = 1L;
	private final Color BG_COLOUR = new Color(255, 255, 255);
	private final Dimension DEFAULT_SIZE = new Dimension(1280, 800);
	private final AppMenuPanel menuPanel;
	private JPanel bgPanel, mainPanel;
	private final PreSimPanel preSimPanel;
	private final NetworkDesigner networkDesigner;
	private final PreVisPanel visPanel;

	private final ApplicationController controller;
	private final ApplicationModel model;

	public MainView() {
		super();

		setTitle("EasySim");
		model = new ApplicationModel();
		model.addObserver(this);
		controller = new ApplicationController(model);

		bgPanel = new JPanel();
		BorderLayout bgLayout = new BorderLayout();
		setLayout(bgLayout);
		add(bgPanel, BorderLayout.CENTER);

		MigLayout layout = new MigLayout("wrap 2, fill",
				"[fill, 200px!][fill]", "[top]");
		bgPanel.setLayout(layout);
		bgPanel.setBackground(BG_COLOUR);
		setSize(DEFAULT_SIZE);
		setPreferredSize(DEFAULT_SIZE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		preSimPanel = new PreSimPanel(controller, model);
		networkDesigner = new NetworkDesigner(controller, null);
		visPanel = new PreVisPanel(controller);

		menuPanel = new AppMenuPanel();
		menuPanel.addButton("Network", networkDesigner);
		menuPanel.addButton("Simulation", preSimPanel);
		menuPanel.addButton("Visualisation", visPanel);
		menuPanel.addListener(this);

		menuPanel.setVisible(true);
		bgPanel.add(menuPanel);
		mainPanel = preSimPanel;
		bgPanel.add(mainPanel);
		selectionChange(networkDesigner);

		pack();
	}

	public static void main(String[] args) {
		MainView appFrame = new MainView();
		appFrame.setVisible(true);

	}

	public void update(Observable o, Object arg) {
		final Exception simEx = model.getSimulationException();
		if (simEx != null) {
			JOptionPane.showMessageDialog(this, simEx.toString(),
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void selectionChange(JPanel selected) {
		bgPanel.remove(mainPanel);
		mainPanel = selected;
		mainPanel.requestFocusInWindow();
		bgPanel.add(mainPanel);
		repaint();
		pack();
	}

}
