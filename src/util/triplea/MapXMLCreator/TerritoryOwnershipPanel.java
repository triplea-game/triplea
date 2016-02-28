package util.triplea.MapXMLCreator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik von der Osten
 * 
 */
public class TerritoryOwnershipPanel extends ImageScrollPanePanel
{

	private final String[] m_players = MapXMLHelper.getPlayersListInclNeutral();
	private String m_lastChosenPlayer = MapXMLHelper.s_playerName.iterator().next();
	
	private TerritoryOwnershipPanel()
	{
		s_instance = this;
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		s_mapXMLCreator = mapXMLCreator;
		final TerritoryOwnershipPanel panel = new TerritoryOwnershipPanel();
		panel.layout(stepActionPanel);
		mapXMLCreator.setAutoFillAction(new AbstractAction()
		{
			private static final long serialVersionUID = -8508734371454749752L;

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				panel.paintPreparation(null);
				panel.repaint();
			}
		});
	}

	protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics, final Point item, final int x_text_start)
	{

		String ownership = MapXMLHelper.s_territoyOwnerships.get(centerName);
		if (ownership == null)
			ownership = MapXMLHelper.s_playerNeutral;
		final Rectangle2D prodStringBounds = fontMetrics.getStringBounds(ownership, g);
		final Rectangle2D centerStringBounds = fontMetrics.getStringBounds(centerName, g);
		double wDiff = (centerStringBounds.getWidth() - prodStringBounds.getWidth())/2;
		g.setColor(Color.yellow);
		g.fillRect(Math.max(0, x_text_start - 2 + (int)wDiff), item.y+6, (int) prodStringBounds.getWidth() + 4, (int) prodStringBounds.getHeight());
		g.setColor(Color.red);
		g.drawString(ownership, Math.max(0, x_text_start + (int) wDiff), item.y + 17);
		g.setColor(Color.red);
	}
	
	protected void paintPreparation(final Map<String, Point> centers)
	{
	}
	
	@Override
	protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers)
	{
	}
	
	protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e)
	{		
		final Point point = e.getPoint();
		final String territoryName = findTerritoryName(point, s_polygons);
		
		if (territoryName == null)
			return;
		
		final String currValue = MapXMLHelper.s_territoyOwnerships.get(territoryName);
		String inputText = (String) JOptionPane.showInputDialog(null, "Which player should be the initial owner of territory '" + territoryName + "'?", "Choose Owner of " + territoryName,
					JOptionPane.QUESTION_MESSAGE, null, m_players, // Array of choices
					(currValue != null ? currValue : m_lastChosenPlayer)); // Initial choice
		final boolean inputIsNeutralPlayer = MapXMLHelper.s_playerNeutral.equals(inputText);
		if (inputText == null || inputText.equals(currValue) || inputIsNeutralPlayer && currValue == null)
			return;
		if (inputIsNeutralPlayer)
			MapXMLHelper.s_territoyOwnerships.remove(territoryName);
		else
			MapXMLHelper.s_territoyOwnerships.put(territoryName, inputText);
		m_lastChosenPlayer = inputText;
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				imagePanel.repaint();
			}
		});
	}
	
}
