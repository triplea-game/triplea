package util.triplea.MapXMLCreator;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import games.strategy.util.AlphanumComparator;
import util.image.ConnectionFinder;


public class TerritoryConnectionsPanel extends ImageScrollPanePanel {

  private Optional<String> selectedTerritory = Optional.empty();

  private TerritoryConnectionsPanel() {}

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    ImageScrollPanePanel.mapXMLCreator = mapXMLCreator;
    final TerritoryConnectionsPanel panel = new TerritoryConnectionsPanel();
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
    if (centerName.equals(selectedTerritory)) {
      final Rectangle2D stringBounds = fontMetrics.getStringBounds(centerName, g);
      g.setColor(Color.yellow);
      final int xRectPadding = 2;
      final int yDiffCenterToRectTop = -6;
      final int yDiffCenterToStringBottom = 5;
      g.fillRect(Math.max(0, x_text_start - xRectPadding), Math.max(0, item.y + yDiffCenterToRectTop),
          (int) stringBounds.getWidth() + 2 * xRectPadding,
          (int) stringBounds.getHeight());
      g.setColor(Color.red);
      g.drawString(centerName, Math.max(0, x_text_start), item.y + yDiffCenterToStringBottom);
    }
    g.setColor(Color.red);
  }

  protected void paintPreparation(final Map<String, Point> centers) {
    if (centers != null && !MapXMLHelper.territoryConnections.isEmpty())
      return;
    final Map<String, List<Area>> territoryAreas = getTerritoryAreasFromPolygons();
    final int lineThickness = showInputDialogForPositiveIntegerInput(
        "Enter the width of territory border lines on your map? \r(eg: 1, or 2, etc.)", "1");
    if (lineThickness == 0) {
      return;
    }
    int scalePixels = 8;
    double minOverlap = 32;
    scalePixels = lineThickness * 4;
    minOverlap = scalePixels * 4;
    if (JOptionPane.showConfirmDialog(null,
        "Scale set to " + scalePixels + " pixels larger, and minimum overlap set to " + minOverlap + " pixels. \r"
            + "Do you wish to continue with this? \rSelect Yes to continue, Select No to override and change the size.",
        "Scale and Overlap Size", JOptionPane.YES_NO_OPTION) == 1) {
      scalePixels = showInputDialogForPositiveIntegerInput(
          "Enter the number of pixels larger each territory should become? \r(Normally 4x bigger than the border line width. eg: 4, or 8, etc)",
          "4");
      if (scalePixels == 0) {
        return;
      }
      minOverlap = showInputDialogForPositiveIntegerInput(
          "Enter the minimum number of overlapping pixels for a connection? \r(Normally 16x bigger than the border line width. eg: 16, or 32, etc.)",
          "16");
      if (minOverlap == 0) {
        return;
      }
    }

    MapXMLHelper.clearTerritoryConnections();
    System.out.print("Now Scanning for Connections ... ");
    // sort so that they are in alphabetic order (makes xml's prettier and easier to update in future)
    final List<String> allTerritories =
        polygons == null ? new ArrayList<String>() : new ArrayList<String>(polygons.keySet());
    Collections.sort(allTerritories, new AlphanumComparator());
    final List<String> allAreas = new ArrayList<String>(territoryAreas.keySet());
    Collections.sort(allAreas, new AlphanumComparator());
    for (final String territory : allTerritories) {
      final Set<String> thisTerritoryConnections = Sets.newLinkedHashSet();
      final List<Polygon> currentPolygons = polygons.get(territory);
      for (final Polygon currentPolygon : currentPolygons) {

        final Shape scaledShape = ConnectionFinder.scale(currentPolygon, scalePixels);

        for (final String otherTerritory : allAreas) {
          if (otherTerritory.equals(territory))
            continue;
          if (thisTerritoryConnections.contains(otherTerritory))
            continue;
          if (MapXMLHelper.territoryConnections.get(otherTerritory) != null
              && MapXMLHelper.territoryConnections.get(otherTerritory).contains(territory))
            continue;
          for (final Area otherArea : territoryAreas.get(otherTerritory)) {
            final Area testArea = new Area(scaledShape);
            testArea.intersect(otherArea);
            if (!testArea.isEmpty() && ConnectionFinder.sizeOfArea(testArea) > minOverlap) {
              thisTerritoryConnections.add(otherTerritory);
            }
          }
        }
        MapXMLHelper.putTerritoryConnections(territory, thisTerritoryConnections);
      }
    }
    System.out.println("finished");
  }

  private Map<String, List<Area>> getTerritoryAreasFromPolygons() {
    final Map<String, List<Area>> territoryAreas = Maps.newHashMap();
    for (final String territoryName : polygons.keySet()) {
      final List<Polygon> listOfPolygons = polygons.get(territoryName);
      final List<Area> listOfAreas = new ArrayList<Area>();
      for (final Polygon p : listOfPolygons) {
        listOfAreas.add(new Area(p));
      }
      territoryAreas.put(territoryName, listOfAreas);

    }
    return territoryAreas;
  }

  /**
   * Forces the user to either enter nothing or a positive integer
   * 
   * @return input value or 0 if nothing has been entered
   */
  public int showInputDialogForPositiveIntegerInput(final String message, final String suggestedInput) {
    String lineWidth = suggestedInput;
    while (1 > 0) {
      lineWidth = JOptionPane.showInputDialog(null,
          message, lineWidth);
      if (lineWidth == null) {
        return 0;
      }
      try {
        return Integer.parseInt(lineWidth);
      } catch (final NumberFormatException ex) {
        JOptionPane.showMessageDialog(null, "'" + lineWidth + "' is not a valid positive integer value.",
            "Invalid Input",
            JOptionPane.ERROR_MESSAGE);
        // do-loop again
      }
    }
  }

  @Override
  protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers) {
    g.setColor(Color.GREEN);
    for (final Entry<String, Set<String>> territoryConnection : MapXMLHelper.territoryConnections
        .entrySet()) {
      final Point center1 = centers.get(territoryConnection.getKey());
      for (final String territory2 : territoryConnection.getValue()) {
        final Point center2 = centers.get(territory2);
        g.drawLine(center1.x, center1.y, center2.x, center2.y);
      }
    }
  }

  protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      if (selectedTerritory != null) {
        selectedTerritory = null;
        SwingUtilities.invokeLater(() -> imagePanel.repaint());
      }
      return;
    }

    boolean repaint = false;
    final Point point = e.getPoint();
    final String territoryName = findTerritoryName(point, polygons);

    if (territoryName == null)
      return;

    if (!selectedTerritory.isPresent() || selectedTerritory.equals(territoryName)) {
      selectedTerritory = Optional.of(territoryName);
      repaint = true;
    } else {
      Collection<String> firstTerritoryConnections;
      String secondterritory;
      if (territoryName.compareTo(selectedTerritory.get()) < 0) {
        firstTerritoryConnections = MapXMLHelper.territoryConnections.get(territoryName);
        secondterritory = selectedTerritory.get();
      } else {
        firstTerritoryConnections = MapXMLHelper.territoryConnections.get(selectedTerritory);
        secondterritory = territoryName;
      }
      if (firstTerritoryConnections.contains(secondterritory)) {
        firstTerritoryConnections.remove(secondterritory);
      } else {
        firstTerritoryConnections.add(secondterritory);
      }
      selectedTerritory = null;
      repaint = true;
    }
    if (repaint) {
      SwingUtilities.invokeLater(() -> imagePanel.repaint());
    }
  }
}
