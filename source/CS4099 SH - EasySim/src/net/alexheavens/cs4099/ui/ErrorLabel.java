package net.alexheavens.cs4099.ui;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class ErrorLabel extends JLabel {

	private static final long serialVersionUID = 1L;
	private static final Color BG_COLOUR = Color.RED;
	private static final Border BORDER = new LineBorder(Color.RED, 0);
	private static final Color TEXT_COLOUR = Color.RED;

	public ErrorLabel() {
		super();
		setVisible(false);
		setBackground(BG_COLOUR);
		setBorder(BORDER);
		setForeground(TEXT_COLOUR);
	}

	public void appendError(String error) {
		String nl = (getText().equals("")) ? "" : "\n";
		setText(getText() + nl + error);
		setVisible(true);
	}

	public void clear() {
		setVisible(false);
		setText("");
	}
}
