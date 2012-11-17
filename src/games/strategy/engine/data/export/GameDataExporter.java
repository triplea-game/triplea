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
 * GameDataExporter.java
 * 
 * Created on May 29, 2011, 12:00 PM by Edwin van der Wal
 */
package games.strategy.engine.data.export;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.FileProperty;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.ComboProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.StringProperty;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class GameDataExporter
{
	private final StringBuffer xmlfile;
	
	public GameDataExporter(final GameData data, final boolean currentAttachmentObjects)
	{
		xmlfile = new StringBuffer();
		init(data);
		tripleaMinimumVersion();
		diceSides(data);
		map(data);
		resourceList(data);
		playerList(data);
		unitList(data);
		relationshipTypeList(data);
		territoryEffectList(data);
		gamePlay(data);
		production(data);
		technology(data);
		attachments(data, currentAttachmentObjects);
		initialize(data);
		propertyList(data);
		finish();
	}
	
	private void tripleaMinimumVersion()
	{
		// Since we do not keep the minimum version info in the game data, just put the current version of triplea here (since we have successfully started the map, it is basically correct)
		xmlfile.append("    <triplea minimumVersion=\"" + EngineVersion.VERSION + "\"/>\n");
	}
	
	private void diceSides(final GameData data)
	{
		final int diceSides = data.getDiceSides();
		xmlfile.append("    <diceSides value=\"" + diceSides + "\"/>\n");
	}
	
	private void technology(final GameData data)
	{
		final String technologies = technologies(data);
		final String playerTechs = playertechs(data);
		if (technologies.length() > 0 || playerTechs.length() > 0)
		{
			xmlfile.append("    <technology>\n");
			xmlfile.append(technologies);
			xmlfile.append(playerTechs);
			xmlfile.append("    </technology>\n");
		}
	}
	
	private String playertechs(final GameData data)
	{
		final Iterator<PlayerID> players = data.getPlayerList().iterator();
		final StringBuffer returnValue = new StringBuffer();
		while (players.hasNext())
		{
			final PlayerID player = players.next();
			final Iterator<TechnologyFrontier> frontierList = player.getTechnologyFrontierList().getFrontiers().iterator();
			if (frontierList.hasNext())
			{
				returnValue.append("        <playerTech player=\"" + player.getName() + "\">\n");
				while (frontierList.hasNext())
				{
					final TechnologyFrontier frontier = frontierList.next();
					returnValue.append("            <category name=\"" + frontier.getName() + "\">\n");
					final Iterator<TechAdvance> techs = frontier.getTechs().iterator();
					while (techs.hasNext())
					{
						final TechAdvance tech = techs.next();
						String name = tech.getName();
						final String cat = tech.getProperty();
						for (final String definedName : TechAdvance.s_allPreDefinedTechnologyNames)
						{
							if (definedName.equals(name))
								name = cat;
						}
						returnValue.append("                <tech name=\"" + name + "\"/>\n");
					}
					returnValue.append("            </category>\n");
				}
				returnValue.append("        </playerTech>\n");
			}
		}
		return returnValue.toString();
	}
	
	private String technologies(final GameData data)
	{
		final Iterator<TechAdvance> techs = data.getTechnologyFrontier().getTechs().iterator();
		final StringBuffer returnValue = new StringBuffer();
		if (techs.hasNext())
		{
			returnValue.append("        <technologies>\n");
			while (techs.hasNext())
			{
				final TechAdvance tech = techs.next();
				String name = tech.getName();
				final String cat = tech.getProperty();
				// definedAdvances are handled differently by gameparser, they are set in xml with the category as the name but
				// stored in java with the normal category and name, this causes an xml bug when exporting.
				for (final String definedName : TechAdvance.s_allPreDefinedTechnologyNames)
				{
					if (definedName.equals(name))
						name = cat;
				}
				returnValue.append("            <techname name=\"" + name + "\"");
				if (!name.equals(cat))
					returnValue.append(" tech=\"" + cat + "\" ");
				returnValue.append("/>\n");
			}
			returnValue.append("        </technologies>\n");
		}
		return returnValue.toString();
	}
	
	@SuppressWarnings("unchecked")
	private void propertyList(final GameData data)
	{
		xmlfile.append("    <propertyList>\n");
		try
		{
			final GameProperties gameProperties = data.getProperties();
			final Field conPropField = GameProperties.class.getDeclaredField("m_constantProperties");
			conPropField.setAccessible(true);
			final Field edPropField = GameProperties.class.getDeclaredField("m_editableProperties");
			edPropField.setAccessible(true);
			printConstantProperties((Map<String, Object>) conPropField.get(gameProperties));
			printEditableProperties((Map<String, IEditableProperty>) edPropField.get(gameProperties));
		} catch (final SecurityException e)
		{
			e.printStackTrace();
		} catch (final NoSuchFieldException e)
		{
		} catch (final IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (final IllegalAccessException e)
		{
			e.printStackTrace();
		}
		xmlfile.append("    </propertyList>\n");
	}
	
	private void printEditableProperties(final Map<String, IEditableProperty> edProperties)
	{
		final Iterator<String> propertyNames = edProperties.keySet().iterator();
		while (propertyNames.hasNext())
		{
			printEditableProperty(edProperties.get(propertyNames.next()));
		}
	}
	
	@SuppressWarnings("unchecked")
	private void printEditableProperty(final IEditableProperty prop)
	{
		String typeString = "";
		String value = "" + prop.getValue();
		if (prop.getClass().equals(BooleanProperty.class))
			typeString = "            <boolean/>\n";
		if (prop.getClass().equals(FileProperty.class))
			typeString = "            <file/>\n";
		if (prop.getClass().equals(StringProperty.class))
			typeString = "            <string/>\n";
		if (prop.getClass().equals(ColorProperty.class))
		{
			typeString = "            <color/>\n";
			value = "0x" + Integer.toHexString((((Integer) prop.getValue()).intValue())).toUpperCase();
		}
		if (prop.getClass().equals(ComboProperty.class))
		{
			Field listField;
			try
			{
				listField = ComboProperty.class.getDeclaredField("m_possibleValues");
				listField.setAccessible(true);
				final Iterator<String> values = ((ArrayList<String>) listField.get(prop)).iterator();
				String possibleValues = values.next();
				while (values.hasNext())
				{
					possibleValues = possibleValues + "," + values.next();
				}
				typeString = "            <list>" + possibleValues + "</list>\n";
			} catch (final NoSuchFieldException e)
			{
				e.printStackTrace();
			} catch (final IllegalArgumentException e)
			{
				e.printStackTrace();
			} catch (final IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
		if (prop.getClass().equals(NumberProperty.class))
		{
			try
			{
				final Field maxField = NumberProperty.class.getDeclaredField("m_max");
				final Field minField = NumberProperty.class.getDeclaredField("m_min");
				maxField.setAccessible(true);
				minField.setAccessible(true);
				final int max = maxField.getInt(prop);
				final int min = minField.getInt(prop);
				typeString = "            <number min=\"" + min + "\" max=\"" + max + "\"/>\n";
			} catch (final SecurityException e)
			{
				e.printStackTrace();
			} catch (final NoSuchFieldException e)
			{
				e.printStackTrace();
			} catch (final IllegalArgumentException e)
			{
				e.printStackTrace();
			} catch (final IllegalAccessException e)
			{
				e.printStackTrace();
			}
		}
		xmlfile.append("        <property name=\"" + prop.getName() + "\" value=\"" + value + "\" editable=\"true\">\n");
		xmlfile.append(typeString);
		xmlfile.append("        </property>\n");
	}
	
	private void printConstantProperties(final Map<String, Object> conProperties)
	{
		final Iterator<String> propertyNames = conProperties.keySet().iterator();
		while (propertyNames.hasNext())
		{
			final String propName = propertyNames.next();
			if (propName.equals("notes"))
			{
				// Special handling of notes property
				printNotes((String) conProperties.get(propName));
			}
			else if (propName.equals("EditMode") || propName.equals("GAME_UUID") || propName.equals("games.strategy.engine.framework.ServerGame.GameHasBeenSaved"))
			{
				// Don't print these options
			}
			else
			{
				printConstantProperty(propName, conProperties.get(propName));
			}
		}
	}
	
	private void printNotes(final String notes)
	{
		xmlfile.append("        <property name=\"notes\">\n");
		xmlfile.append("            <value>\n");
		xmlfile.append("            <![CDATA[\n");
		xmlfile.append(notes);
		xmlfile.append("]]>\n");
		xmlfile.append("            </value>\n");
		xmlfile.append("        </property>\n");
	}
	
	private void printConstantProperty(final String propName, final Object property)
	{
		xmlfile.append("        <property name=\"" + propName + "\" value=\"" + property.toString() + "\" editable=\"false\">\n");
		if (property.getClass().equals(java.lang.String.class))
			xmlfile.append("            <string/>\n");
		if (property.getClass().equals(java.io.File.class))
			xmlfile.append("            <file/>\n");
		if (property.getClass().equals(java.lang.Boolean.class))
			xmlfile.append("            <boolean/>\n");
		xmlfile.append("        </property>\n");
	}
	
	private void initialize(final GameData data)
	{
		xmlfile.append("    <initialize>\n");
		ownerInitialize(data);
		unitInitialize(data);
		resourceInitialize(data);
		relationshipInitialize(data);
		xmlfile.append("    </initialize>\n");
	}
	
	private void relationshipInitialize(final GameData data)
	{
		if (data.getRelationshipTypeList().getAllRelationshipTypes().size() <= 4)
			return;
		final RelationshipTracker rt = data.getRelationshipTracker();
		xmlfile.append("        <relationshipInitialize>\n");
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		final Collection<PlayerID> playersAlreadyDone = new HashSet<PlayerID>();
		for (final PlayerID p1 : players)
		{
			for (final PlayerID p2 : players)
			{
				if (p1.equals(p2) || playersAlreadyDone.contains(p2))
					continue;
				final RelationshipType type = rt.getRelationshipType(p1, p2);
				final int roundValue = rt.getRoundRelationshipWasCreated(p1, p2);
				xmlfile.append("            <relationship type=\"" + type.getName() + "\" player1=\"" + p1.getName() + "\" player2=\""
							+ p2.getName() + "\" roundValue=\"" + roundValue + "\"/>\n");
			}
			playersAlreadyDone.add(p1);
		}
		xmlfile.append("        </relationshipInitialize>\n");
	}
	
	private void resourceInitialize(final GameData data)
	{
		xmlfile.append("        <resourceInitialize>\n");
		final Iterator<PlayerID> players = data.getPlayerList().iterator();
		while (players.hasNext())
		{
			final PlayerID player = players.next();
			final Iterator<Resource> resources = data.getResourceList().getResources().iterator();
			while (resources.hasNext())
			{
				final Resource resource = resources.next();
				if (player.getResources().getQuantity(resource.getName()) > 0)
					xmlfile.append("            <resourceGiven player=\"" + player.getName() + "\" resource=\"" + resource.getName() + "\" quantity=\""
								+ player.getResources().getQuantity(resource.getName()) + "\"/>\n");
			}
		}
		xmlfile.append("        </resourceInitialize>\n");
	}
	
	private void unitInitialize(final GameData data)
	{
		xmlfile.append("        <unitInitialize>\n");
		final Iterator<Territory> terrs = data.getMap().getTerritories().iterator();
		while (terrs.hasNext())
		{
			final Territory terr = terrs.next();
			final UnitCollection uc = terr.getUnits();
			final Iterator<PlayerID> playersWithUnits = uc.getPlayersWithUnits().iterator();
			while (playersWithUnits.hasNext())
			{
				final PlayerID player = playersWithUnits.next();
				final IntegerMap<UnitType> ucp = uc.getUnitsByType(player);
				final Iterator<UnitType> units = ucp.keySet().iterator();
				while (units.hasNext())
				{
					final UnitType unit = units.next();
					if (player == null || player.getName().equals("Neutral"))
						xmlfile.append("            <unitPlacement unitType=\"" + unit.getName() + "\" territory=\"" + terr.getName() + "\" quantity=\"" + ucp.getInt(unit) + "\"/>\n");
					else
						xmlfile.append("            <unitPlacement unitType=\"" + unit.getName() + "\" territory=\"" + terr.getName() + "\" quantity=\"" + ucp.getInt(unit) + "\" owner=\""
									+ player.getName() + "\"/>\n");
				}
			}
		}
		xmlfile.append("        </unitInitialize>\n");
	}
	
	private void ownerInitialize(final GameData data)
	{
		xmlfile.append("        <ownerInitialize>\n");
		final Iterator<Territory> terrs = data.getMap().getTerritories().iterator();
		while (terrs.hasNext())
		{
			final Territory terr = terrs.next();
			if (!terr.getOwner().getName().equals("Neutral"))
				xmlfile.append("            <territoryOwner territory=\"" + terr.getName() + "\" owner=\"" + terr.getOwner().getName() + "\"/>\n");
		}
		xmlfile.append("        </ownerInitialize>\n");
	}
	
	private void attachments(final GameData data, final boolean currentAttachmentObjects)
	{
		xmlfile.append("\n");
		xmlfile.append("    <attatchmentList>\n");
		final Iterator<Tuple<IAttachment, ArrayList<Tuple<String, String>>>> attachments = data.getAttachmentOrderAndValues().iterator();
		while (attachments.hasNext())
		{
			// TODO: use a ui switch to determine if we are printing the xml as it was created, or as it stands right now (including changes to the game data)
			final Tuple<IAttachment, ArrayList<Tuple<String, String>>> current = attachments.next();
			printAttachments(current, currentAttachmentObjects);
		}
		xmlfile.append("    </attatchmentList>\n");
	}
	
	private String printAttachmentOptionsBasedOnOriginalXML(final ArrayList<Tuple<String, String>> attachmentPlusValues, final IAttachment attachment)
	{
		if (attachmentPlusValues.isEmpty())
			return "";
		final StringBuffer sb = new StringBuffer("");
		boolean alreadyHasOccupiedTerrOf = false;
		for (final Tuple<String, String> current : attachmentPlusValues)
		{
			sb.append("            <option name=\"" + current.getFirst() + "\" value=\"" + current.getSecond() + "\"/>\n");
			if (current.getFirst().equals("occupiedTerrOf"))
				alreadyHasOccupiedTerrOf = true;
		}
		// add occupiedTerrOf until we fix engine to only use originalOwner
		if (!alreadyHasOccupiedTerrOf && attachment instanceof games.strategy.triplea.attatchments.TerritoryAttachment)
		{
			final games.strategy.triplea.attatchments.TerritoryAttachment ta = (games.strategy.triplea.attatchments.TerritoryAttachment) attachment;
			if (ta.getOriginalOwner() != null)
				sb.append("            <option name=\"occupiedTerrOf\" value=\"" + ta.getOriginalOwner().getName() + "\"/>\n");
		}
		
		return sb.toString();
	}
	
	private void printAttachments(final Tuple<IAttachment, ArrayList<Tuple<String, String>>> attachmentPlusValues, final boolean currentAttachmentObjects)
	{
		try
		{
			final IAttachment attachment = attachmentPlusValues.getFirst();
			// TODO: none of the attachment exporter classes have been updated since TripleA version 1.3.2.2
			final String attachmentOptions;
			if (currentAttachmentObjects)
			{
				final IAttachmentExporter exporter = AttachmentExporterFactory.getExporter(attachment);
				attachmentOptions = exporter.getAttachmentOptions(attachment);
			}
			else
			{
				attachmentOptions = printAttachmentOptionsBasedOnOriginalXML(attachmentPlusValues.getSecond(), attachment);
			}
			final NamedAttachable attachTo = (NamedAttachable) attachment.getAttachedTo();
			// TODO: keep this list updated
			String type = "";
			if (attachTo.getClass().equals(PlayerID.class))
				type = "player";
			if (attachTo.getClass().equals(UnitType.class))
				type = "unitType";
			if (attachTo.getClass().equals(Territory.class))
				type = "territory";
			if (attachTo.getClass().equals(TerritoryEffect.class))
				type = "territoryEffect";
			if (attachTo.getClass().equals(Resource.class))
				type = "resource";
			if (attachTo.getClass().equals(RelationshipType.class))
				type = "relationship";
			if (TechAdvance.class.isAssignableFrom(attachTo.getClass()))
				type = "technology";
			if (type.equals(""))
				throw new AttachmentExportException("no attachmentType known for " + attachTo.getClass().getCanonicalName());
			if (attachmentOptions.length() > 0)
			{
				xmlfile.append("        <attatchment name=\"" + attachment.getName() + "\" attatchTo=\"" + attachTo.getName() + "\" javaClass=\"" + attachment.getClass().getCanonicalName()
							+ "\" type=\"" + type + "\">\n");
				xmlfile.append(attachmentOptions);
				xmlfile.append("        </attatchment>\n");
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void production(final GameData data)
	{
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
	
	private void repairRules(final GameData data)
	{
		final Iterator<RepairRule> iRepairRules = data.getRepairRuleList().getRepairRules().iterator();
		while (iRepairRules.hasNext())
		{
			final RepairRule rr = iRepairRules.next();
			xmlfile.append("        <repairRule name=\"" + rr.getName() + "\">\n");
			final Iterator<Resource> costs = rr.getCosts().keySet().iterator();
			while (costs.hasNext())
			{
				final Resource cost = costs.next();
				xmlfile.append("            <cost resource=\"" + cost.getName() + "\" quantity=\"" + rr.getCosts().getInt(cost) + "\"/>\n");
			}
			final Iterator<NamedAttachable> results = rr.getResults().keySet().iterator();
			while (results.hasNext())
			{
				final NamedAttachable result = results.next();
				xmlfile.append("            <result resourceOrUnit=\"" + result.getName() + "\" quantity=\"" + rr.getResults().getInt(result) + "\"/>\n");
			}
			xmlfile.append("        </repairRule>\n");
		}
	}
	
	private void repairFrontiers(final GameData data)
	{
		final Iterator<String> frontiers = data.getRepairFrontierList().getRepairFrontierNames().iterator();
		while (frontiers.hasNext())
		{
			final RepairFrontier frontier = data.getRepairFrontierList().getRepairFrontier(frontiers.next());
			xmlfile.append("\n");
			xmlfile.append("        <repairFrontier name=\"" + frontier.getName() + "\">\n");
			final Iterator<RepairRule> rules = frontier.getRules().iterator();
			while (rules.hasNext())
			{
				xmlfile.append("            <repairRules name=\"" + rules.next().getName() + "\"/>\n");
			}
			xmlfile.append("        </repairFrontier>\n");
		}
		xmlfile.append("\n");
	}
	
	private void playerRepair(final GameData data)
	{
		final Iterator<PlayerID> players = data.getPlayerList().iterator();
		while (players.hasNext())
		{
			final PlayerID player = players.next();
			try
			{
				final String playerRepair = player.getRepairFrontier().getName();
				final String playername = player.getName();
				xmlfile.append("        <playerRepair player=\"" + playername + "\" frontier=\"" + playerRepair + "\"/>\n");
			} catch (final NullPointerException npe)
			{
				// neutral?
			}
		}
	}
	
	private void playerProduction(final GameData data)
	{
		final Iterator<PlayerID> players = data.getPlayerList().iterator();
		while (players.hasNext())
		{
			final PlayerID player = players.next();
			try
			{
				final String playerfrontier = player.getProductionFrontier().getName();
				final String playername = player.getName();
				xmlfile.append("        <playerProduction player=\"" + playername + "\" frontier=\"" + playerfrontier + "\"/>\n");
			} catch (final NullPointerException npe)
			{
				// neutral?
			}
		}
	}
	
	private void productionFrontiers(final GameData data)
	{
		final Iterator<String> frontiers = data.getProductionFrontierList().getProductionFrontierNames().iterator();
		while (frontiers.hasNext())
		{
			final ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(frontiers.next());
			xmlfile.append("\n");
			xmlfile.append("        <productionFrontier name=\"" + frontier.getName() + "\">\n");
			final Iterator<ProductionRule> rules = frontier.getRules().iterator();
			while (rules.hasNext())
			{
				xmlfile.append("            <frontierRules name=\"" + rules.next().getName() + "\"/>\n");
			}
			xmlfile.append("        </productionFrontier>\n");
		}
		xmlfile.append("\n");
	}
	
	private void productionRules(final GameData data)
	{
		final Iterator<ProductionRule> productionRules = data.getProductionRuleList().getProductionRules().iterator();
		while (productionRules.hasNext())
		{
			final ProductionRule pr = productionRules.next();
			xmlfile.append("        <productionRule name=\"" + pr.getName() + "\">\n");
			final Iterator<Resource> costs = pr.getCosts().keySet().iterator();
			while (costs.hasNext())
			{
				final Resource cost = costs.next();
				xmlfile.append("            <cost resource=\"" + cost.getName() + "\" quantity=\"" + pr.getCosts().getInt(cost) + "\"/>\n");
			}
			final Iterator<NamedAttachable> results = pr.getResults().keySet().iterator();
			while (results.hasNext())
			{
				final NamedAttachable result = results.next();
				xmlfile.append("            <result resourceOrUnit=\"" + result.getName() + "\" quantity=\"" + pr.getResults().getInt(result) + "\"/>\n");
			}
			xmlfile.append("        </productionRule>\n");
		}
	}
	
	private void gamePlay(final GameData data)
	{
		xmlfile.append("\n");
		xmlfile.append("    <gamePlay>\n");
		final Iterator<IDelegate> delegates = data.getDelegateList().iterator();
		while (delegates.hasNext())
		{
			final IDelegate delegate = delegates.next();
			if (!delegate.getName().equals("edit"))
				xmlfile.append("        <delegate name=\"" + delegate.getName() + "\" javaClass=\"" + delegate.getClass().getCanonicalName() + "\" display=\"" + delegate.getDisplayName() + "\"/>\n");
		}
		sequence(data);
		xmlfile.append("    </gamePlay>\n");
	}
	
	private void sequence(final GameData data)
	{
		xmlfile.append("\n");
		xmlfile.append("        <sequence>\n");
		final Iterator<GameStep> steps = data.getSequence().iterator();
		while (steps.hasNext())
		{
			final GameStep step = steps.next();
			try
			{
				final Field mDelegateField = GameStep.class.getDeclaredField("m_delegate");
				mDelegateField.setAccessible(true);
				final String delegate = (String) mDelegateField.get(step);
				xmlfile.append("            <step name=\"" + step.getName() + "\" delegate=\"" + delegate + "\"");
			} catch (final NullPointerException npe)
			{
				npe.printStackTrace();
			} catch (final NoSuchFieldException e)
			{
				e.printStackTrace();
			} catch (final IllegalArgumentException e)
			{
				e.printStackTrace();
			} catch (final IllegalAccessException e)
			{
				e.printStackTrace();
			}
			if (step.getPlayerID() != null)
				xmlfile.append(" player=\"" + step.getPlayerID().getName() + "\"");
			if (step.getDisplayName() != null)
				xmlfile.append(" display=\"" + step.getDisplayName() + "\"");
			if (step.getMaxRunCount() > -1)
			{
				int maxRun = step.getMaxRunCount();
				if (maxRun == 0)
					maxRun = 1;
				xmlfile.append(" maxRunCount=\"" + maxRun + "\"");
			}
			xmlfile.append("/>\n");
		}
		xmlfile.append("        </sequence>\n");
	}
	
	private void unitList(final GameData data)
	{
		xmlfile.append("\n");
		xmlfile.append("    <unitList>\n");
		final Iterator<UnitType> units = data.getUnitTypeList().iterator();
		while (units.hasNext())
		{
			xmlfile.append("        <unit name=\"" + units.next().getName() + "\"/>\n");
		}
		xmlfile.append("    </unitList>\n");
	}
	
	private void playerList(final GameData data)
	{
		xmlfile.append("\n");
		xmlfile.append("    <playerList>\n");
		final Iterator<PlayerID> players = data.getPlayerList().getPlayers().iterator();
		while (players.hasNext())
		{
			final PlayerID player = players.next();
			xmlfile.append("        <player name=\"" + player.getName() + "\" optional=\"" + player.getOptional() + "\"/>\n");
		}
		final Iterator<String> alliances = data.getAllianceTracker().getAlliances().iterator();
		while (alliances.hasNext())
		{
			final String allianceName = alliances.next();
			final Iterator<PlayerID> alliedPlayers = data.getAllianceTracker().getPlayersInAlliance(allianceName).iterator();
			while (alliedPlayers.hasNext())
			{
				xmlfile.append("        <alliance player=\"" + alliedPlayers.next().getName() + "\" alliance=\"" + allianceName + "\"/>\n");
			}
		}
		xmlfile.append("    </playerList>\n");
	}
	
	private void relationshipTypeList(final GameData data)
	{
		final Collection<RelationshipType> types = data.getRelationshipTypeList().getAllRelationshipTypes();
		if (types.size() <= 4)
			return;
		xmlfile.append("\n");
		xmlfile.append("    <relationshipTypes>\n");
		final Iterator<RelationshipType> iter = types.iterator();
		while (iter.hasNext())
		{
			final RelationshipType current = iter.next();
			final String name = current.getName();
			if (name.equals(Constants.RELATIONSHIP_TYPE_SELF) || name.equals(Constants.RELATIONSHIP_TYPE_NULL)
						|| name.equals(Constants.RELATIONSHIP_TYPE_DEFAULT_WAR) || name.equals(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED))
				continue;
			xmlfile.append("        <relationshipType name=\"" + name + "\"/>\n");
		}
		xmlfile.append("    </relationshipTypes>\n");
	}
	
	private void territoryEffectList(final GameData data)
	{
		final Collection<TerritoryEffect> types = data.getTerritoryEffectList().values();
		if (types.isEmpty())
			return;
		xmlfile.append("\n");
		xmlfile.append("    <territoryEffectList>\n");
		final Iterator<TerritoryEffect> iter = types.iterator();
		while (iter.hasNext())
		{
			final TerritoryEffect current = iter.next();
			xmlfile.append("        <territoryEffect name=\"" + current.getName() + "\"/>\n");
		}
		xmlfile.append("    </territoryEffectList>\n");
	}
	
	private void resourceList(final GameData data)
	{
		xmlfile.append("\n");
		xmlfile.append("    <resourceList>\n");
		final Iterator<Resource> resources = data.getResourceList().getResources().iterator();
		while (resources.hasNext())
		{
			xmlfile.append("        <resource name=\"" + resources.next().getName() + "\"/>\n");
		}
		xmlfile.append("    </resourceList>\n");
	}
	
	private void map(final GameData data)
	{
		xmlfile.append("\n");
		xmlfile.append("    <map>\n");
		xmlfile.append("        <!-- Territory Definitions -->\n");
		final GameMap map = data.getMap();
		final Iterator<Territory> terrs = map.getTerritories().iterator();
		while (terrs.hasNext())
		{
			final Territory ter = terrs.next();
			xmlfile.append("        <territory name=\"" + ter.getName() + "\"");
			if (ter.isWater())
				xmlfile.append(" water=\"true\"");
			xmlfile.append("/>\n");
		}
		connections(data);
		xmlfile.append("    </map>\n");
	}
	
	
	private class Connection
	{
		private final Territory _t1;
		private final Territory _t2;
		
		private Connection(final Territory t1, final Territory t2)
		{
			_t1 = t1;
			_t2 = t2;
		}
		
		@Override
		public int hashCode()
		{
			return _t1.hashCode() + _t2.hashCode();
		}
		
		@Override
		public boolean equals(final Object o)
		{
			if (o == null)
				return false;
			final Connection con = (Connection) o;
			return (_t1 == con._t1 && _t2 == con._t2);
		}
	}
	
	private void connections(final GameData data)
	{
		xmlfile.append("        <!-- Territory Connections -->\n");
		final GameMap map = data.getMap();
		final ArrayList<Connection> reverseConnectionTracker = new ArrayList<Connection>();
		final Iterator<Territory> terrs = map.getTerritories().iterator();
		while (terrs.hasNext())
		{
			final Territory ter = terrs.next();
			final Iterator<Territory> nbs = map.getNeighbors(ter).iterator();
			while (nbs.hasNext())
			{
				final Territory nb = nbs.next();
				if (!reverseConnectionTracker.contains(new Connection(ter, nb)))
				{
					xmlfile.append("        <connection t1=\"" + ter.getName() + "\" t2=\"" + nb.getName() + "\"/>\n");
					reverseConnectionTracker.add(new Connection(nb, ter));
				}
			}
		}
	}
	
	private void init(final GameData data)
	{
		xmlfile.append("<?xml version=\"1.0\"?>\n");
		xmlfile.append("<!DOCTYPE game SYSTEM \"game.dtd\">\n");
		xmlfile.append("<game>\n");
		xmlfile.append("    <info name=\"" + data.getGameName() + "\" version=\"" + data.getGameVersion().toString() + "\"/>\n");
		xmlfile.append("    <loader javaClass=\"" + data.getGameLoader().getClass().getCanonicalName() + "\"/>\n");
	}
	
	private void finish()
	{
		xmlfile.append("\n");
		xmlfile.append("</game>\n");
	}
	
	public String getXML()
	{
		return xmlfile.toString();
	}
}
