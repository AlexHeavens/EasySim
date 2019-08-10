package net.alexheavens.cs4099.ui;

import java.awt.Color;
import java.awt.Insets;

import javax.swing.JButton;

public class AppGeneralButton extends JButton {

	private static final long serialVersionUID = 1L;
	private static final Color BG_COLOR = new Color(255, 255, 255);

	public AppGeneralButton(String name) {
		super(name);
		
		setBackground(BG_COLOR);
		setMargin(new Insets(3, 50, 3, 50));
	}
}
