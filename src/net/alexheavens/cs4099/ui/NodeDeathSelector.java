package net.alexheavens.cs4099.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

/**
 * Used to take a list of Node death events from the user.
 * 
 * @author Alexander Heavens <alexander.heavens@gmail.com>
 * @verison 1.0
 */
public class NodeDeathSelector extends JPanel {

	private static final long serialVersionUID = -1610441205381781072L;

	private final JLabel titleLabel = new JLabel("Trigger Node Deaths at:");
	private final JPanel rowPanel = new JPanel();
	private final AppGeneralButton newRowButton = new AppGeneralButton("New");
	private final JScrollPane rowWrapper = new JScrollPane(rowPanel);

	private final Collection<RowPanel> rowPanels = new HashSet<RowPanel>();

	private boolean validIds = true;

	/**
	 * Create a NodeDeathSelectorPanel containing an empty list of node deaths.
	 */
	public NodeDeathSelector() {
		setLayout(new MigLayout("wrap 1"));
		rowWrapper.setMinimumSize(new Dimension(750, 0));
		setBackground(Color.WHITE);
		add(titleLabel);
		rowPanel.setLayout(new MigLayout("wrap 3"));
		rowPanel.setBackground(Color.WHITE);
		setMap(null);
		rowWrapper
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		rowWrapper
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		add(rowWrapper, "grow");
		add(newRowButton);

		// On the user clicking the new button, a new row for selecting a node
		// death must be created.
		newRowButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				addRow(0, 0l);
				revalidateDuplicateRowIds();
			}
		});
	}

	private void addRow(int id, long timestep) {
		final RowPanel newPanel = new RowPanel();
		newPanel.idSpinner.setValue(id);
		newPanel.timestepSpinner.setValue(timestep);
		rowPanel.add(newPanel, "span");
		rowPanels.add(newPanel);
	}

	/**
	 * @return A map containing the mappings of node IDs to the timesteps that
	 *         they die.
	 */
	public synchronized Map<Integer, Long> toMap() {
		final Map<Integer, Long> returnMap = new HashMap<Integer, Long>();
		for (RowPanel panel : rowPanels) {
			returnMap.put(panel.getId(), panel.getTimestep());
		}
		return returnMap;
	}

	private class RowPanel extends JPanel {

		private static final long serialVersionUID = 3935939393006105926L;
		private final JSpinner idSpinner = new JSpinner(new SpinnerNumberModel(
				0, 0, Integer.MAX_VALUE / 2, 1));
		private final JSpinner timestepSpinner = new JSpinner(
				new SpinnerNumberModel(0, 0, Long.MAX_VALUE, 1));
		private final AppGeneralButton deleteButton = new AppGeneralButton(
				"Delete");

		public RowPanel() {
			setLayout(new MigLayout("wrap 5, fill", "[fill][fill][]", ""));
			add(new JLabel("ID:"));
			add(idSpinner);
			idSpinner.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					revalidateDuplicateRowIds();
				}
			});
			add(new JLabel("Time:"));
			add(timestepSpinner);
			add(deleteButton);
			deleteButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					synchronized (NodeDeathSelector.this) {
						rowPanel.remove(RowPanel.this);
						rowPanels.remove(RowPanel.this);
						revalidateDuplicateRowIds();
					}
				}
			});
		}

		public int getId() {
			return ((Number) idSpinner.getValue()).intValue();
		}

		public long getTimestep() {
			return ((Number) timestepSpinner.getValue()).longValue();
		}
	}

	private void revalidateDuplicateRowIds() {

		final Map<Integer, RowPanel> seenRows = new HashMap<Integer, RowPanel>();

		// Highlight rows that duplicate messages
		validIds = true;
		for (RowPanel panel : rowPanels) {
			final RowPanel seenPanel = seenRows.get(panel.getId());
			if (seenPanel == null) {
				panel.setBackground(Color.WHITE);
			} else {
				panel.setBackground(Color.RED);
				seenPanel.setBackground(Color.RED);
				validIds = false;
			}

			seenRows.put(panel.getId(), panel);
		}

		rowWrapper.setPreferredSize(rowPanel.getPreferredSize());
		revalidate();
	}

	/**
	 * @return Whether the mapping of Node IDs to death timesteps are valid.
	 */
	public boolean hasValidIds() {
		return validIds;
	}

	/**
	 * @param nodeDeathEvents
	 */
	public void setMap(Map<Integer, Long> nodeDeathEvents) {
		rowPanel.removeAll();
		rowPanels.clear();
		if (nodeDeathEvents != null) {
			for (int id : nodeDeathEvents.keySet()) {
				addRow(id, nodeDeathEvents.get(id));
			}
		}
		revalidateDuplicateRowIds();
	}

}
