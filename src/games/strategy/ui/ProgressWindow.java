package games.strategy.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ProgressWindow extends JWindow
{
	public ProgressWindow(final Frame owner, final String title)
	{
		super(owner);
		final JLabel label = new JLabel(title);
		label.setBorder(new EmptyBorder(10, 10, 10, 10));
		final JProgressBar progressBar = new JProgressBar();
		progressBar.setBorder(new EmptyBorder(10, 10, 10, 10));
		progressBar.setIndeterminate(true);
		final JPanel panel = new JPanel();
		panel.setBorder(new LineBorder(Color.BLACK));
		panel.setLayout(new BorderLayout());
		panel.add(BorderLayout.NORTH, label);
		panel.add(progressBar, BorderLayout.CENTER);
		setLayout(new BorderLayout());
		setSize(200, 80);
		add(panel, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
	}
}
