package util.triplea.mapXmlCreator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import games.strategy.common.swing.SwingAction;
import games.strategy.ui.Util;
import util.triplea.mapXmlCreator.TerritoryDefinitionDialog.DEFINITION;


final public class CanalDefinitionsPanel extends ImageScrollPanePanel {
  private static final String MSG_TITLE_AUTO_FILL_RESULT = "Auto-Fill Result";
  // TODO: consider rework HTML strings for XML file creation and parsing
  static final String HTML_CANAL_KEY_POSTFIX = ": ";
  static final String HTML_CANAL_KEY_PREFIX = "<br/> - ";
  private static final double PI_HALF = Math.PI / 2;
  private static final String NEW_CANAL_OPTION = "<new Canal>";

  private Set<String> selectedLandTerritories = new TreeSet<String>();
  private Set<String> selectedWaterTerritories = new TreeSet<String>();
  private Optional<String> currentCanalName = Optional.empty();

  private CanalDefinitionsPanel() {}

  public static void layout(final MapXmlCreator mapXmlCreator) {
    setMapXmlCreator(mapXmlCreator);
    final CanalDefinitionsPanel panel = new CanalDefinitionsPanel();
    panel.layout(mapXmlCreator.getStepActionPanel());
    mapXmlCreator.setAutoFillAction(SwingAction.of(e -> {
      final int prevCanalCount = MapXmlHelper.getCanalDefinitionsMap().size();
      if (prevCanalCount > 0) {
        if (JOptionPane.YES_OPTION != MapXmlUIHelper.showYesNoOptionDialog("Auto-Fill Warning",
            "All current canal definitions will be deleted.\rDo you want to continue with Auto-Fill?",
            JOptionPane.WARNING_MESSAGE)) {
          return;
        }
        MapXmlHelper.clearCanalDefinitions();
      }
      panel.clearSelection();

      final Map<String, Set<String>> landWaterTerritoyConnections = MapXmlData.getLandWaterTerritoryConnections();

      MapXmlHelper.validateAndAddCanalDefinitions(landWaterTerritoyConnections);

      final boolean noNewCanalsBuild = MapXmlHelper.getCanalDefinitionsMap().isEmpty();
      if (noNewCanalsBuild) {
        JOptionPane.showMessageDialog(null, "No canals have been build!", MSG_TITLE_AUTO_FILL_RESULT,
            JOptionPane.PLAIN_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(null, MapXmlHelper.getHtmlStringFromCanalDefinitions(),
            MSG_TITLE_AUTO_FILL_RESULT,
            JOptionPane.PLAIN_MESSAGE);
      }
      if (prevCanalCount > 0 || !noNewCanalsBuild) {
        panel.repaint();
      }

    }));
  }


  @Override
  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int xTextStart) {
    if (selectedLandTerritories.contains(centerName) || selectedWaterTerritories.contains(centerName)) {
      final Rectangle2D stringBounds = fontMetrics.getStringBounds(centerName, g);
      g.setColor(Color.yellow);
      g.fillRect(Math.max(0, xTextStart - 2), Math.max(0, item.y - 6), (int) stringBounds.getWidth() + 4,
          (int) stringBounds.getHeight());
      g.setColor(Color.red);
      g.drawString(centerName, Math.max(0, xTextStart), item.y + 5);
    }
    g.setColor(Color.red);
  }

  @Override
  protected void paintPreparation(final Map<String, Point> centers) {}

  @Override
  protected void paintOwnSpecifics(final Graphics g, final Map<String, Point> centers) {
    final Graphics2D g2d = (Graphics2D) g;
    final FontMetrics fontMetrics = g.getFontMetrics();
    for (final Entry<String, CanalTerritoriesTuple> canalDef : MapXmlHelper.getCanalDefinitionsMap()
        .entrySet()) {
      final Set<String> terrSet1 = canalDef.getValue().getWaterTerritories();
      final Set<String> remainingTerrs = new TreeSet<String>(terrSet1);
      paintOwnSpecificsToWaterTerritories(centers, g2d, fontMetrics, terrSet1, remainingTerrs, canalDef.getKey());
      paintOwnSpecificsToLandTerritories(g, centers, canalDef);
    }

  }

  public void paintOwnSpecificsToLandTerritories(final Graphics g, final Map<String, Point> centers,
      final Entry<String, CanalTerritoriesTuple> canalDef) {
    g.setColor(Color.GREEN);
    final Set<String> terrLandSet = canalDef.getValue().getLandTerritories();
    final Set<String> terrLandRemainingSet = new TreeSet<String>(terrLandSet);
    for (final String terrLand : terrLandSet) {
      final Point centerLandTerr = centers.get(terrLand);
      terrLandRemainingSet.remove(terrLand);
      for (final String terrLandRemaining : terrLandRemainingSet) {
        final Point centerLandRemainingTerr = centers.get(terrLandRemaining);
        g.drawLine(centerLandTerr.x, centerLandTerr.y, centerLandRemainingTerr.x, centerLandRemainingTerr.y);
      }
    }
  }

  public void paintOwnSpecificsToWaterTerritories(final Map<String, Point> centers, final Graphics2D g2d,
      final FontMetrics fontMetrics, final Set<String> terrSet1, final Set<String> remainingTerrs,
      final String canalName) {
    g2d.setColor(Color.BLUE);
    for (final String terr1 : terrSet1) {
      final Point center1 = centers.get(terr1);
      remainingTerrs.remove(terr1);
      for (final String terr2 : remainingTerrs) {
        final Point center2 = centers.get(terr2);
        g2d.drawLine(center1.x, center1.y, center2.x, center2.y);
        final Rectangle2D stringBounds = fontMetrics.getStringBounds(canalName, g2d);
        final int dX = center2.x - center1.x;
        final int dY = center2.y - center1.y;
        final Point lineCenter = new Point(center1.x + dX / 2, center1.y + dY / 2);
        final double centerDistance = center2.distance(center1);
        if (centerDistance > stringBounds.getWidth()) {
          drawRotate(g2d, lineCenter.x, lineCenter.y, Math.atan2(dY, dX), canalName,
              (int) (stringBounds.getWidth()) / -2);
        } else {
          g2d.drawString(canalName, lineCenter.x, lineCenter.y);
        }
      }
    }
  }

  public static void drawRotate(final Graphics2D g2d, final double x, final double y, double radianAngle,
      final String text, final int xOffset) {
    g2d.translate((float) x, (float) y);
    if (radianAngle > PI_HALF) {
      radianAngle -= Math.PI;
    } else if (radianAngle < -PI_HALF) {
      radianAngle += Math.PI;
    }
    g2d.rotate(radianAngle);
    g2d.drawString(text, xOffset, -2);
    g2d.rotate(-radianAngle);
    g2d.translate(-(float) x, -(float) y);
  }

  @Override
  protected void mouseClickedOnImage(final Map<String, Point> centers, final MouseEvent e) {
    if (SwingUtilities.isRightMouseButton(e)) {
      mouseRightClickedOnImage();
      return;
    }

    final Optional<String> newTerrNameOptional = Util.findTerritoryName(e.getPoint(), polygons);
    if (!newTerrNameOptional.isPresent()) {
      return;
    }
    final String newTerrName = newTerrNameOptional.get();

    Boolean newTerrIsWater = MapXmlHelper.getTerritoryDefintionsMap().get(newTerrName).get(DEFINITION.IS_WATER);
    if (newTerrIsWater == null) {
      newTerrIsWater = false;
    }

    final Set<String> newTerrNeighborsDiffType = getNeighborsByType(newTerrName, !newTerrIsWater);

    final List<String> terrCanals = getCanalsLinkedToTerritory(newTerrName, newTerrIsWater);

    if (!evaluateSelectedTerritoryToCurrentCanal(newTerrName, newTerrIsWater, newTerrNeighborsDiffType, terrCanals)) {
      return;
    }

    if (!handleSelectedTerritoryToCurrentCanal(newTerrName, newTerrIsWater, newTerrNeighborsDiffType)) {
      return;
    }

    repaint();
  }

  /**
   * @param newTerrName - territory name
   * @param newTerrIsWater - IS_WATER property of newTerrName territory
   * @param newTerrNeighborsDiffType - list of neighbor territories with/without (defined by newTerrIsWater) property
   *        IS_WATER
   * @return whether handling was successful or not
   */
  private boolean handleSelectedTerritoryToCurrentCanal(final String newTerrName, final Boolean newTerrIsWater,
      final Set<String> newTerrNeighborsDiffType) {
    if (currentCanalName.isPresent()) {
      setSelectedTerritoriesFromTerritory();

      final Set<String> selectedTerrsSameType;
      if (newTerrIsWater) {
        selectedTerrsSameType = selectedWaterTerritories;
      } else {
        selectedTerrsSameType = selectedLandTerritories;
      }

      if (selectedTerrsSameType.size() > 0) {
        final Set<String> commonNeighborsDiffType =
            getCommonNeighborsOfType(selectedTerrsSameType, !newTerrIsWater);
        commonNeighborsDiffType.retainAll(newTerrNeighborsDiffType);

        if (commonNeighborsDiffType.size() < 2) {
          JOptionPane.showMessageDialog(null,
              getMessageTextOnToFewSuitableNeighbors(newTerrIsWater, selectedTerrsSameType.size()), "Input Error",
              JOptionPane.ERROR_MESSAGE);
          return false;
        }
      }

      if (selectedTerrsSameType.contains(newTerrName)) {
        selectedTerrsSameType.remove(newTerrName);
      } else {
        selectedTerrsSameType.add(newTerrName);
      }

    }
    return true;
  }

  private void setSelectedTerritoriesFromTerritory() {
    CanalTerritoriesTuple canalTerrs = MapXmlHelper.getCanalDefinitionsMap().get(currentCanalName);
    if (canalTerrs == null) {
      canalTerrs = new CanalTerritoriesTuple();
      MapXmlHelper.putCanalDefinitions(currentCanalName.get(), canalTerrs);
    }

    selectedWaterTerritories = canalTerrs.getWaterTerritories();
    selectedLandTerritories = canalTerrs.getLandTerritories();
  }


  /**
   * The method evaluated the current selected territory with the current canal name and provides some message feedback
   * in case of problems (either by an error message or by the possibility to refine the selection purpose).
   *
   * @param imagePanel - JPanel
   * @param newTerrName - territory name
   * @param newTerrIsWater - IS_WATER property of newTerrName territory
   * @param newTerrNeighborsDiffType - list of neighbor territories with/without (defined by newTerrIsWater) property
   *        IS_WATER
   * @param terrCanals - list of canals the newTerrName territory is linked to
   * @return evaluation result
   */
  private boolean evaluateSelectedTerritoryToCurrentCanal(final String newTerrName, final Boolean newTerrIsWater,
      final Set<String> newTerrNeighborsDiffType, final List<String> terrCanals) {
    if (currentCanalName.isPresent()) {
      if (newTerrNeighborsDiffType.size() < 2) {
        JOptionPane.showMessageDialog(null, "The selected " + (newTerrIsWater ? "water" : "land")
            + " territory is connected to less than 2 " + (!newTerrIsWater ? "water" : "land")
            + " territories!", "Input Error", JOptionPane.ERROR_MESSAGE);
        return false;
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

        while (MapXmlHelper.getCanalDefinitionsMap().keySet().contains(currentCanalName)) {
          JOptionPane.showMessageDialog(null, "The canal name " + currentCanalName + " is already in use!",
              "Input Error", JOptionPane.ERROR_MESSAGE);
          currentCanalName = Optional.ofNullable(JOptionPane.showInputDialog(null,
              "Which canal should be selected for territory '" + newTerrName + "?", currentCanalName));
        }
      } else if (currentCanalName.isPresent()) {
        final CanalTerritoriesTuple canalTerrs =
            MapXmlHelper.getCanalDefinitionsMap().get(currentCanalName);
        selectedWaterTerritories = canalTerrs.getWaterTerritories();
        selectedLandTerritories = canalTerrs.getLandTerritories();
        repaint();
        return false;
      }
      if (!currentCanalName.isPresent()) {
        return false;
      }
    }
    return true;
  }

  private void mouseRightClickedOnImage() {
    if (currentCanalName.isPresent()
        && (selectedLandTerritories.size() < 2 || selectedWaterTerritories.size() < 2)) {
      if (JOptionPane.YES_OPTION != MapXmlUIHelper.showYesNoOptionDialog("Canal incomplete",
          "Canal '" + currentCanalName
              + "' is incomplete. A canal needs at least 2 land and 2 water territories.\rDo you want to continue to deselect the canal?",
          JOptionPane.WARNING_MESSAGE)) {
        return;
      }
      MapXmlHelper.getCanalDefinitionsMap().remove(currentCanalName);
    }
    currentCanalName = Optional.empty();
    if (!selectedLandTerritories.isEmpty() || !selectedWaterTerritories.isEmpty()) {
      clearSelection();
      SwingUtilities.invokeLater(() -> getImagePanel().repaint());
    }
  }

  /**
   * @param newTerrName - base territory
   * @param newTerrIsWater - IS_WATER property of base territory
   * @param terrCanals - list of canals the base territory is linked to
   */
  private List<String> getCanalsLinkedToTerritory(final String newTerrName, final Boolean newTerrIsWater) {
    final List<String> terrCanals = new ArrayList<String>();
    if (newTerrIsWater) {
      for (final Entry<String, CanalTerritoriesTuple> canalDef : MapXmlHelper.getCanalDefinitionsMap()
          .entrySet()) {
        if (canalDef.getValue().getWaterTerritories().contains(newTerrName)) {
          terrCanals.add(canalDef.getKey());
        }
      }
    } else {
      for (final Entry<String, CanalTerritoriesTuple> canalDef : MapXmlHelper.getCanalDefinitionsMap()
          .entrySet()) {
        if (canalDef.getValue().getLandTerritories().contains(newTerrName)) {
          terrCanals.add(canalDef.getKey());
        }
      }
    }
    return terrCanals;
  }

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
    int counter = MapXmlHelper.getCanalDefinitionsMap().size();
    do {
      suggestedCanalName = "Canal" + counter;
      ++counter;
    } while (MapXmlHelper.getCanalDefinitionsMap().keySet().contains(suggestedCanalName));
    return suggestedCanalName;
  }

  protected void clearSelection() {
    currentCanalName = Optional.empty();
    selectedLandTerritories = new TreeSet<String>();
    selectedWaterTerritories = new TreeSet<String>();
  }

  /**
   *
   * @param newTerrName - territory name
   * @param waterNeighbors - whether IS_WATER property is supposed to be present or not
   * @return list of neighbor territories with/without (defined by waterNeighbors) property IS_WATER
   */
  protected Set<String> getNeighborsByType(final String newTerrName, final boolean waterNeighbors) {
    final Set<String> neighborsByType = Sets.newLinkedHashSet();
    final Set<String> neighbors = MapXmlHelper.getTerritoryConnectionsMap().get(newTerrName);
    for (final Entry<String, Set<String>> terrConnEntry : MapXmlHelper.getTerritoryConnectionsMap().entrySet()) {
      if (MapXmlHelper.getTerritoryDefintionsMap().get(terrConnEntry.getKey())
          .get(DEFINITION.IS_WATER) == waterNeighbors
          && terrConnEntry.getValue().contains(newTerrName)) {
        neighborsByType.add(terrConnEntry.getKey());
      }
    }
    if (neighbors != null) {
      for (final String neighbor : neighbors) {
        if (MapXmlHelper.getTerritoryDefintionsMap().get(neighbor).get(DEFINITION.IS_WATER) == waterNeighbors) {
          neighborsByType.add(neighbor);
        }
      }
    }
    return neighborsByType;
  }

  private Set<String> getCommonNeighborsOfType(final Set<String> terrList, final boolean typeIsWater) {
    final Set<String> commonNeighborsOfType = new TreeSet<String>();
    final Map<String, Collection<String>> neighborsMap = getNeighborsMapWaterDefinitionBeing(terrList, typeIsWater);
    commonNeighborsOfType.addAll(neighborsMap.values().iterator().next());
    if (commonNeighborsOfType.size() >= 2) {
      for (final Collection<String> waterNeighors : neighborsMap.values()) {
        commonNeighborsOfType.retainAll(waterNeighors);
        if (commonNeighborsOfType.size() < 2) {
          break;
        }
      }
    }
    return commonNeighborsOfType;
  }

  /**
   * @param terrList - list of territories for which neighbors should be evaluated
   * @param typeIsWater - filter criteria of neighbors by their property IS_WATER
   * @return map of terrList list territory entry -> neighbor territories with/without (defined by typeIsWater) IS_WATER
   *         property
   */
  private Map<String, Collection<String>> getNeighborsMapWaterDefinitionBeing(final Set<String> terrList,
      final boolean typeIsWater) {
    final Map<String, Collection<String>> neighborsMap = Maps.newHashMap();
    for (final String terr : terrList) {
      neighborsMap.put(terr, new ArrayList<String>());
    }
    for (final Entry<String, Set<String>> terrConnEntry : MapXmlHelper.getTerritoryConnectionsMap().entrySet()) {
      final String terr1 = terrConnEntry.getKey();
      if (terrList.contains(terr1)) {
        for (final String terr2 : terrConnEntry.getValue()) {
          if (MapXmlHelper.getTerritoryDefintionsMap().get(terr2).get(DEFINITION.IS_WATER) == typeIsWater) {
            neighborsMap.get(terr1).add(terr2);
          }
        }
      } else {
        if (MapXmlHelper.getTerritoryDefintionsMap().get(terr1).get(DEFINITION.IS_WATER) == typeIsWater) {
          final SortedSet<String> selectedTerritoriesCopy = new TreeSet<String>(terrList);
          selectedTerritoriesCopy.retainAll(terrConnEntry.getValue());
          for (final String terr2 : selectedTerritoriesCopy) {
            neighborsMap.get(terr2).add(terr1);
          }
        }
      }
    }
    return neighborsMap;
  }
}
