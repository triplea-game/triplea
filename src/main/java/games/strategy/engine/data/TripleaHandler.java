package games.strategy.engine.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Abstract common implementation of a DefaultHandler.
 */
public abstract class TripleaHandler extends DefaultHandler {

  public static final String XML_NODE_NAME_ALLIANCE = "alliance";
  public static final String XML_NODE_NAME_ATTACHMENT = "attachment";
  public static final String XML_NODE_NAME_ATTACHMENT_LIST = "attachmentList";
  public static final String XML_NODE_NAME_BOOLEAN = "boolean";
  public static final String XML_NODE_NAME_CONNECTION = "connection";
  public static final String XML_NODE_NAME_COST = "cost";
  public static final String XML_NODE_NAME_DELEGATE = "delegate";
  public static final String XML_NODE_NAME_FRONTIER_RULES = "frontierRules";
  public static final String XML_NODE_NAME_GAME = "game";
  public static final String XML_NODE_NAME_GAME_PLAY = "gamePlay";
  public static final String XML_NODE_NAME_INFO = "info";
  public static final String XML_NODE_NAME_INITIALIZE = "initialize";
  public static final String XML_NODE_NAME_MAP = "map";
  public static final String XML_NODE_NAME_NUMBER = "number";
  public static final String XML_NODE_NAME_OPTION = "option";
  public static final String XML_NODE_NAME_OWNER_INITIALIZE = "ownerInitialize";
  public static final String XML_NODE_NAME_PLAYER = "player";
  public static final String XML_NODE_NAME_PLAYER_LIST = "playerList";
  public static final String XML_NODE_NAME_PRODUCTION = "production";
  public static final String XML_NODE_NAME_PRODUCTION_FRONTIER = "productionFrontier";
  public static final String XML_NODE_NAME_PRODUCTION_RULE = "productionRule";
  public static final String XML_NODE_NAME_PROPERTY = "property";
  public static final String XML_NODE_NAME_PROPERTY_LIST = "propertyList";
  public static final String XML_NODE_NAME_RESOURCE = "resource";
  public static final String XML_NODE_NAME_RESOURCE_LIST = "resourceList";
  public static final String XML_NODE_NAME_RESULT = "result";
  public static final String XML_NODE_NAME_SEQUENCE = "sequence";
  public static final String XML_NODE_NAME_STEP = "step";
  public static final String XML_NODE_NAME_TERRITORY = "territory";
  public static final String XML_NODE_NAME_TERRITORY_OWNER = "territoryOwner";
  public static final String XML_NODE_NAME_UNIT_INITIALIZE = "unitInitialize";
  public static final String XML_NODE_NAME_UNIT_LIST = "unitList";
  public static final String XML_NODE_NAME_UNIT_PLACEMENT = "unitPlacement";
  public static final String XML_NODE_NAME_VALUE = "value";
  public static final String XML_NODE_NAME_UNIT = "unit";
  public static final String XML_NODE_NAME_LOADER = "loader";
  public static final String XML_NODE_NAME_TRIPLEA = "triplea";
  public static final String XML_NODE_NAME_DICE_SIDES = "diceSides";
  public static final String XML_NODE_NAME_RELATIONSHIP_TYPES = "relationshipTypes";
  public static final String XML_NODE_NAME_RELATIONSHIP_TYPE = "relationshipType";
  public static final String XML_NODE_NAME_TERRITORY_EFFECT_LIST = "territoryEffectList";
  public static final String XML_NODE_NAME_STRING = "string";
  public static final String XML_NODE_NAME_TECHNOLOGY = "technology";
  public static final String XML_NODE_NAME_TECHNOLOGIES = "technologies";
  public static final String XML_NODE_NAME_OFFSET = "offset";
  public static final String XML_NODE_NAME_REPAIR_RULE = "repairRule";
  public static final String XML_NODE_NAME_REPAIR_RULES = "repairRules";
  public static final String XML_NODE_NAME_REPAIR_FRONTIER = "repairFrontier";
  public static final String XML_NODE_NAME_PLAYER_PRODUCTION = "playerProduction";
  public static final String XML_NODE_NAME_PLAYER_REPAIR = "playerRepair";
  public static final String XML_NODE_NAME_RESOURCE_INITIALIZE = "resourceInitialize";
  public static final String XML_NODE_NAME_RELATIONSHIP_INITIALIZE = "relationshipInitialize";
  public static final String XML_NODE_NAME_RELATIONSHIP = "relationship";
  public static final String XML_NODE_NAME_RESOURCE_GIVEN = "resourceGiven";
  public static final String XML_NODE_NAME_TECHNAME = "techname";
  public static final String XML_NODE_NAME_PLAYER_TECH = "playerTech";
  public static final String XML_NODE_NAME_CATEGORY = "category";
  public static final String XML_NODE_NAME_TECH = "tech";
  public static final String XML_NODE_NAME_TERRITORY_EFFECT = "territoryEffect";
  public static final String XML_NODE_NAME_STEP_PROPERTY = "stepProperty";

  private final GameData gameData;
  private final Deque<PseudoElement> stack = new LinkedList<>();
  private static final Multimap<String, String> requiredParents = HashMultimap.create();

  static {
    requiredParents.put(XML_NODE_NAME_GAME, null);
    requiredParents.put(XML_NODE_NAME_INFO, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_LOADER, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_TRIPLEA, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_DICE_SIDES, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_GAME_PLAY, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_PLAYER_LIST, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_MAP, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_PROPERTY_LIST, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_RESOURCE_LIST, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_UNIT_LIST, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_RELATIONSHIP_TYPES, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_TERRITORY_EFFECT_LIST, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_PRODUCTION, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_TECHNOLOGY, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_ATTACHMENT_LIST, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_INITIALIZE, XML_NODE_NAME_GAME);
    requiredParents.put(XML_NODE_NAME_TERRITORY, XML_NODE_NAME_MAP);
    requiredParents.put(XML_NODE_NAME_CONNECTION, XML_NODE_NAME_MAP);
    requiredParents.put(XML_NODE_NAME_RESOURCE, XML_NODE_NAME_RESOURCE_LIST);
    requiredParents.put(XML_NODE_NAME_PLAYER, XML_NODE_NAME_PLAYER_LIST);
    requiredParents.put(XML_NODE_NAME_ALLIANCE, XML_NODE_NAME_PLAYER_LIST);
    requiredParents.put(XML_NODE_NAME_UNIT, XML_NODE_NAME_UNIT_LIST);
    requiredParents.put(XML_NODE_NAME_DELEGATE, XML_NODE_NAME_GAME_PLAY);
    requiredParents.put(XML_NODE_NAME_SEQUENCE, XML_NODE_NAME_GAME_PLAY);
    requiredParents.put(XML_NODE_NAME_OFFSET, XML_NODE_NAME_GAME_PLAY);
    requiredParents.put(XML_NODE_NAME_STEP, XML_NODE_NAME_SEQUENCE);
    requiredParents.put(XML_NODE_NAME_PRODUCTION_RULE, XML_NODE_NAME_PRODUCTION);
    requiredParents.put(XML_NODE_NAME_REPAIR_RULE, XML_NODE_NAME_PRODUCTION);
    requiredParents.put(XML_NODE_NAME_REPAIR_FRONTIER, XML_NODE_NAME_PRODUCTION);
    requiredParents.put(XML_NODE_NAME_PLAYER_PRODUCTION, XML_NODE_NAME_PRODUCTION);
    requiredParents.put(XML_NODE_NAME_PLAYER_REPAIR, XML_NODE_NAME_PRODUCTION);
    requiredParents.put(XML_NODE_NAME_PRODUCTION_FRONTIER, XML_NODE_NAME_PRODUCTION);
    requiredParents.putAll(XML_NODE_NAME_COST, Arrays.asList(XML_NODE_NAME_PRODUCTION_RULE, XML_NODE_NAME_REPAIR_RULE));
    requiredParents.putAll(XML_NODE_NAME_RESULT,
        Arrays.asList(XML_NODE_NAME_PRODUCTION_RULE, XML_NODE_NAME_REPAIR_RULE));
    requiredParents.put(XML_NODE_NAME_FRONTIER_RULES, XML_NODE_NAME_PRODUCTION_FRONTIER);
    requiredParents.put(XML_NODE_NAME_ATTACHMENT, XML_NODE_NAME_ATTACHMENT_LIST);
    requiredParents.put(XML_NODE_NAME_OPTION, XML_NODE_NAME_ATTACHMENT);
    requiredParents.put(XML_NODE_NAME_OWNER_INITIALIZE, XML_NODE_NAME_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_UNIT_INITIALIZE, XML_NODE_NAME_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_RESOURCE_INITIALIZE, XML_NODE_NAME_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_RELATIONSHIP_INITIALIZE, XML_NODE_NAME_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_RELATIONSHIP, XML_NODE_NAME_RELATIONSHIP_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_TERRITORY_OWNER, XML_NODE_NAME_OWNER_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_UNIT_PLACEMENT, XML_NODE_NAME_UNIT_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_RESOURCE_GIVEN, XML_NODE_NAME_RESOURCE_INITIALIZE);
    requiredParents.put(XML_NODE_NAME_PROPERTY, XML_NODE_NAME_PROPERTY_LIST);
    requiredParents.put(XML_NODE_NAME_NUMBER, XML_NODE_NAME_PROPERTY);
    requiredParents.put(XML_NODE_NAME_BOOLEAN, XML_NODE_NAME_PROPERTY);
    requiredParents.put(XML_NODE_NAME_VALUE, XML_NODE_NAME_PROPERTY);
    requiredParents.put(XML_NODE_NAME_STRING, XML_NODE_NAME_PROPERTY);
    requiredParents.put(XML_NODE_NAME_TECHNOLOGIES, XML_NODE_NAME_TECHNOLOGY);
    requiredParents.put(XML_NODE_NAME_TECHNAME, XML_NODE_NAME_TECHNOLOGIES);
    requiredParents.put(XML_NODE_NAME_PLAYER_TECH, XML_NODE_NAME_TECHNOLOGY);
    requiredParents.put(XML_NODE_NAME_CATEGORY, XML_NODE_NAME_PLAYER_TECH);
    requiredParents.put(XML_NODE_NAME_TECH, XML_NODE_NAME_CATEGORY);
    requiredParents.put(XML_NODE_NAME_REPAIR_RULES, XML_NODE_NAME_REPAIR_FRONTIER);
    requiredParents.put(XML_NODE_NAME_RELATIONSHIP_TYPE, XML_NODE_NAME_RELATIONSHIP_TYPES);
    requiredParents.put(XML_NODE_NAME_TERRITORY_EFFECT, XML_NODE_NAME_TERRITORY_EFFECT_LIST);
    requiredParents.put(XML_NODE_NAME_STEP_PROPERTY, XML_NODE_NAME_STEP);
  }

  public TripleaHandler(GameData gameData) {
    this.gameData = gameData;
  }

  @Override
  public void startElement(String uri, String localName, String qname, Attributes attributes) throws SAXException {
    try {
      checkParentNode(qname);
    } catch (GameParseException e) {
      throw new SAXException(e);
    }
    handleElement(qname, attributes);
    stack.addFirst(new PseudoElement(qname, new AttributesImpl(attributes)));
  }

  @Override
  public void endElement(String uri, String localName, String qname) throws SAXException {
    stack.pop();
    try {
      switch (qname) {
        case XML_NODE_NAME_MAP:
          handleMap();
          break;
        case XML_NODE_NAME_PLAYER_LIST:
          handlePlayerList();
          break;
        case XML_NODE_NAME_PRODUCTION:
          handleProduction();
          break;
        case XML_NODE_NAME_PRODUCTION_RULE:
          handleProductionRule();
          break;
        case XML_NODE_NAME_PRODUCTION_FRONTIER:
          handleProductionFrontier();
          break;
        case XML_NODE_NAME_GAME_PLAY:
          handleGamePlay();
          break;
        case XML_NODE_NAME_ATTACHMENT_LIST:
          handleAttachmentList();
          break;
        case XML_NODE_NAME_ATTACHMENT:
          handleAttachment();
          break;
        case XML_NODE_NAME_INITIALIZE:
          handleInitialize();
          break;
        case XML_NODE_NAME_PROPERTY_LIST:
          handlePropertyList();
          break;
        default:
          break;
      }
    } catch (GameParseException e) {
      throw new SAXException(e);
    }
  }

  @Override
  public void warning(SAXParseException e) throws SAXException {
    logError(e);
  }

  @Override
  public void error(SAXParseException e) throws SAXException {
    logError(e);
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    logError(e);
    throw e;
  }

  private void logError(SAXParseException e) {
    System.err.println("SAXParseException: game: " + printIfNotNull(gameData, printIfNotNull(gameData.getGameName()))
        + ", line: " + e.getLineNumber() + ", column: " + e.getColumnNumber() + ", error: " + e.getMessage());
  }

  private String printIfNotNull(Object object, Object alt) {
    return String.valueOf(object == null ? "?" : alt);
  }

  private String printIfNotNull(Object object) {
    return printIfNotNull(object, object);
  }

  private void handleElement(String qname, Attributes attributes) throws SAXException {
    try {
      switch (qname) {
        case XML_NODE_NAME_INFO:
          handleInfo(attributes);
          break;
        case "loader":
          handleLoader(attributes);
          break;
        case "triplea":
          handleTriplea(attributes);
          break;
        case "diceSides":
          handleDiceSides(attributes);
          break;
        case XML_NODE_NAME_TERRITORY:
          handleTerritory(attributes);
          break;
        case XML_NODE_NAME_CONNECTION:
          handleConnection(attributes);
          break;
        case XML_NODE_NAME_RESOURCE:
          handleResource(attributes);
          break;
        case XML_NODE_NAME_PLAYER:
          handlePlayer(attributes);
          break;
        case XML_NODE_NAME_ALLIANCE:
          handleAlliance(attributes);
          break;
        case XML_NODE_NAME_UNIT:
          handleUnit(attributes);
          break;
        case XML_NODE_NAME_DELEGATE:
          handleDelegate(attributes);
          break;
        case XML_NODE_NAME_STEP:
          handleStep(attributes);
          break;
        case "playerProduction":
          handlePlayerProduction(attributes);
          break;
        case XML_NODE_NAME_COST:
          handleCost(attributes);
          break;
        case XML_NODE_NAME_RESULT:
          handleResult(attributes);
          break;
        case XML_NODE_NAME_FRONTIER_RULES:
          handleFrontierRules(attributes);
          break;
        case XML_NODE_NAME_OPTION:
          handleOption(attributes);
          break;
        case XML_NODE_NAME_TERRITORY_OWNER:
          handleTerritoryOwner(attributes);
          break;
        case XML_NODE_NAME_UNIT_PLACEMENT:
          handleUnitPlacement(attributes);
          break;
        case "resourceGiven":
          handleResourceGiven(attributes);
          break;
        case XML_NODE_NAME_PROPERTY:
          handleProperty(attributes);
          break;
        case XML_NODE_NAME_NUMBER:
          handleNumber();
          break;
        case XML_NODE_NAME_BOOLEAN:
          handleBoolean();
          break;
        case "tech":
          handleTech(attributes);
          break;
        case XML_NODE_NAME_PRODUCTION_FRONTIER:
          handleProductionFrontier(attributes);
          break;
        case XML_NODE_NAME_ATTACHMENT:
          handleAttachment(attributes);
          break;
        case XML_NODE_NAME_VALUE:
        case XML_NODE_NAME_SEQUENCE:
        case XML_NODE_NAME_RESOURCE_LIST:
        case XML_NODE_NAME_UNIT_LIST:
        case "relationshipTypes":
        case "territoryEffectList":
        case "technology":
        case XML_NODE_NAME_OWNER_INITIALIZE:
        case XML_NODE_NAME_UNIT_INITIALIZE:
        case "resouceInitialize":
        case "technologies":
        case "playerTech":
        case "category":
        default:
          break;
      }
    } catch (GameParseException e) {
      throw new SAXException(e);
    }
  }

  private void checkParentNode(String qname) throws GameParseException {
    if (!stack.isEmpty()) {
      String actualName = stack.peek().getName();
      Collection<String> parents = requiredParents.get(qname);
      if (parents.isEmpty()) {
        throw new GameParseException("Unknown element '" + qname + "'");
      } else if (!parents.contains(actualName)) {
        throw new GameParseException(
            "Parent of '" + qname + "' must be one of " + Arrays.toString(parents.toArray()) + ", but was '"
                + actualName + "'");
      }
    } else if (!qname.equals(XML_NODE_NAME_GAME)) {
      throw new GameParseException("Root element must be 'game', found '" + qname + "' instead");
    }
  }

  protected abstract void handleInfo(Attributes attributes) throws GameParseException;

  protected abstract void handleLoader(Attributes attributes) throws GameParseException;

  protected abstract void handleTriplea(Attributes attributes) throws GameParseException;

  protected abstract void handleDiceSides(Attributes attributes) throws GameParseException;

  protected abstract void handleTerritory(Attributes attributes) throws GameParseException;

  protected abstract void handleConnection(Attributes attributes) throws GameParseException;

  protected abstract void handleResource(Attributes attributes) throws GameParseException;

  protected abstract void handlePlayer(Attributes attributes) throws GameParseException;

  protected abstract void handleAlliance(Attributes attributes) throws GameParseException;

  protected abstract void handleUnit(Attributes attributes) throws GameParseException;

  protected abstract void handleDelegate(Attributes attributes) throws GameParseException;

  protected abstract void handleStep(Attributes attributes) throws GameParseException;

  protected abstract void handlePlayerProduction(Attributes attributes) throws GameParseException;

  protected abstract void handleCost(Attributes attributes) throws GameParseException;

  protected abstract void handleResult(Attributes attributes) throws GameParseException;

  protected abstract void handleFrontierRules(Attributes attributes) throws GameParseException;

  protected abstract void handleOption(Attributes attributes) throws GameParseException;

  protected abstract void handleUnitPlacement(Attributes attributes) throws GameParseException;

  protected abstract void handleTerritoryOwner(Attributes attributes) throws GameParseException;

  protected abstract void handleResourceGiven(Attributes attributes) throws GameParseException;

  protected abstract void handleProperty(Attributes attributes) throws GameParseException;

  protected abstract void handleNumber() throws GameParseException;

  protected abstract void handleBoolean() throws GameParseException;

  protected abstract void handleTech(Attributes attributes) throws GameParseException;

  protected abstract void handleMap() throws GameParseException;

  protected abstract void handlePlayerList() throws GameParseException;

  protected abstract void handleProduction() throws GameParseException;

  protected abstract void handleProductionRule() throws GameParseException;

  protected abstract void handleProductionFrontier() throws GameParseException;

  protected abstract void handleProductionFrontier(Attributes attributes) throws GameParseException;

  protected abstract void handleGamePlay() throws GameParseException;

  protected abstract void handleAttachmentList() throws GameParseException;

  protected abstract void handleAttachment() throws GameParseException;

  protected abstract void handleAttachment(Attributes attributes) throws GameParseException;

  protected abstract void handleInitialize() throws GameParseException;

  protected abstract void handlePropertyList() throws GameParseException;

  protected PseudoElement getCurrentParent() {
    if (stack.isEmpty()) {
      throw new IllegalStateException("This method must only be called while parsing");
    }
    return stack.peek();
  }


  /**
   * Data class for wrapping the important parameters of DefaultHandler methods into a single object.
   */
  protected final class PseudoElement {

    private final String qname;
    private final Attributes attributes;

    public PseudoElement(String qname, Attributes attributes) {
      this.qname = qname;
      this.attributes = attributes;
    }

    public String getName() {
      return qname;
    }

    public Attributes getAttributes() {
      return attributes;
    }

    @Override
    public String toString() {
      return qname + ": " + attrToString(attributes);
    }
  }

  /**
   * A Small utility method which nicely formats an attribute as a String.
   */
  public static String attrToString(Attributes attributes) {
    Joiner joiner = Joiner.on(' ');
    final String[] attrs = new String[attributes.getLength()];
    for (int i = 0; i < attributes.getLength(); i++) {
      attrs[i] = attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"";
    }
    return joiner.join(attrs);
  }
}
