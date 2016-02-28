package util.triplea.MapXMLCreator;

import games.strategy.engine.data.GameParser;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.awt.HeadlessException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import util.image.FileOpen;
import util.image.FileSave;
import util.triplea.MapXMLCreator.TerritoryDefinitionDialog.DEFINITION;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

/**
 * This class reads, writes and keeps the Map XML properties.
 * 
 * @author Erik von der Osten
 * 
 */
public class MapXMLHelper {

	private static final String TRIPLEA_JAVA_CLASS_DELEGATE_PATH = "games.strategy.triplea.delegate.";
	static LinkedHashMap<String, String> s_xmlStrings = new LinkedHashMap<String, String>();
	static ArrayList<String> s_resourceList = new ArrayList<String>();
	static HashMap<String, HashMap<DEFINITION, Boolean>> s_territoyDefintions = new HashMap<String, HashMap<DEFINITION, Boolean>>();
	static HashMap<String, LinkedHashSet<String>> s_territoyConnections = new HashMap<String, LinkedHashSet<String>>();
	static ArrayList<String> s_playerName = new ArrayList<String>();
	static HashMap<String, String> s_playerAlliance = new HashMap<String, String>();
	static HashMap<String, Integer> s_playerInitResources = new HashMap<String, Integer>();
	static LinkedHashMap<String, ArrayList<Integer>> s_unitDefinitions = new LinkedHashMap<String, ArrayList<Integer>>();
	static LinkedHashMap<String, ArrayList<String>> s_gamePlaySequence = new LinkedHashMap<String, ArrayList<String>>();
	static LinkedHashMap<String, Triple<String, String, Integer>> s_playerSequence = new LinkedHashMap<String, Triple<String, String, Integer>>();
	static LinkedHashMap<String, ArrayList<String>> s_technologyDefinitions = new LinkedHashMap<String, ArrayList<String>>();
	static LinkedHashMap<String, ArrayList<String>> s_productionFrontiers = new LinkedHashMap<String, ArrayList<String>>();
	static LinkedHashMap<String, ArrayList<String>> s_unitAttatchments = new LinkedHashMap<String, ArrayList<String>>();
	static HashMap<String, Integer> s_territoyProductions = new HashMap<String, Integer>();
	static HashMap<String, Tuple<SortedSet<String>, SortedSet<String>>> s_canalDefinitions = new HashMap<String, Tuple<SortedSet<String>, SortedSet<String>>>();
	static HashMap<String, String> s_territoyOwnerships = new HashMap<String, String>();
	static HashMap<String, HashMap<String, LinkedHashMap<String, Integer>>> s_unitPlacements = new HashMap<String, HashMap<String, LinkedHashMap<String, Integer>>>();
	static HashMap<String, ArrayList<String>> s_gameSettings = new HashMap<String, ArrayList<String>>();
	static String s_notes = "";

	static File s_mapXMLFile;

	static int loadXML() {
		try {
			final String gameXMLPath = new FileOpen("Load a Game XML File", s_mapXMLFile, ".xml").getPathString();
			if (gameXMLPath == null) {
				return -1;
			}
			System.out.println("Load Game XML from " + gameXMLPath);
			final FileInputStream in = new FileInputStream(gameXMLPath);

			// parse using builder to get DOM representation of the XML file
			Document dom = new GameParser().getDocument(in);

			int goToStep = parseValuesFromXML(dom);

			// set map file, image file and map folder
			MapXMLHelper.s_mapXMLFile = new File(gameXMLPath);
			File mapFolder = MapXMLHelper.s_mapXMLFile.getParentFile();
			if (mapFolder.getName().equals("games"))
				mapFolder = mapFolder.getParentFile();
			MapXMLCreator.s_mapFolderLocation = mapFolder;
			final File[] imageFiles = MapXMLCreator.s_mapFolderLocation.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File arg0, String arg1) {
					return (arg1.endsWith(".gif") || arg1.endsWith(".png"));
				}
			});
			if (imageFiles.length == 1)
				MapXMLCreator.s_mapImageFile = imageFiles[0];
			else {
				MapPropertiesPanel.selectMapImageFile();
			}

			final File fileGuess = new File(MapXMLCreator.s_mapFolderLocation, "centers.txt");
			if (fileGuess.exists()) {
				MapXMLCreator.s_mapCentersFile = fileGuess;
			} else
				MapPropertiesPanel.selectCentersFile();

			if (MapXMLCreator.s_mapImageFile == null || MapXMLCreator.s_mapCentersFile == null)
				goToStep = 1;

			ImageScrollPanePanel.s_polygonsInvalid = true;

			return goToStep;
		} catch (final FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (final IOException ex) {
			ex.printStackTrace();
		} catch (final HeadlessException ex) {
			ex.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static int parseValuesFromXML(Document dom) {
		int stepToGo = 1;
		// clear current values
		MapXMLCreator.s_mapFolderLocation = null;
		MapXMLCreator.s_mapImageFile = null;
		MapXMLCreator.s_mapCentersFile = null;
		s_xmlStrings.clear();
		s_resourceList.clear();
		s_playerName.clear();
		s_playerAlliance.clear();
		s_playerInitResources.clear();
		s_territoyDefintions.clear();
		s_territoyConnections.clear();
		s_gamePlaySequence.clear();
		s_playerSequence.clear();
		s_productionFrontiers.clear();
		s_technologyDefinitions.clear();
		s_unitAttatchments.clear();
		s_territoyProductions.clear();
		s_canalDefinitions.clear();
		s_territoyOwnerships.clear();
		s_unitPlacements.clear();
		s_gameSettings.clear();
		s_notes = "";

		final Node mainlastChild = dom.getLastChild();
		if (!mainlastChild.getNodeName().equals("game")) {
			throw new IllegalArgumentException("Last node of XML document is not the expeced 'game' node, but '" + mainlastChild.getNodeName() + "'");
		}
		final NodeList children = mainlastChild.getChildNodes();
		for (int i = 0; i < children.getLength(); ++i) {
			final Node childNode = children.item(i);
			final String childNodeName = childNode.getNodeName();
			if (childNodeName.equals("info")) {
				final HashMap<String, String> infoAttr = getAttributesMap(childNode.getAttributes());
				for (final Entry<String, String> infoAttrEntry : infoAttr.entrySet())
					s_xmlStrings.put("info_@" + infoAttrEntry.getKey(), infoAttrEntry.getValue());
			} else if (childNodeName.equals("resourceList")) {
				NodeList resourceNodes = childNode.getChildNodes();

				for (int res_i = 0; res_i < resourceNodes.getLength(); ++res_i) {
					final Node resourceNode = resourceNodes.item(res_i);
					if (resourceNode.getNodeName().equals("resource"))
						s_resourceList.add(resourceNode.getAttributes().item(0).getNodeValue());
				}
			} else if (childNodeName.equals("map")) {
				parseMapNode(childNode.getChildNodes());
				stepToGo = Math.max(stepToGo, (s_territoyConnections.isEmpty() ? 2 : 3));
			} else if (childNodeName.equals("playerList")) {
				parsePlayerListNode(childNode.getChildNodes());
				stepToGo = Math.max(stepToGo, 4);
			} else if (childNodeName.equals("production")) {
				final NodeList productionChildNodes = childNode.getChildNodes();
				for (int p_i = 0; p_i < productionChildNodes.getLength(); ++p_i) {
					final Node productionRule = productionChildNodes.item(p_i);
					if (productionRule.getNodeName().equals("productionRule"))
						parseProductionRuleNode(productionRule.getChildNodes());
					else if (productionRule.getNodeName().equals("productionFrontier"))
					{
						final String playerName = productionRule.getAttributes().getNamedItem("name").getNodeValue().substring(10);
						final ArrayList<String> frontierRules = new ArrayList<String>();
						final NodeList productionFrontierChildNodes = productionRule.getChildNodes();
						for (int pr_i = 0; pr_i < productionFrontierChildNodes.getLength(); ++pr_i) {
							final Node productionFrontierChildNode = productionFrontierChildNodes.item(pr_i);
							if (productionFrontierChildNode.getNodeName().equals("frontierRules")) {
								frontierRules.add(productionFrontierChildNode.getAttributes().getNamedItem("name").getNodeValue().substring(3));
							}
						}
						s_productionFrontiers.put(playerName, frontierRules);
					}
				}
				stepToGo = Math.max(stepToGo, (s_productionFrontiers.isEmpty() ? 5 : 9));
			} else if (childNodeName.equals("gamePlay")) {
				final NodeList gamePlayChildNodes = childNode.getChildNodes();
				for (int p_i = 0; p_i < gamePlayChildNodes.getLength(); ++p_i) {
					final Node gamePlayChildNode = gamePlayChildNodes.item(p_i);
					if (gamePlayChildNode.getNodeName().equals("delegate")) {
						final HashMap<String, String> attrDelegate = getAttributesMap(gamePlayChildNode.getAttributes());
						final ArrayList<String> newValues = new ArrayList<String>();
						newValues.add(attrDelegate.get("javaClass").replace(TRIPLEA_JAVA_CLASS_DELEGATE_PATH, ""));
						newValues.add(attrDelegate.get("display"));
						s_gamePlaySequence.put(attrDelegate.get("name"), newValues);
					} else if (gamePlayChildNode.getNodeName().equals("sequence")) {
						final NodeList sequenceChildNodes = gamePlayChildNode.getChildNodes();
						for (int seq_i = 0; seq_i < sequenceChildNodes.getLength(); ++seq_i) {
							final Node sequenceChildNode = sequenceChildNodes.item(seq_i);
							if (sequenceChildNode.getNodeName().equals("step")) {
								final HashMap<String, String> attrSequence = getAttributesMap(sequenceChildNode.getAttributes());
								final String maxRunCount = attrSequence.get("maxRunCount");
								final String player = attrSequence.get("player");
								final Triple<String, String, Integer> newValues = new Triple<String, String, Integer>(attrSequence.get("delegate"), (player==null?"":player), (maxRunCount == null ? 0 : Integer.parseInt(maxRunCount)));
								s_playerSequence.put(attrSequence.get("name"), newValues);
							}
						}
					}
				}
				stepToGo = Math.max(stepToGo, (s_playerSequence.isEmpty() ? 6 : 7));
			}
			else if (childNodeName.equals("attatchmentList"))
			{
				final NodeList attatchmentListChildNodes = childNode.getChildNodes();
				for (int p_i = 0; p_i < attatchmentListChildNodes.getLength(); ++p_i)
				{
					final Node attatchment = attatchmentListChildNodes.item(p_i);
					if (attatchment.getNodeName().equals("attatchment"))
						parseAttatchmentNode(attatchment);
				}
				stepToGo = Math.max(Math.max(stepToGo, s_unitAttatchments.isEmpty() ? 8 : 10), s_territoyProductions.isEmpty() ? 0 : 11);
			}
			else if (childNodeName.equals("initialize"))
			{
				final NodeList initializeChildNodes = childNode.getChildNodes();
				for (int init_i = 0; init_i < initializeChildNodes.getLength(); ++init_i)
				{
					final Node ownerInitialize = initializeChildNodes.item(init_i);
					if (ownerInitialize.getNodeName().equals("ownerInitialize"))
					{
						final NodeList initializeOwnerChildNodes = ownerInitialize.getChildNodes();
						for (int initOwner_i = 0; initOwner_i < initializeOwnerChildNodes.getLength(); ++initOwner_i)
						{
							final Node territoryOwner = initializeOwnerChildNodes.item(initOwner_i);
							if (territoryOwner.getNodeName().equals("territoryOwner"))
							{
								final HashMap<String, String> attrTerrOwner = getAttributesMap(territoryOwner.getAttributes());
								s_territoyOwnerships.put(attrTerrOwner.get("territory"), attrTerrOwner.get("owner"));
							}
						}
					} else if (ownerInitialize.getNodeName().equals("unitInitialize"))
					{
						final NodeList initializeUnitChildNodes = ownerInitialize.getChildNodes();
						for (int initOwner_i = 0; initOwner_i < initializeUnitChildNodes.getLength(); ++initOwner_i)
						{
							final Node unitPlacement = initializeUnitChildNodes.item(initOwner_i);
							if (unitPlacement.getNodeName().equals("unitPlacement"))
							{
								final HashMap<String, String> attrUnitPlacements = getAttributesMap(unitPlacement.getAttributes());
								final String territory = attrUnitPlacements.get("territory");
								final String owner = attrUnitPlacements.get("owner");
								HashMap<String, LinkedHashMap<String, Integer>> terrPlacements = s_unitPlacements.get(territory);
								if (terrPlacements == null)
								{
									terrPlacements = new HashMap<String, LinkedHashMap<String, Integer>>();
									s_unitPlacements.put(territory, terrPlacements);
								}
								LinkedHashMap<String, Integer> terrOwnerPlacements = terrPlacements.get(owner);
								if (terrOwnerPlacements == null)
								{
									terrOwnerPlacements = new LinkedHashMap<String, Integer>();
									terrPlacements.put(owner, terrOwnerPlacements);
								}
								terrOwnerPlacements.put(attrUnitPlacements.get("unitType"), Integer.parseInt(attrUnitPlacements.get("quantity")));
							}
						}
					}
				}
				stepToGo = Math.max(stepToGo, (s_unitPlacements.isEmpty() ? 13 : 14));
			}
			else if (childNodeName.equals("propertyList"))
			{
				final NodeList propertyListChildNodes = childNode.getChildNodes();
				for (int prop_i = 0; prop_i < propertyListChildNodes.getLength(); ++prop_i)
				{
					final Node property = propertyListChildNodes.item(prop_i);
					if (property.getNodeName().equals("property"))
						parsePropertyNode(property);
				}
				if (!s_gameSettings.isEmpty())
					stepToGo = (s_notes.length() > 0 ? 16 : 15);
			}
		}
		return stepToGo;
	}

	private static void parsePropertyNode(final Node property)
	{
		final HashMap<String, String> propertyAttr = getAttributesMap(property.getAttributes());
		final ArrayList<String> settingValues = new ArrayList<String>();
		final String propertyName = propertyAttr.get("name");
		if (propertyName.equals("notes") || propertyName.equals("mapName"))
		{
			final NodeList propertyListChildNodes = property.getChildNodes();
			for (int prop_i = 0; prop_i < propertyListChildNodes.getLength(); ++prop_i)
			{
				final Node subProperty = propertyListChildNodes.item(prop_i);
				if (subProperty.getNodeName().equals("value"))
					s_notes = subProperty.getTextContent();
			}
			return;
		}
		settingValues.add(propertyAttr.get("value"));
		settingValues.add(Boolean.toString(Boolean.parseBoolean(propertyAttr.get("editable"))));
		final NodeList propertyNodes = property.getChildNodes();
		for (int pr_i = 0; pr_i < propertyNodes.getLength(); ++pr_i)
		{
			final Node propertyRange = propertyNodes.item(pr_i);
			if (propertyRange.getNodeName().equals("number"))
			{
				final HashMap<String, String> propertyRangeAttr = getAttributesMap(propertyRange.getAttributes());
				settingValues.add(propertyRangeAttr.get("min"));
				settingValues.add(propertyRangeAttr.get("max"));
				s_gameSettings.put(propertyName, settingValues);
				break;
			}
			else if (propertyRange.getNodeName().equals("boolean"))
			{
				settingValues.add("0"); // min
				settingValues.add("0"); // max
				s_gameSettings.put(propertyName, settingValues);
				break;
			}
		}
	}

	private static void parseAttatchmentNode(final Node attatchment) {
		final HashMap<String, String> attatchmentAttr = getAttributesMap(attatchment.getAttributes());
		final String attatachmentName = attatchmentAttr.get("name");
		final String attatachmentType = attatchmentAttr.get("type");
		final String attatachmentAttatchTo = attatchmentAttr.get("attatchTo");
		if (attatachmentName.equals("techAttatchment") && attatachmentType.equals("player"))
		{
			final NodeList attatchmentOptionNodes = attatchment.getChildNodes();
			for (int pr_i = 0; pr_i < attatchmentOptionNodes.getLength(); ++pr_i) {
				final Node attatchmentOption = attatchmentOptionNodes.item(pr_i);
				if (attatchmentOption.getNodeName().equals("option")) {
					final HashMap<String, String> attatchmentOptionAttr = getAttributesMap(attatchmentOption.getAttributes());
					final ArrayList<String> values = new ArrayList<String>();
					values.add(attatachmentAttatchTo); //playerName
					values.add(attatchmentOptionAttr.get("value"));
					s_technologyDefinitions.put(attatchmentOptionAttr.get("name")+"_"+attatachmentAttatchTo, values);
				}
			}
		}
		else if (attatachmentName.equals("unitAttatchment") && attatachmentType.equals("unitType"))
			{
				final NodeList attatchmentOptionNodes = attatchment.getChildNodes();
				for (int pr_i = 0; pr_i < attatchmentOptionNodes.getLength(); ++pr_i) {
					final Node attatchmentOption = attatchmentOptionNodes.item(pr_i);
					if (attatchmentOption.getNodeName().equals("option")) {
						final HashMap<String, String> attatchmentOptionAttr = getAttributesMap(attatchmentOption.getAttributes());
						final ArrayList<String> values = new ArrayList<String>();
					values.add(attatachmentAttatchTo); // unitName
						values.add(attatchmentOptionAttr.get("value"));
						s_unitAttatchments.put(attatchmentOptionAttr.get("name")+"_"+attatachmentAttatchTo, values);
					}
				}
			}
		else if (attatachmentName.equals("canalAttatchment") && attatachmentType.equals("territory"))
		{
			final NodeList attatchmentOptionNodes = attatchment.getChildNodes();
			
			Tuple<SortedSet<String>, SortedSet<String>> canalDef = null;
			String newCanalName = null;
			SortedSet<String> newLandTerritories = new TreeSet<String>();
			for (int pr_i = 0; pr_i < attatchmentOptionNodes.getLength(); ++pr_i)
			{
				final Node attatchmentOption = attatchmentOptionNodes.item(pr_i);
				if (attatchmentOption.getNodeName().equals("option"))
				{
					final HashMap<String, String> attatchmentOptionAttr = getAttributesMap(attatchmentOption.getAttributes());
					final String attatOptAttrName = attatchmentOptionAttr.get("name");
					if (attatOptAttrName.equals("canalName"))
					{
						newCanalName = attatchmentOptionAttr.get("value");
						canalDef = s_canalDefinitions.get(newCanalName);
						if (canalDef != null)
							break;
					}
					else if (attatOptAttrName.equals("landTerritories"))
					{
						newLandTerritories.addAll(Arrays.asList(attatchmentOptionAttr.get("value").split(":")));
					}
				}
			}
			if (canalDef == null)
			{
				final SortedSet<String> newWaterTerritories = new TreeSet<String>();
				newWaterTerritories.add(attatachmentAttatchTo);
				s_canalDefinitions.put(newCanalName, new Tuple<SortedSet<String>, SortedSet<String>>(newWaterTerritories, newLandTerritories));
			}
			else
				canalDef.getFirst().add(attatachmentAttatchTo);
		}
		else if (attatachmentName.equals("territoryAttatchment") && attatachmentType.equals("territory"))
			{
				final NodeList attatchmentOptionNodes = attatchment.getChildNodes();
				for (int pr_i = 0; pr_i < attatchmentOptionNodes.getLength(); ++pr_i) {
					final Node attatchmentOption = attatchmentOptionNodes.item(pr_i);
					if (attatchmentOption.getNodeName().equals("option")) {
						final HashMap<String, String> attatchmentOptionAttr = getAttributesMap(attatchmentOption.getAttributes());
						final String optionNameAttr = attatchmentOptionAttr.get("name");
						if (optionNameAttr.equals("production"))
							s_territoyProductions.put(attatachmentAttatchTo, Integer.parseInt(attatchmentOptionAttr.get("value")));
						else {
							HashMap<DEFINITION, Boolean> terrDefinitions = s_territoyDefintions.get(attatachmentAttatchTo);
							if (terrDefinitions == null)
							{
								terrDefinitions = new HashMap<TerritoryDefinitionDialog.DEFINITION, Boolean>();
								s_territoyDefintions.put(attatachmentAttatchTo, terrDefinitions);
							}
							switch (TerritoryDefinitionDialog.valueOf(optionNameAttr))
							{
							case IS_CAPITAL:
								terrDefinitions.put(DEFINITION.IS_CAPITAL, true);
								break;
							case IS_VICTORY_CITY:
								terrDefinitions.put(DEFINITION.IS_VICTORY_CITY, true);
								break;
							case IS_WATER:
								terrDefinitions.put(DEFINITION.IS_WATER, true);
								break;
							case IMPASSABLE:
								terrDefinitions.put(DEFINITION.IMPASSABLE, true);
								break;
							}
						}
					}
				}
			}
	}

	private static void parseProductionRuleNode(final NodeList productionRuleChildNodes) {
		HashMap<String, String> attrMapCost = null;
		HashMap<String, String> attrMapResult = null;
		for (int pr_i = 0; pr_i < productionRuleChildNodes.getLength(); ++pr_i) {
			final Node productionRuleChildNode = productionRuleChildNodes.item(pr_i);
			final String productionRuleChildNodeName = productionRuleChildNode.getNodeName();
			if (productionRuleChildNodeName.equals("cost")) {
				attrMapCost = getAttributesMap(productionRuleChildNode.getAttributes());
				if (attrMapResult != null)
					break;
			} else if (productionRuleChildNodeName.equals("result")) {
				attrMapResult = getAttributesMap(productionRuleChildNode.getAttributes());
				if (attrMapCost != null)
					break;
			}
		}
		final ArrayList<Integer> newValues = new ArrayList<Integer>();
		newValues.add(Integer.parseInt(attrMapCost.get("quantity")));
		newValues.add(Integer.parseInt(attrMapResult.get("quantity")));
		MapXMLHelper.s_unitDefinitions.put(attrMapResult.get("resourceOrUnit"), newValues);
	}

	private static HashMap<String, String> getAttributesMap(final NamedNodeMap attrNodeMap) {
		final HashMap<String, String> rVal = new HashMap<String, String>();
		for (int i = 0; i < attrNodeMap.getLength(); ++i) {
			final Node attrNodeItem = attrNodeMap.item(i);
			rVal.put(attrNodeItem.getNodeName(), attrNodeItem.getNodeValue());
		}
		return rVal;
	}

	private static void parsePlayerListNode(final NodeList playerListChildNodes) {
		for (int pl_i = 0; pl_i < playerListChildNodes.getLength(); ++pl_i) {
			final Node playerListChildNode = playerListChildNodes.item(pl_i);
			final String playerListChildNodeName = playerListChildNode.getNodeName();
			if (playerListChildNodeName.equals("player")) {
				final HashMap<String, String> attrMapPlayer = getAttributesMap(playerListChildNode.getAttributes());

				final String playerName = attrMapPlayer.get("name");
				s_playerName.add(playerName);
				s_playerInitResources.put(playerName, 0);
				// TODO: add logic for optional value
				// attrMapPlayer.get("optional")
			} else if (playerListChildNodeName.equals("alliance")) {
				final HashMap<String, String> attrMapPlayer = getAttributesMap(playerListChildNode.getAttributes());
				s_playerAlliance.put(attrMapPlayer.get("player"), attrMapPlayer.get("alliance"));
			}
		}
	}

	private static void parseMapNode(NodeList mapChildNodes) {
		for (int map_i = 0; map_i < mapChildNodes.getLength(); ++map_i) {
			final Node mapChildNode = mapChildNodes.item(map_i);
			final String mapChildNodeName = mapChildNode.getNodeName();
			if (mapChildNodeName.equals("territory")) {
				final NamedNodeMap terrAttrNodes = mapChildNode.getAttributes();
				String terrName = null;
				final HashMap<DEFINITION, Boolean> terrDef = new HashMap<TerritoryDefinitionDialog.DEFINITION, Boolean>();
				for (int terrAttr_i = 0; terrAttr_i < terrAttrNodes.getLength(); ++terrAttr_i) {
					final Node terrAttrNode = terrAttrNodes.item(terrAttr_i);
					if (terrAttrNode.getNodeName().equals("name")) {
						terrName = terrAttrNode.getNodeValue();
					} else {
						terrDef.put(TerritoryDefinitionDialog.valueOf(terrAttrNode.getNodeName()), Boolean.valueOf(terrAttrNode.getNodeValue()));
					}
				}
				s_territoyDefintions.put(terrName, terrDef);
			} else if (mapChildNodeName.equals("connection")) {
				final NamedNodeMap connectionAttrNodes = mapChildNode.getAttributes();
				String t1Name = connectionAttrNodes.item(0).getNodeValue();
				String t2Name = connectionAttrNodes.item(1).getNodeValue();
				if (t1Name.compareTo(t2Name) > 0) {
					final String swapHelper = t1Name;
					t1Name = t2Name;
					t2Name = swapHelper;
				}
				LinkedHashSet<String> t1Connections = s_territoyConnections.get(t1Name);
				if (t1Connections != null)
					t1Connections.add(t2Name);
				else {
					t1Connections = new LinkedHashSet<String>();
					t1Connections.add(t2Name);
					s_territoyConnections.put(t1Name, t1Connections);
				}
			}
		}
	}

	static void saveXML() {
		try {
			final String fileName = new FileSave("Where to Save the Game XML ?", s_xmlStrings.get("info_@name") + ".xml", MapXMLCreator.s_mapFolderLocation).getPathString();
			if (fileName == null) {
				return;
			}

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			// transformerFactory.setAttribute("indent-number", new Integer(2));
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "game.dtd");
			DOMSource source = new DOMSource(getXMLDocument());
			final File newFile = new File(fileName);
			StreamResult result = new StreamResult(newFile);

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);

			System.out.println("");
			System.out.println("Game XML written to " + newFile.getCanonicalPath());
		} catch (final FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();

		} catch (final HeadlessException ex) {
			ex.printStackTrace();
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}

	private static Document getXMLDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document doc = db.newDocument();
		Element game = doc.createElement("game");
		doc.appendChild(game);

		Element currentElem = null;
		String prevKeyNode = null;
		for (final Entry<String, String> entryXMLString : s_xmlStrings.entrySet()) {
			String key = entryXMLString.getKey();
			String[] split = key.split("_@");
			if (!split[0].equals(prevKeyNode)) {
				Element parentElem = game;
				for (String newNode : split[0].split("_")) {
					currentElem = doc.createElement(newNode);
					parentElem.appendChild(currentElem);
					parentElem = currentElem;
				}
			}
			currentElem.setAttribute(split[1], entryXMLString.getValue());
			prevKeyNode = split[0];
		}

		Element map = doc.createElement("map");
		game.appendChild(map);

		boolean territoryAttatchmentNeeded = false;

		map.appendChild(doc.createComment(" Territory Definitions "));
		for (final Entry<String, HashMap<DEFINITION, Boolean>> entryTerritoryDefinition : s_territoyDefintions.entrySet()) {
			Element territory = doc.createElement("territory");
			territory.setAttribute("name", entryTerritoryDefinition.getKey());
			final HashMap<DEFINITION, Boolean> territoryDefinition = entryTerritoryDefinition.getValue();
			final int territoryDefinitionSize = territoryDefinition.size();
			final Boolean isWater = territoryDefinition.get(DEFINITION.IS_WATER);
			if (isWater != null && isWater == Boolean.TRUE)
			{
				territory.setAttribute(TerritoryDefinitionDialog.getDefinitionString(DEFINITION.IS_WATER), "true");
				if (territoryDefinitionSize > 1)
					territoryAttatchmentNeeded = true;
			}
			else if (territoryDefinitionSize > 1 || isWater == null && territoryDefinitionSize > 0)
				territoryAttatchmentNeeded = true;
			map.appendChild(territory);
		}

		map.appendChild(doc.createComment(" Territory Connections "));
		for (final Entry<String, LinkedHashSet<String>> entryTerritoryConnection : s_territoyConnections.entrySet()) {
			final Element connectionTemp = doc.createElement("connection");
			connectionTemp.setAttribute("t1", entryTerritoryConnection.getKey());
			for (final String t2 : entryTerritoryConnection.getValue()) {
				connectionTemp.setAttribute("t2", t2);
				map.appendChild(connectionTemp.cloneNode(false));
			}
		}

		if (!s_resourceList.isEmpty()) {
			Element resourceList = doc.createElement("resourceList");
			game.appendChild(resourceList);
			for (final String resourceName : s_resourceList) {
				final Element resource = doc.createElement("resource");
				resource.setAttribute("name", resourceName);
				resourceList.appendChild(resource);
			}
		}

		if (!s_playerName.isEmpty()) {
			Element playerList = doc.createElement("playerList");
			game.appendChild(playerList);
			playerList.appendChild(doc.createComment(" In Turn Order "));
			for (final String playerName : s_playerName) {
				final Element player = doc.createElement("player");
				player.setAttribute("name", playerName);
				player.setAttribute("optional", "false");
				playerList.appendChild(player);
			}

			final HashMap<String, ArrayList<String>> alliances = new HashMap<String, ArrayList<String>>();
			for (final Entry<String, String> allianceEntry : s_playerAlliance.entrySet()) {
				final String allianceName = allianceEntry.getValue();
				ArrayList<String> players = alliances.get(allianceName);
				if (players == null) {
					players = new ArrayList<String>();
					alliances.put(allianceName, players);
				}
				players.add(allianceEntry.getKey());
			}
			for (final Entry<String, ArrayList<String>> allianceEntry : alliances.entrySet()) {
				final String allianceName = allianceEntry.getKey();
				playerList.appendChild(doc.createComment(" " + allianceName + " Alliance "));
				for (final String playerName : allianceEntry.getValue()) {
					final Element player = doc.createElement("alliance");
					player.setAttribute("player", playerName);
					player.setAttribute("alliance", allianceName);
					playerList.appendChild(player);
				}
			}
		}

		if (!s_unitDefinitions.isEmpty()) {
			map.appendChild(doc.createComment(" Unit Definitions "));
			final Element unitList = doc.createElement("unitList");
			game.appendChild(unitList);

			final Element production = doc.createElement("production");
			game.appendChild(production);
			final String firstResourceName = s_resourceList.get(0);
			for (final Entry<String, ArrayList<Integer>> unitDefinition : s_unitDefinitions.entrySet()) {
				final String unitName = unitDefinition.getKey();

				final Element unit = doc.createElement("unit");
				unit.setAttribute("name", unitName);
				unitList.appendChild(unit);

				final ArrayList<Integer> definition = unitDefinition.getValue();
				final Element productionRule = doc.createElement("productionRule");
				productionRule.setAttribute("name", "buy" + unitName.substring(0, 1).toUpperCase() + unitName.substring(1));
				production.appendChild(productionRule);
				final Element prCost = doc.createElement("cost");
				prCost.setAttribute("resource", firstResourceName);
				prCost.setAttribute("quantity", definition.get(0).toString());
				productionRule.appendChild(prCost);
				final Element prResult = doc.createElement("result");
				prResult.setAttribute("resourceOrUnit", unitName);
				prResult.setAttribute("quantity", definition.get(1).toString());
				productionRule.appendChild(prResult);
			}

			for (final Entry<String, ArrayList<String>> productionFrontierEntry : s_productionFrontiers.entrySet()) {
				final String productionFrontierName = productionFrontierEntry.getKey();

				final Element productionFrontier = doc.createElement("productionFrontier");
				productionFrontier.setAttribute("name", productionFrontierName);
				production.appendChild(productionFrontier);

				for (final String frontierRuleName : productionFrontierEntry.getValue())
				{
					final Element frontierRule = doc.createElement("frontierRules");
					frontierRule.setAttribute("name", "buy" + frontierRuleName);
					productionFrontier.appendChild(frontierRule);
				}
			}
		}

		if (!s_gamePlaySequence.isEmpty()) {
			final Element gamePlay = doc.createElement("gamePlay");
			game.appendChild(gamePlay);

			for (final Entry<String, ArrayList<String>> delegateStep : s_gamePlaySequence.entrySet()) {
				final ArrayList<String> delegateProperties = delegateStep.getValue();
				final Element delegate = doc.createElement("delegate");
				delegate.setAttribute("name", delegateStep.getKey());
				delegate.setAttribute("javaClass", TRIPLEA_JAVA_CLASS_DELEGATE_PATH + delegateProperties.get(0));
				delegate.setAttribute("display", delegateProperties.get(1));
				gamePlay.appendChild(delegate);
			}

			if (!s_playerSequence.isEmpty()) {
				final Element sequence = doc.createElement("sequence");
				gamePlay.appendChild(sequence);

				for (final Entry<String, Triple<String, String, Integer>> sequenceStep : s_playerSequence.entrySet()) {
					final Triple<String, String, Integer> sequenceProperties = sequenceStep.getValue();
					final Element step = doc.createElement("step");
					final String sequenceStepKey = sequenceStep.getKey();
					step.setAttribute("name", sequenceStepKey);
					step.setAttribute("delegate", sequenceProperties.getFirst());
					if (sequenceProperties.getSecond().length() > 0)
						step.setAttribute("player", sequenceProperties.getSecond());
					final Integer maxRunCount = sequenceProperties.getThird();
					if (maxRunCount > 0)
						step.setAttribute("maxRunCount", maxRunCount.toString());
					if (sequenceStepKey.endsWith("NonCombatMove"))
						step.setAttribute("display", "Non Combat Move");
					sequence.appendChild(step);
				}
			}
		}
		
		if (!s_territoyProductions.isEmpty())
			territoryAttatchmentNeeded = true;

		if (!s_technologyDefinitions.isEmpty() || !s_unitAttatchments.isEmpty() || territoryAttatchmentNeeded || !s_canalDefinitions.isEmpty())
		{
			final Element attatchmentList = doc.createElement("attatchmentList");
			game.appendChild(attatchmentList);
			
			final Element attatchmentTemplate = doc.createElement("attatchment");
			if (!s_technologyDefinitions.isEmpty())
			{
				attatchmentTemplate.setAttribute("name", "techAttatchment");
				attatchmentTemplate.setAttribute("javaClass", "games.strategy.triplea.attatchments.TechAttachment");
				attatchmentTemplate.setAttribute("type", "player");
				writeAttatchmentNodes(doc, attatchmentList, s_technologyDefinitions, attatchmentTemplate);
			}
			if (!s_unitAttatchments.isEmpty())
			{
				attatchmentTemplate.setAttribute("name", "unitAttatchment");
				attatchmentTemplate.setAttribute("javaClass", "games.strategy.triplea.attatchments.UnitAttachment");
				attatchmentTemplate.setAttribute("type", "unitType");
				writeAttatchmentNodes(doc, attatchmentList, s_unitAttatchments, attatchmentTemplate);
			}
			if (territoryAttatchmentNeeded)
			{
				attatchmentTemplate.setAttribute("name", "territoryAttatchment");
				attatchmentTemplate.setAttribute("javaClass", "games.strategy.triplea.attatchments.TerritoryAttachment");
				attatchmentTemplate.setAttribute("type", "territory");
				LinkedHashMap<String, ArrayList<String>> territoryAttatchments = new LinkedHashMap<String, ArrayList<String>>();
				for (final Entry<String, HashMap<DEFINITION, Boolean>> territoryDefinition : s_territoyDefintions.entrySet())
				{
					final String territoryName = territoryDefinition.getKey();
					for (final Entry<DEFINITION, Boolean> definition : territoryDefinition.getValue().entrySet())
					{
						if (definition.getValue() == Boolean.TRUE)
						{
							final ArrayList<String> attatchmentOptions = new ArrayList<String>();
							attatchmentOptions.add(territoryName);
							attatchmentOptions.add("true");
							// TODO: handle capital different based on owner
							// owner defined (step 13) only after territory definitions (step 2)
							// <option name="capital" value="Italians"/>
							territoryAttatchments.put(TerritoryDefinitionDialog.getDefinitionString(definition.getKey()) + "_" + territoryName, attatchmentOptions);
						}
					}
				}
				for (final Entry<String, Integer> productionEntry : s_territoyProductions.entrySet())
				{
					final Integer production = productionEntry.getValue();
					if (production > 0)
					{
						final String territoryName = productionEntry.getKey();
						final ArrayList<String> attatchmentOptions = new ArrayList<String>();
						attatchmentOptions.add(territoryName);
						attatchmentOptions.add(production.toString());
						territoryAttatchments.put("production_" + territoryName, attatchmentOptions);
					}
				}
				writeAttatchmentNodes(doc, attatchmentList, territoryAttatchments, attatchmentTemplate);
			}
			if (!s_canalDefinitions.isEmpty())
			{
				attatchmentTemplate.setAttribute("name", "canalAttatchment");
				attatchmentTemplate.setAttribute("javaClass", "games.strategy.triplea.attatchments.CanalAttachment");
				attatchmentTemplate.setAttribute("type", "territory");
				for (final Entry<String, Tuple<SortedSet<String>, SortedSet<String>>> canalDefEntry : s_canalDefinitions.entrySet())
				{
					final Tuple<SortedSet<String>, SortedSet<String>> canalDef = canalDefEntry.getValue();
					Iterator<String> iter_landTerrs = canalDef.getSecond().iterator();
					final StringBuilder sb = new StringBuilder(iter_landTerrs.next());
					while (iter_landTerrs.hasNext())
						sb.append(":").append(iter_landTerrs.next());
					final String landTerritories = sb.toString();
					
					final Element canalOptionName = doc.createElement("option");
					canalOptionName.setAttribute("name", "canalName");
					canalOptionName.setAttribute("value", canalDefEntry.getKey());
					attatchmentTemplate.appendChild(canalOptionName);
					final Element canalOptionLandTerrs = doc.createElement("option");
					canalOptionLandTerrs.setAttribute("name", "landTerritories");
					canalOptionLandTerrs.setAttribute("value", landTerritories);
					attatchmentTemplate.appendChild(canalOptionLandTerrs);
					for (final String waterTerr : canalDef.getFirst())
					{
						final Element canalAttatchment = (Element) attatchmentTemplate.cloneNode(true);
						canalAttatchment.setAttribute("attatchTo", waterTerr);
						attatchmentList.appendChild(canalAttatchment);
					}
				}
				writeAttatchmentNodes(doc, attatchmentList, s_unitAttatchments, attatchmentTemplate);
			}
		}
		
		final Element initialize = doc.createElement("initialize");
		game.appendChild(initialize);
		
		if (!s_territoyOwnerships.isEmpty())
		{
			final Element ownerInitialize = doc.createElement("ownerInitialize");
			initialize.appendChild(ownerInitialize);
			final HashMap<String, ArrayList<String>> playerTerritories = new HashMap<String, ArrayList<String>>();
			for (final String player : s_playerName)
				playerTerritories.put(player, new ArrayList<String>());
			for (final Entry<String, String> ownershipEntry : s_territoyOwnerships.entrySet())
				playerTerritories.get(ownershipEntry.getValue()).add(ownershipEntry.getKey());
			for (final Entry<String, ArrayList<String>> playerTerritoriesEntry : playerTerritories.entrySet())
			{
				doc.createComment(" " + playerTerritoriesEntry.getKey() + " Owned Territories ");
				final Element territoryOwnerTemplate = doc.createElement("territoryOwner");
				territoryOwnerTemplate.setAttribute("owner", playerTerritoriesEntry.getKey());
				for (final String territory : playerTerritoriesEntry.getValue())
				{
					final Element territoryOwner = (Element) territoryOwnerTemplate.cloneNode(false);
					territoryOwner.setAttribute("territory", territory);
					ownerInitialize.appendChild(territoryOwner);
				}
			}
		}
		
		if (!s_unitPlacements.isEmpty())
		{
			final Element unitInitialize = doc.createElement("unitInitialize");
			initialize.appendChild(unitInitialize);
			for (final Entry<String, HashMap<String, LinkedHashMap<String, Integer>>> placementEntry : s_unitPlacements.entrySet())
			{
				final Element unitPlacementTemplate = doc.createElement("unitPlacement");
				unitPlacementTemplate.setAttribute("territory", placementEntry.getKey());
				for (final Entry<String, LinkedHashMap<String, Integer>> playerPlacements : placementEntry.getValue().entrySet())
				{
					final Element playerUnitPlacementTemplate = (Element) unitPlacementTemplate.cloneNode(false);
					if (playerPlacements.getKey() != null)
						playerUnitPlacementTemplate.setAttribute("owner", playerPlacements.getKey());
					for (final Entry<String, Integer> unitDetails : playerPlacements.getValue().entrySet())
					{
						if (unitDetails.getValue() > 0)
						{
							final Element unitPlacement = (Element) playerUnitPlacementTemplate.cloneNode(false);
							unitPlacement.setAttribute("unitType", unitDetails.getKey());
							unitPlacement.setAttribute("quantity", unitDetails.getValue().toString());
							unitInitialize.appendChild(unitPlacement);
						}
					}
				}
			}
		}

		final Element propertyList = doc.createElement("propertyList");
		game.appendChild(propertyList);
		
		if (!s_gameSettings.isEmpty())
		{
			for (final Entry<String, ArrayList<String>> gameSettingsEntry : s_gameSettings.entrySet())
			{
				final Element property = doc.createElement("property");
				final ArrayList<String> settingsValue = gameSettingsEntry.getValue();
				property.setAttribute("name", gameSettingsEntry.getKey());
				final String valueString = settingsValue.get(0);
				property.setAttribute("value", valueString);
				property.setAttribute("editable", settingsValue.get(1));
				if (valueString.equals("true") || valueString.equals("false"))
				{
					property.appendChild(doc.createElement("boolean"));
				}
				else
				{
					final String minString = settingsValue.get(2);
					final String maxString = settingsValue.get(3);
					try
					{
						Integer.valueOf(minString);
						Integer.valueOf(maxString);
						final Element number = doc.createElement("number");
						number.setAttribute("min", minString);
						number.setAttribute("max", maxString);
						property.appendChild(number);
					} catch (NumberFormatException nfe)
					{
						// nothing to do
					}
				}
				propertyList.appendChild(property);
			}
		}
		
		if (s_notes.length() > 0)
		{
			final Element property = doc.createElement("property");
			property.setAttribute("name", "notes");
			final Element propertyValue = doc.createElement("value");
			propertyValue.setTextContent(s_notes);
			property.appendChild(propertyValue);
			propertyList.appendChild(property);
		}

		if (s_mapXMLFile != null)
		{
			propertyList.appendChild(doc.createComment(" Map Name: also used for map utils when asked "));
			final Element property = doc.createElement("property");
			property.setAttribute("name", "mapName");
			final String fileName = s_mapXMLFile.getName();
			property.setAttribute("value", fileName.substring(0, fileName.lastIndexOf(".") - 1));
			property.setAttribute("editable", "false");
			propertyList.appendChild(property);
		}

		return doc;
	}

	protected static void writeAttatchmentNodes(Document doc, final Element attatchmentList, final LinkedHashMap<String, ArrayList<String>> hashMap, final Element attatchmentTemplate)
	{
		final HashMap<String,ArrayList<Element>> playerAttatchOptions = new HashMap<String, ArrayList<Element>>();
		for (final Entry<String, ArrayList<String>> technologyDefinition : hashMap.entrySet()) {
			final ArrayList<String> definitionValues = technologyDefinition.getValue();
			final Element option = doc.createElement("option");
			final String techKey = technologyDefinition.getKey();
			option.setAttribute("name", techKey.substring(0, techKey.lastIndexOf(definitionValues.get(0))-1));
			option.setAttribute("value", definitionValues.get(1));
			final String playerName = definitionValues.get(0);
			ArrayList<Element> elementList = playerAttatchOptions.get(playerName);
			if (elementList == null)
			{
				elementList = new ArrayList<Element>();
				playerAttatchOptions.put(playerName, elementList);
			}
			elementList.add(option);
		}
		for (final Entry<String, ArrayList<Element>> optionElementList : playerAttatchOptions.entrySet()) {
			final String playerName = optionElementList.getKey();
			final Element attatchment = (Element) attatchmentTemplate.cloneNode(false);
			attatchment.setAttribute("attatchTo", playerName);
			attatchmentList.appendChild(attatchment);
			for (final Element option : optionElementList.getValue())
				attatchment.appendChild(option);
		}
	}

	public final static String s_playerNeutral = "<Neutral>";

	public static String[] getPlayersListInclNeutral()
	{
		s_playerName.add(s_playerNeutral);
		final String[] rVal = s_playerName.toArray(new String[s_playerName.size()]);
		s_playerName.remove(s_playerNeutral);
		return rVal;
	}
}
