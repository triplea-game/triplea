/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * Parser.java
 * 
 * Created on October 12, 2001, 12:55 PM
 */
package games.strategy.engine.data;

import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.FileProperty;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.ListProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.Version;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * 
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 */
public class GameParser
{
	private static final Class<?>[] SETTER_ARGS = { String.class };
	private GameData data;
	
	public GameParser()
	{
	}
	
	public synchronized GameData parse(final InputStream stream) throws GameParseException, SAXException
	{
		if (stream == null)
			throw new IllegalArgumentException("Stream must be non null");
		Document doc = null;
		try
		{
			doc = getDocument(stream);
		} catch (final IOException e)
		{
			throw new IllegalStateException(e);
		} catch (final ParserConfigurationException e)
		{
			throw new IllegalStateException(e);
		}
		final Node root = doc.getDocumentElement();
		data = new GameData();
		// mandatory fields
		parseInfo(getSingleChild("info", root));
		parseGameLoader(getSingleChild("loader", root));
		final Node diceSides = getSingleChild("diceSides", root, true);
		if (diceSides != null)
			parseDiceSides(diceSides);
		else
			data.setDiceSides(6);
		parseMap(getSingleChild("map", root));
		final Node resourceList = getSingleChild("resourceList", root, true);
		if (resourceList != null)
			parseResources(resourceList);
		// Parse all different relationshipTypes that are defined in the xml, for example: War, Allied, Neutral, NAP
		final Node relationshipTypes = getSingleChild("relationshipTypes", root, true);
		if (relationshipTypes != null)
			parseRelationshipTypes(relationshipTypes);
		final Node territoryEffectList = getSingleChild("territoryEffectList", root, true);
		if (territoryEffectList != null)
			parseTerritoryEffects(territoryEffectList);
		final Node playerListNode = getSingleChild("playerList", root);
		parsePlayerList(playerListNode);
		parseAlliances(playerListNode);
		parseGamePlay(getSingleChild("gamePlay", root));
		// optional
		final Node unitList = getSingleChild("unitList", root, true);
		if (unitList != null)
			parseUnits(unitList);
		final Node production = getSingleChild("production", root, true);
		if (production != null)
			parseProduction(production);
		final Node technology = getSingleChild("technology", root, true);
		if (technology != null)
			parseTechnology(technology);
		final Node attachmentList = getSingleChild("attatchmentList", root, true);
		if (attachmentList != null)
			parseAttachments(attachmentList);
		final Node initialization = getSingleChild("initialize", root, true);
		if (initialization != null)
			parseInitialization(initialization);
		final Node properties = getSingleChild("propertyList", root, true);
		if (properties != null)
			parseProperties(properties);
		// set & override default relationships
		data.getRelationshipTracker().setNullPlayerRelations(); // sets the relationship between all players and the NullPlayer to NullRelation (with archeType War)
		data.getRelationshipTracker().setSelfRelations(); // sets the relationship for all players with themselfs to the SelfRelation (with archeType Allied)
		validate();
		return data;
	}
	
	private void parseDiceSides(final Node diceSides)
	{
		data.setDiceSides(Integer.parseInt(((Element) diceSides).getAttribute("value")));
	}
	
	private void validate() throws GameParseException
	{
		// validate unit attachments
		for (final UnitType u : data.getUnitTypeList())
		{
			validateAttachments(u);
		}
		for (final Territory t : data.getMap())
		{
			validateAttachments(t);
		}
		for (final Resource r : data.getResourceList().getResources())
		{
			validateAttachments(r);
		}
		for (final PlayerID r : data.getPlayerList().getPlayers())
		{
			validateAttachments(r);
		}
		for (final RelationshipType r : data.getRelationshipTypeList().getAllRelationshipTypes())
		{
			validateAttachments(r);
		}
		for (final TerritoryEffect r : data.getTerritoryEffectList().values())
		{
			validateAttachments(r);
		}
		// if relationships are used, every player should have a relationship with every other player
		validateRelationships();
	}
	
	private void validateRelationships() throws GameParseException
	{
		// for every player
		for (final PlayerID player : data.getPlayerList())
		{
			// in relation to every player
			for (final PlayerID player2 : data.getPlayerList())
			{
				// See if there is a relationship between them
				if ((data.getRelationshipTracker().getRelationshipType(player, player2) == null))
					throw new GameParseException("No relation set for: " + player.getName() + " and " + player2.getName());
				// or else throw an exception!
			}
		}
	}
	
	private void validateAttachments(final Attachable attachable) throws GameParseException
	{
		for (final IAttachment a : attachable.getAttachments().values())
		{
			a.validate(data);
		}
	}
	
	private Document getDocument(final InputStream input) throws SAXException, IOException, ParserConfigurationException
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(true);
		// get the dtd location
		final URL url = GameParser.class.getResource("/games/strategy/engine/xml/");
		final String system = url.toExternalForm();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(input, system);
	}
	
	/**
	 * If mustfind is true and cannot find the player an exception will be thrown.
	 */
	private PlayerID getPlayerID(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final PlayerID player = data.getPlayerList().getPlayerID(name);
		if (player == null && mustFind)
			throw new GameParseException("Could not find player. name:" + name);
		return player;
	}
	
	/**
	 * If mustfind is true and cannot find the player an exception will be thrown.
	 * 
	 * @return a RelationshipType from the relationshipTypeList, at this point all relationshipTypes should have been declared
	 * @throws GameParseException
	 *             when
	 */
	private RelationshipType getRelationshipType(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final RelationshipType relation = data.getRelationshipTypeList().getRelationshipType(name);
		if (relation == null && mustFind)
			throw new GameParseException("Could not find relation name:" + name);
		return relation;
	}
	
	private TerritoryEffect getTerritoryEffect(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final TerritoryEffect effect = data.getTerritoryEffectList().get(name);
		if (effect == null && mustFind)
			throw new GameParseException("Could not find territoryEffect name:" + name);
		return effect;
	}
	
	/**
	 * If mustfind is true and cannot find the productionRule an exception will be thrown.
	 */
	private ProductionRule getProductionRule(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final ProductionRule productionRule = data.getProductionRuleList().getProductionRule(name);
		if (productionRule == null && mustFind)
			throw new GameParseException("Could not find production rule. name:" + name);
		return productionRule;
	}
	
	/**
	 * If mustfind is true and cannot find the productionRule an exception will be thrown.
	 */
	private RepairRule getRepairRule(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final RepairRule repairRule = data.getRepairRuleList().getRepairRule(name);
		if (repairRule == null && mustFind)
			throw new GameParseException("Could not find production rule. name:" + name);
		return repairRule;
	}
	
	/**
	 * If mustfind is true and cannot find the territory an exception will be thrown.
	 */
	private Territory getTerritory(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final Territory territory = data.getMap().getTerritory(name);
		if (territory == null && mustFind)
			throw new GameParseException("Could not find territory. name:" + name);
		return territory;
	}
	
	/**
	 * If mustfind is true and cannot find the unitType an exception will be thrown.
	 */
	private UnitType getUnitType(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final UnitType type = data.getUnitTypeList().getUnitType(name);
		if (type == null && mustFind)
			throw new GameParseException("Could not find unitType. name:" + name);
		return type;
	}
	
	/**
	 * If mustfind is true and cannot find the Delegate an exception will be thrown.
	 */
	private IDelegate getDelegate(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final IDelegate delegate = data.getDelegateList().getDelegate(name);
		if (delegate == null && mustFind)
			throw new GameParseException("Could not find delegate. name:" + name);
		return delegate;
	}
	
	/**
	 * If mustfind is true and cannot find the Resource an exception will be thrown.
	 */
	private Resource getResource(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final Resource resource = data.getResourceList().getResource(name);
		if (resource == null && mustFind)
			throw new GameParseException("Could not find resource. name:" + name);
		return resource;
	}
	
	/**
	 * If mustfind is true and cannot find the productionRule an exception will be thrown.
	 */
	private ProductionFrontier getProductionFrontier(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final ProductionFrontier productionFrontier = data.getProductionFrontierList().getProductionFrontier(name);
		if (productionFrontier == null && mustFind)
			throw new GameParseException("Could not find production frontier. name:" + name);
		return productionFrontier;
	}
	
	/**
	 * If mustfind is true and cannot find the productionRule an exception will be thrown.
	 */
	private RepairFrontier getRepairFrontier(final Element element, final String attribute, final boolean mustFind) throws GameParseException
	{
		final String name = element.getAttribute(attribute);
		final RepairFrontier repairFrontier = data.getRepairFrontierList().getRepairFrontier(name);
		if (repairFrontier == null && mustFind)
			throw new GameParseException("Could not find production frontier. name:" + name);
		return repairFrontier;
	}
	
	/**
	 * Loads an instance of the given class.
	 * Assumes a zero argument constructor.
	 */
	private Object getInstance(final String className) throws GameParseException
	{
		Object instance = null;
		try
		{
			final Class<?> instanceClass = Class.forName(className);
			instance = instanceClass.newInstance();
		}
		// a lot can go wrong, the following list is just a subset of potential pitfalls
		catch (final ClassNotFoundException cnfe)
		{
			throw new GameParseException("Class <" + className + "> could not be found.");
		} catch (final InstantiationException ie)
		{
			throw new GameParseException("Class <" + className + "> could not be instantiated. ->" + ie.getMessage());
		} catch (final IllegalAccessException iae)
		{
			throw new GameParseException("Constructor could not be accessed ->" + iae.getMessage());
		}
		return instance;
	}
	
	/**
	 * Get the given child.
	 * If there is not exactly one child throw a SAXExcpetion
	 */
	private Node getSingleChild(final String name, final Node node) throws GameParseException
	{
		return getSingleChild(name, node, false);
	}
	
	/**
	 * If optional is true, will not throw an exception if there are 0 children
	 */
	private Node getSingleChild(final String name, final Node node, final boolean optional) throws GameParseException
	{
		final List<Node> children = getChildren(name, node);
		// none found
		if (children.size() == 0)
		{
			if (optional)
				return null;
			throw new GameParseException("No child called " + name);
		}
		// too many found
		if (children.size() > 1)
		{
			throw new GameParseException("Too many children named " + name);
		}
		return children.get(0);
	}
	
	private List<Node> getChildren(final String name, final Node node)
	{
		final ArrayList<Node> found = new ArrayList<Node>();
		final NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node current = children.item(i);
			if (current.getNodeName().equals(name))
				found.add(current);
		}
		return found;
	}
	
	private List<Node> getNonTextNodesIgnoring(final Node node, final String ignore)
	{
		final List<Node> rVal = getNonTextNodes(node);
		final Iterator<Node> iter = rVal.iterator();
		while (iter.hasNext())
		{
			if (((Element) iter.next()).getTagName().equals(ignore))
			{
				iter.remove();
			}
		}
		return rVal;
	}
	
	private List<Node> getNonTextNodes(final Node node)
	{
		final ArrayList<Node> found = new ArrayList<Node>();
		final NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node current = children.item(i);
			if (!(current.getNodeType() == Node.TEXT_NODE))
				found.add(current);
		}
		return found;
	}
	
	private void parseInfo(final Node info)
	{
		final String gameName = ((Element) info).getAttribute("name");
		data.setGameName(gameName);
		final String version = ((Element) info).getAttribute("version");
		data.setGameVersion(new Version(version));
	}
	
	private void parseGameLoader(final Node loader) throws GameParseException
	{
		final String className = ((Element) loader).getAttribute("javaClass");
		final Object instance = getInstance(className);
		if (!(instance instanceof IGameLoader))
		{
			throw new GameParseException("Loader must implement IGameLoader.  Class Name:" + className);
		}
		data.setGameLoader((IGameLoader) instance);
	}
	
	private void parseMap(final Node map) throws GameParseException
	{
		final List<Node> grids = getChildren("grid", map);
		parseGrids(grids);
		// get the Territories
		final List<Node> territories = getChildren("territory", map);
		parseTerritories(territories);
		final List<Node> connections = getChildren("connection", map);
		parseConnections(connections);
	}
	
	private void parseGrids(final List<Node> grids) throws GameParseException
	{
		final GameMap map = data.getMap();
		final Iterator<Node> iter = grids.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final List<Node> waterNodes = getChildren("water", current);
			final Set<String> water = parseGridWater(waterNodes);
			final String horizontalConnections = current.getAttribute("horizontal-connections");
			boolean horizontalConnectionsImplict;
			if (horizontalConnections.equals("implicit"))
				horizontalConnectionsImplict = true;
			else if (horizontalConnections.equals("explicit"))
				horizontalConnectionsImplict = false;
			else
				throw new GameParseException("horizontal-connections attribute must be either \"explicit\" or \"implicit\"");
			final String verticalConnections = current.getAttribute("vertical-connections");
			boolean verticalConnectionsImplict;
			if (verticalConnections.equals("implicit"))
				verticalConnectionsImplict = true;
			else if (verticalConnections.equals("explicit"))
				verticalConnectionsImplict = false;
			else
				throw new GameParseException("vertical-connections attribute must be either \"explicit\" or \"implicit\"");
			final String diagonalConnections = current.getAttribute("diagonal-connections");
			boolean diagonalConnectionsImplict;
			if (diagonalConnections.equals("implicit"))
				diagonalConnectionsImplict = true;
			else if (diagonalConnections.equals("explicit"))
				diagonalConnectionsImplict = false;
			else
				throw new GameParseException("diagonal-connections attribute must be either \"explicit\" or \"implicit\"");
			final String gridType = current.getAttribute("type");
			final String name = current.getAttribute("name");
			final String xs = current.getAttribute("x");
			final String ys = current.getAttribute("y");
			final int x_size = Integer.valueOf(xs);
			int y_size;
			if (ys != null)
				y_size = Integer.valueOf(ys);
			else
				y_size = 0;
			map.setGridDimensions(x_size, y_size);
			if (gridType.equals("square"))
			{
				// Add territories
				for (int y = 0; y < y_size; y++)
				{
					for (int x = 0; x < x_size; x++)
					{
						boolean isWater;
						if (water.contains(x + "-" + y))
							isWater = true;
						else
							isWater = false;
						final Territory newTerritory = new Territory(name + "_" + x + "_" + y, isWater, data, x, y);
						map.addTerritory(newTerritory);
					}
				}
				// Add any implicit horizontal connections
				if (horizontalConnectionsImplict)
					for (int y = 0; y < y_size; y++)
						for (int x = 0; x < x_size - 1; x++)
							map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x + 1, y));
				// Add any implicit vertical connections
				if (verticalConnectionsImplict)
					for (int x = 0; x < x_size; x++)
						for (int y = 0; y < y_size - 1; y++)
							map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x, y + 1));
				// Add any implicit acute diagonal connections
				if (diagonalConnectionsImplict)
					for (int y = 0; y < y_size - 1; y++)
						for (int x = 0; x < x_size - 1; x++)
							map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x + 1, y + 1));
				// Add any implicit obtuse diagonal connections
				if (diagonalConnectionsImplict)
					for (int y = 0; y < y_size - 1; y++)
						for (int x = 1; x < x_size; x++)
							map.addConnection(map.getTerritoryFromCoordinates(x, y), map.getTerritoryFromCoordinates(x - 1, y + 1));
			}
			else if (gridType.equals("points-and-lines"))
			{ // This type is a triangular grid of points and lines,
				// used for in several rail games
				// Add territories
				for (int y = 0; y < y_size; y++)
				{
					for (int x = 0; x < x_size; x++)
					{
						final boolean isWater = false;
						if (!water.contains(x + "-" + y))
						{
							final Territory newTerritory = new Territory(name + "_" + x + "_" + y, isWater, data, x, y);
							map.addTerritory(newTerritory);
						}
					}
				}
				// Add any implicit horizontal connections
				if (horizontalConnectionsImplict)
					for (int y = 0; y < y_size; y++)
						for (int x = 0; x < x_size - 1; x++)
						{
							final Territory from = map.getTerritoryFromCoordinates(x, y);
							final Territory to = map.getTerritoryFromCoordinates(x + 1, y);
							if (from != null && to != null)
								map.addConnection(from, to);
						}
				// Add any implicit acute diagonal connections
				if (diagonalConnectionsImplict)
					for (int y = 1; y < y_size; y++)
						for (int x = 0; x < x_size - 1; x++)
							if (y % 4 == 0 || (y + 1) % 4 == 0)
							{
								final Territory from = map.getTerritoryFromCoordinates(x, y);
								final Territory to = map.getTerritoryFromCoordinates(x, y - 1);
								if (from != null && to != null)
									map.addConnection(from, to);
							}
							else
							{
								final Territory from = map.getTerritoryFromCoordinates(x, y);
								final Territory to = map.getTerritoryFromCoordinates(x + 1, y - 1);
								if (from != null && to != null)
									map.addConnection(from, to);
							}
				// Add any implicit obtuse diagonal connections
				if (diagonalConnectionsImplict)
					for (int y = 1; y < y_size; y++)
						for (int x = 0; x < x_size - 1; x++)
							if (y % 4 == 0 || (y + 1) % 4 == 0)
							{
								final Territory from = map.getTerritoryFromCoordinates(x, y);
								final Territory to = map.getTerritoryFromCoordinates(x - 1, y - 1);
								if (from != null && to != null)
									map.addConnection(from, to);
							}
							else
							{
								final Territory from = map.getTerritoryFromCoordinates(x, y);
								final Territory to = map.getTerritoryFromCoordinates(x, y - 1);
								if (from != null && to != null)
									map.addConnection(from, to);
							}
			}
		}
	}
	
	private Set<String> parseGridWater(final List<Node> waterNodes)
	{
		final Set<String> set = new HashSet<String>();
		final Iterator<Node> iter = waterNodes.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final int x = Integer.valueOf(current.getAttribute("x"));
			final int y = Integer.valueOf(current.getAttribute("y"));
			set.add(x + "-" + y);
		}
		return set;
	}
	
	private void parseTerritories(final List<Node> territories)
	{
		final GameMap map = data.getMap();
		final Iterator<Node> iter = territories.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final boolean water = current.getAttribute("water").trim().equalsIgnoreCase("true");
			final String name = current.getAttribute("name");
			final Territory newTerritory = new Territory(name, water, data);
			map.addTerritory(newTerritory);
		}
	}
	
	private void parseConnections(final List<Node> connections) throws GameParseException
	{
		final GameMap map = data.getMap();
		final Iterator<Node> iter = connections.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final Territory t1 = getTerritory(current, "t1", true);
			final Territory t2 = getTerritory(current, "t2", true);
			map.addConnection(t1, t2);
		}
	}
	
	private void parseResources(final Node root)
	{
		final Iterator<Node> iter = getChildren("resource", root).iterator();
		while (iter.hasNext())
		{
			data.getResourceList().addResource(new Resource(((Element) iter.next()).getAttribute("name"), data));
		}
	}
	
	private void parseRelationshipTypes(final Node root)
	{
		final Iterator<Node> iter = getChildren("relationshipType", root).iterator();
		while (iter.hasNext())
		{
			data.getRelationshipTypeList().addRelationshipType(new RelationshipType(((Element) iter.next()).getAttribute("name"), data));
		}
	}
	
	private void parseTerritoryEffects(final Node root)
	{
		final Iterator<Node> iter = getChildren("territoryEffect", root).iterator();
		while (iter.hasNext())
		{
			final String name = ((Element) iter.next()).getAttribute("name");
			data.getTerritoryEffectList().put(name, new TerritoryEffect(name, data));
		}
	}
	
	private void parseUnits(final Node root)
	{
		final Iterator<Node> iter = getChildren("unit", root).iterator();
		while (iter.hasNext())
		{
			data.getUnitTypeList().addUnitType(new UnitType(((Element) iter.next()).getAttribute("name"), data));
		}
	}
	
	/**
	 * @param root
	 *            root node containing the playerList
	 * @throws GameParseException
	 */
	private void parsePlayerList(final Node root)
	{
		final PlayerList playerList = data.getPlayerList();
		final Iterator<Node> iter = getChildren("player", root).iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final String name = current.getAttribute("name");
			// It appears the commented line ALWAYS returns false regardless of the value of current.getAttribute("optional")
			// boolean isOptional = Boolean.getBoolean(current.getAttribute("optional"));
			final boolean isOptional = current.getAttribute("optional").equals("true");
			final PlayerID newPlayer = new PlayerID(name, isOptional, data);
			playerList.addPlayerID(newPlayer);
		}
	}
	
	private void parseAlliances(final Node root) throws GameParseException
	{
		final AllianceTracker allianceTracker = data.getAllianceTracker();
		final Iterator<Node> iter = getChildren("alliance", root).iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final PlayerID p1 = getPlayerID(current, "player", true);
			final String alliance = current.getAttribute("alliance");
			allianceTracker.addToAlliance(p1, alliance);
		}
		// if relationships aren't initialized based on relationshipInitialize we use the alliances to set the relationships
		if (getSingleChild("relationshipInitialize", root, true) == null)
		{
			final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
			final RelationshipTypeList relationshipTypeList = data.getRelationshipTypeList();
			final Iterator<PlayerID> iterPlayers = data.getPlayerList().getPlayers().iterator();
			// iterate through all players to get known allies and enemies
			while (iterPlayers.hasNext())
			{
				final PlayerID currentPlayer = iterPlayers.next();
				final HashSet<PlayerID> enemies = new HashSet<PlayerID>(data.getPlayerList().getPlayers()); // start with all players as enemies
				final HashSet<PlayerID> allies = new HashSet<PlayerID>(); // start with no players as allies
				// iterate through all alliances the player is in
				if (allianceTracker.getAlliancesMap().get(currentPlayer) != null)
				{
					final Iterator<String> iterAlliances = allianceTracker.getAlliancesMap().get(currentPlayer).iterator();
					while (iterAlliances.hasNext())
					{
						// iterate through the members of the alliances
						final Iterator<PlayerID> iterMembers = allianceTracker.getPlayersInAlliance(iterAlliances.next()).iterator();
						while (iterMembers.hasNext())
						{
							final PlayerID currentMember = iterMembers.next();
							allies.add(currentMember); // add each allianceMember to the alliesList
							enemies.remove(currentMember); // remove each allianceMember from the enemiesList
						}
					}
				}
				enemies.remove(currentPlayer); // remove self from enemieslist (in case of free-for-all)
				allies.remove(currentPlayer); // remove self from allieslist (in case you are a member of an alliance)
				// At this point enemies and allies should be set for this player.
				final Iterator<PlayerID> iterAllies = allies.iterator();
				while (iterAllies.hasNext())
				{
					relationshipTracker.setRelationship(currentPlayer, iterAllies.next(), relationshipTypeList.getDefaultAlliedRelationship());
				}
				final Iterator<PlayerID> iterEnemies = enemies.iterator();
				while (iterEnemies.hasNext())
				{
					relationshipTracker.setRelationship(currentPlayer, iterEnemies.next(), relationshipTypeList.getDefaultWarRelationship());
				}
			}
		}
	}
	
	private void parseRelationInitialize(final List<Node> relations) throws GameParseException
	{
		if (relations.size() > 0)
		{
			final RelationshipTracker tracker = data.getRelationshipTracker();
			final Iterator<Node> iter = relations.iterator();
			while (iter.hasNext())
			{
				final Element current = (Element) iter.next();
				final PlayerID p1 = getPlayerID(current, "player1", true);
				final PlayerID p2 = getPlayerID(current, "player2", true);
				final RelationshipType r = getRelationshipType(current, "type", true);
				final int roundValue = Integer.valueOf(current.getAttribute("roundValue"));
				tracker.setRelationship(p1, p2, r, roundValue);
			}
		}
	}
	
	private void parseGamePlay(final Node root) throws GameParseException
	{
		parseDelegates(getChildren("delegate", root));
		parseSequence(getSingleChild("sequence", root));
	}
	
	private void parseProperties(final Node root) throws GameParseException
	{
		final Collection<String> runningList = new ArrayList<String>();
		final GameProperties properties = data.getProperties();
		final Iterator<Node> children = getChildren("property", root).iterator();
		while (children.hasNext())
		{
			final Element current = (Element) children.next();
			final String editable = current.getAttribute("editable");
			final String property = current.getAttribute("name");
			String value = current.getAttribute("value");
			runningList.add(property);
			if (value == null || value.length() == 0)
			{
				final List<Node> valueChildren = getChildren("value", current);
				if (!valueChildren.isEmpty())
				{
					final Element valueNode = (Element) valueChildren.get(0);
					if (valueNode != null)
					{
						value = valueNode.getTextContent();
					}
				}
			}
			if (editable != null && editable.equalsIgnoreCase("true"))
				parseEditableProperty(current, property, value);
			else
			{
				final List<Node> children2 = getNonTextNodesIgnoring(current, "value");
				if (children2.size() == 0)
				{
					// we don't know what type this property is!!, it appears like only numbers and string may be represented without proper type definition
					
					try
					{
						// test if it is an integer
						final Integer integer = Integer.parseInt(value);
						int intValue = 0;
						if (integer != null)
						{
							intValue = integer;
						}
						properties.set(property, intValue);
					} catch (final NumberFormatException e)
					{
						// then it must be a string
						properties.set(property, value);
					}
					
				}
				else
				{
					final String type = children2.get(0).getNodeName();
					if (type.equals("boolean"))
					{
						properties.set(property, Boolean.valueOf(value));
					}
					else if (type.equals("file"))
					{
						properties.set(property, new File(value));
					}
					else if (type.equals("number"))
					{
						int intValue = 0;
						if (value != null)
						{
							try
							{
								intValue = Integer.parseInt(value);
							} catch (final NumberFormatException e)
							{
								// value already 0
							}
						}
						properties.set(property, intValue);
					}
					
					else
					{
						properties.set(property, value);
					}
				}
			}
		}
		if (data.getGameLoader() instanceof games.strategy.triplea.TripleA)
		{
			// add properties for all triplea related maps here:
			if (!runningList.contains(Constants.AI_BONUS_INCOME_FLAT_RATE))
			{
				data.getProperties().addEditableProperty(new NumberProperty(Constants.AI_BONUS_INCOME_FLAT_RATE, 40, -20, 0));
			}
			if (!runningList.contains(Constants.AI_BONUS_INCOME_PERCENTAGE))
			{
				data.getProperties().addEditableProperty(new NumberProperty(Constants.AI_BONUS_INCOME_PERCENTAGE, 200, -100, 0));
			}
			if (!runningList.contains(Constants.AI_BONUS_ATTACK))
			{
				data.getProperties().addEditableProperty(new NumberProperty(Constants.AI_BONUS_ATTACK, data.getDiceSides(), 0, 0));
			}
			if (!runningList.contains(Constants.AI_BONUS_DEFENSE))
			{
				data.getProperties().addEditableProperty(new NumberProperty(Constants.AI_BONUS_DEFENSE, data.getDiceSides(), 0, 0));
			}
		}
	}
	
	private void parseEditableProperty(final Element property, final String name, final String defaultValue) throws GameParseException
	{
		// what type
		final List<Node> children = getNonTextNodes(property);
		if (children.size() != 1)
			throw new GameParseException("Editable properties must have exactly 1 child specifying the type. Number of children found:" + children.size() + " for node:" + property.getNodeName());
		final Element child = (Element) children.get(0);
		final String childName = child.getNodeName();
		IEditableProperty editableProperty;
		if (childName.equals("boolean"))
		{
			editableProperty = new BooleanProperty(name, Boolean.valueOf(defaultValue).booleanValue());
		}
		else if (childName.equals("file"))
		{
			editableProperty = new FileProperty(name, defaultValue);
		}
		else if (childName.equals("list"))
		{
			final StringTokenizer tokenizer = new StringTokenizer(child.getAttribute("values"), ",");
			final Collection<String> values = new ArrayList<String>();
			while (tokenizer.hasMoreElements())
				values.add(tokenizer.nextToken());
			editableProperty = new ListProperty(name, defaultValue, values);
		}
		else if (childName.equals("number"))
		{
			final int max = Integer.valueOf(child.getAttribute("max")).intValue();
			final int min = Integer.valueOf(child.getAttribute("min")).intValue();
			final int def = Integer.valueOf(defaultValue).intValue();
			editableProperty = new NumberProperty(name, max, min, def);
		}
		else if (childName.equals("color"))
		{
			// Parse the value as a hexidecimal number
			final int def = Integer.valueOf(defaultValue, 16).intValue();
			editableProperty = new ColorProperty(name, def);
		}
		else if (childName.equals("string"))
		{
			editableProperty = new StringProperty(name, defaultValue);
		}
		else
		{
			throw new IllegalStateException("Unrecognized property type:" + childName);
		}
		data.getProperties().addEditableProperty(editableProperty);
	}
	
	private void parseDelegates(final List<Node> delegateList) throws GameParseException
	{
		final DelegateList delegates = data.getDelegateList();
		final Iterator<Node> iterator = delegateList.iterator();
		while (iterator.hasNext())
		{
			final Element current = (Element) iterator.next();
			// load the class
			final String className = current.getAttribute("javaClass");
			IDelegate delegate = null;
			try
			{
				delegate = (IDelegate) getInstance(className);
			} catch (final ClassCastException cce)
			{
				throw new GameParseException("Class <" + className + "> is not a delegate.");
			}
			final String name = current.getAttribute("name");
			String displayName = current.getAttribute("display");
			if (displayName == null)
				displayName = name;
			delegate.initialize(name, displayName);
			delegates.addDelegate(delegate);
		}
	}
	
	private void parseSequence(final Node sequence) throws GameParseException
	{
		parseSteps(getChildren("step", sequence));
	}
	
	private void parseSteps(final List<Node> stepList) throws GameParseException
	{
		final Iterator<Node> iterator = stepList.iterator();
		while (iterator.hasNext())
		{
			final Element current = (Element) iterator.next();
			final IDelegate delegate = getDelegate(current, "delegate", true);
			final PlayerID player = getPlayerID(current, "player", false);
			final String name = current.getAttribute("name");
			String displayName = null;
			final List<Node> propertyElements = getChildren("stepProperty", current);
			final Properties stepProperties = pareStepProperties(propertyElements);
			if (current.hasAttribute("display"))
				displayName = current.getAttribute("display");
			final GameStep step = new GameStep(name, displayName, player, delegate, data, stepProperties);
			if (current.hasAttribute("maxRunCount"))
			{
				final int runCount = Integer.parseInt(current.getAttribute("maxRunCount"));
				if (runCount <= 0)
					throw new GameParseException("maxRunCount must be positive");
				step.setMaxRunCount(runCount);
			}
			data.getSequence().addStep(step);
		}
	}
	
	private Properties pareStepProperties(final List<Node> properties)
	{
		final Properties rVal = new Properties();
		final Iterator<Node> iter = properties.iterator();
		while (iter.hasNext())
		{
			final Element stepProperty = (Element) iter.next();
			final String name = stepProperty.getAttribute("name");
			final String value = stepProperty.getAttribute("value");
			rVal.setProperty(name, value);
		}
		return rVal;
	}
	
	private void parseProduction(final Node root) throws GameParseException
	{
		parseProductionRules(getChildren("productionRule", root));
		parseProductionFrontiers(getChildren("productionFrontier", root));
		parsePlayerProduction(getChildren("playerProduction", root));
		parseRepairRules(getChildren("repairRule", root));
		parseRepairFrontiers(getChildren("repairFrontier", root));
		parsePlayerRepair(getChildren("playerRepair", root));
	}
	
	private void parseTechnology(final Node root) throws GameParseException
	{
		parseTechnologies(getSingleChild("technologies", root, false));
		parsePlayerTech(getChildren("playerTech", root));
	}
	
	private void parseProductionRules(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final String name = current.getAttribute("name");
			final ProductionRule rule = new ProductionRule(name, data);
			parseCosts(rule, getChildren("cost", current));
			parseResults(rule, getChildren("result", current));
			data.getProductionRuleList().addProductionRule(rule);
		}
	}
	
	private void parseRepairRules(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final String name = current.getAttribute("name");
			final RepairRule rule = new RepairRule(name, data);
			parseRepairCosts(rule, getChildren("cost", current));
			parseRepairResults(rule, getChildren("result", current));
			data.getRepairRuleList().addRepairRule(rule);
		}
	}
	
	private void parseCosts(final ProductionRule rule, final List<Node> elements) throws GameParseException
	{
		if (elements.size() == 0)
			throw new GameParseException("no costs  for rule:" + rule.getName());
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final Resource resource = getResource(current, "resource", true);
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			rule.addCost(resource, quantity);
		}
	}
	
	private void parseRepairCosts(final RepairRule rule, final List<Node> elements) throws GameParseException
	{
		if (elements.size() == 0)
			throw new GameParseException("no costs  for rule:" + rule.getName());
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final Resource resource = getResource(current, "resource", true);
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			rule.addCost(resource, quantity);
		}
	}
	
	private void parseResults(final ProductionRule rule, final List<Node> elements) throws GameParseException
	{
		if (elements.size() == 0)
			throw new GameParseException("no results  for rule:" + rule.getName());
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			// must find either a resource or a unit with the given name
			NamedAttachable result = null;
			result = getResource(current, "resourceOrUnit", false);
			if (result == null)
				result = getUnitType(current, "resourceOrUnit", false);
			if (result == null)
				throw new GameParseException("Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			rule.addResult(result, quantity);
		}
	}
	
	private void parseRepairResults(final RepairRule rule, final List<Node> elements) throws GameParseException
	{
		if (elements.size() == 0)
			throw new GameParseException("no results  for rule:" + rule.getName());
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			// must find either a resource or a unit with the given name
			NamedAttachable result = null;
			result = getResource(current, "resourceOrUnit", false);
			if (result == null)
				result = getUnitType(current, "resourceOrUnit", false);
			if (result == null)
				throw new GameParseException("Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			rule.addResult(result, quantity);
		}
	}
	
	private void parseProductionFrontiers(final List<Node> elements) throws GameParseException
	{
		final ProductionFrontierList frontiers = data.getProductionFrontierList();
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final String name = current.getAttribute("name");
			final ProductionFrontier frontier = new ProductionFrontier(name, data);
			parseFrontierRules(getChildren("frontierRules", current), frontier);
			frontiers.addProductionFrontier(frontier);
		}
	}
	
	private void parseTechnologies(final Node element)
	{
		final TechnologyFrontier techs = data.getTechnologyFrontier();
		parseTechs(getChildren("techname", element), techs);
	}
	
	private void parsePlayerTech(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final PlayerID player = getPlayerID(current, "player", true);
			final TechnologyFrontierList categories = player.getTechnologyFrontierList();
			parseCategories(getChildren("category", current), categories);
		}
	}
	
	private void parseCategories(final List<Node> elements, final TechnologyFrontierList categories) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final TechnologyFrontier tf = new TechnologyFrontier(current.getAttribute("name"), data);
			parseCategoryTechs(getChildren("tech", current), tf);
			categories.addTechnologyFrontier(tf);
		}
	}
	
	private void parseRepairFrontiers(final List<Node> elements) throws GameParseException
	{
		final RepairFrontierList frontiers = data.getRepairFrontierList();
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final String name = current.getAttribute("name");
			final RepairFrontier frontier = new RepairFrontier(name, data);
			parseRepairFrontierRules(getChildren("repairRules", current), frontier);
			frontiers.addRepairFrontier(frontier);
		}
	}
	
	private void parsePlayerProduction(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final PlayerID player = getPlayerID(current, "player", true);
			final ProductionFrontier frontier = getProductionFrontier(current, "frontier", true);
			player.setProductionFrontier(frontier);
		}
	}
	
	private void parsePlayerRepair(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final PlayerID player = getPlayerID(current, "player", true);
			final RepairFrontier repairFrontier = getRepairFrontier(current, "frontier", true);
			player.setRepairFrontier(repairFrontier);
		}
	}
	
	private void parseFrontierRules(final List<Node> elements, final ProductionFrontier frontier) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final ProductionRule rule = getProductionRule(((Element) iter.next()), "name", true);
			frontier.addRule(rule);
		}
	}
	
	private void parseTechs(final List<Node> elements, final TechnologyFrontier frontier)
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final String name = current.getAttribute("name");
			final String tech = current.getAttribute("tech");
			TechAdvance ta;
			if (tech.length() > 0)
			{
				ta = new GenericTechAdvance(name, TechAdvance.findDefinedAdvance(tech));
			}
			else
			{
				try
				{
					ta = TechAdvance.findDefinedAdvance(name);
				} catch (final IllegalArgumentException e)
				{
					ta = new GenericTechAdvance(name, null);
				}
			}
			frontier.addAdvance(ta);
		}
	}
	
	private void parseCategoryTechs(final List<Node> elements, final TechnologyFrontier frontier) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			TechAdvance ta = data.getTechnologyFrontier().getAdvanceByProperty(current.getAttribute("name"));
			if (ta == null)
				ta = data.getTechnologyFrontier().getAdvanceByName(current.getAttribute("name"));
			if (ta == null)
				throw new GameParseException("Technology not found :" + current.getAttribute("name"));
			frontier.addAdvance(ta);
		}
	}
	
	private void parseRepairFrontierRules(final List<Node> elements, final RepairFrontier frontier) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final RepairRule rule = getRepairRule(((Element) iter.next()), "name", true);
			frontier.addRule(rule);
		}
	}
	
	private void parseAttachments(final Node root) throws GameParseException
	{
		final Iterator<Node> iter = getChildren("attatchment", root).iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			// create the attachment
			final String className = current.getAttribute("javaClass");
			final Object obj = getInstance(className);
			if (!(obj instanceof IAttachment))
				throw new IllegalStateException(className + " does not implement Attachable");
			final IAttachment attachment = (IAttachment) obj;
			attachment.setData(data);
			// set the values
			final List<Node> values = getChildren("option", current);
			// find the attachable
			final String type = current.getAttribute("type");
			final Attachable attachable = findAttachment(current, type);
			// attach
			if (obj instanceof RulesAttachment)
			{
				final Map<String, IAttachment> map = attachable.getAttachments();
				// TODO: CHECK if this block is necessary or will be
			}
			final String name = current.getAttribute("name");
			attachable.addAttachment(name, attachment);
			attachment.setAttatchedTo(attachable);
			attachment.setName(name);
			setValues(attachment, values);
			data.setAttachmentOrder(attachment); // keep a list of attachment references in the order they were added
			if (obj instanceof RulesAttachment)
			{
				final Map<String, IAttachment> map = attachable.getAttachments();
				// TODO: CHECK if this block is necessary or will be
			}
		}
	}
	
	private Attachable findAttachment(final Element element, final String type) throws GameParseException
	{
		Attachable returnVal;
		final String name = "attatchTo";
		if (type.equals("unitType"))
		{
			returnVal = getUnitType(element, name, true);
		}
		else if (type.equals("territory"))
		{
			returnVal = getTerritory(element, name, true);
		}
		else if (type.equals("resource"))
		{
			returnVal = getResource(element, name, true);
		}
		else if (type.equals("territoryEffect"))
		{
			returnVal = getTerritoryEffect(element, name, true);
		}
		else if (type.equals("player"))
		{
			returnVal = getPlayerID(element, name, true);
		}
		else if (type.equals("relationship"))
		{
			returnVal = this.getRelationshipType(element, name, true);
		}
		else
		{
			throw new GameParseException("Type not found to attach to:" + type);
		}
		return returnVal;
	}
	
	private String capitalizeFirstLetter(final String aString)
	{
		char first = aString.charAt(0);
		first = Character.toUpperCase(first);
		return first + aString.substring(1);
	}
	
	private void setValues(final Object obj, final List<Node> values) throws GameParseException
	{
		final Iterator<Node> iter = values.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			// find the setter
			String name = null;
			Method setter = null;
			// boolean intArgs = false;
			try
			{
				name = current.getAttribute("name");
				if (name.length() == 0)
					throw new GameParseException("option name with 0 length");
				setter = obj.getClass().getMethod("set" + capitalizeFirstLetter(name), SETTER_ARGS);
			} catch (final NoSuchMethodException nsme)
			{
				/*try
				{
				    if(obj.getClass().getMethod( "set" + capitalizeFirstLetter(name), SETTER_ARGS_INT) != null)
				    {
				        intArgs = true;
				    }
				} catch(NoSuchMethodException nsmf)
				{*/
				throw new GameParseException("No setter for attachment option. Setter:" + name + " Class:" + obj.getClass().getName());
				// }
			}
			// find the value
			final String value = current.getAttribute("value");
			final String count = current.getAttribute("count");
			String itemValues = new String();
			if (count.length() > 0)
				itemValues = count + ":";
			/*if (intArgs)
			{
			    itemValues = itemValues + Integer.parseInt(value);
			}
			else*/
			itemValues = itemValues + value;
			// invoke
			try
			{
				final Object[] args = { itemValues };
				setter.invoke(obj, args);
			} catch (final IllegalAccessException iae)
			{
				throw new GameParseException("Setter not public. Setter:" + name + " Class:" + obj.getClass().getName());
			} catch (final InvocationTargetException ite)
			{
				ite.getCause().printStackTrace(System.out);
				throw new GameParseException("Error setting property:" + name + " cause:" + ite.getCause().getMessage());
			}
		}
	}
	
	private void parseInitialization(final Node root) throws GameParseException
	{
		// parse territory owners
		final Node owner = getSingleChild("ownerInitialize", root, true);
		if (owner != null)
			parseOwner(getChildren("territoryOwner", owner));
		// parse initial unit placement
		final Node unit = getSingleChild("unitInitialize", root, true);
		if (unit != null)
		{
			parseUnitPlacement(getChildren("unitPlacement", unit));
			parseHeldUnits(getChildren("heldUnits", unit));
		}
		// parse resources given
		final Node resource = getSingleChild("resourceInitialize", root, true);
		if (resource != null)
			parseResourceInitialization(getChildren("resourceGiven", resource));
		// parse relationships
		final Node relationInitialize = getSingleChild("relationshipInitialize", root, true);
		if (relationInitialize != null)
			parseRelationInitialize(getChildren("relationship", relationInitialize));
	}
	
	private void parseOwner(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final Territory territory = getTerritory(current, "territory", true);
			final PlayerID owner = getPlayerID(current, "owner", true);
			territory.setOwner(owner);
			// Set the original owner on startup.
			// TODO Look into this
			// The addition of this caused the automated tests to fail as TestAttachment can't be cast to TerritoryAttachment
			// The addition of this IF to pass the tests is wrong, but works until a better solution is found.
			// Kevin will look into it.
			if (!territory.getData().getGameName().equals("gameExample") && !territory.getData().getGameName().equals("test"))
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(territory);
				if (ta != null)
					ta.setOriginalOwner(owner);
			}
		}
	}
	
	private void parseUnitPlacement(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final Territory territory = getTerritory(current, "territory", true);
			final UnitType type = getUnitType(current, "unitType", true);
			final String ownerString = current.getAttribute("owner");
			PlayerID owner;
			if (ownerString == null || ownerString.trim().length() == 0)
				owner = PlayerID.NULL_PLAYERID;
			else
				owner = getPlayerID(current, "owner", false);
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			territory.getUnits().addAllUnits(type.create(quantity, owner));
		}
	}
	
	private void parseHeldUnits(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final PlayerID player = getPlayerID(current, "player", true);
			final UnitType type = getUnitType(current, "unitType", true);
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			player.getUnits().addAllUnits(type.create(quantity, player));
		}
	}
	
	private void parseResourceInitialization(final List<Node> elements) throws GameParseException
	{
		final Iterator<Node> iter = elements.iterator();
		while (iter.hasNext())
		{
			final Element current = (Element) iter.next();
			final PlayerID player = getPlayerID(current, "player", true);
			final Resource resource = getResource(current, "resource", true);
			final int quantity = Integer.parseInt(current.getAttribute("quantity"));
			player.getResources().addResource(resource, quantity);
		}
	}
}
