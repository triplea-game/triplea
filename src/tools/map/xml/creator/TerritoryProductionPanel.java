package tools.map.xml.creator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Optional;

import javax.swing.JOptionPane;

import games.strategy.common.swing.SwingAction;
import games.strategy.ui.Util;


public class TerritoryProductionPanel extends ImageScrollPanePanel {

  private TerritoryProductionPanel() {}

  public static void layout(final MapXmlCreator mapXmlCreator) {
    setMapXmlCreator(mapXmlCreator);
    final TerritoryProductionPanel panel = new TerritoryProductionPanel();
    panel.layout(mapXmlCreator.getStepActionPanel());
    mapXmlCreator.setAutoFillAction(SwingAction.of(Exception -> {
      panel.paintPreparation(null);
      panel.repaint();
    }));
  }

  @Override
  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {

    final Integer production = MapXmlHelper.getTerritoyProductionsMap().get(centerName);
    if (production != null && production > 0) {
      final String productionString = production.toString();
      final Rectangle2D prodStringBounds = fontMetrics.getStringBounds(productionString, g);
      final Rectangle2D centerStringBounds = fontMetrics.getStringBounds(centerName, g);
      final double wDiff = (centerStringBounds.getWidth() - prodStringBounds.getWidth()) / 2;
      g.setColor(Color.yellow);
      g.fillRect(Math.max(0, x_text_start - 2 + (int) wDiff), item.y + 6, (int) prodStringBounds.getWidth() + 4,
          (int) prodStringBounds.getHeight());
      g.setColor(Color.red);
      g.drawString(productionString, Math.max(0, x_text_start + (int) wDiff), item.y + 17);
    }
    g.setColor(Color.red);
  }

  @Override
  protected void paintPreparation(final Map<String, Point> centers) {
    if (centers != null && !MapXmlHelper.getTerritoyProductionsMap().isEmpty()) {
      return;
    }
    for (final String territoryName : polygons.keySet()) {
      MapXmlHelper.putTerritoyProductions(territoryName, 0);

    }
  }

  @Override
  protected void paintOwnSpecifics(final Graphics g, final Map<String, Point> centers) {
    // nothing to do
  }

  @Override
  protected void mouseClickedOnImage(final Map<String, Point> centers, final MouseEvent e) {
    final Optional<String> territoryNameOptional = Util.findTerritoryName(e.getPoint(), polygons);
    if (!territoryNameOptional.isPresent()) {
      return;
    }
    final String territoryName = territoryNameOptional.get();

    final Integer currValue = MapXmlHelper.getTerritoyProductionsMap().get(territoryName);
    final String inputText = JOptionPane.showInputDialog(null,
        "Enter the new production value for territory " + territoryName + ":", (currValue != null ? currValue : 0));
    try {
      final Integer newValue = Integer.parseInt(inputText);
      MapXmlHelper.putTerritoyProductions(territoryName, newValue);
    } catch (final NumberFormatException nfe) {
      JOptionPane.showMessageDialog(null, "'" + inputText + "' is no integer value.", "Input error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    repaint();
  }
}
