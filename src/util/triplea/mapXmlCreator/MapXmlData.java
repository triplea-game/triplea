package util.triplea.mapXmlCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import games.strategy.util.Triple;
import util.triplea.mapXmlCreator.TerritoryDefinitionDialog.DEFINITION;

public class MapXmlData {
  private String notes = "";
  private File mapXMLFile;

  private Map<String, String> xmlStringsMap = Maps.newLinkedHashMap();
  private List<String> resourceList = new ArrayList<String>();
  private Map<String, HashMap<DEFINITION, Boolean>> territoryDefintionsMap =
      new HashMap<String, HashMap<DEFINITION, Boolean>>();
  private Map<String, Set<String>> territoryConnectionsMap = new HashMap<String, Set<String>>();
  private List<String> playerNames = new ArrayList<String>();
  private Map<String, String> playerAllianceMap = new HashMap<String, String>();
  private Map<String, Integer> playerInitResourcesMap = new HashMap<String, Integer>();
  private Map<String, List<Integer>> unitDefinitionsMap = Maps.newLinkedHashMap();
  private Map<String, List<String>> gamePlaySequenceMap = Maps.newLinkedHashMap();
  private Map<String, Triple<String, String, Integer>> playerSequenceMap =
      Maps.newLinkedHashMap();
  private Map<String, List<String>> technologyDefinitionsMap =
      Maps.newLinkedHashMap();
  private Map<String, List<String>> productionFrontiersMap = Maps.newLinkedHashMap();
  private Map<String, List<String>> unitAttatchmentsMap = Maps.newLinkedHashMap();
  private Map<String, Integer> territoyProductionsMap = new HashMap<String, Integer>();
  private Map<String, CanalTerritoriesTuple> canalDefinitionsMap =
      new HashMap<String, CanalTerritoriesTuple>();
  private Map<String, String> territoryOwnershipsMap = new HashMap<String, String>();
  private Map<String, Map<String, Map<String, Integer>>> unitPlacementsMap =
      new HashMap<String, Map<String, Map<String, Integer>>>();
  private Map<String, List<String>> gameSettingsMap = new HashMap<String, List<String>>();

  public MapXmlData() {
    initialize();
  }

  @SuppressWarnings("unused")
  // Maybe for testing needed
  private MapXmlData(final Map<String, String> xmlStringsMap, final List<String> resourceList,
      final Map<String, HashMap<DEFINITION, Boolean>> territoryDefintionsMap,
      final Map<String, Set<String>> territoryConnectionsMap, final List<String> playerNames,
      final Map<String, String> playerAllianceMap,
      final Map<String, Integer> playerInitResourcesMap, final Map<String, List<Integer>> unitDefinitionsMap,
      final Map<String, List<String>> gamePlaySequenceMap,
      final Map<String, Triple<String, String, Integer>> playerSequenceMap,
      final Map<String, List<String>> technologyDefinitionsMap, final Map<String, List<String>> productionFrontiersMap,
      final Map<String, List<String>> unitAttatchmentsMap, final Map<String, Integer> territoyProductionsMap,
      final Map<String, CanalTerritoriesTuple> canalDefinitionsMap,
      final Map<String, String> territoryOwnershipsMap,
      final Map<String, Map<String, Map<String, Integer>>> unitPlacementsMap,
      final Map<String, List<String>> gameSettingsMap,
      final String notes, final File mapXMLFile) {
    this.xmlStringsMap = xmlStringsMap;
    this.resourceList = resourceList;
    this.territoryDefintionsMap = territoryDefintionsMap;
    this.territoryConnectionsMap = territoryConnectionsMap;
    this.playerNames = playerNames;
    this.playerAllianceMap = playerAllianceMap;
    this.playerInitResourcesMap = playerInitResourcesMap;
    this.unitDefinitionsMap = unitDefinitionsMap;
    this.gamePlaySequenceMap = gamePlaySequenceMap;
    this.playerSequenceMap = playerSequenceMap;
    this.technologyDefinitionsMap = technologyDefinitionsMap;
    this.productionFrontiersMap = productionFrontiersMap;
    this.unitAttatchmentsMap = unitAttatchmentsMap;
    this.territoyProductionsMap = territoyProductionsMap;
    this.canalDefinitionsMap = canalDefinitionsMap;
    this.territoryOwnershipsMap = territoryOwnershipsMap;
    this.unitPlacementsMap = unitPlacementsMap;
    this.gameSettingsMap = gameSettingsMap;
    this.notes = notes;
    this.mapXMLFile = mapXMLFile;
  }

  public void initialize() {
    getXmlStringsMap().clear();
    getResourceList().clear();
    getPlayerNames().clear();
    getPlayerAllianceMap().clear();
    getPlayerInitResourcesMap().clear();
    getTerritoryDefintionsMap().clear();
    getTerritoryConnectionsMap().clear();
    getGamePlaySequenceMap().clear();
    getPlayerSequenceMap().clear();
    getProductionFrontiersMap().clear();
    getTechnologyDefinitionsMap().clear();
    getUnitAttatchmentsMap().clear();
    getTerritoyProductionsMap().clear();
    getCanalDefinitionsMap().clear();
    getTerritoryOwnershipsMap().clear();
    getUnitPlacementsMap().clear();
    getGameSettingsMap().clear();
    setNotes("");
  }

  public Map<String, String> getXmlStringsMap() {
    return xmlStringsMap;
  }

  public void setXmlStringsMap(Map<String, String> xmlStringsMap) {
    this.xmlStringsMap = xmlStringsMap;
  }

  public List<String> getResourceList() {
    return resourceList;
  }

  public void setResourceList(List<String> resourceList) {
    this.resourceList = resourceList;
  }

  public Map<String, HashMap<DEFINITION, Boolean>> getTerritoryDefintionsMap() {
    return territoryDefintionsMap;
  }

  public void setTerritoryDefintionsMap(Map<String, HashMap<DEFINITION, Boolean>> territoryDefintionsMap) {
    this.territoryDefintionsMap = territoryDefintionsMap;
  }

  public Map<String, Set<String>> getTerritoryConnectionsMap() {
    return territoryConnectionsMap;
  }

  public void setTerritoryConnectionsMap(Map<String, Set<String>> territoryConnectionsMap) {
    this.territoryConnectionsMap = territoryConnectionsMap;
  }

  public List<String> getPlayerNames() {
    return playerNames;
  }

  public void setPlayerNames(List<String> playerNames) {
    this.playerNames = playerNames;
  }

  public Map<String, String> getPlayerAllianceMap() {
    return playerAllianceMap;
  }

  public void setPlayerAllianceMap(Map<String, String> playerAllianceMap) {
    this.playerAllianceMap = playerAllianceMap;
  }

  public Map<String, Integer> getPlayerInitResourcesMap() {
    return playerInitResourcesMap;
  }

  public void setPlayerInitResourcesMap(Map<String, Integer> playerInitResourcesMap) {
    this.playerInitResourcesMap = playerInitResourcesMap;
  }

  public Map<String, List<Integer>> getUnitDefinitionsMap() {
    return unitDefinitionsMap;
  }

  public void setUnitDefinitionsMap(Map<String, List<Integer>> unitDefinitionsMap) {
    this.unitDefinitionsMap = unitDefinitionsMap;
  }

  public Map<String, List<String>> getGamePlaySequenceMap() {
    return gamePlaySequenceMap;
  }

  public void setGamePlaySequenceMap(Map<String, List<String>> gamePlaySequenceMap) {
    this.gamePlaySequenceMap = gamePlaySequenceMap;
  }

  public Map<String, Triple<String, String, Integer>> getPlayerSequenceMap() {
    return playerSequenceMap;
  }

  public void setPlayerSequenceMap(Map<String, Triple<String, String, Integer>> playerSequenceMap) {
    this.playerSequenceMap = playerSequenceMap;
  }

  public Map<String, List<String>> getTechnologyDefinitionsMap() {
    return technologyDefinitionsMap;
  }

  public void setTechnologyDefinitionsMap(Map<String, List<String>> technologyDefinitionsMap) {
    this.technologyDefinitionsMap = technologyDefinitionsMap;
  }

  public Map<String, List<String>> getProductionFrontiersMap() {
    return productionFrontiersMap;
  }

  public void setProductionFrontiersMap(Map<String, List<String>> productionFrontiersMap) {
    this.productionFrontiersMap = productionFrontiersMap;
  }

  public Map<String, List<String>> getUnitAttatchmentsMap() {
    return unitAttatchmentsMap;
  }

  public void setUnitAttatchmentsMap(Map<String, List<String>> unitAttatchmentsMap) {
    this.unitAttatchmentsMap = unitAttatchmentsMap;
  }

  public Map<String, Integer> getTerritoyProductionsMap() {
    return territoyProductionsMap;
  }

  public void setTerritoyProductionsMap(Map<String, Integer> territoyProductionsMap) {
    this.territoyProductionsMap = territoyProductionsMap;
  }

  public Map<String, CanalTerritoriesTuple> getCanalDefinitionsMap() {
    return canalDefinitionsMap;
  }

  public void setCanalDefinitionsMap(Map<String, CanalTerritoriesTuple> canalDefinitionsMap) {
    this.canalDefinitionsMap = canalDefinitionsMap;
  }

  public Map<String, String> getTerritoryOwnershipsMap() {
    return territoryOwnershipsMap;
  }

  public void setTerritoryOwnershipsMap(Map<String, String> territoryOwnershipsMap) {
    this.territoryOwnershipsMap = territoryOwnershipsMap;
  }

  public Map<String, Map<String, Map<String, Integer>>> getUnitPlacementsMap() {
    return unitPlacementsMap;
  }

  public void setUnitPlacementsMap(Map<String, Map<String, Map<String, Integer>>> unitPlacementsMap) {
    this.unitPlacementsMap = unitPlacementsMap;
  }

  public Map<String, List<String>> getGameSettingsMap() {
    return gameSettingsMap;
  }

  public void setGameSettingsMap(Map<String, List<String>> gameSettingsMap) {
    this.gameSettingsMap = gameSettingsMap;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public File getMapXMLFile() {
    return mapXMLFile;
  }

  public void setMapXMLFile(File mapXMLFile) {
    this.mapXMLFile = mapXMLFile;
  }

  /**
   * The method separates the MapXml territoryConnectionsMap into connections starting in a land territory and those
   * starting in a water territory.
   * Each connection is only in one of the maps.
   * 
   * <p>
   * <b>Example:</b>
   * 
   * <p>
   * Output:
   * <ul>
   * <li>map1 { land1 -> {water1, water2} }
   * <li>map2 { water3 -> {land2, land3} }
   * </ul>
   * 
   * @param landWaterTerritoyConnections - map of landTerritory -> waterTerritories with a connection to landTerritory
   * @param waterLandTerritoyConnections - map of waterTerritory -> landTerritories with a connection to waterTerritory
   */
  static void setSplittedTerritoryConnectionsByIsWaterSingle(
      final Map<String, Set<String>> landWaterTerritoyConnections,
      final Map<String, Set<String>> waterLandTerritoyConnections) {
    for (final Entry<String, Set<String>> terrConn : MapXmlHelper.getTerritoryConnectionsMap().entrySet()) {
      if (MapXmlHelper.getTerritoryDefintionsMap().get(terrConn.getKey()).get(DEFINITION.IS_WATER)) {
        // terrConn.Key is water territory, get none-water entries in terrConn.Value
        @SuppressWarnings("unchecked")
        final Set<String> landTerrValue = (Set<String>) terrConn.getValue().stream()
            .filter(terr -> !MapXmlHelper.getTerritoryDefintionsMap().get(terr).get(DEFINITION.IS_WATER));
        if (!landTerrValue.isEmpty())
          waterLandTerritoyConnections.put(terrConn.getKey(), landTerrValue);
      } else {
        // terrConn.Key is land territory, get water entries in terrConn.Value
        @SuppressWarnings("unchecked")
        final Set<String> waterTerrValue = (Set<String>) terrConn.getValue().stream()
            .filter(terr -> MapXmlHelper.getTerritoryDefintionsMap().get(terr).get(DEFINITION.IS_WATER));
        landWaterTerritoyConnections.put(terrConn.getKey(), waterTerrValue);
      }
    }
  }

  /**
   * The method assumes that a connection is not represented in both provided maps.
   * It enhances the first map with the symmetric fitting entries from the second map.
   * 
   * <p>
   * <b>Example:</b>
   * <p>
   * Input:
   * <ul>
   * <li>map1 = { land1 -> {water1, water2} }
   * <li>map2 = { water3 -> {land2, land3} }
   * </ul>
   * <p>
   * Output:
   * <ul>
   * <li>map1 = { land1 -> {water1, water2, water3} }
   * <li>map2 = { water3 -> {land2, land3} }
   * </ul>
   * 
   * @param landWaterTerritoyConnections - map of landTerritory -> waterTerritories with a connection to landTerritory
   * @param waterLandTerritoyConnections - map of waterTerritory -> landTerritories with a connection to waterTerritory
   */
  static void enhanceFirstSplittedTerritoryConnectionsWithSymmetryClosure(
      final Map<String, Set<String>> landWaterTerritoyConnections,
      final Map<String, Set<String>> waterLandTerritoyConnections) {
    for (final Entry<String, Set<String>> terrConn : waterLandTerritoyConnections.entrySet()) {
      final String waterTerr = terrConn.getKey();
      for (final String landTerr : terrConn.getValue()) {
        final Set<String> waterTerrs = landWaterTerritoyConnections.get(landTerr);
        if (waterTerrs == null) {
          final Set<String> newWaterTerrs = Sets.newLinkedHashSet();
          newWaterTerrs.add(waterTerr);
          landWaterTerritoyConnections.put(landTerr, newWaterTerrs);
        } else
          waterTerrs.add(waterTerr);
      }
    }
  }

  /**
   * The method extracts connections from the MapXml territoryConnectionsMap which are going from a land territory
   * into a water territory or vice versa. They are represented in a new map with map.key values being the land
   * territories
   * and a map.value value being the list of water territories linked to the map.key land territory.
   * 
   * @return map of landTerritory -> waterTerritories with a connection to landTerritory
   */
  public static Map<String, Set<String>> getLandWaterTerritoryConnections() {
    final Map<String, Set<String>> landWaterTerritoyConnections =
        Maps.newHashMap();
    final Map<String, Set<String>> waterLandTerritoyConnections =
        Maps.newHashMap();
    MapXmlData.setSplittedTerritoryConnectionsByIsWaterSingle(landWaterTerritoyConnections,
        waterLandTerritoyConnections);
    MapXmlData.enhanceFirstSplittedTerritoryConnectionsWithSymmetryClosure(landWaterTerritoyConnections,
        waterLandTerritoyConnections);
    return landWaterTerritoyConnections;
  }
}
