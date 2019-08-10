package net.alexheavens.cs4099.ui;

import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

public class AppMenuPanel extends SelectionPanel {

	private static final long serialVersionUID = 1L;
	private final Color BG_COLOUR = new Color(255, 255, 255);

	public AppMenuPanel() {
		super();
		setBackground(BG_COLOUR);
		MigLayout layout = new MigLayout("wrap 1", "[center]", "[top]");
		setLayout(layout);
	}

	public void addButton(String label, JPanel panel) {
		JButton button = new AppMenuButton(label);
		addButton(button, panel);
	}

}
