package util.triplea.MapXMLCreator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import util.triplea.MapXMLCreator.TerritoryDefinitionDialog.DEFINITION;

/**
 * 
 * @author Erik von der Osten
 * 
 */
class TerritoryDefinitionsPanel extends ImageScrollPanePanel
{
	
	private TerritoryDefinitionsPanel()
	{
		s_instance = this;
	}
	
	public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel)
	{
		s_mapXMLCreator = mapXMLCreator;
		new TerritoryDefinitionsPanel().layout(stepActionPanel);
	}

	protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics, final Point item, final int x_text_start)
	{
		final HashMap<DEFINITION, Boolean> territoryDefinition = MapXMLHelper.s_territoyDefintions.get(centerName);
		if (territoryDefinition != null)
		{
			final int y_value = item.y + 10;
			short definition_count = 0;
			g.setFont(g.getFontMetrics().getFont().deriveFont(Font.BOLD));
			final FontMetrics fm = g.getFontMetrics();
			int h = fm.getAscent();
			for (Entry<DEFINITION, Boolean> definitionEntry : territoryDefinition.entrySet())
			{
				if (definitionEntry.getValue())
				{
					final int x_value = x_text_start + 17 * definition_count;
					int w;
					switch (definitionEntry.getKey())
					{
						case IS_WATER:
							g.setColor(Color.blue);
							g.fillOval(x_value, y_value, 16, 16);
							g.setColor(Color.red);
							w = fm.stringWidth("W");
							h = fm.getAscent();
							g.drawString("W", x_value + 8 - (w / 2), y_value + 8 + (h / 2));
							break;
						case IS_VICTORY_CITY:
							g.setColor(Color.yellow);
							g.fillOval(x_value, y_value, 16, 16);
							g.setColor(Color.red);
							w = fm.stringWidth("V");
							h = fm.getAscent();
							g.drawString("V", x_value + 8 - (w / 2), y_value + 8 + (h / 2));
							break;
						case IMPASSABLE:
							g.setColor(Color.gray);
							g.fillOval(x_value, y_value, 16, 16);
							g.setColor(Color.red);
							w = fm.stringWidth("I");
							h = fm.getAscent();
							g.drawString("I", x_value + 8 - (w / 2), y_value + 8 + (h / 2));
							break;
						case IS_CAPITAL:
							g.setColor(Color.green);
							g.fillOval(x_value, y_value, 16, 16);
							g.setColor(Color.red);
							w = fm.stringWidth("C");
							h = fm.getAscent();
							g.drawString("C", x_value + 8 - (w / 2), y_value + 8 + (h / 2));
							break;
						default:
							break;
					}
				}
				++definition_count;
			}
			g.setColor(Color.red);
			g.setFont(g.getFontMetrics().getFont().deriveFont(Font.PLAIN));
		}
	}
	
	protected void paintPreparation(final Map<String, Point> centers)
	{
		if (!MapXMLCreator.s_waterFilterString.isEmpty() && MapXMLHelper.s_territoyDefintions.isEmpty())
		{
			for (final String centerName : centers.keySet())
			{
				final HashMap<DEFINITION, Boolean> territoyDefintion = new HashMap<TerritoryDefinitionDialog.DEFINITION, Boolean>();
				if (centerName.startsWith(MapXMLCreator.s_waterFilterString))
				{
					territoyDefintion.put(DEFINITION.IS_WATER, true);
				}
				MapXMLHelper.s_territoyDefintions.put(centerName, territoyDefintion);
			}
		}
		else
		{
			for (final String centerName : centers.keySet())
			{
				final HashMap<DEFINITION, Boolean> territoyDefintion = new HashMap<TerritoryDefinitionDialog.DEFINITION, Boolean>();
				MapXMLHelper.s_territoyDefintions.put(centerName, territoyDefintion);
			}
		}
	}
	
	@Override
	protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers)
	{
	}
	
	protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e)
	{
		final Point point = e.getPoint();
		final String territoryName = findTerritoryName(point, s_polygons);
		if (SwingUtilities.isRightMouseButton(e))
		{
			String territoryNameNew = JOptionPane.showInputDialog(imagePanel, "Enter the territory name:", territoryName);
			if (territoryNameNew == null || territoryNameNew.trim().length() == 0)
				return;
			if (!territoryName.equals(territoryNameNew) && centers.containsKey(territoryNameNew)
						&& JOptionPane.showConfirmDialog(imagePanel, "Another center exists with the same name. Are you sure you want to replace it with this one?") != 0)
				return;
			centers.put(territoryNameNew, centers.get(territoryName));
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					HashMap<DEFINITION, Boolean> territoyDefintions = MapXMLHelper.s_territoyDefintions.get(territoryName);
					if (territoyDefintions == null)
						territoyDefintions = new HashMap<DEFINITION, Boolean>();
					new TerritoryDefinitionDialog(s_mapXMLCreator, territoryName, territoyDefintions);
					imagePanel.repaint();
				}
			});
		}
	}
}
