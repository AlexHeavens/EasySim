package net.alexheavens.cs4099.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

public class SelectionPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = -5719469124569699652L;
	private final HashMap<JButton, JPanel> buttonPanelMap;
	private final EventListenerList listenerList;

	public SelectionPanel() {
		buttonPanelMap = new HashMap<JButton, JPanel>();
		listenerList = new EventListenerList();
	}

	public void addButton(JButton button, JPanel panel) {
		buttonPanelMap.put(button, panel);
		button.addActionListener(this);
		add(button);
	}

	public void removeButton(JButton button) {
		buttonPanelMap.remove(button);
		remove(button);
	}

	public void addListener(SelectionListener listener) {
		listenerList.add(SelectionListener.class, listener);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		for (SelectionListener listener : listenerList
				.getListeners(SelectionListener.class)) {
			listener.selectionChange(buttonPanelMap.get(e.getSource()));
		}
	}
}
