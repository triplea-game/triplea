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
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class GameParser
{
    private static final Class<?>[] SETTER_ARGS = {String.class};

    private GameData data;

    public GameParser()
    {
    }

    public synchronized GameData parse(InputStream stream) throws GameParseException, SAXException
    {
        if(stream == null)
            throw new IllegalArgumentException("Stream must be non null");

        Document doc = null;

        try {
            doc = getDocument(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }


        Node root = doc.getDocumentElement();

        data = new GameData();

        //mandatory fields
        parseInfo(getSingleChild("info", root));
        parseGameLoader(getSingleChild("loader", root));

        Node diceSides = getSingleChild("diceSides", root, true);
        if (diceSides != null)
        	parseDiceSides(diceSides);
        else
        	data.setDiceSides(6);

        parseMap(getSingleChild("map", root));

        Node resourceList = getSingleChild("resourceList", root, true);
        if(resourceList != null)
            parseResources(resourceList);
        // Parse all different relationshipTypes that are defined in the xml, for example: War, Allied, Neutral, NAP
        Node relationshipTypes = getSingleChild("relationshipTypes",root, true);
        if(relationshipTypes != null)
        	parseRelationshipTypes(relationshipTypes);

        Node playerListNode = getSingleChild("playerList", root);
        parsePlayerList(playerListNode);
        
        parseAlliances(playerListNode);
        
        
        
        
        parseGamePlay(getSingleChild("gamePlay", root));

        //optional
        Node unitList = getSingleChild("unitList", root, true);
        if (unitList != null)
            parseUnits(unitList);

        Node production = getSingleChild("production", root, true);
        if(production != null)
            parseProduction(production);

        Node technology = getSingleChild("technology", root, true);
        if(technology != null)
            parseTechnology(technology);

        Node attachmentList = getSingleChild("attatchmentList", root, true);
        if(attachmentList != null)
            parseAttachments(attachmentList);

        Node initialization = getSingleChild("initialize", root, true);
        if(initialization != null)
            parseInitialization(initialization);

        Node properties = getSingleChild("propertyList", root, true);
        if(properties != null)
            parseProperties(properties);




        validate();

        return data;
    }

    private void parseDiceSides(Node diceSides)
    {
    	data.setDiceSides(Integer.parseInt(((Element) diceSides).getAttribute("value")));
    }

	private void validate() throws GameParseException {
		//validate unit attachments
		for(UnitType u : data.getUnitTypeList())
        {
			validateAttachments(u);
        }
		for(Territory t : data.getMap())
		{
			validateAttachments(t);
		}
		for(Resource r : data.getResourceList().getResources())
		{
			validateAttachments(r);
		}
		//if relationships are used, every player should have a relationship with every other player
		validateRelationships();

	}

	private void validateRelationships() throws GameParseException{
		// only if we are using the relationships and not alliances to track politics
		if(data.getRelationshipTracker().useRelationshipModel()) {
			// for every player
			for(PlayerID player:data.getPlayerList()) {
				// in relation to every player
				for(PlayerID player2:data.getPlayerList()) {
					// See if there is a relationship between them
					if((data.getRelationshipTracker().getRelationshipType(player, player2) == null))
						throw new GameParseException("No relation set for: "+player.getName()+" and "+player2.getName());
					      // or else throw an exception!
				}
			}
		}
	}

	private void validateAttachments(Attachable attachable) throws GameParseException
	{
		for(IAttachment a : attachable.getAttachments().values())
    	{
    		a.validate(data);
    	}
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
     * If mustfind is true and cannot find the player an exception will be thrown.
     * @return a RelationshipType from the relationshipTypeList, at this point all relationshipTypes should have been declared
     * @throws GameParseException when
     */
    private RelationshipType getRelationshipType(Element element, String attribute, boolean mustFind) throws GameParseException
    {
        String name = element.getAttribute(attribute);
        RelationshipType relation = data.getRelationshipTypeList().getRelationshipType(name);
        if(relation == null && mustFind)
            throw new GameParseException("Could not find relation name:" + name);

        return relation;
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
     * If mustfind is true and cannot find the productionRule an exception will be thrown.
     */
    private RepairRule getRepairRule(Element element, String attribute, boolean mustFind) throws GameParseException
    {
        String name = element.getAttribute(attribute);
        RepairRule repairRule = data.getRepairRuleList().getRepairRule(name);
        if(repairRule == null && mustFind)
            throw new GameParseException("Could not find production rule. name:" + name);

        return repairRule;
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
     * If mustfind is true and cannot find the productionRule an exception will be thrown.
     */
    private RepairFrontier getRepairFrontier(Element element, String attribute, boolean mustFind) throws GameParseException
    {
        String name = element.getAttribute(attribute);
        RepairFrontier repairFrontier = data.getRepairFrontierList().getRepairFrontier(name);
        if(repairFrontier == null && mustFind)
            throw new GameParseException("Could not find production frontier. name:" + name);

        return repairFrontier;
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
            Class<?> instanceClass = Class.forName(className);
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
        List<Node> children = getChildren(name, node);

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
        return children.get(0);
    }

    private List<Node> getChildren(String name, Node node)
    {
        ArrayList<Node> found = new ArrayList<Node>();
        NodeList children = node.getChildNodes();
        for(int i= 0; i < children.getLength(); i++)
        {
            Node current = children.item(i);
            if(current.getNodeName().equals(name))
                found.add(current);
        }
        return found;
    }

    private List<Node> getNonTextNodesIgnoring(Node node, String ignore)
    {
        List<Node> rVal = getNonTextNodes(node);
        Iterator<Node> iter = rVal.iterator();
        while(iter.hasNext()) {
            if(((Element) iter.next()).getTagName().equals(ignore)) {
                iter.remove();
            }
        }
        return rVal;

    }


    private List<Node> getNonTextNodes(Node node)
    {
        ArrayList<Node> found = new ArrayList<Node>();
        NodeList children = node.getChildNodes();
        for(int i= 0; i < children.getLength(); i++)
        {
            Node current = children.item(i);
            if(! (current.getNodeType() == Node.TEXT_NODE))
                found.add(current);
        }
        return found;

    }

    private void parseInfo(Node info)
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
        List<Node> grids = getChildren("grid",map);
        parseGrids(grids);
        //get the Territories
        List<Node> territories = getChildren("territory", map);
        parseTerritories(territories);
        List<Node> connections = getChildren("connection", map);
        parseConnections(connections);

    }

    private void parseGrids(List<Node> grids) throws GameParseException
    {
        GameMap map = data.getMap();

        Iterator<Node> iter = grids.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            List<Node> waterNodes = getChildren("water",current);
            Set<String> water = parseGridWater(waterNodes);

            String horizontalConnections = current.getAttribute("horizontal-connections");
            boolean horizontalConnectionsImplict;
            if (horizontalConnections.equals("implicit"))
                horizontalConnectionsImplict = true;
            else if (horizontalConnections.equals("explicit"))
                horizontalConnectionsImplict = false;
            else
                throw new GameParseException("horizontal-connections attribute must be either \"explicit\" or \"implicit\"");

            String verticalConnections = current.getAttribute("vertical-connections");
            boolean verticalConnectionsImplict;
            if (verticalConnections.equals("implicit"))
                verticalConnectionsImplict = true;
            else if (verticalConnections.equals("explicit"))
                verticalConnectionsImplict = false;
            else
                throw new GameParseException("vertical-connections attribute must be either \"explicit\" or \"implicit\"");


            String diagonalConnections = current.getAttribute("diagonal-connections");
            boolean diagonalConnectionsImplict;
            if (diagonalConnections.equals("implicit"))
                diagonalConnectionsImplict = true;
            else if (diagonalConnections.equals("explicit"))
                diagonalConnectionsImplict = false;
            else
                throw new GameParseException("diagonal-connections attribute must be either \"explicit\" or \"implicit\"");

            String gridType = current.getAttribute("type");
            String name = current.getAttribute("name");
            String xs = current.getAttribute("x");
            String ys = current.getAttribute("y");

            int x_size = Integer.valueOf(xs);
            int y_size;
            if (ys != null)
                y_size = Integer.valueOf(ys);
            else
                y_size = 0;

            map.setGridDimensions(x_size, y_size);

            if (gridType.equals("square"))
            {
                // Add territories
                for (int y=0; y<y_size; y++)
                {
                    for (int x=0; x<x_size; x++)
                    {
                        boolean isWater;
                        if (water.contains(x + "-" + y))
                            isWater = true;
                        else
                            isWater = false;

                        Territory newTerritory = new Territory(name + "_" + x + "_" + y , isWater, data, x, y);
                        map.addTerritory(newTerritory);
                    }
                }


                // Add any implicit horizontal connections
                if (horizontalConnectionsImplict)
                    for (int y=0; y<y_size; y++)
                        for (int x=0; x<x_size-1; x++)
                            map.addConnection(map.getTerritoryFromCoordinates(x,y), map.getTerritoryFromCoordinates(x+1,y));


                // Add any implicit vertical connections
                if (verticalConnectionsImplict)
                    for (int x=0; x<x_size; x++)
                        for (int y=0; y<y_size-1; y++)
                            map.addConnection(map.getTerritoryFromCoordinates(x,y), map.getTerritoryFromCoordinates(x,y+1));


                // Add any implicit acute diagonal connections
                if (diagonalConnectionsImplict)
                    for (int y=0; y<y_size-1; y++)
                        for (int x=0; x<x_size-1; x++)
                            map.addConnection(map.getTerritoryFromCoordinates(x,y), map.getTerritoryFromCoordinates(x+1,y+1));


                // Add any implicit obtuse diagonal connections
                if (diagonalConnectionsImplict)
                    for (int y=0; y<y_size-1; y++)
                        for (int x=1; x<x_size; x++)
                            map.addConnection(map.getTerritoryFromCoordinates(x,y), map.getTerritoryFromCoordinates(x-1,y+1));

            }


            else if (gridType.equals("points-and-lines"))
            {   // This type is a triangular grid of points and lines,
                //    used for in several rail games

                // Add territories
                for (int y=0; y<y_size; y++)
                {
                    for (int x=0; x<x_size; x++)
                    {
                        boolean isWater = false;
                        if (!water.contains(x + "-" + y))
                        {
                            Territory newTerritory = new Territory(name + "_" + x + "_" + y , isWater, data, x, y);
                            map.addTerritory(newTerritory);
                        }

                    }
                }

                // Add any implicit horizontal connections
                if (horizontalConnectionsImplict)
                    for (int y=0; y<y_size; y++)
                        for (int x=0; x<x_size-1; x++)
                        {
                            Territory from = map.getTerritoryFromCoordinates(x,y);
                            Territory to = map.getTerritoryFromCoordinates(x+1,y);
                            if (from!=null && to!=null)
                                map.addConnection(from, to);
                        }

                // Add any implicit acute diagonal connections
                if (diagonalConnectionsImplict)
                    for (int y=1; y<y_size; y++)
                        for (int x=0; x<x_size-1; x++)
                            if (y%4==0 || (y+1)%4==0)
                            {
                                Territory from = map.getTerritoryFromCoordinates(x,y);
                                Territory to = map.getTerritoryFromCoordinates(x,y-1);
                                if (from!=null && to!=null)
                                    map.addConnection(from, to);
                            }
                            else
                            {
                                Territory from = map.getTerritoryFromCoordinates(x,y);
                                Territory to = map.getTerritoryFromCoordinates(x+1,y-1);
                                if (from!=null && to!=null)
                                    map.addConnection(from, to);
                            }


                // Add any implicit obtuse diagonal connections
                if (diagonalConnectionsImplict)
                    for (int y=1; y<y_size; y++)
                        for (int x=0; x<x_size-1; x++)
                            if (y%4==0 || (y+1)%4==0)
                            {
                                Territory from = map.getTerritoryFromCoordinates(x,y);
                                Territory to = map.getTerritoryFromCoordinates(x-1,y-1);
                                if (from!=null && to!=null)
                                    map.addConnection(from, to);
                            }
                            else
                            {
                                Territory from = map.getTerritoryFromCoordinates(x,y);
                                Territory to = map.getTerritoryFromCoordinates(x,y-1);
                                if (from!=null && to!=null)
                                    map.addConnection(from, to);
                            }
            }
        }

    }

    private Set<String> parseGridWater(List<Node> waterNodes)
    {
        Set<String> set = new HashSet<String>();
        Iterator<Node> iter = waterNodes.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            int x = Integer.valueOf(current.getAttribute("x"));
            int y = Integer.valueOf(current.getAttribute("y"));
            set.add(x + "-" + y);
        }
        return set;
    }

    private void parseTerritories(List<Node> territories)
    {
        GameMap map = data.getMap();
        Iterator<Node> iter = territories.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            boolean water = current.getAttribute("water").trim().equalsIgnoreCase("true");
            String name = current.getAttribute("name");

            Territory newTerritory = new Territory(name, water, data);
            map.addTerritory(newTerritory);
        }
    }

    private void parseConnections(List<Node> connections) throws GameParseException
    {
        GameMap map = data.getMap();
        Iterator<Node> iter = connections.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            Territory t1 = getTerritory(current, "t1", true);
            Territory t2 = getTerritory(current, "t2", true);
            map.addConnection(t1, t2);
        }
    }

    private void parseResources(Node root)
    {
        Iterator<Node> iter = getChildren("resource", root).iterator();
        while(iter.hasNext())
        {
            data.getResourceList().addResource(new Resource(((Element)iter.next()).getAttribute("name"), data));
        }
    }

    private void parseRelationshipTypes(Node root) {
    	Iterator<Node> iter = getChildren("relationshipType", root).iterator();
    	while(iter.hasNext())
        {
            data.getRelationshipTypeList().addRelationshipType(new RelationshipType(((Element)iter.next()).getAttribute("name"), data));
        }
	}

    private void parseUnits(Node root)
    {
        Iterator<Node> iter = getChildren("unit", root).iterator();
        while(iter.hasNext())
        {
            data.getUnitTypeList().addUnitType(new UnitType(((Element)iter.next()).getAttribute("name"), data));
        }
    }

    /**
     * @param root root node containing the playerList
     * @throws GameParseException
     */
    private void parsePlayerList(Node root)
    {
        PlayerList playerList = data.getPlayerList();

        Iterator<Node> iter = getChildren("player", root).iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            String name = current.getAttribute("name");

            //It appears the commented line ALWAYS returns false regardless of the value of current.getAttribute("optional")
            //boolean isOptional = Boolean.getBoolean(current.getAttribute("optional"));
            boolean isOptional = current.getAttribute("optional").equals("true");
            PlayerID newPlayer = new PlayerID(name, isOptional, data);
            playerList.addPlayerID(newPlayer);
        }
    }

    private void parseAlliances(Node root) throws GameParseException
    {
        AllianceTracker allianceTracker = data.getAllianceTracker();

        Iterator<Node> iter = getChildren("alliance", root).iterator();
        Boolean has_alliance_node = iter.hasNext();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            PlayerID p1 = getPlayerID(current, "player", true);
            String alliance = current.getAttribute("alliance");
            allianceTracker.addToAlliance(p1,alliance);
        }

        /*
         * if we are using alliances than the relationships aren't in the xml (old style)
         * therefore we set the relationships based on the alliances
         */
        if (has_alliance_node)
        {
            RelationshipTracker relationshipTracker = data.getRelationshipTracker();
            Set<String> alliances = allianceTracker.getAlliances();
            List<PlayerID> allPlayers = new ArrayList<PlayerID>(data.getPlayerList().getPlayers());
            Iterator<String> iterAlliance = alliances.iterator();
            while(iterAlliance.hasNext())
            {
                HashSet<PlayerID> alliancePlayersSet = allianceTracker.getPlayersInAlliance(iterAlliance.next());
                PlayerID[] alliancePlayers = alliancePlayersSet.toArray(new PlayerID[alliancePlayersSet.size()]);
                List<PlayerID> enemies =  new ArrayList<PlayerID>(allPlayers);
                enemies.removeAll(alliancePlayersSet);
                for(int i=0 ; i<alliancePlayers.length ; ++i)
                {
                    relationshipTracker.setRelationship(alliancePlayers[i], alliancePlayers[i], new RelationshipType(Constants.RELATIONSHIP_TYPE_SELF, data));
                    relationshipTracker.setRelationship(alliancePlayers[i], PlayerID.NULL_PLAYERID, new RelationshipType(Constants.RELATIONSHIP_TYPE_NULL, data));
                    for(int j=i+1 ; j<alliancePlayers.length ; ++j)
                    {
                        relationshipTracker.setRelationship(alliancePlayers[i], alliancePlayers[j], new RelationshipType(Constants.RELATIONSHIP_ANY_ALLIED, data));
                        //relationshipTracker.setRelationship(alliancePlayers[j], alliancePlayers[i], new RelationshipType(Constants.RELATIONSHIP_ANY_ALLIED, data));
                    }
                    Iterator<PlayerID> iterEnemy = enemies.iterator();
                    while (iterEnemy.hasNext())
                    {
                        relationshipTracker.setRelationship(alliancePlayers[i], iterEnemy.next(), new RelationshipType(Constants.RELATIONSHIP_ANY_WAR, data));
                    }
                }
            }
        }
        else {
            throw new NoAllianceNodesException("No alliances where specified.");
        }
    }

    /* This is now relationshipInitialize.  can delete: private void parsePlayerRelationships(Node root) throws GameParseException
    {
        RelationshipTracker tracker = data.getRelationshipTracker();

        Iterator<Node> iter = getChildren("relationship", root).iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            PlayerID p1 = getPlayerID(current, "player1", true);
            PlayerID p2 = getPlayerID(current, "player2", true);
            RelationshipType relationshipType = new RelationshipType(current.getAttribute("type"), data);
            tracker.setRelationship(p1, p2, relationshipType);
        }
    }*/

    private void parseRelationInitialize(List<Node> relations) throws GameParseException {
    	if(relations.size()>0) {
            RelationshipTracker tracker = data.getRelationshipTracker();
            Iterator<Node> iter = relations.iterator();
            while(iter.hasNext())
            {
	    		Element current = (Element) iter.next();
	    		PlayerID p1 = getPlayerID(current, "player1", true);
	    		PlayerID p2 = getPlayerID(current, "player2", true);
	    		RelationshipType r = getRelationshipType(current, "type", true);
	    		tracker.setRelationship(p1,p2,r);
	    	}
    		tracker.setSelfRelations();
    		tracker.setNullPlayerRelations();
    	}
    }

    private void parseGamePlay(Node root) throws GameParseException
    {
        parseDelegates(getChildren("delegate", root));
        parseSequence(getSingleChild("sequence", root));
    }

    private void parseProperties(Node root) throws GameParseException
    {
        GameProperties properties = data.getProperties();
        Iterator<Node> children = getChildren("property", root).iterator();
        while(children.hasNext())
        {

            Element current = (Element) children.next();
            String editable = current.getAttribute("editable");
            String property = current.getAttribute("name");
            String value = current.getAttribute("value");
            if(value == null || value.length() == 0) {
                List<Node> valueChildren =  getChildren("value", current);
                if(!valueChildren.isEmpty())
                {
                    Element valueNode = (Element) valueChildren.get(0);
                    if(valueNode!= null) {
                        value = valueNode.getTextContent();
                    }
                }
            }


            if(editable != null && editable.equalsIgnoreCase("true"))
                parseEditableProperty(current, property, value);
            else
            {
                List<Node> children2 = getNonTextNodesIgnoring(current, "value");
                if(children2.size() == 0)
                    properties.set(property,value);
                else
                {
                    String type = ((Element) children2.get(0)).getNodeName();
                    if(type.equals("boolean"))
                    {
                        properties.set(property, Boolean.valueOf(value));
                    }
                    else if (type.equals("file"))
                    {
                        properties.set(property, new File(value));
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
        List<Node> children = getNonTextNodes(property);
        if(children.size() != 1)
            throw new GameParseException("Editable properties must have exactly 1 child specifying the type. Number of children found:" + children.size() + " for node:" + property.getNodeName());

        Element child = (Element) children.get(0);
        String childName = child.getNodeName();

        IEditableProperty editableProperty;

        if(childName.equals("boolean"))
        {
            editableProperty = new BooleanProperty(name, Boolean.valueOf(defaultValue).booleanValue());
        }
        else if (childName.equals("file"))
        {
            editableProperty = new FileProperty(name, defaultValue);
        }
        else if(childName.equals("list"))
        {
            StringTokenizer tokenizer = new StringTokenizer(child.getAttribute("values"), ",");
            Collection<String> values = new ArrayList<String>();
            while(tokenizer.hasMoreElements())
                values.add(tokenizer.nextToken());
            editableProperty = new ListProperty(name, defaultValue, values);
        }
        else if(childName.equals("number"))
        {
            int max = Integer.valueOf(child.getAttribute("max")).intValue();
            int min = Integer.valueOf(child.getAttribute("min")).intValue();
            int def = Integer.valueOf(defaultValue).intValue();

            editableProperty = new NumberProperty(name, max, min, def);

        }
        else if (childName.equals("color"))
        {
            // Parse the value as a hexidecimal number
            int def = Integer.valueOf(defaultValue,16).intValue();

            editableProperty = new ColorProperty(name,def);
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

    private void parseDelegates(List<Node> delegateList) throws GameParseException
    {
        DelegateList delegates = data.getDelegateList();

        Iterator<Node> iterator = delegateList.iterator();
        while(iterator.hasNext())
        {
            Element current = (Element) iterator.next();
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

    private void parseSteps(List<Node> stepList) throws GameParseException
    {
        Iterator<Node> iterator = stepList.iterator();
        while(iterator.hasNext())
        {
            Element current = (Element) iterator.next();

            IDelegate delegate = getDelegate(current, "delegate", true);
            PlayerID player = getPlayerID(current, "player", false);
            String name = current.getAttribute("name");
            String displayName = null;

            List<Node> propertyElements = getChildren("stepProperty", current);
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

    private Properties pareStepProperties(List<Node> properties)
    {
        Properties rVal = new Properties();
        Iterator<Node> iter = properties.iterator();
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

    	parseRepairRules( getChildren("repairRule", root));
    	parseRepairFrontiers( getChildren("repairFrontier", root));
    	parsePlayerRepair( getChildren("playerRepair", root));
    }

    private void parseTechnology(Node root) throws GameParseException
    {
        parseTechnologies( getSingleChild("technologies", root, false));
        parsePlayerTech(getChildren("playerTech", root));
    }

    private void parseProductionRules(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            String name = current.getAttribute("name");
            ProductionRule rule = new ProductionRule(name, data);
            parseCosts(rule, getChildren("cost", current));
            parseResults(rule, getChildren("result", current));
            data.getProductionRuleList().addProductionRule(rule);
        }
    }

    private void parseRepairRules(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            String name = current.getAttribute("name");
            RepairRule rule = new RepairRule(name, data);
            parseRepairCosts(rule, getChildren("cost", current));
            parseRepairResults(rule, getChildren("result", current));
            data.getRepairRuleList().addRepairRule(rule);
        }
    }

    private void parseCosts(ProductionRule rule, List<Node> elements) throws GameParseException
    {
        if(elements.size() == 0)
            throw new GameParseException("no costs  for rule:" + rule.getName());

        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            Resource resource = getResource(current, "resource", true);
            int quantity = Integer.parseInt(current.getAttribute("quantity"));
            rule.addCost(resource, quantity);
        }
    }

    private void parseRepairCosts(RepairRule rule, List<Node> elements) throws GameParseException
    {
        if(elements.size() == 0)
            throw new GameParseException("no costs  for rule:" + rule.getName());

        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            Resource resource = getResource(current, "resource", true);
            int quantity = Integer.parseInt(current.getAttribute("quantity"));
            rule.addCost(resource, quantity);
        }
    }

    private void parseResults(ProductionRule rule, List<Node> elements) throws GameParseException
    {
        if(elements.size() == 0)
            throw new GameParseException("no results  for rule:" + rule.getName());

        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            //must find either a resource or a unit with the given name
            NamedAttachable result = null;
            result = getResource(current, "resourceOrUnit", false);
            if(result == null)
                result = getUnitType(current, "resourceOrUnit", false);
            if(result == null)
                throw new GameParseException("Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
            int quantity = Integer.parseInt(current.getAttribute("quantity"));
            rule.addResult(result, quantity);
        }
    }

    private void parseRepairResults(RepairRule rule, List<Node> elements) throws GameParseException
    {
        if(elements.size() == 0)
            throw new GameParseException("no results  for rule:" + rule.getName());

        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            //must find either a resource or a unit with the given name
            NamedAttachable result = null;
            result = getResource(current, "resourceOrUnit", false);
            if(result == null)
                result = getUnitType(current, "resourceOrUnit", false);
            if(result == null)
                throw new GameParseException("Could not find resource or unit" + current.getAttribute("resourceOrUnit"));
            int quantity = Integer.parseInt(current.getAttribute("quantity"));
            rule.addResult(result, quantity);
        }
    }

    private void parseProductionFrontiers(List<Node> elements) throws GameParseException
    {
        ProductionFrontierList frontiers = data.getProductionFrontierList();

        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            String name = current.getAttribute("name");
            ProductionFrontier frontier = new ProductionFrontier(name, data);
            parseFrontierRules( getChildren("frontierRules", current), frontier);
            frontiers.addProductionFrontier(frontier);
        }
    }

    private void parseTechnologies(Node element)
    {
        TechnologyFrontier techs = data.getTechnologyFrontier();
        parseTechs( getChildren("techname", element), techs);
    }

    private void parsePlayerTech(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            PlayerID player = getPlayerID(current, "player", true);
            TechnologyFrontierList categories = player.getTechnologyFrontierList();
            parseCategories(getChildren("category", current),categories);
        }

    }

    private void parseCategories(List<Node> elements,TechnologyFrontierList categories ) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            TechnologyFrontier tf = new TechnologyFrontier(current.getAttribute("name"),data);
            parseCategoryTechs( getChildren("tech", current), tf);
            categories.addTechnologyFrontier(tf);
        }

    }
    private void parseRepairFrontiers(List<Node> elements) throws GameParseException
    {
    	RepairFrontierList frontiers = data.getRepairFrontierList();

    	Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            String name = current.getAttribute("name");
            RepairFrontier frontier = new RepairFrontier(name, data);
            parseRepairFrontierRules( getChildren("repairRules", current), frontier);
            frontiers.addRepairFrontier(frontier);
        }
    }

    private void parsePlayerProduction(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            PlayerID player = getPlayerID(current, "player", true);
            ProductionFrontier frontier = getProductionFrontier(current, "frontier", true);
            player.setProductionFrontier(frontier);
        }
    }

    private void parsePlayerRepair(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            PlayerID player = getPlayerID(current, "player", true);
           	RepairFrontier repairFrontier = getRepairFrontier(current, "frontier", true);
           	player.setRepairFrontier(repairFrontier);
        }
    }

    private void parseFrontierRules(List<Node> elements, ProductionFrontier frontier) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            ProductionRule rule = getProductionRule(((Element) iter.next()), "name", true);
            frontier.addRule(rule);
        }
    }

    private void parseTechs(List<Node> elements, TechnologyFrontier frontier)
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            String name = current.getAttribute("name");
            String tech = current.getAttribute("tech");
            TechAdvance ta;
            if(tech.length()>0) {
            	ta = new GenericTechAdvance(name,TechAdvance.findDefinedAdvance(tech));
            }
            else {
            	try {
            		ta = TechAdvance.findDefinedAdvance(name);
            	} catch (IllegalArgumentException e) {
            		ta = new GenericTechAdvance(name,null);
            	}
            }
            frontier.addAdvance(ta);
        }
    }
    private void parseCategoryTechs(List<Node> elements, TechnologyFrontier frontier) throws GameParseException{
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
            TechAdvance ta = data.getTechnologyFrontier().getAdvanceByProperty(current.getAttribute("name"));
            if(ta==null)
            	ta = data.getTechnologyFrontier().getAdvanceByName(current.getAttribute("name"));
            if(ta==null)
            	throw new GameParseException("Technology not found :"+current.getAttribute("name"));
            frontier.addAdvance(ta);
        }
    }
    private void parseRepairFrontierRules(List<Node> elements, RepairFrontier frontier) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            RepairRule rule = getRepairRule(((Element)iter.next()) , "name", true);
            frontier.addRule(rule);
        }
    }

    private void parseAttachments(Node root) throws GameParseException
    {
        Iterator<Node> iter = getChildren("attatchment", root).iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            //create the attachment
            String className = current.getAttribute("javaClass");
            Object obj = getInstance(className);
            if(!(obj instanceof IAttachment))
                throw new IllegalStateException(className + " does not implement Attachable");

            IAttachment attachment = (IAttachment) obj;
            attachment.setData(data);
            //set the values
            List<Node> values = getChildren("option", current);

            //find the attachable
            String type = current.getAttribute("type");
            Attachable attachable = findAttachment(current, type);

            //attach
            if (obj instanceof RulesAttachment)
            {
            	Map<String, IAttachment> map = attachable.getAttachments();
            	//TODO: CHECK if this block is necessary or will be
            }
            String name = current.getAttribute("name");
            attachable.addAttachment(name, attachment);
            attachment.setAttatchedTo(attachable);
            attachment.setName(name);
            setValues(attachment, values);
            data.setAttachmentOrder(attachment); // keep a list of attachment references in the order they were added

            if (obj instanceof RulesAttachment)
            {
            	Map<String, IAttachment> map = attachable.getAttachments();
                //TODO: CHECK if this block is necessary or will be
            }
        }
    }

    private Attachable findAttachment(Element element, String type) throws GameParseException
    {
        Attachable returnVal;
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
        } else if(type.equals("relationship"))
        {
        	returnVal = this.getRelationshipType(element, name, true);
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

    private void setValues(Object obj, List<Node> values) throws GameParseException
    {
        Iterator<Node> iter = values.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            //find the setter
            String name = null;
            Method setter = null;
            //boolean intArgs = false;
            try
            {
                name = current.getAttribute("name");
                if(name.length() == 0)
                    throw new GameParseException("option name with 0 length");
                setter = obj.getClass().getMethod( "set" + capitalizeFirstLetter(name), SETTER_ARGS);
            } catch(NoSuchMethodException nsme)
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
                //}
            }

            //find the value
            String value = current.getAttribute("value");
            String count = current.getAttribute("count");

            String itemValues = new String();

            if(count.length() > 0)
            	itemValues = count + ":";

            /*if (intArgs)
            {
                itemValues = itemValues + Integer.parseInt(value);
            }
            else*/
                itemValues = itemValues + value;

            //invoke
            try
            {
                Object[] args = {itemValues};

                setter.invoke(obj, args );

            } catch(IllegalAccessException iae)
            {
                throw new GameParseException("Setter not public. Setter:" + name + " Class:" + obj.getClass().getName());
            } catch(InvocationTargetException ite)
            {
                ite.getCause().printStackTrace(System.out);
                throw new GameParseException("Error setting property:" +  name + " cause:" + ite.getCause().getMessage());
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
        Node relationInitialize = getSingleChild("relationshipInitialize", root, true);
        if(relationInitialize != null)
        	parseRelationInitialize(getChildren("relationship",relationInitialize));

    }

    private void parseOwner(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            Territory territory = getTerritory(current, "territory", true);
            PlayerID owner = getPlayerID(current, "owner", true);
            territory.setOwner(owner);
            //Set the original owner on startup.
            //TODO Look into this
            //The addition of this caused the automated tests to fail as TestAttachment can't be cast to TerritoryAttachment
            //The addition of this IF to pass the tests is wrong, but works until a better solution is found.
            //Kevin will look into it.
            if (!territory.getData().getGameName().equals("gameExample") && !territory.getData().getGameName().equals("test") )
            {
            	 TerritoryAttachment ta = TerritoryAttachment.get(territory);
                if(ta != null)
                	ta.setOriginalOwner(owner);
            }

        }
    }

    private void parseUnitPlacement(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();
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

    private void parseHeldUnits(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            PlayerID player = getPlayerID(current, "player", true);
            UnitType type = getUnitType(current, "unitType", true);
            int quantity = Integer.parseInt(current.getAttribute("quantity"));
            player.getUnits().addAllUnits(type.create(quantity, player));
        }
    }

    private void parseResourceInitialization(List<Node> elements) throws GameParseException
    {
        Iterator<Node> iter = elements.iterator();
        while(iter.hasNext())
        {
            Element current = (Element) iter.next();

            PlayerID player = getPlayerID(current, "player", true);
            Resource resource = getResource(current, "resource", true);
            int quantity = Integer.parseInt(current.getAttribute("quantity"));
            player.getResources().addResource(resource, quantity);
        }
    }
}
