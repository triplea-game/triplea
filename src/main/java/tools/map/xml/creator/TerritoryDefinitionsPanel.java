package tools.map.xml.creator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import games.strategy.ui.Util;


class TerritoryDefinitionsPanel extends ImageScrollPanePanel {

  private TerritoryDefinitionsPanel() {}

  public static void layout(final MapXmlCreator mapXmlCreator) {
    setMapXmlCreator(mapXmlCreator);
    new TerritoryDefinitionsPanel().layout(mapXmlCreator.getStepActionPanel());
  }

  @Override
  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int textStartX) {
    final Map<TerritoryDefinitionDialog.Definition, Boolean> territoryDefinition =
        MapXmlHelper.getTerritoryDefintionsMap().get(centerName);
    if (territoryDefinition != null) {
      final int y_value = item.y + 10;
      short definitionCount = 0;
      g.setFont(g.getFontMetrics().getFont().deriveFont(Font.BOLD));
      final FontMetrics fm = g.getFontMetrics();
      int h = fm.getAscent();
      final int oneCharacterWidthSpace = 17;
      for (final Entry<TerritoryDefinitionDialog.Definition, Boolean> definitionEntry : territoryDefinition
          .entrySet()) {
        if (definitionEntry.getValue()) {
          final int x_value = textStartX + oneCharacterWidthSpace * definitionCount;
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
            default:
              throw new IllegalStateException("No valid value for " + TerritoryDefinitionDialog.Definition.class);
          }
          g.fillOval(x_value, y_value, 16, 16);
          g.setColor(Color.red);
          final int w = fm.stringWidth(character);
          h = fm.getAscent();
          g.drawString(character, x_value + 8 - (w / 2), y_value + 8 + (h / 2));
        }
        ++definitionCount;
      }
      g.setColor(Color.red);
      g.setFont(g.getFontMetrics().getFont().deriveFont(Font.PLAIN));
    }
  }

  @Override
  protected void paintPreparation(final Map<String, Point> centers) {
    if (!MapXmlCreator.waterFilterString.isEmpty() && MapXmlHelper.getTerritoryDefintionsMap().isEmpty()) {
      for (final String centerName : centers.keySet()) {
        final HashMap<TerritoryDefinitionDialog.Definition, Boolean> territoyDefintion =
            Maps.newHashMap();
        if (centerName.startsWith(MapXmlCreator.waterFilterString)) {
          territoyDefintion.put(TerritoryDefinitionDialog.Definition.IS_WATER, true);
        }
        MapXmlHelper.putTerritoryDefintions(centerName, territoyDefintion);
      }
    } else {
      for (final String centerName : centers.keySet()) {
        final HashMap<TerritoryDefinitionDialog.Definition, Boolean> territoyDefintion =
            Maps.newHashMap();
        MapXmlHelper.putTerritoryDefintions(centerName, territoyDefintion);
      }
    }
  }

  @Override
  protected void paintOwnSpecifics(final Graphics g, final Map<String, Point> centers) {}

  @Override
  protected void mouseClickedOnImage(final Map<String, Point> centers, final MouseEvent e) {
    final Optional<String> territoryNameOptional = Util.findTerritoryName(e.getPoint(), polygons);
    if (!territoryNameOptional.isPresent()) {
      return;
    }
    final String territoryName = territoryNameOptional.get();

    if (SwingUtilities.isRightMouseButton(e)) {
      final String territoryNameNew =
          JOptionPane.showInputDialog(getImagePanel(), "Enter the territory name:", territoryName);
      if (territoryNameNew == null || territoryNameNew.trim().length() == 0) {
        return;
      }
      if (!territoryName.equals(territoryNameNew) && centers.containsKey(territoryNameNew)
          && JOptionPane.showConfirmDialog(getImagePanel(),
              "Another center exists with the same name. Are you sure you want to replace it with this one?") != 0) {
        return;
      }
      centers.put(territoryNameNew, centers.get(territoryName));
    } else {
      SwingUtilities.invokeLater(() -> {
        Map<TerritoryDefinitionDialog.Definition, Boolean> territoyDefintions =
            MapXmlHelper.getTerritoryDefintionsMap().get(territoryName);
        if (territoyDefintions == null) {
          territoyDefintions = Maps.newHashMap();
        }
        new TerritoryDefinitionDialog(getMapXmlCreator(), territoryName, territoyDefintions);
        getImagePanel().repaint();
      });
    }
  }
}
