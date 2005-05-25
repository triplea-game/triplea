/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Parser.java
 *
 * Created on October 12, 2001, 12:55 PM
 */

package games.strategy.engine.data;

import games.strategy.engine.data.properties.*;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.util.Version;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 *
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class GameParser
{
  private static final Class[] SETTER_ARGS = {String.class};

  private GameData data;

  public GameParser()
  {
  }

  public synchronized GameData parse(InputStream stream) throws GameParseException, SAXException
  {
    if(stream == null)
      throw new IllegalArgumentException("Stream must be non null");

    Document doc = null;
    try
    {
      doc = getDocument(stream);
    } catch(Exception e)
    {
       //not terribly elegant, but we cant deal with the error here
      throw new SAXException("Error parsing stream:" + e.getMessage(), e);
    }


    Node root = doc.getDocumentElement();

    data = new GameData();

    //mandatory fields
    parseInfo(getSingleChild("info", root));
    parseGameLoader(getSingleChild("loader", root));
    parseMap(getSingleChild("map", root));
    parseResources(getSingleChild("resourceList", root));
    parseUnits(getSingleChild("unitList", root));
    parsePlayers(getSingleChild("playerList", root));
    parseGamePlay(getSingleChild("gamePlay", root));

    //optional
    Node production = getSingleChild("production", root, true);
    if(production != null)
      parseProduction(production);

    Node attatchmentList = getSingleChild("attatchmentList", root, true);
    if(attatchmentList != null)
      parseAttatchments(attatchmentList);

    Node initialization = getSingleChild("initialize", root, true);
    if(initialization != null)
      parseInitialization(initialization);

    Node properties = getSingleChild("propertyList", root, true);
    if(properties != null)
      pareseProperties(properties);

    return data;
  }

  private Document getDocument(InputStream input) throws SAXException, IOException, ParserConfigurationException
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(true);

    //get the dtd location
    URL url = GameParser.class.getResource("/games/strategy/engine/xml/");
    String system = url.toExternalForm();

    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(input, system);
  }

  /**
   * If mustfind is true and cannot find the player an exception will be thrown.
   */
  private PlayerID getPlayerID(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    PlayerID player = data.getPlayerList().getPlayerID(name);
    if(player == null && mustFind)
      throw new GameParseException("Could not find player. name:" + name);

    return player;
  }

  /**
   * If mustfind is true and cannot find the productionRule an exception will be thrown.
   */
  private ProductionRule getProductionRule(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    ProductionRule productionRule = data.getProductionRuleList().getProductionRule(name);
    if(productionRule == null && mustFind)
      throw new GameParseException("Could not find production rule. name:" + name);

    return productionRule;
  }

  /**
   * If mustfind is true and cannot find the territory an exception will be thrown.
   */
  private Territory getTerritory(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    Territory territory = data.getMap().getTerritory(name);
    if(territory == null && mustFind)
      throw new GameParseException("Could not find territory. name:" + name);

    return territory;
  }

  /**
   * If mustfind is true and cannot find the unitType an exception will be thrown.
   */
  private UnitType getUnitType(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    UnitType type = data.getUnitTypeList().getUnitType(name);
    if(type == null && mustFind)
      throw new GameParseException("Could not find unitType. name:" + name);

    return type;
  }

  /**
   * If mustfind is true and cannot find the Delegate an exception will be thrown.
   */
  private IDelegate getDelegate(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    IDelegate delegate = data.getDelegateList().getDelegate(name);
    if(delegate == null && mustFind)
      throw new GameParseException("Could not find delegate. name:" + name);

    return delegate;
  }

  /**
   * If mustfind is true and cannot find the Resource an exception will be thrown.
   */
  private Resource getResource(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    Resource resource = data.getResourceList().getResource(name);
    if(resource == null && mustFind)
      throw new GameParseException("Could not find resource. name:" + name);

    return resource;
  }

  /**
   * If mustfind is true and cannot find the productionRule an exception will be thrown.
   */
  private ProductionFrontier getProductionFrontier(Element element, String attribute, boolean mustFind) throws GameParseException
  {
    String name = element.getAttribute(attribute);
    ProductionFrontier productionFrontier = data.getProductionFrontierList().getProductionFrontier(name);
    if(productionFrontier == null && mustFind)
      throw new GameParseException("Could not find production frontier. name:" + name);

    return productionFrontier;
  }

  /**
   * Loads an instance of the given class.
   * Assumes a zero argument constructor.
   */
  private Object getInstance(String className) throws GameParseException
  {
    Object instance = null;

    try
    {
      Class instanceClass = Class.forName(className);
      instance = instanceClass.newInstance();
    }
    //a lot can go wrong, the following list is just a subset of potential pitfalls
    catch(ClassNotFoundException cnfe)
    {
      throw new GameParseException("Class <" + className + "> could not be found.");
    } catch(InstantiationException ie)
    {
      throw new GameParseException("Class <" + className + "> could not be instantiated. ->" + ie.getMessage());
    } catch(IllegalAccessException iae)
    {
      throw new GameParseException("Constructor could not be accessed ->" + iae.getMessage());
    }

    return instance;
  }

  /**
   * Get the given child.
   * If there is not exactly one child throw a SAXExcpetion
   */
  private Node getSingleChild(String name, Node node) throws GameParseException
  {
    return getSingleChild(name, node, false);
  }

  /**
   * If optional is true, will not throw an exception if there are 0 children
   */
  private Node getSingleChild(String name, Node node, boolean optional) throws GameParseException
  {
    List children = getChildren(name, node);

    //none found
    if(children.size() == 0)
    {
      if(optional)
        return null;

      throw new GameParseException("No child called " + name);
    }
    //too many found
    if(children.size() > 1)
    {
      throw new GameParseException("Too many children named " + name);
    }
    return (Node) children.get(0);
  }

  private List getChildren(String name, Node node)
  {
    ArrayList found = new ArrayList();
    NodeList children = node.getChildNodes();
    for(int i= 0; i < children.getLength(); i++)
    {
      Node current = children.item(i);
      if(current.getNodeName().equals(name))
        found.add(current);
    }
    return found;
  }

  private List getNonTextNodes(Node node)
  {
    ArrayList found = new ArrayList();
    NodeList children = node.getChildNodes();
    for(int i= 0; i < children.getLength(); i++)
    {
      Node current = children.item(i);
      if(! (current.getNodeType() == Node.TEXT_NODE))
        found.add(current);
    }
    return found;

  }

  private void parseInfo(Node info) throws GameParseException
  {
    String gameName = ( (Element) info).getAttribute("name");
    data.setGameName(gameName);

    String version = ( (Element) info).getAttribute("version");
    data.setGameVersion(new Version(version));
  }

  private void parseGameLoader(Node loader) throws GameParseException
  {
    String className = ( (Element) loader).getAttribute("javaClass");
    Object instance = getInstance(className);
    if(!(instance instanceof IGameLoader))
    {
      throw new GameParseException("Loader must implement IGameLoader.  Class Name:" + className);
    }
    data.setGameLoader( (IGameLoader) instance);
  }

  private void parseMap(Node map) throws GameParseException
  {
    //get the Territories
    List territories = getChildren("territory", map);
    parseTerritories(territories);
    List connections = getChildren("connection", map);
    parseConnections(connections);

  }

  private void parseTerritories(List territories)
  {
    GameMap map = data.getMap();

    for(int i = 0; i < territories.size(); i++)
    {
      Element current = (Element) territories.get(i);
      boolean water = current.getAttribute("water").trim().equalsIgnoreCase("true");
      String name = current.getAttribute("name");

      Territory newTerritory = new Territory(name, water, data);
      map.addTerritory(newTerritory);

    }
  }

  private void parseConnections(List connections) throws GameParseException
  {
    GameMap map = data.getMap();

    for(int i = 0; i < connections.size(); i++)
    {
      Element current = (Element) connections.get(i);
      Territory t1 = getTerritory(current, "t1", true);
      Territory t2 = getTerritory(current, "t2", true);
      map.addConnection(t1, t2);
    }
  }

  private void parseResources(Node root)
  {
    List resourceList = getChildren("resource", root);

    for(int i = 0; i < resourceList.size(); i++)
    {
      Element resource = (Element) resourceList.get(i);
      data.getResourceList().addResource(new Resource(resource.getAttribute("name"), data));
    }
  }


  private void parseUnits(Node root)
  {
    List unitList = getChildren("unit", root);

    for(int i = 0; i < unitList.size(); i++)
    {
      Element unit = (Element) unitList.get(i);
      data.getUnitTypeList().addUnitType(new UnitType(unit.getAttribute("name"), data));
    }
  }

  private void parsePlayers(Node root) throws GameParseException
  {
    parsePlayerList(root);
    parseAlliances(root);
  }

  private void parsePlayerList(Node root)
  {
    List playerElements = getChildren("player", root);
    PlayerList playerList = data.getPlayerList();

    for(int i = 0; i < playerElements.size(); i++)
    {
      Element current = (Element) playerElements.get(i);
      String name = current.getAttribute("name");
      boolean isOptional = Boolean.getBoolean(current.getAttribute("optional"));
      PlayerID newPlayer = new PlayerID(name, isOptional, data);
      playerList.addPlayerID(newPlayer);
    }
  }

  private void parseAlliances(Node root) throws GameParseException
  {
    List alliances = getChildren("alliance", root);
    AllianceTracker tracker = data.getAllianceTracker();

    for(int i = 0; i < alliances.size(); i++)
    {
      Element current = (Element) alliances.get(i);

      PlayerID p1 = getPlayerID(current, "player", true);
      String alliance = current.getAttribute("alliance");
      tracker.addToAlliance(p1,alliance);
    }
  }

  private void parseGamePlay(Node root) throws GameParseException
  {
    parseDelegates(getChildren("delegate", root));
    parseSequence(getSingleChild("sequence", root));
  }

  private void pareseProperties(Node root) throws GameParseException
  {
    GameProperties properties = data.getProperties();
    Iterator children = getChildren("property", root).iterator();
    while(children.hasNext())
    {

        Element current = (Element) children.next();
        String editable = current.getAttribute("editable");
        String property = current.getAttribute("name");
        String value = current.getAttribute("value");

        if(editable != null && editable.equalsIgnoreCase("true"))
            parseEditableProperty(current, property, value);
        else
        {
            List children2 = getNonTextNodes(current);
            if(children2.size() == 0)
                properties.set(property,value);
            else
            {
                String type = ((Element) children2.get(0)).getNodeName();
                if(type.equals("boolean"))
                {
                    properties.set(property, Boolean.valueOf(value));
                }
                else
                {
                    properties.set(property,value);
                }
                  
                 
                   
            }
        }
    }
  }

  private void parseEditableProperty(Element property, String name, String defaultValue) throws GameParseException
  {
    //what type
    List children = getNonTextNodes(property);
    if(children.size() != 1)
      throw new GameParseException("Editable properties must have exactly 1 child specifying the type. Number of children found:" + children.size() + " for node:" + property.getNodeName());

    Element child = (Element) children.get(0);
    String childName = child.getNodeName();

    IEditableProperty editableProperty;

    if(childName.equals("boolean"))
    {
      editableProperty = new BooleanProperty(name, Boolean.valueOf(defaultValue).booleanValue());
    }
    else if(childName.equals("list"))
    {
      StringTokenizer tokenizer = new StringTokenizer(child.getAttribute("values"), ",");
      Collection values = new ArrayList();
      while(tokenizer.hasMoreElements())
        values.add(tokenizer.nextElement());
      editableProperty = new ListProperty(name, defaultValue, values);
    }
    else if(childName.equals("number"))
    {
      int max = Integer.valueOf(child.getAttribute("max")).intValue();
      int min = Integer.valueOf(child.getAttribute("min")).intValue();
      int def = Integer.valueOf(defaultValue).intValue();

      editableProperty = new NumberProperty(name, max, min, def);

    }
    else if(childName.equals("string"))
    {
      editableProperty = new StringProperty(name, defaultValue);
    }
    else
    {
      throw new IllegalStateException("Unrecognized property type:" + childName);
    }
    data.getProperties().addEditableProperty(editableProperty);
  }

  private void parseDelegates(List delegateList) throws GameParseException
  {
    DelegateList delegates = data.getDelegateList();

    for(int i = 0; i < delegateList.size(); i++)
    {
      Element current = (Element) delegateList.get(i);
      //load the class
      String className = current.getAttribute("javaClass");
      IDelegate delegate = null;

      try
      {
        delegate = (IDelegate) getInstance(className);
      } catch(ClassCastException cce)
      {
        throw new GameParseException("Class <" + className + "> is not a delegate.");
      }

      String name = current.getAttribute("name");
      String displayName = current.getAttribute("display");

      if (displayName == null)
        displayName = name;

      delegate.initialize(name, displayName);
      delegates.addDelegate(delegate);
    }
  }

  private void parseSequence(Node sequence) throws GameParseException
  {
    parseSteps(getChildren("step", sequence));
  }

  private void parseSteps(List stepList) throws GameParseException
  {
    for(int i = 0; i < stepList.size(); i++)
    {
      Element current = (Element) stepList.get(i);
      
      IDelegate delegate = getDelegate(current, "delegate", true);
      PlayerID player = getPlayerID(current, "player", false);
      String name = current.getAttribute("name");
      String displayName = null;

      List propertyElements = getChildren("stepProperty", current);
      Properties stepProperties = pareStepProperties(propertyElements);
      
      if(current.hasAttribute("display"))
        displayName = current.getAttribute("display");

      GameStep step = new GameStep(name, displayName, player, delegate, data, stepProperties);

      if(current.hasAttribute("maxRunCount"))
      {
        int runCount = Integer.parseInt(current.getAttribute("maxRunCount"));
        if(runCount <= 0)
          throw new GameParseException("maxRunCount must be positive");
        step.setMaxRunCount(runCount);
      }

      data.getSequence().addStep(step);
    }
  }

  private Properties pareStepProperties(List properties)
  {
     Properties rVal = new Properties();
     Iterator iter = properties.iterator();
     while (iter.hasNext())
     {
        Element stepProperty = (Element) iter.next();
        String name = stepProperty.getAttribute("name");
        String value = stepProperty.getAttribute("value");
        rVal.setProperty(name, value);
     }
     return rVal;
  }
  
  private void parseProduction(Node root) throws GameParseException
  {
    parseProductionRules( getChildren("productionRule", root));
    parseProductionFrontiers( getChildren("productionFrontier", root));
    parsePlayerProduction( getChildren("playerProduction", root));
  }

  private void parseProductionRules(List elements) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      String name = current.getAttribute("name");
      ProductionRule rule = new ProductionRule(name, data);
      parseCosts(rule, getChildren("cost", current));
      parseResults(rule, getChildren("result", current));
      data.getProductionRuleList().addProductionRule(rule);
    }
  }

  private void parseCosts(ProductionRule rule, List elements) throws GameParseException
  {
    if(elements.size() == 0)
      throw new GameParseException("no costs  for rule:" + rule.getName());

    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      Resource resource = getResource(current, "resource", true);
      int quantity = Integer.parseInt(current.getAttribute("quantity"));
      rule.addCost(resource, quantity);
    }
  }

  private void parseResults(ProductionRule rule, List elements) throws GameParseException
  {
    if(elements.size() == 0)
      throw new GameParseException("no results  for rule:" + rule.getName());

    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      //must find either a resource or a unit with the given name
      Object result = null;
      result = getResource(current, "resourceOrUnit", false);
      if(result == null)
        result = getUnitType(current, "resourceOrUnit", false);
      if(result == null)
        throw new GameParseException("Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
      int quantity = Integer.parseInt(current.getAttribute("quantity"));
      rule.addResult(result, quantity);
    }
  }

  private void parseProductionFrontiers(List elements) throws GameParseException
  {
    ProductionFrontierList frontiers = data.getProductionFrontierList();

    for( int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);
      String name = current.getAttribute("name");
      ProductionFrontier frontier = new ProductionFrontier(name, data);
      parseFrontierRules( getChildren("frontierRules", current), frontier);
      frontiers.addProductionFrontier(frontier);
    }
  }

  private void parsePlayerProduction(List elements) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);
      PlayerID player = getPlayerID(current, "player", true);
      ProductionFrontier frontier = getProductionFrontier(current, "frontier", true);
      player.setProductionFrontier(frontier);
    }
  }

  private void parseFrontierRules(List elements, ProductionFrontier frontier) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);
      ProductionRule rule = getProductionRule(current, "name", true);
      frontier.addRule(rule);
    }
  }

  private void parseAttatchments(Node root) throws GameParseException
  {
    List elements = getChildren("attatchment", root);

    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      //create the attatchment
      String className = current.getAttribute("javaClass");
      Object obj = getInstance(className);
      if(!(obj instanceof Attatchment))
        throw new IllegalStateException(className + " does not implement Attatchable");

      Attatchment attatchment = (Attatchment) obj;
      attatchment.setData(data);
      //set the values
      List values = getChildren("option", current);
      setValues(attatchment, values);
      attatchment.validate();

      //find the attatchable
      String type = current.getAttribute("type");
      Attatchable attatchable = findAttatchment(current, type);

      //attatch
      String name = current.getAttribute("name");
      attatchable.addAttatchment(name, attatchment);
      attatchment.setAttatchedTo(attatchable);
      attatchment.setName(name);
    }
  }

  private Attatchable findAttatchment(Element element, String type) throws GameParseException
  {
    Attatchable returnVal;
    final String name = "attatchTo";

    if(type.equals("unitType"))
    {
      returnVal = getUnitType(element, name, true);
    } else if(type.equals("territory"))
    {
      returnVal = getTerritory(element, name, true);
    } else if(type.equals("resource"))
    {
      returnVal = getResource(element, name, true);
    } else if(type.equals("player"))
    {
      returnVal = getPlayerID(element, name, true);
    } else
    {
      throw new GameParseException("Type not found to attach to:" + type);
    }
    return returnVal;
  }

  private String capitalizeFirstLetter(String aString)
  {
    char first = aString.charAt(0);
    first = Character.toUpperCase(first);
    return first + aString.substring(1);
  }

  private void setValues(Object obj, List values) throws GameParseException
  {
    for(int i = 0; i < values.size() ; i++)
    {
      Element current = (Element) values.get(i);

      //find the setter
      String name = null;
      Method setter = null;
      try
      {
        name = current.getAttribute("name");
        if(name.length() == 0)
          throw new GameParseException("option name with 0 length");
        name = "set" + capitalizeFirstLetter(name);
        setter = obj.getClass().getMethod(name, SETTER_ARGS);
      } catch(NoSuchMethodException nsme)
      {
        throw new GameParseException("No setter for attatchment option. Setter:" + name + " Class:" + obj.getClass().getName());
      }

      //find the value
      String value = current.getAttribute("value");

      //invoke
      try
      {
        Object[] args = {value};
        setter.invoke(obj, args );
      } catch(IllegalAccessException iae)
      {
        throw new GameParseException("Setter not public. Setter:" + name + " Class:" + obj.getClass().getName());
      } catch(InvocationTargetException ite)
      {
        throw new GameParseException("Invocation Exception. Message:" + ite.getMessage());
      }
    }
  }

  private void parseInitialization(Node root) throws GameParseException
  {
    //parse territory owners
    Node owner = getSingleChild("ownerInitialize", root, true);
    if(owner != null)
      parseOwner( getChildren("territoryOwner", owner));

    //parse initial unit placement
    Node unit = getSingleChild("unitInitialize", root, true);
    if(unit  != null)
    {
      parseUnitPlacement( getChildren("unitPlacement", unit));
      parseHeldUnits( getChildren("heldUnits", unit));
    }

    //parse resources given
    Node resource = getSingleChild("resourceInitialize", root, true);
    if(resource != null)
      parseResourceInitialization(getChildren("resourceGiven", resource));
  }

  private void parseOwner(List elements) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      Territory territory = getTerritory(current, "territory", true);
      PlayerID owner = getPlayerID(current, "owner", true);
      territory.setOwner(owner);
    }
  }

  private void parseUnitPlacement(List elements) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      Territory territory = getTerritory(current, "territory", true);
      UnitType type = getUnitType(current, "unitType", true);
      String ownerString = current.getAttribute("owner");
      PlayerID owner;
      if(ownerString == null || ownerString.trim().length() == 0)
        owner = PlayerID.NULL_PLAYERID;
      else
        owner = getPlayerID(current, "owner", false);
      int quantity = Integer.parseInt(current.getAttribute("quantity"));
      territory.getUnits().addAllUnits(type.create(quantity, owner));
    }
  }

  private void parseHeldUnits(List elements) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      PlayerID player = getPlayerID(current, "player", true);
      UnitType type = getUnitType(current, "unitType", true);
      int quantity = Integer.parseInt(current.getAttribute("quantity"));
      player.getUnits().addAllUnits(type.create(quantity, player));
    }
  }

  private void parseResourceInitialization(List elements) throws GameParseException
  {
    for(int i = 0; i < elements.size(); i++)
    {
      Element current = (Element) elements.get(i);

      PlayerID player = getPlayerID(current, "player", true);
      Resource resource = getResource(current, "resource", true);
      int quantity = Integer.parseInt(current.getAttribute("quantity"));
      player.getResources().addResource(resource, quantity);
    }
  }
}
