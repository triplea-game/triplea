package games.strategy.engine.framework.ui.background;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

public class WaitDialog extends JDialog
{
	private static final long serialVersionUID = 7433959812027467868L;
	
	public WaitDialog(final Component parent, final String waitMessage)
	{
		this(parent, waitMessage, null);
	}
	
	public WaitDialog(final Component parent, final String waitMessage, final Action cancelAction)
	{
		super(JOptionPane.getFrameForComponent(parent), "Please Wait", true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		final WaitPanel panel = new WaitPanel(waitMessage);
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		if (cancelAction != null)
		{
			final JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(cancelAction);
			add(cancelButton, BorderLayout.SOUTH);
		}
	}
}
