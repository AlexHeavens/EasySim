package net.alexheavens.cs4099.ui;

import java.awt.Color;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.alexheavens.cs4099.usercode.ClassLimitation;
import net.miginfocom.swing.MigLayout;

public class LimitationCheckBox extends JPanel {

	private static final long serialVersionUID = 6622661832472052539L;
	private final JCheckBox staticCheckBox, constantCheckBox, scrambleIdsBox;

	public LimitationCheckBox() {
		setBackground(Color.WHITE);
		setLayout(new MigLayout("", "[]", "[]"));
		staticCheckBox = new JCheckBox("Allow Static");
		constantCheckBox = new JCheckBox("Allow Constants");
		scrambleIdsBox = new JCheckBox("Scramble IDs");
		add(staticCheckBox);
		add(constantCheckBox);
		add(scrambleIdsBox);
	}

	public ClassLimitation generateLimitation() {
		return new ClassLimitation(staticCheckBox.isSelected(),
				constantCheckBox.isSelected());
	}

	public boolean isScramblingIds() {
		return scrambleIdsBox.isSelected();
	}

}
