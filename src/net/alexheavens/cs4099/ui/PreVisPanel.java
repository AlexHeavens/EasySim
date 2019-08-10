package net.alexheavens.cs4099.ui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.alexheavens.cs4099.ApplicationController;
import net.alexheavens.cs4099.network.configuration.NetworkConfigException;
import net.alexheavens.cs4099.visualisation.VisualisationException;
import net.alexheavens.cs4099.visualisation.VisualisationPanel;
import net.miginfocom.swing.MigLayout;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class PreVisPanel extends JPanel {

	private static final File DEFAULT_DIR = new File(".");

	private static final long serialVersionUID = 1L;
	private final JLabel fileLabelA = new JLabel("Results file:");
	private final JTextField fileFieldA = new JTextField();
	private final JButton fileButtonA = new AppGeneralButton("Browse...");
	private final JLabel fileLabelB = new JLabel("Comparison file:");
	private final JTextField fileFieldB = new JTextField();
	private final JButton fileButtonB = new AppGeneralButton("Browse...");
	private final JButton visButton = new AppGeneralButton("Visualise");
	private final JFileChooser fileChooser = new JFileChooser(DEFAULT_DIR);
	private final ApplicationController controller;

	private VisualisationPanel visPanel, compPanel;

	public PreVisPanel(ApplicationController controller) {

		this.controller = controller;

		setBackground(Color.white);

		MigLayout layout = new MigLayout("wrap 4, fill",
				"[align right][100%,fill][][]", "");
		setLayout(layout);

		add(fileLabelA);
		add(fileFieldA);
		add(fileButtonA);
		add(visButton);
		add(fileLabelB);
		add(fileFieldB);
		add(fileButtonB, "span");
		fileButtonA.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fileChooser.showOpenDialog(fileButtonA);
				File file = fileChooser.getSelectedFile();
				if (file != null)
					fileFieldA.setText(file.getAbsolutePath());
			}
		});
		fileButtonB.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fileChooser.showOpenDialog(fileButtonB);
				File file = fileChooser.getSelectedFile();
				if (file != null)
					fileFieldB.setText(file.getAbsolutePath());
			}
		});
		visButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					if (visPanel != null)
						remove(visPanel);
					if (compPanel != null)
						remove(compPanel);
					File resultsFile = new File(fileFieldA.getText());
					File compFile = (fileFieldB.getText().equals("")) ? null
							: new File(fileFieldB.getText());

					JSONObject results = readResults(resultsFile);
					visPanel = new VisualisationPanel(results);
					add(visPanel, "span");
					if (compFile != null) {
						JSONObject compResults = readResults(compFile);
						compPanel = new VisualisationPanel(compResults,
								visPanel.getClock());
						final long visLength = visPanel.getClock().getLength();
						final long compLength = compPanel.getClock()
								.getLength();
						if (visLength > compLength) {
							compPanel.getClock().setLength(visLength);
						}
						visPanel.setPlayerEnabled(false);
						add(compPanel, "span");
					}
					revalidate();
				} catch (FileNotFoundException e1) {
					PreVisPanel.this.controller.noteError(e1);
				} catch (IOException e1) {
					PreVisPanel.this.controller.noteError(e1);
				} catch (JSONException e1) {
					PreVisPanel.this.controller.noteError(e1);
				} catch (NetworkConfigException e1) {
					PreVisPanel.this.controller.noteError(e1);
				} catch (VisualisationException e1) {
					PreVisPanel.this.controller.noteError(e1);
				}
			}
		});

	}

	private JSONObject readResults(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br;

		br = new BufferedReader(new FileReader(file));

		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
		br.close();

		return JSONObject.fromObject(sb.toString());
	}
}
