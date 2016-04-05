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


public class TerritoryProductionPanel extends ImageScrollPanePanel {

  private TerritoryProductionPanel() {}

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    ImageScrollPanePanel.mapXMLCreator = mapXMLCreator;
    final TerritoryProductionPanel panel = new TerritoryProductionPanel();
    panel.layout(stepActionPanel);
    mapXMLCreator.setAutoFillAction(new AbstractAction() {
      private static final long serialVersionUID = -8508734371454749752L;

      @Override
      public void actionPerformed(ActionEvent arg0) {
        panel.paintPreparation(null);
        panel.repaint();
      }
    });
  }

  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {

    final Integer production = MapXMLHelper.territoyProductions.get(centerName);
    if (production != null && production > 0) {
      final String productionString = production.toString();
      final Rectangle2D prodStringBounds = fontMetrics.getStringBounds(productionString, g);
      final Rectangle2D centerStringBounds = fontMetrics.getStringBounds(centerName, g);
      double wDiff = (centerStringBounds.getWidth() - prodStringBounds.getWidth()) / 2;
      g.setColor(Color.yellow);
      g.fillRect(Math.max(0, x_text_start - 2 + (int) wDiff), item.y + 6, (int) prodStringBounds.getWidth() + 4,
          (int) prodStringBounds.getHeight());
      g.setColor(Color.red);
      g.drawString(productionString, Math.max(0, x_text_start + (int) wDiff), item.y + 17);
    }
    g.setColor(Color.red);
  }

  protected void paintPreparation(final Map<String, Point> centers) {
    if (centers != null && !MapXMLHelper.territoyProductions.isEmpty())
      return;
    for (final String territoryName : polygons.keySet()) {
      MapXMLHelper.putTerritoyProductions(territoryName, 0);

    }
  }

  @Override
  protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers) {
    // nothing to do
  }

  protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e) {
    final Point point = e.getPoint();
    final String territoryName = findTerritoryName(point, polygons);

    if (territoryName == null)
      return;

    final Integer currValue = MapXMLHelper.territoyProductions.get(territoryName);
    String inputText = JOptionPane.showInputDialog(null,
        "Enter the new production value for territory " + territoryName + ":", (currValue != null ? currValue : 0));
    try {
      final Integer newValue = Integer.parseInt(inputText);
      MapXMLHelper.putTerritoyProductions(territoryName, newValue);
    } catch (NumberFormatException nfe) {
      JOptionPane.showMessageDialog(null, "'" + inputText + "' is no integer value.", "Input error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        imagePanel.repaint();
      }
    });
  }
}
