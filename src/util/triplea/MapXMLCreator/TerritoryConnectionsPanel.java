package util.triplea.MapXMLCreator;

import games.strategy.util.AlphanumComparator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import util.image.ConnectionFinder;

/**
 * 
 * @author Erik von der Osten
 * 
 */
public class TerritoryConnectionsPanel extends ImageScrollPanePanel
{
	
	String m_selectedTerritory = null;

	private TerritoryConnectionsPanel()
	{
		s_instance = this;
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		s_mapXMLCreator = mapXMLCreator;
		final TerritoryConnectionsPanel panel = new TerritoryConnectionsPanel();
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
		if (centerName.equals(m_selectedTerritory))
		{
			final Rectangle2D stringBounds = fontMetrics.getStringBounds(centerName, g);
			g.setColor(Color.yellow);
			g.fillRect(Math.max(0, x_text_start - 2), Math.max(0, item.y - 6), (int) stringBounds.getWidth() + 4, (int) stringBounds.getHeight());
			g.setColor(Color.red);
			g.drawString(centerName, Math.max(0, x_text_start), item.y + 5);
		}
		g.setColor(Color.red);
	}
	
	protected void paintPreparation(final Map<String, Point> centers)
	{
		if (centers != null && !MapXMLHelper.s_territoyConnections.isEmpty())
			return;
		final Map<String, List<Area>> territoryAreas = new HashMap<String, List<Area>>();
		for (final String territoryName : s_polygons.keySet())
		{
			final List<Polygon> listOfPolygons = s_polygons.get(territoryName);
			final List<Area> listOfAreas = new ArrayList<Area>();
			for (final Polygon p : listOfPolygons)
			{
				listOfAreas.add(new Area(p));
			}
			territoryAreas.put(territoryName, listOfAreas);

		}
		final String lineWidth = JOptionPane.showInputDialog(null, "Enter the width of territory border lines on your map? \r\n(eg: 1, or 2, etc.)", "1");
		if (lineWidth == null)
			return;
		int scalePixels = 8;
		double minOverlap = 32;
		try
		{
			final int lineThickness = Integer.parseInt(lineWidth);
			scalePixels = lineThickness * 4;
			minOverlap = scalePixels * 4;
		} catch (final NumberFormatException ex)
		{
		}
		if (JOptionPane.showConfirmDialog(null, "Scale set to " + scalePixels + " pixels larger, and minimum overlap set to " + minOverlap + " pixels. \r\n"
					+ "Do you wish to continue with this? \r\nSelect Yes to continue, Select No to override and change the size.", "Scale and Overlap Size", JOptionPane.YES_NO_OPTION) == 1)
		{
			final String scale = JOptionPane.showInputDialog(null,
						"Enter the number of pixels larger each territory should become? \r\n(Normally 4x bigger than the border line width. eg: 4, or 8, etc)", "4");
			if (scale == null)
				return;
			try
			{
				scalePixels = Integer.parseInt(scale);
			} catch (final NumberFormatException ex)
			{
			}
			final String overlap = JOptionPane.showInputDialog(null,
						"Enter the minimum number of overlapping pixels for a connection? \r\n(Normally 16x bigger than the border line width. eg: 16, or 32, etc.)", "16");
			if (overlap == null)
				return;
			try
			{
				minOverlap = Integer.parseInt(overlap);
			} catch (final NumberFormatException ex)
			{
			}
		}
		
		MapXMLHelper.s_territoyConnections.clear();
		System.out.print("Now Scanning for Connections ... ");
		// sort so that they are in alphabetic order (makes xml's prettier and easier to update in future)
		final List<String> allTerritories = s_polygons == null ? new ArrayList<String>() : new ArrayList<String>(s_polygons.keySet());
		Collections.sort(allTerritories, new AlphanumComparator());
		final List<String> allAreas = new ArrayList<String>(territoryAreas.keySet());
		Collections.sort(allAreas, new AlphanumComparator());
		for (final String territory : allTerritories)
		{
			final LinkedHashSet<String> thisTerritoryConnections = new LinkedHashSet<String>();
			final List<Polygon> currentPolygons = s_polygons.get(territory);
			for (final Polygon currentPolygon : currentPolygons)
			{
				
				final Shape scaledShape = ConnectionFinder.scale(currentPolygon, scalePixels);
				
				for (final String otherTerritory : allAreas)
				{
					if (otherTerritory.equals(territory))
						continue;
					if (thisTerritoryConnections.contains(otherTerritory))
						continue;
					if (MapXMLHelper.s_territoyConnections.get(otherTerritory) != null && MapXMLHelper.s_territoyConnections.get(otherTerritory).contains(territory))
						continue;
					for (final Area otherArea : territoryAreas.get(otherTerritory))
					{
						final Area testArea = new Area(scaledShape);
						testArea.intersect(otherArea);
						if (!testArea.isEmpty() && ConnectionFinder.sizeOfArea(testArea) > minOverlap)
						{
							thisTerritoryConnections.add(otherTerritory);
						}
						else if (!testArea.isEmpty())
						{
							
						}
					}
				}
				MapXMLHelper.s_territoyConnections.put(territory, thisTerritoryConnections);
			}
		}
		System.out.println("finished");
	}
	
	@Override
	protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers)
	{
		g.setColor(Color.GREEN);
		for (final Entry<String, LinkedHashSet<String>> territoryConnection : MapXMLHelper.s_territoyConnections.entrySet())
		{
			final Point center1 = centers.get(territoryConnection.getKey());
			for (final String territory2 : territoryConnection.getValue())
			{
				final Point center2 = centers.get(territory2);
				g.drawLine(center1.x, center1.y, center2.x, center2.y);
			}
		}
	}
	
	protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e)
	{
		if (SwingUtilities.isRightMouseButton(e))
		{
			if (m_selectedTerritory != null)
			{
				m_selectedTerritory = null;
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						imagePanel.repaint();
					}
				});
			}
			return;
		}
		
		boolean repaint = false;
		final Point point = e.getPoint();
		final String territoryName = findTerritoryName(point, s_polygons);
		
		if (territoryName == null)
			return;
		
		if (m_selectedTerritory == null || m_selectedTerritory.equals(territoryName))
		{
			m_selectedTerritory = territoryName;
			repaint = true;
		}
		else
		{
			Collection<String> firstTerritoryConnections;
			String secondterritory;
			if (territoryName.compareTo(m_selectedTerritory) < 0)
			{
				firstTerritoryConnections = MapXMLHelper.s_territoyConnections.get(territoryName);
				secondterritory = m_selectedTerritory;
			}
			else
			{
				firstTerritoryConnections = MapXMLHelper.s_territoyConnections.get(m_selectedTerritory);
				secondterritory = territoryName;
			}
			if (firstTerritoryConnections.contains(secondterritory))
			{
				firstTerritoryConnections.remove(secondterritory);
			}
			else
			{
				firstTerritoryConnections.add(secondterritory);
			}
			m_selectedTerritory = null;
			repaint = true;
		}
		if (repaint)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					imagePanel.repaint();
				}
			});
		}
	}
}
