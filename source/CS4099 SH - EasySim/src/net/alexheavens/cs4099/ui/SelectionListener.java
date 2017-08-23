package net.alexheavens.cs4099.ui;

import javax.swing.JPanel;
import java.util.EventListener;

public interface SelectionListener extends EventListener {

	public void selectionChange(JPanel selected);
	
}
