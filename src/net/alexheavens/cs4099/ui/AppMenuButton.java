package net.alexheavens.cs4099.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;

public class AppMenuButton extends JButton {

	private static final long serialVersionUID = 1L;
	private static final Color BG_COLOR = new Color(255, 255, 255);
	private static final Dimension SIZE = new Dimension(200, 50);

	public AppMenuButton(String name) {
		super(name);
		
		setBackground(BG_COLOR);
		setPreferredSize(SIZE);
		setSize(SIZE);
		setMinimumSize(SIZE);
		setMaximumSize(SIZE);
		setMargin(new Insets(5, 5, 5, 5));
	}
}
