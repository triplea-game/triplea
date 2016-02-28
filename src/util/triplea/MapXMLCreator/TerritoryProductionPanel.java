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
public class TerritoryProductionPanel extends ImageScrollPanePanel
{

	private TerritoryProductionPanel()
	{
		s_instance = this;
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		s_mapXMLCreator = mapXMLCreator;
		final TerritoryProductionPanel panel = new TerritoryProductionPanel();
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

		final Integer production = MapXMLHelper.s_territoyProductions.get(centerName);
		if (production != null && production > 0)
		{
			final String productionString = production.toString();
			final Rectangle2D prodStringBounds = fontMetrics.getStringBounds(productionString, g);
			final Rectangle2D centerStringBounds = fontMetrics.getStringBounds(centerName, g);
			double wDiff = (centerStringBounds.getWidth() - prodStringBounds.getWidth())/2;
			g.setColor(Color.yellow);
			g.fillRect(Math.max(0, x_text_start - 2 + (int)wDiff), item.y+6, (int) prodStringBounds.getWidth() + 4, (int) prodStringBounds.getHeight());
			g.setColor(Color.red);
			g.drawString(productionString, Math.max(0, x_text_start + (int)wDiff), item.y + 17);
		}
		g.setColor(Color.red);
	}
	
	protected void paintPreparation(final Map<String, Point> centers)
	{
		if (centers != null && !MapXMLHelper.s_territoyProductions.isEmpty())
			return;
		for (final String territoryName : s_polygons.keySet())
		{
			MapXMLHelper.s_territoyProductions.put(territoryName, 0);

		}
	}
	
	@Override
	protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers)
	{
//		g.setColor(Color.GREEN);
//		for (final Entry<String, LinkedHashSet<String>> territoryConnection : MapXMLHelper.s_territoyConnections.entrySet())
//		{
//			final Point center1 = centers.get(territoryConnection.getKey());
//			for (final String territory2 : territoryConnection.getValue())
//			{
//				final Point center2 = centers.get(territory2);
//				g.drawLine(center1.x, center1.y, center2.x, center2.y);
//			}
//		}
	}
	
	protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e)
	{		
		final Point point = e.getPoint();
		final String territoryName = findTerritoryName(point, s_polygons);
		
		if (territoryName == null)
			return;
		
		final Integer currValue = MapXMLHelper.s_territoyProductions.get(territoryName);
		String inputText = JOptionPane.showInputDialog(null, "Enter the new production value for territory "+territoryName+":", (currValue!=null?currValue:0));
		try
		{
			final Integer newValue = Integer.parseInt(inputText);
			MapXMLHelper.s_territoyProductions.put(territoryName, newValue);
		} catch (NumberFormatException nfe)
		{
			JOptionPane.showMessageDialog(null, "'" + inputText + "' is no integer value.", "Input error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				imagePanel.repaint();
			}
		});
	}
}
