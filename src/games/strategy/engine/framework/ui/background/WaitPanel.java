package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

public class WaitPanel extends JPanel
{
	private static final long serialVersionUID = -8625021554802312498L;
	
	public WaitPanel(final String waitMessage)
	{
		setLayout(new BorderLayout());
		final JLabel label = new JLabel(waitMessage);
		label.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(BorderLayout.NORTH, label);
		final int min = 0;
		final int max = 100;
		final JProgressBar progress = new JProgressBar(min, max);
		progress.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(progress, BorderLayout.CENTER);
		progress.setIndeterminate(true);
	}
}
