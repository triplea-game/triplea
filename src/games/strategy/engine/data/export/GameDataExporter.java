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
 * GameDataExporter.java
 *
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */



package games.strategy.engine.data.export;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.*;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;

import java.util.*;

import java.lang.reflect.Field;

public class GameDataExporter {
	
	private StringBuffer xmlfile;	
	

	public GameDataExporter(GameData data) {
		xmlfile = new StringBuffer();
		init(data);
		diceSides(data);
		map(data);
		resourceList(data);
		playerList(data);
		unitList(data);
		gamePlay(data);
		production(data);
		technology(data);
		attachments(data);
		initialize(data);
		propertyList(data);
		finish();
	}

	private void diceSides(GameData data) {
		int diceSides = data.getDiceSides();
		xmlfile.append("    <diceSides value=\""+diceSides+"\"/>\n");
	}

	private void technology(GameData data) {
		String technologies = technologies(data);
		String playerTechs = playertechs(data);
		if(technologies.length()>0 || playerTechs.length()>0) {
			xmlfile.append("    <technology>\n");
			xmlfile.append(technologies);
			xmlfile.append(playerTechs);
			xmlfile.append("    </technology>\n");
		}
	}

	private String playertechs(GameData data) {
		Iterator<PlayerID> players = data.getPlayerList().iterator();
		StringBuffer returnValue = new StringBuffer();
		
		while(players.hasNext()) {
			PlayerID player = players.next();
			Iterator<TechnologyFrontier> frontierList = player.getTechnologyFrontierList().getFrontiers().iterator();
			if(frontierList.hasNext()) {
				returnValue.append("        <playerTech player=\""+player.getName()+"\">\n");
				while(frontierList.hasNext()) {
					TechnologyFrontier frontier = frontierList.next();
					returnValue.append("            <category name=\""+frontier.getName()+"\">\n");
					Iterator<TechAdvance> techs = frontier.getTechs().iterator();
					while(techs.hasNext()) {
						TechAdvance tech = techs.next();
						String name = tech.getName();
						String cat = tech.getProperty();
						Iterator<TechAdvance> definedAdvances = TechAdvance.getDefinedAdvances().iterator();
						while(definedAdvances.hasNext()) {
							if(definedAdvances.next().getName().equals(name))
								name = cat;
						}
							
						returnValue.append("                <tech name=\""+name+"\"/>\n");					
					}
					returnValue.append("            </category>\n");
				}
				returnValue.append("        </playerTech>\n");
			}
		}
		return returnValue.toString();
	}

	private String technologies(GameData data) {
		Iterator<TechAdvance> techs = data.getTechnologyFrontier().getTechs().iterator();
		StringBuffer returnValue = new StringBuffer();
		if(techs.hasNext()) {
			returnValue.append("        <technologies>\n");
			while(techs.hasNext()) {
				TechAdvance tech = techs.next();
				String name = tech.getName();
				String cat = tech.getProperty();
				
				// definedAdvances are handled differently by gameparser, they are set in xml with the category as the name but
				// stored in java with the normal category and name, this causes an xml bug when exporting.
				Iterator<TechAdvance> definedAdvances = TechAdvance.getDefinedAdvances().iterator();
				while(definedAdvances.hasNext()) {
					if(definedAdvances.next().getName().equals(name))
						name = cat;
				}
				
				returnValue.append("            <techname name=\""+name+"\"");
				if(!name.equals(cat))
					returnValue.append(" tech=\""+cat+"\" ");
				returnValue.append("/>\n");
			}		
			returnValue.append("        </technologies>\n");
		}
		return returnValue.toString();
	}

	@SuppressWarnings("unchecked")
	private void propertyList(GameData data) {
		xmlfile.append("    <propertyList>\n");
		
		try {
			
			GameProperties gameProperties = data.getProperties();
			Field conPropField = GameProperties.class.getDeclaredField("m_constantProperties");
			conPropField.setAccessible(true);
			Field edPropField = GameProperties.class.getDeclaredField("m_editableProperties");
			edPropField.setAccessible(true);
			printConstantProperties((Map<String, Object>) conPropField.get(gameProperties));
			printEditableProperties((Map<String, IEditableProperty>) edPropField.get(gameProperties));
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {	} 
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		
		xmlfile.append("    </propertyList>\n");
	}

	private void printEditableProperties(Map<String, IEditableProperty> edProperties) {
		Iterator<String> propertyNames = edProperties.keySet().iterator();
		while(propertyNames.hasNext()) {
			printEditableProperty(edProperties.get(propertyNames.next()));
		}
	}

	@SuppressWarnings("unchecked")
	private void printEditableProperty(IEditableProperty prop) {
		String typeString = "";
		String value = ""+prop.getValue();
		if(prop.getClass().equals(BooleanProperty.class))
			typeString = "            <boolean/>\n";
		if(prop.getClass().equals(FileProperty.class))
			typeString = "            <file/>\n";
		if(prop.getClass().equals(StringProperty.class))
			typeString = "            <string/>\n";
		
		if(prop.getClass().equals(ColorProperty.class)) {
			typeString = "            <color/>\n";
			value = "0x" + Integer.toHexString((((Integer) prop.getValue()).intValue())).toUpperCase();
		}
		
		if(prop.getClass().equals(ListProperty.class)) {
			Field listField;
			try {
				listField = ListProperty.class.getDeclaredField("m_possibleValues");
				listField.setAccessible(true);
				Iterator<String> values = ((ArrayList<String>) listField.get(prop)).iterator();
				String possibleValues = values.next();
				while(values.hasNext()) {
					possibleValues = possibleValues + "," + values.next();
				}
				typeString = "            <list>"+possibleValues+"</list>\n";				
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
		}
		if(prop.getClass().equals(NumberProperty.class)) {
			try {
				Field maxField = NumberProperty.class.getDeclaredField("m_max");
				Field minField = NumberProperty.class.getDeclaredField("m_min");
				maxField.setAccessible(true);
				minField.setAccessible(true);
				int max = maxField.getInt(prop);
				int min = minField.getInt(prop);
				typeString = "            <number min=\""+min+"\" max=\""+max+"\"/>\n";
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
			
		}

		xmlfile.append("        <property name=\""+prop.getName()+"\" value=\""+value+"\" editable=\"true\">\n");
		xmlfile.append(typeString);
		xmlfile.append("        </property>\n");

		
	}

	private void printConstantProperties(Map<String, Object> conProperties) {
		Iterator<String> propertyNames = conProperties.keySet().iterator();
		while(propertyNames.hasNext()) {
			String propName = propertyNames.next();
			if(propName.equals("notes")) {
				// Special handling of notes property
				printNotes((String) conProperties.get(propName));
			} else if(propName.equals("EditMode") || propName.equals("GAME_UUID") || propName.equals("games.strategy.engine.framework.ServerGame.GameHasBeenSaved")) {
				// Don't print these options
			} else {
				printConstantProperty(propName,conProperties.get(propName));
			}
		}
	}

	private void printNotes(String notes) {
		xmlfile.append("        <property name=\"notes\">\n");
		xmlfile.append("            <value>\n");
		xmlfile.append("            <![CDATA[\n");
		xmlfile.append(notes);
		xmlfile.append("]]>\n");
		xmlfile.append("            </value>\n");
		xmlfile.append("        </property>\n");
	}

	private void printConstantProperty(String propName, Object property) {
		
		xmlfile.append("        <property name=\""+propName+"\" value=\""+property.toString()+"\" editable=\"false\">\n");
		if(property.getClass().equals(java.lang.String.class))
				xmlfile.append("            <string/>\n");
		if(property.getClass().equals(java.io.File.class))
			    xmlfile.append("            <file/>\n");
		if(property.getClass().equals(java.lang.Boolean.class))
		    xmlfile.append("            <boolean/>\n");

		xmlfile.append("        </property>\n");
	}

	private void initialize(GameData data) {
		xmlfile.append("    <initialize>\n");
		ownerInitialize(data);
		unitInitialize(data);
		resourceInitialize(data);
		xmlfile.append("    </initialize>\n");

	}

	private void resourceInitialize(GameData data) {
		xmlfile.append("        <resourceInitialize>\n");
		Iterator<PlayerID> players = data.getPlayerList().iterator();
		while(players.hasNext()) {
			PlayerID player = players.next();
			Iterator<Resource> resources = data.getResourceList().getResources().iterator();
			while(resources.hasNext()) {
				Resource resource = resources.next();
				if(player.getResources().getQuantity(resource.getName()) > 0)
						xmlfile.append("            <resourceGiven player=\""+player.getName()+"\" resource=\""+resource.getName()+"\" quantity=\""+player.getResources().getQuantity(resource.getName())+"\"/>\n");
			}
		}
		xmlfile.append("        </resourceInitialize>\n");
	}


	private void unitInitialize(GameData data) {
		xmlfile.append("        <unitInitialize>\n");
		Iterator<Territory> terrs = data.getMap().getTerritories().iterator();
		while(terrs.hasNext()) { 
			Territory terr = terrs.next();
			UnitCollection uc = terr.getUnits();
			Iterator<PlayerID> playersWithUnits = uc.getPlayersWithUnits().iterator();
			while(playersWithUnits.hasNext()) {
				PlayerID player = playersWithUnits.next();
				IntegerMap<UnitType> ucp = uc.getUnitsByType(player);
				Iterator<UnitType> units = ucp.keySet().iterator();
				while(units.hasNext()) {
					UnitType unit = units.next();
					if (player == null || player.getName().equals("Neutral"))
						xmlfile.append("            <unitPlacement unitType=\""+unit.getName()+"\" territory=\""+terr.getName()+"\" quantity=\""+ucp.getInt(unit)+"\"/>\n");
					else
						xmlfile.append("            <unitPlacement unitType=\""+unit.getName()+"\" territory=\""+terr.getName()+"\" quantity=\""+ucp.getInt(unit)+"\" owner=\""+player.getName()+"\"/>\n");
				}
			}
			
		}		
		xmlfile.append("        </unitInitialize>\n");			
	}


	private void ownerInitialize(GameData data) {
		xmlfile.append("        <ownerInitialize>\n");
		Iterator<Territory> terrs = data.getMap().getTerritories().iterator();
		while(terrs.hasNext()) {
			Territory terr = terrs.next();
			if(!terr.getOwner().getName().equals("Neutral"))
				xmlfile.append("            <territoryOwner territory=\""+terr.getName()+"\" owner=\""+terr.getOwner().getName()+"\"/>\n");
		}	
		xmlfile.append("        </ownerInitialize>\n");		
	}


	private void attachments(GameData data) {
		xmlfile.append("\n");
		xmlfile.append("    <attatchmentList>\n");
		Iterator<IAttachment> attachments = data.getOrderedAttachmentList().iterator();
		while(attachments.hasNext()) {
			printAttachments(attachments.next());
		}
		xmlfile.append("    </attatchmentList>\n");
	}

	private void printAttachments(IAttachment attachment) {
			try {
				IAttachmentExporter exporter = AttachmentExporterFactory.getExporter(attachment);
			 	String attachmentOptions = exporter.getAttachmentOptions(attachment);
				NamedAttachable attachTo = (NamedAttachable) attachment.getAttatchedTo();
				String type = "";
				if(attachTo.getClass().equals(PlayerID.class))
					type = "player";
				if(attachTo.getClass().equals(UnitType.class))
					type = "unitType";
				if(attachTo.getClass().equals(Territory.class))
					type = "territory";
				if(attachTo.getClass().equals(Resource.class))
					type = "resource";
				if(type.equals(""))
					throw new AttachmentExportException("no attachmentType known for "+attachTo.getClass().getCanonicalName());
				if(attachmentOptions.length()>0) { 
					xmlfile.append("        <attatchment name=\""+attachment.getName()+"\" attatchTo=\""+attachTo.getName()+"\" javaClass=\""+attachment.getClass().getCanonicalName()+"\" type=\""+type+"\">\n");
					xmlfile.append(attachmentOptions);		 				 
					xmlfile.append("        </attatchment>\n");
				}
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
	}

	private void production(GameData data) {
		xmlfile.append("\n");
		xmlfile.append("    <production>\n");
		productionRules(data);
		repairRules(data);
		repairFrontiers(data);
		productionFrontiers(data);
		playerProduction(data);
		playerRepair(data);
		xmlfile.append("    </production>\n");	
	}


	private void repairRules(GameData data) {
		//Can't get a list of productionrules from the rulelist, so using workaround through frontiers
		Iterator<String> frontiers = data.getRepairFrontierList().getRepairFrontierNames().iterator();
		HashSet<RepairRule> repairRules = new HashSet<RepairRule>();
		while (frontiers.hasNext()) {
			RepairFrontier frontier = data.getRepairFrontierList().getRepairFrontier(frontiers.next());
			repairRules.addAll(frontier.getRules());
		}
		Iterator<RepairRule> iRepairRules =  repairRules.iterator();
		while(iRepairRules.hasNext()) {
			RepairRule rr = iRepairRules.next();
			xmlfile.append("        <repairRule name=\""+rr.getName()+"\">\n");
			Iterator<Resource> costs = rr.getCosts().keySet().iterator();
			while(costs.hasNext()) {
				Resource cost = costs.next();
				xmlfile.append("            <cost resource=\""+cost.getName()+"\" quantity=\""+rr.getCosts().getInt(cost)+"\"/>\n");
			}
			Iterator<NamedAttachable> results = rr.getResults().keySet().iterator();
			while(results.hasNext()) {
				NamedAttachable result = results.next();
				xmlfile.append("            <result resourceOrUnit=\""+result.getName()+"\" quantity=\""+rr.getResults().getInt(result)+"\"/>\n");
			}
			xmlfile.append("        </repairRule>\n");
		}				
	}

	private void repairFrontiers(GameData data) {
		Iterator<String> frontiers = data.getRepairFrontierList().getRepairFrontierNames().iterator();
		while (frontiers.hasNext()) {
			RepairFrontier frontier = data.getRepairFrontierList().getRepairFrontier(frontiers.next());
			xmlfile.append("\n");	
			xmlfile.append("        <repairFrontier name=\""+frontier.getName()+"\">\n");
			Iterator<RepairRule> rules = frontier.getRules().iterator();
			while(rules.hasNext()) {
				xmlfile.append("            <repairRules name=\""+rules.next().getName()+"\"/>\n");
			}
			xmlfile.append("        </repairFrontier>\n");
		}		
		
		xmlfile.append("\n");			
	}

	private void playerRepair(GameData data) {
		Iterator<PlayerID> players = data.getPlayerList().iterator();
		while(players.hasNext()) {
			PlayerID player = players.next();
			try {
				String playerRepair = player.getRepairFrontier().getName();
				String playername = player.getName();
				xmlfile.append("        <playerRepair player=\""+playername+"\" frontier=\""+playerRepair+"\"/>\n");
			} catch (NullPointerException npe) {
				// neutral?
			}
		}			
	}

	private void playerProduction(GameData data) {
		Iterator<PlayerID> players = data.getPlayerList().iterator();
		while(players.hasNext()) {
			PlayerID player = players.next();
			try {
				String playerfrontier = player.getProductionFrontier().getName();
				String playername = player.getName();
				xmlfile.append("        <playerProduction player=\""+playername+"\" frontier=\""+playerfrontier+"\"/>\n");
			} catch (NullPointerException npe) {
				// neutral?
			}
		}				
	}

	private void productionFrontiers(GameData data) {
		Iterator<String> frontiers = data.getProductionFrontierList().getProductionFrontierNames().iterator();
		while (frontiers.hasNext()) {
			ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(frontiers.next());
			xmlfile.append("\n");	
			xmlfile.append("        <productionFrontier name=\""+frontier.getName()+"\">\n");
			Iterator<ProductionRule> rules = frontier.getRules().iterator();
			while(rules.hasNext()) {
				xmlfile.append("            <frontierRules name=\""+rules.next().getName()+"\"/>\n");
			}
			xmlfile.append("        </productionFrontier>\n");
		}		
		
		xmlfile.append("\n");	
		
	}

	private void productionRules(GameData data) {
		//Can't get a list of productionrules from the rulelist, so using workaround through frontiers
		Iterator<String> frontiers = data.getProductionFrontierList().getProductionFrontierNames().iterator();
		HashSet<ProductionRule> productionrules = new HashSet<ProductionRule>();
		while (frontiers.hasNext()) {
			ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(frontiers.next());
			productionrules.addAll(frontier.getRules());
		}
		Iterator<ProductionRule> productionRules =  productionrules.iterator();
		while(productionRules.hasNext()) {
			ProductionRule pr = productionRules.next();
			xmlfile.append("        <productionRule name=\""+pr.getName()+"\">\n");
			Iterator<Resource> costs = pr.getCosts().keySet().iterator();
			while(costs.hasNext()) {
				Resource cost = costs.next();
				xmlfile.append("            <cost resource=\""+cost.getName()+"\" quantity=\""+pr.getCosts().getInt(cost)+"\"/>\n");
			}
			Iterator<NamedAttachable> results = pr.getResults().keySet().iterator();
			while(results.hasNext()) {
				NamedAttachable result = results.next();
				xmlfile.append("            <result resourceOrUnit=\""+result.getName()+"\" quantity=\""+pr.getResults().getInt(result)+"\"/>\n");
			}
			xmlfile.append("        </productionRule>\n");
		}		
	}

	private void gamePlay(GameData data) {
		xmlfile.append("\n");	
		xmlfile.append("    <gamePlay>\n");	
        Iterator<IDelegate> delegates = data.getDelegateList().iterator();
        while(delegates.hasNext()) {
        	IDelegate delegate = delegates.next();
        	if(!delegate.getName().equals("edit"))
        		xmlfile.append("        <delegate name=\""+delegate.getName()+"\" javaClass=\""+delegate.getClass().getCanonicalName()+"\" display=\""+delegate.getDisplayName()+"\"/>\n");
        }
        sequence(data);
		xmlfile.append("    </gamePlay>\n");	

	}


	private void sequence(GameData data) {
		xmlfile.append("\n");	
		xmlfile.append("        <sequence>\n");	
		Iterator<GameStep> steps = data.getSequence().iterator();
		while(steps.hasNext()) {
			GameStep step = steps.next();
			try {
				Field mDelegateField = GameStep.class.getDeclaredField("m_delegate");
				mDelegateField.setAccessible(true);
				String delegate = (String) mDelegateField.get(step);
			xmlfile.append("            <step name=\""+step.getName()+"\" delegate=\""+delegate+"\"");
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			if(step.getPlayerID()!=null) 
				 xmlfile.append(" player=\""+step.getPlayerID().getName()+"\"");
			if(step.getDisplayName()!=null) 
				 xmlfile.append(" display=\""+step.getDisplayName()+"\"");
			if(step.getMaxRunCount()>-1)  {
				int maxRun = step.getMaxRunCount();
				if (maxRun == 0)
					maxRun = 1;
				xmlfile.append(" maxRunCount=\""+maxRun+"\"");
			}
			xmlfile.append("/>\n");
		}
		xmlfile.append("        </sequence>\n");	
	}


	private void unitList(GameData data) {
		xmlfile.append("\n");	

		xmlfile.append("    <unitList>\n");	
		Iterator<UnitType> units = data.getUnitTypeList().iterator();
		while(units.hasNext()) {
			xmlfile.append("        <unit name=\""+units.next().getName()+"\"/>\n");
		}
		xmlfile.append("    </unitList>\n");	
	}


	private void playerList(GameData data) {
		xmlfile.append("\n");	
		xmlfile.append("    <playerList>\n");

		Iterator<PlayerID> players = data.getPlayerList().getPlayers().iterator();
		while(players.hasNext()){
			PlayerID player = players.next();
			xmlfile.append("        <player name=\""+player.getName()+"\" optional=\""+player.getOptional()+"\"/>\n");
		}
		
		Iterator<String> alliances = data.getAllianceTracker().getAlliances().iterator();
		while(alliances.hasNext()) {
			String allianceName = alliances.next();
			Iterator<PlayerID> alliedPlayers = data.getAllianceTracker().getPlayersInAlliance(allianceName).iterator();
			while(alliedPlayers.hasNext()) {
				xmlfile.append("        <alliance player=\""+alliedPlayers.next().getName()+"\" alliance=\""+allianceName+"\"/>\n");
			}
		}
		xmlfile.append("    </playerList>\n");		
	}


	private void resourceList(GameData data) {
		xmlfile.append("\n");	

		xmlfile.append("    <resourceList>\n");	
		Iterator<Resource> resources = data.getResourceList().getResources().iterator();
		while(resources.hasNext()){
			
			xmlfile.append("        <resource name=\""+resources.next().getName()+"\"/>\n");
			
		}
		xmlfile.append("    </resourceList>\n");	
	}


	private void map(GameData data) {
		xmlfile.append("\n");	

		xmlfile.append("    <map>\n");	
		xmlfile.append("        <!-- Territory Definitions -->\n");
		GameMap map = data.getMap();
        Iterator<Territory> terrs = map.getTerritories().iterator();
        while(terrs.hasNext()) {
        	Territory ter = terrs.next();
        	 xmlfile.append("        <territory name=\""+ter.getName()+"\"");
        	 if(ter.isWater())
        		 xmlfile.append(" water=\"true\"");
        	 xmlfile.append("/>\n");
        }
        
		xmlfile.append("        <!-- Territory Connections -->\n");

        terrs = map.getTerritories().iterator();
        while(terrs.hasNext()) {   
        	Territory ter = terrs.next();
        	Iterator<Territory> nbs = map.getNeighbors(ter).iterator();
        	while(nbs.hasNext()) {
        		Territory nb=nbs.next();
        		xmlfile.append("        <connection t1=\""+ter.getName()+"\" t2=\""+nb.getName()+"\"/>\n");
        	}
        }	
		xmlfile.append("    </map>\n");
	}

	private void init(GameData data) {
		xmlfile.append("<?xml version=\"1.0\"?>\n");
		xmlfile.append("<!DOCTYPE game SYSTEM \"game.dtd\">\n");
		xmlfile.append("<game>\n");
		xmlfile.append("    <info name=\""+data.getGameName()+"\" version=\""+data.getGameVersion().toString()+"\"/>\n");
		xmlfile.append("    <loader javaClass=\""+data.getGameLoader().getClass().getCanonicalName()+"\"/>\n");		
	}

	private void finish() {
		xmlfile.append("\n");	
		xmlfile.append("</game>\n");
	}

	public String getXML() {
		return xmlfile.toString();
	}
	
}
