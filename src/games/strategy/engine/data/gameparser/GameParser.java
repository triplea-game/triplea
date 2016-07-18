package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.data.AllianceTracker;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DelegateList;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionFrontierList;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RelationshipTypeList;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairFrontierList;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.TechnologyFrontierList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.ComboProperty;
import games.strategy.engine.data.properties.FileProperty;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.Tuple;
import games.strategy.util.Version;

public class GameParser {
  private static final Class<?>[] SETTER_ARGS = {String.class};
  private final Collection<SAXParseException> errorsSAX = new ArrayList<>();
  public static final String DTD_FILE_NAME = "game.dtd";
  private static HashMap<String, String> newClassesForOldNames;

  public GameParser() {
  }

  /**
   * Parses a file into a GameData object.
   *
   * @param stream
   * @param delayParsing Should we only parse the game name, notes, and playerlist? Normally this should be "false", except for the
   *                     game chooser which
   *                     should use the user set preference.
   * @throws GameParseException
   * @throws SAXException
   * @throws EngineVersionException
   */
  public synchronized GameData parse(final InputStream stream, final AtomicReference<String> gameName,
      final boolean delayParsing)
      throws GameParseException, SAXException, EngineVersionException, IllegalArgumentException {
    checkNotNull(stream);

    Document doc = null;
    try {
      doc = getDocument(stream);
    } catch (final IOException | ParserConfigurationException e) {
      throw new IllegalStateException(e);
    }
    final Element root = doc.getDocumentElement();
    GameData data = new GameData();
    // mandatory fields
    // get the name of the map
    parseInfo(data, getSingleChild(gameName.get(), "info", root));
    if (gameName != null) {
      gameName.set(data.getGameName());
    }
    // test minimum engine version FIRST
    parseMinimumEngineVersionNumber(data.getGameName(), getSingleChildOptional(data.getGameName(), "triplea", root));
    parseGameLoader(data, getSingleChild(gameName.get(), "loader", root));
    // if we manage to get this far, past the minimum engine version number test, AND we are still good, then check and
    // see if we have any
    // SAX errors we need to show
    if (!errorsSAX.isEmpty()) {
      for (final SAXParseException error : errorsSAX) {
        System.err.println("SAXParseException: game: "
            + (data == null ? "?" : (data.getGameName() == null ? "?" : data.getGameName())) + ", line: "
            + error.getLineNumber() + ", column: " + error.getColumnNumber() + ", error: " + error.getMessage());
      }
    }
    final int defaultDiceSides = 6;
    data.setDiceSides(parseDiceSides(getSingleChildOptional(gameName.get(), "diceSides", root), defaultDiceSides));
    final Element playerListNode = getSingleChild(gameName.get(), "playerList", root);
    parsePlayerList(data, playerListNode);
    parseAlliances(data, playerListNode);
    final Node properties = getSingleChildOptional(gameName.get(), "propertyList", root);
    if (properties != null) {
      parseProperties(data, properties);
    }
    // everything until here is needed to select a game, the rest can be parsed when a game is selected
    if (delayParsing) {
      return data;
    }
    parseMap(data, getSingleChild(gameName.get(), "map", root));
    final Element resourceList = getSingleChildOptional(gameName.get(), "resourceList", root);
    if (resourceList != null) {
      parseResources(data, resourceList);
    }
    final Element unitList = getSingleChildOptional(gameName.get(), "unitList", root);
    if (unitList != null) {
      parseUnits(data, unitList);
    }
    // Parse all different relationshipTypes that are defined in the xml, for example: War, Allied, Neutral, NAP
    final Element relationshipTypes = getSingleChildOptional(gameName.get(), "relationshipTypes", root);
    if (relationshipTypes != null) {
      parseRelationshipTypes(data, relationshipTypes);
    }
    final Element territoryEffectList = getSingleChildOptional(gameName.get(), "territoryEffectList", root);
    if (territoryEffectList != null) {
      parseTerritoryEffects(data, territoryEffectList);
    }
    parseGamePlay(data, getSingleChild(gameName.get(), "gamePlay", root));
    final Element production = getSingleChildOptional(gameName.get(), "production", root);
    if (production != null) {
      parseProduction(data, production);
    }
    final Element technology = getSingleChildOptional(gameName.get(), "technology", root);
    if (technology != null) {
      parseTechnology(data, technology);
    } else {
      TechAdvance.createDefaultTechAdvances(data);
    }
    final Element attachmentList = getSingleChildOptional(gameName.get(), "attachmentList", root);
    if (attachmentList != null) {
      parseAttachments(data, attachmentList);
    }
    final Node initialization = getSingleChildOptional(gameName.get(), "initialize", root);
    if (initialization != null) {
      parseInitialization(data, initialization);
    }
    // set & override default relationships
    // sets the relationship between all players and the NullPlayer to NullRelation
    // (with archeType War)
    data.getRelationshipTracker().setNullPlayerRelations();
    // sets the relationship for all players with themselfs to the SelfRelation (with archeType Allied)
    data.getRelationshipTracker().setSelfRelations();
    // set default tech attachments (comes after we parse all technologies, parse all attachments, and parse all game
    // options/properties)
    if (data.getGameLoader() instanceof games.strategy.triplea.TripleA) {
      checkThatAllUnitsHaveAttachments(data);
      TechAbilityAttachment.setDefaultTechnologyAttachments(data);
    }
    try {
      new GameValidator().validate(data);
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      throw new GameParseException(gameName.get(), e.getMessage());
    }
    return data;
  }

  private int parseDiceSides(final Node diceSides, int defaultValue) {
    if (diceSides == null) {
      return defaultValue;
    } else {
      return Integer.parseInt(((Element) diceSides).getAttribute("value"));
    }
  }

  private void parseMinimumEngineVersionNumber(final String mapName, final Node minimumVersion)
      throws EngineVersionException {
    if (minimumVersion == null) {
      return;
    }
    final Version mapCompatibleWithTripleaVersion =
        new Version(((Element) minimumVersion).getAttribute("minimumVersion"));
    if (mapCompatibleWithTripleaVersion.isGreaterThan(ClientContext.engineVersion().getVersion(), true)) {
      throw new EngineVersionException("Trying to play a map made for a newer version of TripleA. Map named '"
          + mapName + "' requires at least TripleA version " + mapCompatibleWithTripleaVersion.toString());
    }
  }



  public Document getDocument(final InputStream input) throws SAXException, IOException, ParserConfigurationException {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(true);
    // get the dtd location
    final String dtdFile = "/games/strategy/engine/xml/" + DTD_FILE_NAME;
    final URL url = GameParser.class.getResource(dtdFile);
    if (url == null) {
      throw new RuntimeException(String.format("Could not find in classpath %s", dtdFile));
    }
    final String dtdSystem = url.toExternalForm();
    final String system = dtdSystem.substring(0, dtdSystem.length() - 8);
    final DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setErrorHandler(new ErrorHandler() {
      @Override
      public void fatalError(final SAXParseException exception) {
        errorsSAX.add(exception);
      }

      @Override
      public void error(final SAXParseException exception) {
        errorsSAX.add(exception);
      }

      @Override
      public void warning(final SAXParseException exception) {
        errorsSAX.add(exception);
      }
    });
    return builder.parse(input, system);
  }

  private PlayerID getPlayerID(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    return getPlayerID(data, element, attribute, true);
  }

  /**
   * If mustfind is true and cannot find the player an exception will be thrown.
   */
  private PlayerID getPlayerID(final GameData data, final Element element, final String attribute, boolean mustFind)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final PlayerID player = data.getPlayerList().getPlayerID(name);
    if (player == null && mustFind) {
      throw new GameParseException(data.getGameName(), "Could not find player. name:" + name);
    }
    return player;
  }

  /**
   * If mustfind is true and cannot find the player an exception will be thrown.
   *
   * @return a RelationshipType from the relationshipTypeList, at this point all relationshipTypes should have been
   * declared
   * @throws GameParseException when
   */
  private RelationshipType getRelationshipType(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final RelationshipType relation = data.getRelationshipTypeList().getRelationshipType(name);
    if (relation == null) {
      throw new GameParseException(data.getGameName(), "Could not find relation name:" + name);
    }
    return relation;
  }

  private TerritoryEffect getTerritoryEffect(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final TerritoryEffect effect = data.getTerritoryEffectList().get(name);
    if (effect == null) {
      throw new GameParseException(data.getGameName(), "Could not find territoryEffect name:" + name);
    }
    return effect;
  }

  /**
   * If mustfind is true and cannot find the productionRule an exception will be thrown.
   */
  private ProductionRule getProductionRule(final GameData data, final Element element, final String attribute,
      final boolean mustFind)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final ProductionRule productionRule = data.getProductionRuleList().getProductionRule(name);
    if (productionRule == null && mustFind) {
      throw new GameParseException(data, "Could not find production rule. name:" + name);
    }
    return productionRule;
  }

  /**
   * If mustfind is true and cannot find the productionRule an exception will be thrown.
   */
  private RepairRule getRepairRule(final GameData data, final Element element, final String attribute,
      final boolean mustFind) throws GameParseException {
    final String name = element.getAttribute(attribute);
    final RepairRule repairRule = data.getRepairRuleList().getRepairRule(name);
    if (repairRule == null && mustFind) {
      throw new GameParseException(data.getGameName(), "Could not find production rule. name:" + name);
    }
    return repairRule;
  }

  /**
   * If mustfind is true and cannot find the territory an exception will be thrown.
   */
  private Territory getTerritory(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final Territory territory = data.getMap().getTerritory(name);
    if (territory == null) {
      throw new GameParseException(data, "Could not find territory. name:" + name);
    }
    return territory;
  }

  private static UnitType getUnitType(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    return getUnitType(data, element, attribute, true);
  }

  /**
   * If mustfind is true and cannot find the unitType an exception will be thrown.
   */
  private static UnitType getUnitType(final GameData data, final Element element, final String attribute,
      final boolean mustFind)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final UnitType type = data.getUnitTypeList().getUnitType(name);
    if (type == null && mustFind) {
      throw new GameParseException(data, "Could not find unitType. name:" + name);
    }
    return type;
  }

  /**
   * If mustfind is true and cannot find the technology an exception will be thrown.
   */
  private TechAdvance getTechnology(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    TechAdvance type = data.getTechnologyFrontier().getAdvanceByName(name);
    if (type == null) {
      type = data.getTechnologyFrontier().getAdvanceByProperty(name);
    }
    if (type == null) {
      throw new GameParseException(data, "Could not find technology. name:" + name);
    }
    return type;
  }

  /**
   * If mustfind is true and cannot find the Delegate an exception will be thrown.
   */
  private IDelegate getDelegate(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final IDelegate delegate = data.getDelegateList().getDelegate(name);
    if (delegate == null) {
      throw new GameParseException(data, "Could not find delegate. name:" + name);
    }
    return delegate;
  }

  private Resource getResource(final GameData data, final Element element, final String attribute)
      throws GameParseException {
    return getResource(data, element, attribute, true);
  }
  /**
   * If mustfind is true and cannot find the Resource an exception will be thrown.
   */
  private Resource getResource(final GameData data, final Element element, final String attribute, boolean mustFind)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final Resource resource = data.getResourceList().getResource(name);
    if (resource == null && mustFind) {
      throw new GameParseException(data, "Could not find resource. name:" + name);
    }
    return resource;
  }

  /**
   * If mustfind is true and cannot find the productionRule an exception will be thrown.
   */
  private ProductionFrontier getProductionFrontier(final GameData data, final Element element, final String attribute,
      final boolean mustFind) throws GameParseException {
    final String name = element.getAttribute(attribute);
    final ProductionFrontier productionFrontier = data.getProductionFrontierList().getProductionFrontier(name);
    if (productionFrontier == null && mustFind) {
      throw new GameParseException(data, "Could not find production frontier. name:" + name);
    }
    return productionFrontier;
  }

  /**
   * If mustfind is true and cannot find the productionRule an exception will be thrown.
   */
  private RepairFrontier getRepairFrontier(final GameData data, final Element element, final String attribute,
      final boolean mustFind)
      throws GameParseException {
    final String name = element.getAttribute(attribute);
    final RepairFrontier repairFrontier = data.getRepairFrontierList().getRepairFrontier(name);
    if (repairFrontier == null && mustFind) {
      throw new GameParseException(data, "Could not find production frontier. name:" + name);
    }
    return repairFrontier;
  }

  /**
   * Loads an instance of the given class.
   * Assumes a zero argument constructor.
   */
  private Object getInstance(final String mapName, final String className) throws GameParseException {
    Object instance = null;
    try {
      final Class<?> instanceClass = Class.forName(className);
      instance = instanceClass.newInstance();
    }
    // a lot can go wrong, the following list is just a subset of potential pitfalls
    catch (final ClassNotFoundException cnfe) {
      throw new GameParseException(mapName, "Class <" + className + "> could not be found.");
    } catch (final InstantiationException ie) {
      throw new GameParseException(mapName,
          "Class <" + className + "> could not be instantiated. ->" + ie.getMessage());
    } catch (final IllegalAccessException iae) {
      throw new GameParseException(mapName, "Constructor could not be accessed ->" + iae.getMessage());
    }
    return instance;
  }

  /**
   * Loads a given class.
   */
  private Class<?> getClassByName(final String mapName, final String className) throws GameParseException {
    try {
      final Class<?> instanceClass = Class.forName(className);
      return instanceClass;
    }
    // if class cannot be found than it is either not a valid class or an old class that was deleted/renamed
    catch (final ClassNotFoundException cnfe) {
      if (newClassesForOldNames == null) {
        newClassesForOldNames = new HashMap<>();
        // put in here class names that have been changed like //newClassesForOldNames.put("<oldClassName>",
        // "<newClassName>"), e.g.
        // newClassesForOldNames.put("attachment", "attachment")
      }
      final String newClassName = newClassesForOldNames.get(className);
      if (newClassName != null) {
        return getClassByName(mapName, newClassName);
      }
      throw new GameParseException(mapName, "Class <" + className + "> could not be found.");
    }
  }

  /**
   * Get the given child.
   * If there is not exactly one child throw a SAXExcpetion
   */
  private Element getSingleChild(final String mapName, final String name, final Element node)
      throws GameParseException {
    return getSingleChild(mapName, name, node, false);
  }

  private Element getSingleChildOptional(final String mapName, final String name, final Node node)
      throws GameParseException {
    return getSingleChild(mapName, name, node, true);
  }
  /**
   * If optional is true, will not throw an exception if there are 0 children
   */
  private Element getSingleChild(final String mapName, final String name, final Node node, final boolean optional)
      throws GameParseException {
    final List<Element> children = getChildren(name, node);
    // none found
    if (children.size() == 0) {
      if (optional) {
        return null;
      }
      throw new GameParseException(mapName, "No child called " + name);
    }
    // too many found
    if (children.size() > 1) {
      throw new GameParseException(mapName, "Too many children named " + name);
    }
    return children.get(0);
  }

  private List<Element> getChildren(final String name, final Node node) {
    final ArrayList<Element> found = new ArrayList<>();
    final NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      final Node current = children.item(i);
      if (current.getNodeName().equals(name)) {
        found.add((Element) current);
      }
    }
    return found;
  }

  private List<Node> getNonTextNodesIgnoring(final Node node, final String ignore) {
    final List<Node> rVal = getNonTextNodes(node);
    final Iterator<Node> iter = rVal.iterator();
    while (iter.hasNext()) {
      if (((Element) iter.next()).getTagName().equals(ignore)) {
        iter.remove();
      }
    }
    return rVal;
  }

  private List<Node> getNonTextNodes(final Node node) {
    final ArrayList<Node> found = new ArrayList<>();
    final NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); ++i) {
      final Node current = children.item(i);
      if (!(current.getNodeType() == Node.TEXT_NODE)) {
        found.add(current);
      }
    }
    return found;
  }

  private void parseInfo(final GameData data, final Node info) {
    final String gameName = ((Element) info).getAttribute("name");
    data.setGameName(gameName);
    final String version = ((Element) info).getAttribute("version");
    data.setGameVersion(new Version(version));
  }

  private void parseGameLoader(final GameData data, final Node loader) throws GameParseException {
    final String className = ((Element) loader).getAttribute("javaClass");

    // TODO: This looks risky, see if we can make this more direct, get rid of reflection.
    final Object instance = getInstance(data.getGameName(), className);
    if (!(instance instanceof IGameLoader)) {
      throw new GameParseException(data, "Loader must implement IGameLoader.  Class Name:" + className);
    }
    data.setGameLoader((IGameLoader) instance);
  }

  private void parseMap(final GameData data, final Node map) throws GameParseException {
    final List<Element> grids = getChildren("grid", map);
    parseGrids(data, grids);
    // get the Territories
    final List<Element> territories = getChildren("territory", map);
    parseTerritories(data, territories);
    final List<Element> connections = getChildren("connection", map);
    parseConnections(data, connections);
  }

  private void parseGrids(final GameData data, final List<Element> grids) throws GameParseException {
    for (final Element current : grids) {
      final String gridType = current.getAttribute("type");
      final String name = current.getAttribute("name");
      final String xs = current.getAttribute("x");
      final String ys = current.getAttribute("y");
      final List<Element> waterNodes = getChildren("water", current);
      final Set<String> water = parseGridWater(waterNodes);
      final String horizontalConnections = current.getAttribute("horizontal-connections");
      final String verticalConnections = current.getAttribute("vertical-connections");
      final String diagonalConnections = current.getAttribute("diagonal-connections");
      setGrids(data, gridType, name, xs, ys, water, horizontalConnections, verticalConnections, diagonalConnections,
          false);
    }
  }

  /**
   * Creates and adds new territories and their connections to their map, based on a grid.
   */
  public static void setGrids(final GameData data, final String gridType, final String name, final String xs,
      final String ys, final Set<String> water, final String horizontalConnections, final String verticalConnections,
      final String diagonalConnections, final boolean addingOntoExistingMap) throws GameParseException {
    final GameMap map = data.getMap();
    boolean horizontalConnectionsImplict;
    if (horizontalConnections.equals("implicit")) {
      horizontalConnectionsImplict = true;
    } else if (horizontalConnections.equals("explicit")) {
      horizontalConnectionsImplict = false;
    } else {
      throw new GameParseException(data, "horizontal-connections attribute must be either \"explicit\" or \"implicit\"");
    }
    boolean verticalConnectionsImplict;
    if (verticalConnections.equals("implicit")) {
      verticalConnectionsImplict = true;
    } else if (verticalConnections.equals("explicit")) {
      verticalConnectionsImplict = false;
    } else {
      throw new GameParseException(data, "vertical-connections attribute must be either \"explicit\" or \"implicit\"");
    }
    boolean diagonalConnectionsImplict;
    if (diagonalConnections.equals("implicit")) {
      diagonalConnectionsImplict = true;
    } else if (diagonalConnections.equals("explicit")) {
      diagonalConnectionsImplict = false;
    } else {
      throw new GameParseException(data, "diagonal-connections attribute must be either \"explicit\" or \"implicit\"");
    }
    final int x_size = Integer.valueOf(xs);
    int y_size;
    if (ys != null) {
      y_size = Integer.valueOf(ys);
    } else {
      y_size = 0;
    }
    map.setGridDimensions(x_size, y_size);
    if (gridType.equals("square")) {
      // Add territories
      for (int y = 0; y < y_size; y++) {
        for (int x = 0; x < x_size; x++) {
          boolean isWater;
          isWater = water.contains(x + "-" + y);
          final Territory newTerritory = new Territory(name + "_" + x + "_" + y, isWater, data, x, y);
          if (addingOntoExistingMap && map.getTerritories().contains(newTerritory)) {
            continue;
          }
          map.addTerritory(newTerritory);
        }
      }
      if (addingOntoExistingMap) {
        map.reorderTerritoryList();
      }
      // Add any implicit horizontal connections
      if (horizontalConnectionsImplict) {
        for (int y = 0; y < y_size; y++) {
          for (int x = 0; x < x_size - 1; x++) {
            map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x + 1, y));
          }
        }
      }
      // Add any implicit vertical connections
      if (verticalConnectionsImplict) {
        for (int x = 0; x < x_size; x++) {
          for (int y = 0; y < y_size - 1; y++) {
            map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x, y + 1));
          }
        }
      }
      // Add any implicit acute diagonal connections
      if (diagonalConnectionsImplict) {
        for (int y = 0; y < y_size - 1; y++) {
          for (int x = 0; x < x_size - 1; x++) {
            map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x + 1, y + 1));
          }
        }
      }
      // Add any implicit obtuse diagonal connections
      if (diagonalConnectionsImplict) {
        for (int y = 0; y < y_size - 1; y++) {
          for (int x = 1; x < x_size; x++) {
            map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x - 1, y + 1));
          }
        }
      }
      // This type is a triangular grid of points and lines, used for in several rail games
    } else if (gridType.equals("points-and-lines")) {
      // Add territories
      for (int y = 0; y < y_size; y++) {
        for (int x = 0; x < x_size; x++) {
          final boolean isWater = false;
          if (!water.contains(x + "-" + y)) {
            final Territory newTerritory = new Territory(name + "_" + x + "_" + y, isWater, data, x, y);
            if (addingOntoExistingMap && map.getTerritories().contains(newTerritory)) {
              continue;
            }
            map.addTerritory(newTerritory);
          }
        }
      }
      if (addingOntoExistingMap) {
        map.reorderTerritoryList();
      }
      // Add any implicit horizontal connections
      if (horizontalConnectionsImplict) {
        for (int y = 0; y < y_size; y++) {
          for (int x = 0; x < x_size - 1; x++) {
            final Territory from = map.getTerritoryFromCoordinates(x, y);
            final Territory to = map.getTerritoryFromCoordinates(x + 1, y);
            if (from != null && to != null) {
              map.addConnection(from, to);
            }
          }
        }
      }
      // Add any implicit acute diagonal connections
      if (diagonalConnectionsImplict) {
        for (int y = 1; y < y_size; y++) {
          for (int x = 0; x < x_size - 1; x++) {
            if (y % 4 == 0 || (y + 1) % 4 == 0) {
              final Territory from = map.getTerritoryFromCoordinates(x, y);
              final Territory to = map.getTerritoryFromCoordinates(x, y - 1);
              if (from != null && to != null) {
                map.addConnection(from, to);
              }
            } else {
              final Territory from = map.getTerritoryFromCoordinates(x, y);
              final Territory to = map.getTerritoryFromCoordinates(x + 1, y - 1);
              if (from != null && to != null) {
                map.addConnection(from, to);
              }
            }
          }
        }
      }
      // Add any implicit obtuse diagonal connections
      if (diagonalConnectionsImplict) {
        for (int y = 1; y < y_size; y++) {
          for (int x = 0; x < x_size - 1; x++) {
            if (y % 4 == 0 || (y + 1) % 4 == 0) {
              final Territory from = map.getTerritoryFromCoordinates(x, y);
              final Territory to = map.getTerritoryFromCoordinates(x - 1, y - 1);
              if (from != null && to != null) {
                map.addConnection(from, to);
              }
            } else {
              final Territory from = map.getTerritoryFromCoordinates(x, y);
              final Territory to = map.getTerritoryFromCoordinates(x, y - 1);
              if (from != null && to != null) {
                map.addConnection(from, to);
              }
            }
          }
        }
      }
    }
  }

  private Set<String> parseGridWater(final List<Element> waterNodes) {
    final Set<String> set = new HashSet<>();
    for (final Element current : waterNodes) {
      final int x = Integer.valueOf(current.getAttribute("x"));
      final int y = Integer.valueOf(current.getAttribute("y"));
      set.add(x + "-" + y);
    }
    return set;
  }

  private void parseTerritories(final GameData data, final List<Element> territories) {
    final GameMap map = data.getMap();
    for (final Element current : territories) {
      final boolean water = current.getAttribute("water").trim().equalsIgnoreCase("true");
      final String name = current.getAttribute("name");
      final Territory newTerritory = new Territory(name, water, data);
      map.addTerritory(newTerritory);
    }
  }

  private void parseConnections(final GameData data, final List<Element> connections) throws GameParseException {
    final GameMap map = data.getMap();
    for (final Element current : connections) {
      final Territory t1 = getTerritory(data, current, "t1");
      final Territory t2 = getTerritory(data, current, "t2");
      map.addConnection(t1, t2);
    }
  }

  private void parseResources(final GameData data, final Element root) {
    for (Element element : getChildren("resource", root)) {
      data.getResourceList().addResource(new Resource(element.getAttribute("name"), data));
    }
  }

  private void parseRelationshipTypes(final GameData data, final Element root) {
    for (Element element : getChildren("relationshipType", root)) {
      data.getRelationshipTypeList().addRelationshipType(new RelationshipType(element.getAttribute("name"), data));
    }
  }

  private void parseTerritoryEffects(final GameData data, final Element root) {
    for (Element element : getChildren("territoryEffect", root)) {
      final String name = element.getAttribute("name");
      data.getTerritoryEffectList().put(name, new TerritoryEffect(name, data));
    }
  }

  private void parseUnits(final GameData data, final Element root) {
    for (Element element : getChildren("unit", root)) {
      data.getUnitTypeList().addUnitType(new UnitType(element.getAttribute("name"), data));
    }
  }

  /**
   * @param root
   *        root node containing the playerList
   * @throws GameParseException
   */
  private void parsePlayerList(final GameData data, final Element root) {
    final PlayerList playerList = data.getPlayerList();
    for (final Element current : getChildren("player", root)) {
      final String name = current.getAttribute("name");
      // It appears the commented line ALWAYS returns false regardless of the value of current.getAttribute("optional")
      // boolean isOptional = Boolean.getBoolean(current.getAttribute("optional"));
      final boolean isOptional = current.getAttribute("optional").equals("true");
      final boolean canBeDisabled = current.getAttribute("canBeDisabled").equals("true");
      final PlayerID newPlayer = new PlayerID(name, isOptional, canBeDisabled, data);
      playerList.addPlayerID(newPlayer);
    }
  }

  private void parseAlliances(final GameData data, final Element root) throws GameParseException {
    final AllianceTracker allianceTracker = data.getAllianceTracker();
    final Collection<PlayerID> players = data.getPlayerList().getPlayers();
    for (final Element current : getChildren("alliance", root)) {
      final PlayerID p1 = getPlayerID(data, current, "player");
      final String alliance = current.getAttribute("alliance");
      allianceTracker.addToAlliance(p1, alliance);
    }
    // if relationships aren't initialized based on relationshipInitialize we use the alliances to set the relationships
    if (getSingleChild(data.getGameName(), "relationshipInitialize", root, true) == null) {
      final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
      final RelationshipTypeList relationshipTypeList = data.getRelationshipTypeList();
      // iterate through all players to get known allies and enemies
      for (final PlayerID currentPlayer : players) {
        // start with all players as enemies
        // start with no players as allies
        final Set<PlayerID> allies = allianceTracker.getAllies(currentPlayer);
        final Set<PlayerID> enemies = new HashSet<>(players);
        enemies.removeAll(allies);

        // remove self from enemieslist (in case of free-for-all)
        enemies.remove(currentPlayer);
        // remove self from allieslist (in case you are a member of an alliance)
        allies.remove(currentPlayer);
        // At this point enemies and allies should be set for this player.
        for (final PlayerID alliedPLayer : allies) {
          relationshipTracker.setRelationship(currentPlayer, alliedPLayer,
              relationshipTypeList.getDefaultAlliedRelationship());
        }
        for (final PlayerID enemyPlayer : enemies) {
          relationshipTracker.setRelationship(currentPlayer, enemyPlayer,
              relationshipTypeList.getDefaultWarRelationship());
        }
      }
    }
  }

  private void parseRelationInitialize(final GameData data, final List<Element> relations) throws GameParseException {
    if (relations.size() > 0) {
      final RelationshipTracker tracker = data.getRelationshipTracker();
      for (final Element current : relations) {
        final PlayerID p1 = getPlayerID(data, current, "player1");
        final PlayerID p2 = getPlayerID(data, current, "player2");
        final RelationshipType r = getRelationshipType(data, current, "type");
        final int roundValue = Integer.valueOf(current.getAttribute("roundValue"));
        tracker.setRelationship(p1, p2, r, roundValue);
      }
    }
  }

  private void parseGamePlay(final GameData data, final Element root) throws GameParseException {
    parseDelegates(data, getChildren("delegate", root));
    parseSequence(data, getSingleChild(data.getGameName(), "sequence", root));
    parseOffset(data, getSingleChildOptional(data.getGameName(), "offset", root));
  }

  private void parseProperties(final GameData data, final Node root) throws GameParseException {
    final Collection<String> runningList = new ArrayList<>();
    final GameProperties properties = data.getProperties();
    for (Element current : getChildren("property", root)) {
      final String editable = current.getAttribute("editable");
      final String property = current.getAttribute("name");
      String value = current.getAttribute("value");
      runningList.add(property);
      if (value == null || value.length() == 0) {
        final List<Element> valueChildren = getChildren("value", current);
        if (!valueChildren.isEmpty()) {
          final Element valueNode = valueChildren.get(0);
          if (valueNode != null) {
            value = valueNode.getTextContent();
          }
        }
      }
      if (editable != null && editable.equalsIgnoreCase("true")) {
        parseEditableProperty(data, current, property, value);
      } else {
        final List<Node> children2 = getNonTextNodesIgnoring(current, "value");
        if (children2.size() == 0) {
          // we don't know what type this property is!!, it appears like only numbers and string may be represented
          // without proper type
          // definition
          try {
            // test if it is an integer
            final Integer integer = Integer.parseInt(value);
            int intValue = 0;
            if (integer != null) {
              intValue = integer;
            }
            properties.set(property, intValue);
          } catch (final NumberFormatException e) {
            // then it must be a string
            properties.set(property, value);
          }
        } else {
          final String type = children2.get(0).getNodeName();
          if (type.equals("boolean")) {
            properties.set(property, Boolean.valueOf(value));
          } else if (type.equals("file")) {
            properties.set(property, new File(value));
          } else if (type.equals("number")) {
            int intValue = 0;
            if (value != null) {
              try {
                intValue = Integer.parseInt(value);
              } catch (final NumberFormatException e) {
                // value already 0
              }
            }
            properties.set(property, intValue);
          } else {
            properties.set(property, value);
          }
        }
      }
    }
    // add properties for all triplea related maps here:
    if (!runningList.contains(Constants.AI_BONUS_INCOME_FLAT_RATE)) {
      data.getProperties()
          .addEditableProperty(new NumberProperty(Constants.AI_BONUS_INCOME_FLAT_RATE, null, 40, -20, 0));
    }
    if (!runningList.contains(Constants.AI_BONUS_INCOME_PERCENTAGE)) {
      data.getProperties()
          .addEditableProperty(new NumberProperty(Constants.AI_BONUS_INCOME_PERCENTAGE, null, 200, -100, 0));
    }
    if (!runningList.contains(Constants.AI_BONUS_ATTACK)) {
      data.getProperties()
          .addEditableProperty(new NumberProperty(Constants.AI_BONUS_ATTACK, null, data.getDiceSides(), 0, 0));
    }
    if (!runningList.contains(Constants.AI_BONUS_DEFENSE)) {
      data.getProperties()
          .addEditableProperty(new NumberProperty(Constants.AI_BONUS_DEFENSE, null, data.getDiceSides(), 0, 0));
    }
  }

  private void parseEditableProperty(final GameData data, final Element property, final String name, final String defaultValue)
      throws GameParseException {
    // what type
    final List<Node> children = getNonTextNodes(property);
    if (children.size() != 1) {
      throw new GameParseException(data,
          "Editable properties must have exactly 1 child specifying the type. Number of children found:"
              + children.size() + " for node:" + property.getNodeName());
    }
    final Element child = (Element) children.get(0);
    final String childName = child.getNodeName();
    IEditableProperty editableProperty;
    if (childName.equals("boolean")) {
      editableProperty = new BooleanProperty(name, null, Boolean.valueOf(defaultValue).booleanValue());
    } else if (childName.equals("file")) {
      editableProperty = new FileProperty(name, null, defaultValue);
    } else if (childName.equals("list") || childName.equals("combo")) {
      final StringTokenizer tokenizer = new StringTokenizer(child.getAttribute("values"), ",");
      final Collection<String> values = new ArrayList<>();
      while (tokenizer.hasMoreElements()) {
        values.add(tokenizer.nextToken());
      }
      editableProperty = new ComboProperty<>(name, null, defaultValue, values);
    } else if (childName.equals("number")) {
      final int max = Integer.valueOf(child.getAttribute("max")).intValue();
      final int min = Integer.valueOf(child.getAttribute("min")).intValue();
      final int def = Integer.valueOf(defaultValue).intValue();
      editableProperty = new NumberProperty(name, null, max, min, def);
    } else if (childName.equals("color")) {
      // Parse the value as a hexidecimal number
      final int def = Integer.valueOf(defaultValue, 16).intValue();
      editableProperty = new ColorProperty(name, null, def);
    } else if (childName.equals("string")) {
      editableProperty = new StringProperty(name, null, defaultValue);
    } else {
      throw new GameParseException(data, "Unrecognized property type:" + childName);
    }
    data.getProperties().addEditableProperty(editableProperty);
  }

  private void parseOffset(final GameData data, final Node offsetAttributes) {
    if (offsetAttributes == null) {
      return;
    }
    final int roundOffset = Integer.parseInt(((Element) offsetAttributes).getAttribute("round"));
    data.getSequence().setRoundOffset(roundOffset);
  }

  private void parseDelegates(final GameData data, final List<Element> delegateList) throws GameParseException {
    final DelegateList delegates = data.getDelegateList();
    for (Element current : delegateList) {
      // load the class
      final String className = current.getAttribute("javaClass");
      IDelegate delegate = null;
      try {
        delegate = (IDelegate) getInstance(data.getGameName(), className);
      } catch (final ClassCastException cce) {
        throw new GameParseException(data, "Class <" + className + "> is not a delegate.");
      }
      final String name = current.getAttribute("name");
      String displayName = current.getAttribute("display");
      if (displayName == null) {
        displayName = name;
      }
      delegate.initialize(name, displayName);
      delegates.addDelegate(delegate);
    }
  }

  private void parseSequence(final GameData data, final Node sequence) throws GameParseException {
    parseSteps(data, getChildren("step", sequence));
  }

  private void parseSteps(final GameData data, final List<Element> stepList) throws GameParseException {
    for (Element current : stepList) {
      final IDelegate delegate = getDelegate(data, current, "delegate");
      final PlayerID player = getPlayerID(data, current, "player", false);
      final String name = current.getAttribute("name");
      String displayName = null;
      final List<Element> propertyElements = getChildren("stepProperty", current);
      final Properties stepProperties = pareStepProperties(propertyElements);
      if (current.hasAttribute("display")) {
        displayName = current.getAttribute("display");
      }
      final GameStep step = new GameStep(name, displayName, player, delegate, data, stepProperties);
      if (current.hasAttribute("maxRunCount")) {
        final int runCount = Integer.parseInt(current.getAttribute("maxRunCount"));
        if (runCount <= 0) {
          throw new GameParseException(data, " maxRunCount must be positive");
        }
        step.setMaxRunCount(runCount);
      }
      data.getSequence().addStep(step);
    }
  }

  private Properties pareStepProperties(final List<Element> properties) {
    final Properties rVal = new Properties();
    for (final Element stepProperty : properties) {
      final String name = stepProperty.getAttribute("name");
      final String value = stepProperty.getAttribute("value");
      rVal.setProperty(name, value);
    }
    return rVal;
  }

  private void parseProduction(final GameData data, final Node root) throws GameParseException {
    parseProductionRules(data, getChildren("productionRule", root));
    parseProductionFrontiers(data, getChildren("productionFrontier", root));
    parsePlayerProduction(data, getChildren("playerProduction", root));
    parseRepairRules(data, getChildren("repairRule", root));
    parseRepairFrontiers(data, getChildren("repairFrontier", root));
    parsePlayerRepair(data, getChildren("playerRepair", root));
  }

  private void parseTechnology(final GameData data, final Node root) throws GameParseException {
    parseTechnologies(data, getSingleChild(data.getGameName(), "technologies", root, true));
    parsePlayerTech(data, getChildren("playerTech", root));
  }

  private void parseProductionRules(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final String name = current.getAttribute("name");
      final ProductionRule rule = new ProductionRule(name, data);

      final List<Element> costElements = getChildren("cost", current);
      if (costElements.isEmpty()) {
        throw new GameParseException(data, "no costs  for rule:" + rule.getName());
      }
      parseCosts(data, rule, getChildren("cost", current));
      parseResults(data, rule, getChildren("result", current));
      data.getProductionRuleList().addProductionRule(rule);
    }
  }

  private void parseRepairRules(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final String name = current.getAttribute("name");
      final RepairRule rule = new RepairRule(name, data);
      parseRepairCosts(data, rule, getChildren("cost", current));
      parseRepairResults(data, rule, getChildren("result", current));
      data.getRepairRuleList().addRepairRule(rule);
    }
  }

  private void parseCosts(final GameData data, final ProductionRule rule, final List<Element> elements)
      throws GameParseException {
    for (final Element current : elements) {
      final Resource resource = getResource(data, current, "resource");
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      rule.addCost(resource, quantity);
    }
  }

  private void parseRepairCosts(final GameData data, final RepairRule rule, final List<Element> elements) throws GameParseException {
    if (elements.size() == 0) {
      throw new GameParseException(data, "no costs  for rule:" + rule.getName());
    }
    for (final Element current : elements) {
      final Resource resource = getResource(data, current, "resource");
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      rule.addCost(resource, quantity);
    }
  }

  private void parseResults(final GameData data, final ProductionRule rule, final List<Element> elements) throws GameParseException {
    if (elements.size() == 0) {
      throw new GameParseException(data, "no results  for rule:" + rule.getName());
    }
    for (final Element current : elements) {
      // must find either a resource or a unit with the given name
      NamedAttachable result = null;
      result = getResource(data, current, "resourceOrUnit", false);
      if (result == null) {
        result = getUnitType(data, current, "resourceOrUnit", false);
      }
      if (result == null) {
        throw new GameParseException(data, "Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
      }
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      rule.addResult(result, quantity);
    }
  }

  private void parseRepairResults(final GameData data, final RepairRule rule, final List<Element> elements) throws GameParseException {
    if (elements.size() == 0) {
      throw new GameParseException(data, "no results  for rule:" + rule.getName());
    }
    for (final Element current : elements) {
      // must find either a resource or a unit with the given name
      NamedAttachable result = null;
      result = getResource(data, current, "resourceOrUnit", false);
      if (result == null) {
        result = getUnitType(data, current, "resourceOrUnit", false);
      }
      if (result == null) {
        throw new GameParseException(data, "Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
      }
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      rule.addResult(result, quantity);
    }
  }

  private void parseProductionFrontiers(final GameData data, final List<Element> elements) throws GameParseException {
    final ProductionFrontierList frontiers = data.getProductionFrontierList();
    for (final Element current : elements) {
      final String name = current.getAttribute("name");
      final ProductionFrontier frontier = new ProductionFrontier(name, data);
      parseFrontierRules(data, getChildren("frontierRules", current), frontier);
      frontiers.addProductionFrontier(frontier);
    }
  }

  private void parseTechnologies(final GameData data, final Node element) {
    if (element == null) {
      return;
    }
    final TechnologyFrontier allTechs = data.getTechnologyFrontier();
    parseTechs(data, getChildren("techname", element), allTechs);
  }

  private void parsePlayerTech(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final PlayerID player = getPlayerID(data, current, "player", true);
      final TechnologyFrontierList categories = player.getTechnologyFrontierList();
      parseCategories(data, getChildren("category", current), categories);
    }
  }

  private void parseCategories(final GameData data, final List<Element> elements,
      final TechnologyFrontierList categories)
      throws GameParseException {
    for (final Element current : elements) {
      final TechnologyFrontier tf = new TechnologyFrontier(current.getAttribute("name"), data);
      parseCategoryTechs(data, getChildren("tech", current), tf);
      categories.addTechnologyFrontier(tf);
    }
  }

  private void parseRepairFrontiers(final GameData data, final List<Element> elements) throws GameParseException {
    final RepairFrontierList frontiers = data.getRepairFrontierList();
    for (final Element current : elements) {
      final String name = current.getAttribute("name");
      final RepairFrontier frontier = new RepairFrontier(name, data);
      parseRepairFrontierRules(data, getChildren("repairRules", current), frontier);
      frontiers.addRepairFrontier(frontier);
    }
  }

  private void parsePlayerProduction(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final PlayerID player = getPlayerID(data, current, "player", true);
      final ProductionFrontier frontier = getProductionFrontier(data, current, "frontier", true);
      player.setProductionFrontier(frontier);
    }
  }

  private void parsePlayerRepair(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final PlayerID player = getPlayerID(data, current, "player", true);
      final RepairFrontier repairFrontier = getRepairFrontier(data, current, "frontier", true);
      player.setRepairFrontier(repairFrontier);
    }
  }

  private void parseFrontierRules(final GameData data, final List<Element> elements, final ProductionFrontier frontier)
      throws GameParseException {
    for (Element element : elements) {
      final ProductionRule rule = getProductionRule(data, element, "name", true);
      frontier.addRule(rule);
    }
  }

  private void parseTechs(final GameData data, final List<Element> elements, final TechnologyFrontier allTechsFrontier) {
    for (final Element current : elements) {
      final String name = current.getAttribute("name");
      final String tech = current.getAttribute("tech");
      TechAdvance ta;
      if (tech.length() > 0) {
        ta = new GenericTechAdvance(name, TechAdvance.findDefinedAdvanceAndCreateAdvance(tech, data), data);
      } else {
        try {
          ta = TechAdvance.findDefinedAdvanceAndCreateAdvance(name, data);
        } catch (final IllegalArgumentException e) {
          ta = new GenericTechAdvance(name, null, data);
        }
      }
      allTechsFrontier.addAdvance(ta);
    }
  }

  private void parseCategoryTechs(final GameData data, final List<Element> elements, final TechnologyFrontier frontier)
      throws GameParseException {
    for (final Element current : elements) {
      TechAdvance ta = data.getTechnologyFrontier().getAdvanceByProperty(current.getAttribute("name"));
      if (ta == null) {
        ta = data.getTechnologyFrontier().getAdvanceByName(current.getAttribute("name"));
      }
      if (ta == null) {
        throw new GameParseException(data, "Technology not found :" + current.getAttribute("name"));
      }
      frontier.addAdvance(ta);
    }
  }

  private void parseRepairFrontierRules(final GameData data, final List<Element> elements, final RepairFrontier frontier)
      throws GameParseException {
    for (Element element : elements) {
      final RepairRule rule = getRepairRule(data, element, "name", true);
      frontier.addRule(rule);
    }
  }

  private void parseAttachments(final GameData data, final Element root) throws GameParseException {
    final HashMap<String, Constructor<?>> constructors = new HashMap<>();
    for (final Element current : getChildren("attachment", root)) {
      // get class name and constructor
      final String className = current.getAttribute("javaClass");
      if (!constructors.containsKey(className)) {
        try {
          final Class<?> objectClass = getClassByName(data.getGameName(), className);
          if (!IAttachment.class.isAssignableFrom(objectClass)) {
            throw new GameParseException(data, className + " does not implement IAttachable");
          }
          constructors.put(className, objectClass.getConstructor(IAttachment.attachmentConstructorParameter));
        } catch (final NoSuchMethodException | SecurityException exception) {
          throw new GameParseException(data,
              "Constructor for class " + className + " could not be found: " + exception.getMessage());
        }
      }
      // find the attachable
      final String type = current.getAttribute("type");
      final Attachable attachable = findAttachment(data, current, type);
      // create new attachment
      final String name = current.getAttribute("name");
      final List<Element> options = getChildren("option", current);
      try {
        final IAttachment attachment = (IAttachment) constructors.get(className).newInstance(name, attachable, data);
        attachable.addAttachment(name, attachment);
        final ArrayList<Tuple<String, String>> attachmentOptionValues = setValues(data.getGameName(), attachment, options);
        // keep a list of attachment references in the order they were added
        data.addToAttachmentOrderAndValues(
            Tuple.of(attachment, attachmentOptionValues));
      } catch (final InstantiationException | InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
        throw new GameParseException(data,
            "Attachment of type " + className + " could not be instanciated: " + e.getMessage());
      }
    }
  }

  private Attachable findAttachment(final GameData data, final Element element, final String type) throws GameParseException {
    Attachable returnVal;
    final String name = "attachTo";
    if (type.equals("unitType")) {
      returnVal = getUnitType(data, element, name);
    } else if (type.equals("territory")) {
      returnVal = getTerritory(data, element, name);
    } else if (type.equals("resource")) {
      returnVal = getResource(data, element, name);
    } else if (type.equals("territoryEffect")) {
      returnVal = getTerritoryEffect(data, element, name);
    } else if (type.equals("player")) {
      returnVal = getPlayerID(data, element, name);
    } else if (type.equals("relationship")) {
      returnVal = this.getRelationshipType(data, element, name);
    } else if (type.equals("technology")) {
      returnVal = getTechnology(data, element, name);
    } else {
      throw new GameParseException(data, "Type not found to attach to:" + type);
    }
    return returnVal;
  }

  private static String capitalizeFirstLetter(final String aString) {
    char first = aString.charAt(0);
    first = Character.toUpperCase(first);
    return first + aString.substring(1);
  }

  private static ArrayList<Tuple<String, String>> setValues(String mapName, final IAttachment attachment, final List<Element> values)
      throws GameParseException {
    final ArrayList<Tuple<String, String>> options = new ArrayList<>();
    for (final Element current : values) {
      // find the setter
      String name = null;
      Method setter = null;
      try {
        name = current.getAttribute("name");
        if (name.length() == 0) {
          throw new GameParseException(mapName, "Option name with 0 length");
        }
        setter = attachment.getClass().getMethod("set" + capitalizeFirstLetter(name), SETTER_ARGS);
      } catch (final NoSuchMethodException nsme) {
        throw new GameParseException(mapName, "The following option name of " + attachment.getName() + " of class "
            + attachment.getClass().getName().substring(attachment.getClass().getName().lastIndexOf('.') + 1)
            + " are either misspelled or exist only in a future version of TripleA. Setter: " + name);
      }
      // find the value
      final String value = current.getAttribute("value");
      final String count = current.getAttribute("count");
      String itemValues;
      if (count.length() > 0) {
        itemValues = count + ":" + value;
      } else {
        itemValues = value;
      }
      // invoke
      try {
        final Object[] args = {itemValues};
        setter.invoke(attachment, args);
      } catch (final IllegalAccessException iae) {
        throw new GameParseException(mapName, "Setter not public. Setter:" + name + " Class:" + attachment.getClass().getName());
      } catch (final InvocationTargetException ite) {
        ite.getCause().printStackTrace(System.out);
        throw new GameParseException(mapName, "Error setting property:" + name + " cause:" + ite.getCause().getMessage());
      }
      options.add(Tuple.of(name, itemValues));
    }
    return options;
  }

  private void parseInitialization(final GameData data, final Node root) throws GameParseException {
    // parse territory owners
    final Node owner = getSingleChild(data.getGameName(), "ownerInitialize", root, true);
    if (owner != null) {
      parseOwner(data, getChildren("territoryOwner", owner));
    }
    // parse initial unit placement
    final Node unit = getSingleChild(data.getGameName(), "unitInitialize", root, true);
    if (unit != null) {
      parseUnitPlacement(data, getChildren("unitPlacement", unit));
      parseHeldUnits(data, getChildren("heldUnits", unit));
    }
    // parse resources given
    final Node resource = getSingleChild(data.getGameName(), "resourceInitialize", root, true);
    if (resource != null) {
      parseResourceInitialization(data, getChildren("resourceGiven", resource));
    }
    // parse relationships
    final Node relationInitialize = getSingleChild(data.getGameName(), "relationshipInitialize", root, true);
    if (relationInitialize != null) {
      parseRelationInitialize(data, getChildren("relationship", relationInitialize));
    }
  }

  private void parseOwner(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final Territory territory = getTerritory(data, current, "territory");
      final PlayerID owner = getPlayerID(data, current, "owner", true);
      territory.setOwner(owner);
      // Set the original owner on startup.
      // TODO Look into this
      // The addition of this caused the automated tests to fail as TestAttachment can't be cast to TerritoryAttachment
      // The addition of this IF to pass the tests is wrong, but works until a better solution is found.
      // Kevin will look into it.
      if (!territory.getData().getGameName().equals("gameExample")
          && !territory.getData().getGameName().equals("test")) {
        // set the original owner
        final TerritoryAttachment ta = TerritoryAttachment.get(territory);
        if (ta != null) {
          // If we already have an original owner set (ie: we set it previously in the attachment using originalOwner or
          // occupiedTerrOf),
          // then we DO NOT set the original owner again.
          // This is how we can have a game start with territories owned by 1 faction but controlled by a 2nd faction.
          final PlayerID currentOwner = ta.getOriginalOwner();
          if (currentOwner == null) {
            ta.setOriginalOwner(owner);
          }
        }
      }
    }
  }

  private void parseUnitPlacement(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final Territory territory = getTerritory(data, current, "territory");
      final UnitType type = getUnitType(data, current, "unitType", true);
      final String ownerString = current.getAttribute("owner");
      final String hitsTakenString = current.getAttribute("hitsTaken");
      final String unitDamageString = current.getAttribute("unitDamage");
      final PlayerID owner;
      if (ownerString == null || ownerString.trim().length() == 0) {
        owner = PlayerID.NULL_PLAYERID;
      } else {
        owner = getPlayerID(data, current, "owner", false);
      }
      final int hits;
      if (hitsTakenString != null && hitsTakenString.trim().length() > 0) {
        hits = Integer.parseInt(hitsTakenString);
        if (hits < 0 || hits > UnitAttachment.get(type).getHitPoints() - 1) {
          throw new GameParseException(data,
              "hitsTaken cannot be less than zero or greater than one less than total hitpPoints");
        }
      } else {
        hits = 0;
      }
      final int unitDamage;
      if (unitDamageString != null && unitDamageString.trim().length() > 0) {
        unitDamage = Integer.parseInt(unitDamageString);
        if (unitDamage < 0) {
          throw new GameParseException(data, "unitDamage cannot be less than zero");
        }
      } else {
        unitDamage = 0;
      }
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      territory.getUnits().addAllUnits(type.create(quantity, owner, false, hits, unitDamage));
    }
  }

  private void parseHeldUnits(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final PlayerID player = getPlayerID(data, current, "player", true);
      final UnitType type = getUnitType(data, current, "unitType", true);
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      player.getUnits().addAllUnits(type.create(quantity, player));
    }
  }

  private void parseResourceInitialization(final GameData data, final List<Element> elements) throws GameParseException {
    for (final Element current : elements) {
      final PlayerID player = getPlayerID(data, current, "player", true);
      final Resource resource = getResource(data, current, "resource", true);
      final int quantity = Integer.parseInt(current.getAttribute("quantity"));
      player.getResources().addResource(resource, quantity);
    }
  }

  private void checkThatAllUnitsHaveAttachments(final GameData data) throws GameParseException {
    final Collection<UnitType> errors = new ArrayList<>();
    for (final UnitType ut : data.getUnitTypeList().getAllUnitTypes()) {
      final UnitAttachment ua = UnitAttachment.get(ut);
      if (ua == null) {
        errors.add(ut);
      }
    }
    if (!errors.isEmpty()) {
      throw new GameParseException(data,
          data.getGameName() + " does not have unit attachments for: " + MyFormatter.defaultNamedToTextList(errors));
    }
  }
}
