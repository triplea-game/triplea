package util.triplea.MapXMLCreator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import games.strategy.util.Tuple;
import util.triplea.MapXMLCreator.TerritoryDefinitionDialog.DEFINITION;


final public class CanalDefinitionsPanel extends ImageScrollPanePanel {
  private static final double piHalf = Math.PI / 2;
  private static final String NEW_CANAL_OPTION = "<new Canal>";

  private Set<String> selectedLandTerritories = new TreeSet<String>();
  private Set<String> selectedWaterTerritories = new TreeSet<String>();
  private Optional<String> currentCanalName = Optional.empty();

  private CanalDefinitionsPanel() {}

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    ImageScrollPanePanel.mapXMLCreator = mapXMLCreator;
    final CanalDefinitionsPanel panel = new CanalDefinitionsPanel();
    panel.layout(stepActionPanel);
    mapXMLCreator.setAutoFillAction(new AbstractAction() {
      private static final long serialVersionUID = -8508734371454749752L;

      @Override
      public void actionPerformed(ActionEvent arg0) {
        final int prevCanalCount = MapXMLHelper.canalDefinitions.size();
        if (prevCanalCount > 0) {
          if (JOptionPane.YES_OPTION != MapXMLHelper.showYesNoOptionDialog("Auto-Fill Warning",
              "All current canal definitions will be deleted.\rDo you want to continue with Auto-Fill?",
              JOptionPane.WARNING_MESSAGE))
            return;
          MapXMLHelper.clearCanalDefinitions();
        }
        panel.clearSelection();

        final Map<String, Set<String>> landWaterTerritoyConnections =
            Maps.newHashMap();
        final Map<String, Set<String>> waterLandTerritoyConnections =
            Maps.newHashMap();
        getSplittedTerritoryConnectionsByDefintionsSwitch(landWaterTerritoyConnections, waterLandTerritoyConnections);

        validateAndAddCanalDefinitions(landWaterTerritoyConnections);

        boolean noNewCanalsBuild = MapXMLHelper.canalDefinitions.isEmpty();
        if (noNewCanalsBuild)
          JOptionPane.showMessageDialog(null, "No canals have been build!", "Auto-Fill Result",
              JOptionPane.PLAIN_MESSAGE);
        else {
          JOptionPane.showMessageDialog(null, getHTMLStringFromCanalDefinitions(), "Auto-Fill Result",
              JOptionPane.PLAIN_MESSAGE);
        }
        if (prevCanalCount > 0 || !noNewCanalsBuild)
          panel.repaint();
      }

      /**
       * @return HTML string listing the canal definitions in the following format:
       *         ' - <canal name>: <water territory 1>-<water territory 2>- ...'
       */
      public String getHTMLStringFromCanalDefinitions() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>The following " + MapXMLHelper.canalDefinitions.size() + " canals have been build:");
        for (final Entry<String, Tuple<Set<String>, Set<String>>> canalDef : MapXMLHelper.canalDefinitions
            .entrySet()) {
          sb.append("<br/> - " + canalDef.getKey() + ": ");
          sb.append(Joiner.on("-").join(canalDef.getValue().getFirst().iterator()));
        }
        sb.append("</html>");
        return sb.toString();
      }

      /**
       * @param landWaterTerritoyConnections
       */
      public void validateAndAddCanalDefinitions(final Map<String, Set<String>> landWaterTerritoyConnections) {
        final Map<String, Set<String>> landWaterTerrConnChecks =
            Maps.newHashMap(landWaterTerritoyConnections);
        for (final Entry<String, Set<String>> landWaterTerrConn : landWaterTerritoyConnections.entrySet()) {
          final String landTerr = landWaterTerrConn.getKey();
          final Set<String> waterTerrs = landWaterTerrConn.getValue();
          Set<String> landTerrNeighbors = MapXMLHelper.territoryConnections.get(landTerr);
          if (landTerrNeighbors == null)
            landTerrNeighbors = Sets.newLinkedHashSet();
          landWaterTerrConnChecks.remove(landTerr);
          for (final Entry<String, Set<String>> landWaterTerrConn2 : landWaterTerrConnChecks.entrySet()) {
            validateAndAddCanalDefinition(landTerr, waterTerrs, landTerrNeighbors, landWaterTerrConn2);
          }
        }
      }

      /**
       * @param landTerr
       * @param waterTerrs
       * @param landTerrNeighbors
       * @param landWaterTerrConn2
       * @return
       */
      public void validateAndAddCanalDefinition(final String landTerr, final Set<String> waterTerrs,
          Set<String> landTerrNeighbors, final Entry<String, Set<String>> landWaterTerrConn2) {
        final String landTerr2 = landWaterTerrConn2.getKey();

        Set<String> landTerrNeighbors2 = MapXMLHelper.territoryConnections.get(landTerr2);
        if (landTerrNeighbors2 == null)
          landTerrNeighbors2 = Sets.newLinkedHashSet();

        if (!landTerrNeighbors.contains(landTerr2) && !landTerrNeighbors2.contains(landTerr))
          return;
        final Set<String> waterTerrs2 = new TreeSet<String>(landWaterTerrConn2.getValue());
        waterTerrs2.retainAll(waterTerrs);
        if (waterTerrs2.size() > 1) {
          getWaterTerrsWithAtLeastOneWaterNeighbor(waterTerrs2);
          // create canal only if at least 2 water territories remain
          if (waterTerrs2.size() > 1) {
            final Set<String> newLandSet = new TreeSet<String>();
            newLandSet.add(landTerr);
            newLandSet.add(landTerr2);
            final Tuple<Set<String>, Set<String>> terrTuple =
                Tuple.of(new TreeSet<String>(waterTerrs2), newLandSet);
            MapXMLHelper.putCanalDefinitions("Canal" + MapXMLHelper.canalDefinitions.size(), terrTuple);
          }
        }
        return;
      }

      /**
       * @param waterTerrs2
       */
      public void getWaterTerrsWithAtLeastOneWaterNeighbor(final Set<String> waterTerrs2) {
        final Set<String> waterTerrs2Copy = new TreeSet<String>(waterTerrs2);
        for (Iterator<String> iter_waterTerr2 = waterTerrs2.iterator(); iter_waterTerr2.hasNext();) {
          final String waterTerr2 = (String) iter_waterTerr2.next();
          waterTerrs2Copy.remove(waterTerr2);
          final Set<String> waterTerrs2ReqNeighbors = new TreeSet<String>(waterTerrs2Copy);
          final Set<String> waterTerr2Neightbors = MapXMLHelper.territoryConnections.get(waterTerr2);
          if (waterTerr2Neightbors != null) {
            for (final String waterTerr2Neighbor : waterTerr2Neightbors)
              waterTerrs2ReqNeighbors.remove(waterTerr2Neighbor);
          }
          if (!waterTerrs2ReqNeighbors.isEmpty())
            iter_waterTerr2.remove();
        }
      }

      /**
       * @param landWaterTerritoyConnections
       * @param waterLandTerritoyConnections
       */
      public void getSplittedTerritoryConnectionsByDefintionsSwitch(
          final Map<String, Set<String>> landWaterTerritoyConnections,
          final Map<String, Set<String>> waterLandTerritoyConnections) {
        for (final Entry<String, Set<String>> terrConn : MapXMLHelper.territoryConnections.entrySet()) {
          if (MapXMLHelper.territoryDefintions.get(terrConn.getKey()).get(DEFINITION.IS_WATER)) {
            @SuppressWarnings("unchecked")
            final Set<String> landTerrValue = (Set<String>) terrConn.getValue().stream()
                .filter(terr -> !MapXMLHelper.territoryDefintions.get(terr).get(DEFINITION.IS_WATER));
            if (!landTerrValue.isEmpty())
              waterLandTerritoyConnections.put(terrConn.getKey(), landTerrValue);
          } else {
            @SuppressWarnings("unchecked")
            final Set<String> waterTerrValue = (Set<String>) terrConn.getValue().stream()
                .filter(terr -> MapXMLHelper.territoryDefintions.get(terr).get(DEFINITION.IS_WATER));
            landWaterTerritoyConnections.put(terrConn.getKey(), waterTerrValue);
          }
        }
        for (final Entry<String, Set<String>> terrConn : waterLandTerritoyConnections.entrySet()) {
          final String waterTerr = terrConn.getKey();
          for (final String landTerr : terrConn.getValue()) {
            Set<String> waterTerrs = landWaterTerritoyConnections.get(landTerr);
            if (waterTerrs == null) {
              final Set<String> newWaterTerrs = Sets.newLinkedHashSet();
              newWaterTerrs.add(waterTerr);
              landWaterTerritoyConnections.put(landTerr, newWaterTerrs);
            } else
              waterTerrs.add(waterTerr);
          }

        }
      }
    });
  }

  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {
    if (selectedLandTerritories.contains(centerName) || selectedWaterTerritories.contains(centerName)) {
      final Rectangle2D stringBounds = fontMetrics.getStringBounds(centerName, g);
      g.setColor(Color.yellow);
      g.fillRect(Math.max(0, x_text_start - 2), Math.max(0, item.y - 6), (int) stringBounds.getWidth() + 4,
          (int) stringBounds.getHeight());
      g.setColor(Color.red);
      g.drawString(centerName, Math.max(0, x_text_start), item.y + 5);
    }
    g.setColor(Color.red);
  }

  protected void paintPreparation(final Map<String, Point> centers) {}

  @Override
  protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers) {
    Graphics2D g2d = (Graphics2D) g;
    for (final Entry<String, Tuple<Set<String>, Set<String>>> canalDef : MapXMLHelper.canalDefinitions
        .entrySet()) {
      // Water Territories
      g.setColor(Color.BLUE);
      final String canalName = canalDef.getKey();
      final FontMetrics fontMetrics = g.getFontMetrics();
      Set<String> terrSet1 = canalDef.getValue().getFirst();
      Set<String> remainingTerrs = new TreeSet<String>(terrSet1);
      for (final String terr1 : terrSet1) {
        final Point center1 = centers.get(terr1);
        remainingTerrs.remove(terr1);
        for (final String terr2 : remainingTerrs) {
          final Point center2 = centers.get(terr2);
          g.drawLine(center1.x, center1.y, center2.x, center2.y);
          Rectangle2D stringBounds = fontMetrics.getStringBounds(canalName, g);
          int dX = center2.x - center1.x;
          int dY = center2.y - center1.y;
          final Point lineCenter = new Point(center1.x + dX / 2, center1.y + dY / 2);
          double centerDistance = center2.distance(center1);
          if (centerDistance > stringBounds.getWidth())
            drawRotate(g2d, lineCenter.x, lineCenter.y, Math.atan2(dY, dX), canalName,
                (int) (stringBounds.getWidth()) / -2);
          else
            g.drawString(canalName, lineCenter.x, lineCenter.y);
        }
      }
      // Land Territories
      g.setColor(Color.GREEN);
      terrSet1 = canalDef.getValue().getSecond();
      remainingTerrs = new TreeSet<String>(terrSet1);
      for (final String terr1 : terrSet1) {
        final Point center1 = centers.get(terr1);
        remainingTerrs.remove(terr1);
        for (final String terr2 : remainingTerrs) {
          final Point center2 = centers.get(terr2);
          g.drawLine(center1.x, center1.y, center2.x, center2.y);
        }
      }
    }

  }

  public static void drawRotate(final Graphics2D g2d, final double x, final double y, double radianAngle,
      final String text, final int xOffset) {
    g2d.translate((float) x, (float) y);
    if (radianAngle > piHalf)
      radianAngle -= Math.PI;
    else if (radianAngle < -piHalf)
      radianAngle += Math.PI;
    g2d.rotate(radianAngle);
    g2d.drawString(text, xOffset, -2);
    g2d.rotate(-radianAngle);
    g2d.translate(-(float) x, -(float) y);
  }

  protected void mouseClickedOnImage(final Map<String, Point> centers, final JPanel imagePanel, final MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      if (currentCanalName.isPresent()
          && (selectedLandTerritories.size() < 2 || selectedWaterTerritories.size() < 2)) {
        if (JOptionPane.YES_OPTION != MapXMLHelper.showYesNoOptionDialog("Canal incomplete",
            "Canal '" + currentCanalName
                + "' is incomplete. A canal needs at least 2 land and 2 water territories.\rDo you want to continue to deselect the canal?",
            JOptionPane.WARNING_MESSAGE))
          return;
        MapXMLHelper.canalDefinitions.remove(currentCanalName);
      }
      currentCanalName = Optional.empty();
      if (!selectedLandTerritories.isEmpty() || !selectedWaterTerritories.isEmpty()) {
        clearSelection();
        SwingUtilities.invokeLater(() -> imagePanel.repaint());
      }
      return;
    }

    final Point point = e.getPoint();
    final String newTerrName = findTerritoryName(point, polygons);

    if (newTerrName == null)
      return;
    Boolean newTerrIsWater = MapXMLHelper.territoryDefintions.get(newTerrName).get(DEFINITION.IS_WATER);
    if (newTerrIsWater == null)
      newTerrIsWater = false;

    final Set<String> newTerrNeighborsDiffType = getNeighborsByType(newTerrName, !newTerrIsWater);

    final ArrayList<String> terrCanals = new ArrayList<String>();
    if (newTerrIsWater) {
      for (final Entry<String, Tuple<Set<String>, Set<String>>> canalDef : MapXMLHelper.canalDefinitions
          .entrySet()) {
        if (canalDef.getValue().getFirst().contains(newTerrName))
          terrCanals.add(canalDef.getKey());
      }
    } else {
      for (final Entry<String, Tuple<Set<String>, Set<String>>> canalDef : MapXMLHelper.canalDefinitions
          .entrySet()) {
        if (canalDef.getValue().getSecond().contains(newTerrName))
          terrCanals.add(canalDef.getKey());
      }
    }

    if (currentCanalName.isPresent()) {
      if (newTerrNeighborsDiffType.size() < 2) {
        JOptionPane.showMessageDialog(null, "The selected " + (newTerrIsWater ? "water" : "land")
            + " territory is connected to less than 2 " + (!newTerrIsWater ? "water" : "land")
            + " territories!", "Input Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
      if (!terrCanals.isEmpty()) {
        terrCanals.add(NEW_CANAL_OPTION);
        currentCanalName = Optional.ofNullable((String) JOptionPane.showInputDialog(null,
            "Which canal should be selected for territory '" + newTerrName + "?", "Choose Canal",
            JOptionPane.QUESTION_MESSAGE,
            null, terrCanals.toArray(new String[terrCanals.size()]), // Array of choices
            terrCanals.get(0))); // Initial choice
      }
      if (terrCanals.isEmpty() || NEW_CANAL_OPTION.equals(currentCanalName)) {
        final String suggestedCanalName = getSuggestedCanalName();
        currentCanalName = Optional.ofNullable(JOptionPane.showInputDialog(null,
            "Which canal should be selected for territory '" + newTerrName + "?", suggestedCanalName));

        while (MapXMLHelper.canalDefinitions.keySet().contains(currentCanalName)) {
          JOptionPane.showMessageDialog(null, "The canal name " + currentCanalName + " is already in use!",
              "Input Error", JOptionPane.ERROR_MESSAGE);
          currentCanalName = Optional.ofNullable(JOptionPane.showInputDialog(null,
              "Which canal should be selected for territory '" + newTerrName + "?", currentCanalName));
        }
      } else if (currentCanalName.isPresent()) {
        final Tuple<Set<String>, Set<String>> canalTerrs =
            MapXMLHelper.canalDefinitions.get(currentCanalName);
        selectedWaterTerritories = canalTerrs.getFirst();
        selectedLandTerritories = canalTerrs.getSecond();
        SwingUtilities.invokeLater(() -> imagePanel.repaint());
        return;
      }
      if (!currentCanalName.isPresent())
        return;
    }

    if (currentCanalName.isPresent()) {
      Tuple<Set<String>, Set<String>> canalTerrs = MapXMLHelper.canalDefinitions.get(currentCanalName);
      if (canalTerrs == null) {
        canalTerrs = Tuple.of(new TreeSet<String>(), new TreeSet<String>());
        MapXMLHelper.putCanalDefinitions(currentCanalName.get(), canalTerrs);
      }

      selectedWaterTerritories = canalTerrs.getFirst();
      selectedLandTerritories = canalTerrs.getSecond();

      final Set<String> selectedTerrsSameType;
      if (newTerrIsWater)
        selectedTerrsSameType = selectedWaterTerritories;
      else
        selectedTerrsSameType = selectedLandTerritories;

      if (selectedTerrsSameType.size() > 0) {
        final Set<String> commonNeighborsDiffType =
            getCommonNeighborsOfType(selectedTerrsSameType, !newTerrIsWater);
        commonNeighborsDiffType.retainAll(newTerrNeighborsDiffType);

        if (commonNeighborsDiffType.size() < 2) {
          JOptionPane.showMessageDialog(null,
              getMessageTextOnToFewSuitableNeighbors(newTerrIsWater, selectedTerrsSameType.size()), "Input Error",
              JOptionPane.ERROR_MESSAGE);
          return;
        }
      }

      if (selectedTerrsSameType.contains(newTerrName))
        selectedTerrsSameType.remove(newTerrName);
      else
        selectedTerrsSameType.add(newTerrName);

    }

    SwingUtilities.invokeLater(() -> imagePanel.repaint());
  }

  /**
   * @param newTerrIsWater
   * @param selectedTerrsSameTypeCount
   * @return
   */
  private String getMessageTextOnToFewSuitableNeighbors(final Boolean newTerrIsWater,
      final int selectedTerrsSameTypeCount) {
    return "The selected " + (newTerrIsWater ? "water" : "land")
        + " territory is connected to less than 2 common " + (!newTerrIsWater ? "water" : "land")
        + " territories with the other " + (newTerrIsWater ? "water" : "land") + " territor"
        + (selectedTerrsSameTypeCount == 1 ? "y" : "ies")
        + "!\rRight click to deselect current canal '" + currentCanalName + "'.";
  }

  /**
   * @return suggested canal name
   */
  private String getSuggestedCanalName() {
    String suggestedCanalName;
    int counter = MapXMLHelper.canalDefinitions.size();
    do {
      suggestedCanalName = "Canal" + counter;
      ++counter;
    } while (MapXMLHelper.canalDefinitions.keySet().contains(suggestedCanalName));
    return suggestedCanalName;
  }

  protected void clearSelection() {
    currentCanalName = Optional.empty();
    selectedLandTerritories = new TreeSet<String>();
    selectedWaterTerritories = new TreeSet<String>();
  }

  protected Set<String> getNeighborsByType(final String newTerrName, final boolean waterNeighbors) {
    final Set<String> neighborsByType = Sets.newLinkedHashSet();
    final Set<String> neighbors = MapXMLHelper.territoryConnections.get(newTerrName);
    for (final Entry<String, Set<String>> terrConnEntry : MapXMLHelper.territoryConnections.entrySet()) {
      if (MapXMLHelper.territoryDefintions.get(terrConnEntry.getKey()).get(DEFINITION.IS_WATER) == waterNeighbors
          && terrConnEntry.getValue().contains(newTerrName))
        neighborsByType.add(terrConnEntry.getKey());
    }
    if (neighbors != null) {
      for (final String neighbor : neighbors) {
        if (MapXMLHelper.territoryDefintions.get(neighbor).get(DEFINITION.IS_WATER) == waterNeighbors)
          neighborsByType.add(neighbor);
      }
    }
    return neighborsByType;
  }

  private Set<String> getCommonNeighborsOfType(final Set<String> terrList, final boolean typeIsWater) {
    final Set<String> commonNeighborsOfType = new TreeSet<String>();
    final Map<String, Collection<String>> neighborsMap = Maps.newHashMap();
    for (final String terr : terrList)
      neighborsMap.put(terr, new ArrayList<String>());
    for (final Entry<String, Set<String>> terrConnEntry : MapXMLHelper.territoryConnections.entrySet()) {
      final String terr1 = terrConnEntry.getKey();
      if (terrList.contains(terr1)) {
        for (final String terr2 : terrConnEntry.getValue()) {
          if (MapXMLHelper.territoryDefintions.get(terr2).get(DEFINITION.IS_WATER) == typeIsWater)
            neighborsMap.get(terr1).add(terr2);
        }
      } else {
        if (MapXMLHelper.territoryDefintions.get(terr1).get(DEFINITION.IS_WATER) == typeIsWater) {
          SortedSet<String> selectedTerritoriesCopy = new TreeSet<String>(terrList);
          selectedTerritoriesCopy.retainAll(terrConnEntry.getValue());
          for (final String terr2 : selectedTerritoriesCopy) {
            neighborsMap.get(terr2).add(terr1);
          }
        }
      }
    }
    commonNeighborsOfType.addAll(neighborsMap.values().iterator().next());
    if (commonNeighborsOfType.size() >= 2) {
      for (final Collection<String> waterNeighors : neighborsMap.values()) {
        commonNeighborsOfType.retainAll(waterNeighors);
        if (commonNeighborsOfType.size() < 2)
          break;
      }
    }
    return commonNeighborsOfType;
  }
}
