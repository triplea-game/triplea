package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UIContext;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

public class OddsCalculatorDialog extends JDialog
{
	private static final long serialVersionUID = -7625420355087851930L;
	private final OddsCalculatorPanel m_panel;
	
	public static void show(final TripleAFrame taFrame, final Territory t)
	{
		final OddsCalculatorDialog dialog = new OddsCalculatorDialog(taFrame.getGame().getData(), taFrame.getUIContext(), taFrame, t);
		dialog.pack();
		final int maxHeight = 600;
		if (dialog.getHeight() > maxHeight)
			dialog.setSize(new Dimension(dialog.getWidth(), maxHeight));
		dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(final WindowEvent e)
			{
				if (taFrame != null && taFrame.getUIContext() != null)
					taFrame.getUIContext().removeShutdownWindow(dialog);
			}
		});
		// close when hitting the escape key
		final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		final Action closeAction = new AbstractAction()
		{
			private static final long serialVersionUID = 8426179963957717432L;
			
			public void actionPerformed(final ActionEvent arg0)
			{
				dialog.setVisible(false);
			}
		};
		final String key = "odds.calc.invoke.close";
		dialog.getRootPane().getActionMap().put(key, closeAction);
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
		dialog.setLocationRelativeTo(taFrame);
		dialog.setVisible(true);
		taFrame.getUIContext().addShutdownWindow(dialog);
	}
	
	OddsCalculatorDialog(final GameData data, final UIContext context, final JFrame parent, final Territory location)
	{
		super(parent, "Odds Calculator");
		m_panel = new OddsCalculatorPanel(data, context, location, this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(m_panel, BorderLayout.CENTER);
		pack();
	}
	
	@Override
	public void show()
	{
		super.show();
		m_panel.selectCalculateButton();
	}
}
