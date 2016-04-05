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


class TerritoryDefinitionsPanel extends ImageScrollPanePanel {

  private TerritoryDefinitionsPanel() {}

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    ImageScrollPanePanel.mapXMLCreator = mapXMLCreator;
    new TerritoryDefinitionsPanel().layout(stepActionPanel);
  }

  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {
    final HashMap<DEFINITION, Boolean> territoryDefinition = MapXMLHelper.territoryDefintions.get(centerName);
    if (territoryDefinition != null) {
      final int y_value = item.y + 10;
      short definition_count = 0;
      g.setFont(g.getFontMetrics().getFont().deriveFont(Font.BOLD));
      final FontMetrics fm = g.getFontMetrics();
      int h = fm.getAscent();
      final int oneCharacterWidthSpace = 17;
      for (final Entry<DEFINITION, Boolean> definitionEntry : territoryDefinition.entrySet()) {
        if (definitionEntry.getValue()) {
          final int x_value = x_text_start + oneCharacterWidthSpace * definition_count;
          int w;
          String character = null;
          switch (definitionEntry.getKey()) {
            case IS_WATER:
              g.setColor(Color.blue);
              character = "W";
              break;
            case IS_VICTORY_CITY:
              g.setColor(Color.yellow);
              character = "V";
              break;
            case IMPASSABLE:
              g.setColor(Color.gray);
              character = "I";
              break;
            case IS_CAPITAL:
              g.setColor(Color.green);
              break;
          }
          g.fillOval(x_value, y_value, 16, 16);
          g.setColor(Color.red);
          w = fm.stringWidth(character);
          h = fm.getAscent();
          g.drawString(character, x_value + 8 - (w / 2), y_value + 8 + (h / 2));
        }
        ++definition_count;
      }
      g.setColor(Color.red);
      g.setFont(g.getFontMetrics().getFont().deriveFont(Font.PLAIN));
    }
  }

  protected void paintPreparation(final Map<String, Point> centers) {
    if (!MapXMLCreator.waterFilterString.isEmpty() && MapXMLHelper.territoryDefintions.isEmpty()) {
      for (final String centerName : centers.keySet()) {
        final HashMap<DEFINITION, Boolean> territoyDefintion =
            new HashMap<TerritoryDefinitionDialog.DEFINITION, Boolean>();
        if (centerName.startsWith(MapXMLCreator.waterFilterString)) {
          territoyDefintion.put(DEFINITION.IS_WATER, true);
        }
        MapXMLHelper.putTerritoryDefintions(centerName, territoyDefintion);
      }
    } else {
      for (final String centerName : centers.keySet()) {
        final HashMap<DEFINITION, Boolean> territoyDefintion =
            new HashMap<TerritoryDefinitionDialog.DEFINITION, Boolean>();
        MapXMLHelper.putTerritoryDefintions(centerName, territoyDefintion);
      }
    }
  }

  @Override
  protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers) {}

  protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e) {
    final Point point = e.getPoint();
    final String territoryName = findTerritoryName(point, polygons);
    if (SwingUtilities.isRightMouseButton(e)) {
      String territoryNameNew = JOptionPane.showInputDialog(imagePanel, "Enter the territory name:", territoryName);
      if (territoryNameNew == null || territoryNameNew.trim().length() == 0)
        return;
      if (!territoryName.equals(territoryNameNew) && centers.containsKey(territoryNameNew)
          && JOptionPane.showConfirmDialog(imagePanel,
              "Another center exists with the same name. Are you sure you want to replace it with this one?") != 0)
        return;
      centers.put(territoryNameNew, centers.get(territoryName));
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          HashMap<DEFINITION, Boolean> territoyDefintions = MapXMLHelper.territoryDefintions.get(territoryName);
          if (territoyDefintions == null)
            territoyDefintions = new HashMap<DEFINITION, Boolean>();
          new TerritoryDefinitionDialog(mapXMLCreator, territoryName, territoyDefintions);
          imagePanel.repaint();
        }
      });
    }
  }
}
