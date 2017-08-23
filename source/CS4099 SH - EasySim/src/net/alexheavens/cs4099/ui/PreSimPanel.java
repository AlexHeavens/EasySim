package net.alexheavens.cs4099.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import net.alexheavens.cs4099.ApplicationController;
import net.alexheavens.cs4099.ApplicationModel;
import net.miginfocom.swing.MigLayout;

public class PreSimPanel extends JPanel {

	private static final String SIM_DIRECTORY = ".";

	private static final File DEFAULT_DIR = new File(".");

	private static final long serialVersionUID = 1L;
	private final JLabel fileLabel = new JLabel("User Code:");
	private final JTextField fileField = new JTextField();
	private final JButton fileButton = new AppGeneralButton("Browse...");
	private final JFileChooser fileChooser = new JFileChooser(DEFAULT_DIR);

	private final JLabel netLabel = new JLabel("Network File:");
	private final JTextField netField = new JTextField();
	private final JButton netButton = new AppGeneralButton("Browse...");
	private final JFileChooser netChooser = new JFileChooser(DEFAULT_DIR);

	private final JLabel outputLabel = new JLabel("Name:");
	private final JTextField outputField = new JTextField();

	private final JLabel timeLabel = new JLabel("Length (timesteps):");
	private final SpinnerNumberModel timeField = new SpinnerNumberModel(100, 1,
			null, 1);

	private final JLabel timeoutLabel = new JLabel("Timeout (ms):");
	private final JCheckBox timeoutCheck = new JCheckBox();
	private final SpinnerNumberModel timeoutField = new SpinnerNumberModel(
			1000l, 1l, Long.MAX_VALUE / 1000000l, 1l);

	private final LimitationCheckBox limitations = new LimitationCheckBox();

	private final JButton simButton = new AppGeneralButton("Simulate");

	private final ErrorLabel errorLabel = new ErrorLabel();

	private final ApplicationController controller;
	private final ApplicationModel model;

	public PreSimPanel(final ApplicationController newController,
			final ApplicationModel model) {
		super();

		this.controller = newController;
		this.model = model;

		setBackground(Color.white);

		MigLayout layout = new MigLayout("wrap 3, fill",
				"[align right][100%,fill][]", "");
		setLayout(layout);

		add(fileLabel);
		add(fileField);
		add(fileButton);
		fileButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				fileChooser.showOpenDialog(fileButton);
				File file = fileChooser.getSelectedFile();
				if (file != null)
					fileField.setText(file.getAbsolutePath());
			}
		});

		add(netLabel);
		add(netField);
		add(netButton);
		netButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				netChooser.showOpenDialog(netButton);
				File file = netChooser.getSelectedFile();
				if (file != null)
					netField.setText(file.getAbsolutePath());
			}
		});

		add(limitations, "span");

		add(outputLabel);
		add(outputField, "span");

		add(timeLabel);
		JSpinner timeSpinner = new JSpinner(timeField);
		add(timeSpinner, "span");

		add(timeoutLabel);
		JPanel timeoutPanel = new JPanel();
		timeoutPanel.setLayout(new MigLayout("left"));
		timeoutPanel.setBackground(Color.WHITE);
		timeoutCheck.setSelected(true);
		timeoutPanel.add(timeoutCheck);
		timeoutPanel.add(new JSpinner(timeoutField));
		add(timeoutPanel, "right, span, growx");

		add(simButton, "span");

		simButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				errorLabel.clear();

				if (outputField.getText().equals("")) {
					errorLabel.appendError("No name specified for simulation.");
					return;
				}

				File netFile = new File(netField.getText());
				File userCode = new File(fileField.getText());
				File output = new File(SIM_DIRECTORY + File.separatorChar
						+ outputField.getText() + ".json");
				final long length = timeField.getNumber().longValue();
				final long timeout = (timeoutCheck.isSelected()) ? 1000000l * timeoutField
						.getNumber().longValue() : 0l;
				controller.simulate(netFile, length, userCode,
						limitations.generateLimitation(), output, timeout,
						limitations.isScramblingIds());
				if (PreSimPanel.this.model.getSimulationException() == null)
					JOptionPane.showMessageDialog(PreSimPanel.this,
							"Simulation was Successful!",
							"Simulation Complete",
							JOptionPane.INFORMATION_MESSAGE);
			}
		});

		add(errorLabel, "span");

	}
}
