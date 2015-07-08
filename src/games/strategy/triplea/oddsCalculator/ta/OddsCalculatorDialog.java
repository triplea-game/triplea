package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.TripleAFrame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
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
import javax.swing.WindowConstants;

public class OddsCalculatorDialog extends JDialog
{
	private static final long serialVersionUID = -7625420355087851930L;
	private static final int MAX_HEIGHT = 640;
	
	private static Point lastPosition;
	private static Dimension lastShape;
	
	private final OddsCalculatorPanel panel;
	
	public static void show(final TripleAFrame taFrame, final Territory t)
	{
		final OddsCalculatorDialog dialog = new OddsCalculatorDialog(taFrame.getGame().getData(), taFrame.getUIContext(), taFrame, t);
		dialog.pack();
		dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosed(final WindowEvent e)
			{
				if (taFrame != null && taFrame.getUIContext() != null && !taFrame.getUIContext().isShutDown())
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
				dialog.dispose();
			}
		};
		final String key = "odds.calc.invoke.close";
		dialog.getRootPane().getActionMap().put(key, closeAction);
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		if (lastPosition == null)
		{
			dialog.setLocationRelativeTo(taFrame);
			if (dialog.getHeight() > MAX_HEIGHT)
				dialog.setSize(new Dimension(dialog.getWidth(), MAX_HEIGHT));
		}
		else
		{
			dialog.setLocation(lastPosition);
			dialog.setSize(lastShape);
		}
		dialog.setVisible(true);
		taFrame.getUIContext().addShutdownWindow(dialog);
	}
	
	OddsCalculatorDialog(final GameData data, final IUIContext context, final JFrame parent, final Territory location)
	{
		super(parent, "Odds Calculator");
		panel = new OddsCalculatorPanel(data, context, location, this);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
		pack();
	}
	
	@Override
	public void dispose()
	{
		lastPosition = new Point(getLocation());
		lastShape = new Dimension(getSize());
		panel.shutdown();
		super.dispose();
	}
	
	@Override
	public void show()
	{
		super.show();
		panel.selectCalculateButton();
	}
}
