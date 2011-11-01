package games.strategy.triplea.attatchments;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author SquidDaddy and Veqryn [Mark Christopher Duncan]
 * 
 */
public class TriggerAttachment extends DefaultAttachment
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3327739180569606093L;
	
	public static final String AFTER = "after";
	public static final String BEFORE = "before";
	
	private List<RulesAttachment> m_trigger = null;
	private ProductionFrontier m_frontier = null;
	private List<String> m_productionRule = null;
	private boolean m_invert = false;
	private List<TechAdvance> m_tech = new ArrayList<TechAdvance>();
	private Map<Territory, IntegerMap<UnitType>> m_placement = null;
	private IntegerMap<UnitType> m_purchase = null;
	private String m_resource = null;
	private int m_resourceCount = 0;
	private int m_uses = -1;
	private boolean m_usedThisRound = false;
	private Map<UnitSupportAttachment, Boolean> m_support = null;
	// List of relationshipChanges that should be executed when this trigger hits.
	private List<String> m_relationshipChange = new ArrayList<String>();
	
	private Map<String, Map<TechAdvance, Boolean>> m_availableTech = null;
	private String m_victory = null;
	private String m_conditionType = "AND";
	private String m_notification = null;
	private Tuple<String, String> m_when = null;
	
	// raw property changes below:
	
	private List<UnitType> m_unitType = new ArrayList<UnitType>(); // really m_unitTypes
	private Tuple<String, String> m_unitAttachmentName = null; // covers UnitAttachment, UnitSupportAttachment
	private List<Tuple<String, String>> m_unitProperty = null;
	
	private List<Territory> m_territories = new ArrayList<Territory>();
	private Tuple<String, String> m_territoryAttachmentName = null; // covers TerritoryAttachment, CanalAttachment
	private List<Tuple<String, String>> m_territoryProperty = null;
	
	private List<PlayerID> m_players = new ArrayList<PlayerID>();
	private Tuple<String, String> m_playerAttachmentName = null; // covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment
	private List<Tuple<String, String>> m_playerProperty = null;
	
	private List<RelationshipType> m_relationshipTypes = new ArrayList<RelationshipType>();
	private Tuple<String, String> m_relationshipTypeAttachmentName = null; // covers RelationshipTypeAttachment
	private List<Tuple<String, String>> m_relationshipTypeProperty = null;
	
	private List<TerritoryEffect> m_territoryEffects = new ArrayList<TerritoryEffect>();
	private Tuple<String, String> m_territoryEffectAttachmentName = null; // covers TerritoryEffectAttachment
	private List<Tuple<String, String>> m_territoryEffectProperty = null;
	
	public TriggerAttachment()
	{
	}
	
	/**
	 * Convenience method.
	 * 
	 * @param player
	 * @param nameOfAttachment
	 * @return
	 */
	public static TriggerAttachment get(PlayerID player, String nameOfAttachment)
	{
		TriggerAttachment rVal = (TriggerAttachment) player.getAttachment(nameOfAttachment);
		if (rVal == null)
			throw new IllegalStateException("Triggers: No trigger attachment for:" + player.getName() + " with name: " + nameOfAttachment);
		return rVal;
	}
	
	/**
	 * If you use null for the match condition, you will get all triggers for this player.
	 * 
	 * @param player
	 * @param data
	 * @param cond
	 * @return
	 */
	public static Set<TriggerAttachment> getTriggers(PlayerID player, GameData data, Match<TriggerAttachment> cond)
	{
		Set<TriggerAttachment> trigs = new HashSet<TriggerAttachment>();
		Map<String, IAttachment> map = player.getAttachments();
		Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext())
		{
			IAttachment a = map.get(iter.next());
			if (a instanceof TriggerAttachment)
			{
				if (cond == null || cond.match((TriggerAttachment) a))
					trigs.add((TriggerAttachment) a);
			}
		}
		return trigs;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param triggers
	 * @throws GameParseException
	 */
	public void setTrigger(String triggers) throws GameParseException
	{
		String[] s = triggers.split(":");
		for (int i = 0; i < s.length; i++)
		{
			RulesAttachment trigger = null;
			for (PlayerID p : getData().getPlayerList().getPlayers())
			{
				trigger = (RulesAttachment) p.getAttachment(s[i]);
				if (trigger != null)
					break;
			}
			if (trigger == null)
				throw new GameParseException("Triggers: Could not find rule. name:" + s[i]);
			if (m_trigger == null)
				m_trigger = new ArrayList<RulesAttachment>();
			m_trigger.add(trigger);
		}
	}
	
	public List<RulesAttachment> getTrigger()
	{
		return m_trigger;
	}
	
	public void clearTrigger()
	{
		m_trigger.clear();
	}
	
	public void setFrontier(String s) throws GameParseException
	{
		ProductionFrontier front = getData().getProductionFrontierList().getProductionFrontier(s);
		if (front == null)
			throw new GameParseException("Triggers: Could not find frontier. name:" + s);
		m_frontier = front;
	}
	
	public ProductionFrontier getFrontier()
	{
		return m_frontier;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param prop
	 * @throws GameParseException
	 */
	public void setProductionRule(String prop) throws GameParseException
	{
		String[] s = prop.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: Invalid productionRule declaration: " + prop);
		if (m_productionRule == null)
			m_productionRule = new ArrayList<String>();
		if (getData().getProductionFrontierList().getProductionFrontier(s[0]) == null)
			throw new GameParseException("Triggers: Could not find frontier. name:" + s[0]);
		String rule = s[1];
		if (rule.startsWith("-"))
			rule = rule.replaceFirst("-", "");
		if (getData().getProductionRuleList().getProductionRule(rule) == null)
			throw new GameParseException("Triggers: Could not find production rule. name:" + rule);
		m_productionRule.add(prop);
	}
	
	public List<String> getProductionRule()
	{
		return m_productionRule;
	}
	
	public void clearProductionRule()
	{
		m_productionRule.clear();
	}
	
	public int getResourceCount()
	{
		return m_resourceCount;
	}
	
	public void setResourceCount(String s)
	{
		m_resourceCount = getInt(s);
	}
	
	public void setUses(String s)
	{
		m_uses = getInt(s);
	}
	
	public void setUses(Integer u)
	{
		m_uses = u;
	}
	
	public void setUses(int u)
	{
		m_uses = u;
	}
	
	public void setUsedThisRound(String s)
	{
		m_usedThisRound = getBool(s);
	}
	
	public void setUsedThisRound(boolean usedThisRound)
	{
		m_usedThisRound = usedThisRound;
	}
	
	public boolean getUsedThisRound()
	{
		return m_usedThisRound;
	}
	
	public int getUses()
	{
		return m_uses;
	}
	
	public boolean getInvert()
	{
		return m_invert;
	}
	
	public void setInvert(String s)
	{
		m_invert = getBool(s);
	}
	
	public String getVictory()
	{
		return m_victory;
	}
	
	public void setVictory(String s)
	{
		m_victory = s;
	}
	
	public void setWhen(String when) throws GameParseException
	{
		String[] s = when.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: when must exist in 2 parts: \"before/after:stepName\".");
		if (!(s[0].equals(AFTER) || s[0].equals(BEFORE)))
			throw new GameParseException("Triggers: notificaition must start with: " + BEFORE + " or " + AFTER);
		m_when = new Tuple<String, String>(s[0], s[1]);
	}
	
	public Tuple<String, String> getWhen()
	{
		return m_when;
	}
	
	public void setNotification(String sNotification)
	{
		m_notification = sNotification;
	}
	
	protected String getNotification()
	{
		return m_notification;
	}
	
	public String getConditionType()
	{
		return m_conditionType;
	}
	
	public void setConditionType(String s) throws GameParseException
	{
		if (!(s.equals("and") || s.equals("AND") || s.equals("or") || s.equals("OR") || s.equals("XOR") || s.equals("xor")))
		{
			String[] nums = s.split("-");
			if (nums.length == 1)
			{
				if (Integer.parseInt(nums[0]) < 0)
					throw new GameParseException(
								"Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
			}
			else if (nums.length == 2)
			{
				if (Integer.parseInt(nums[0]) < 0 || Integer.parseInt(nums[1]) < 0 || !(Integer.parseInt(nums[0]) < Integer.parseInt(nums[1])))
					throw new GameParseException(
								"Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
			}
			else
				throw new GameParseException(
							"Rules & Conditions: conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y and Z are valid positive integers and Z is greater than Y");
		}
		m_conditionType = s;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param techs
	 * @throws GameParseException
	 */
	public void setTech(String techs) throws GameParseException
	{
		String[] s = techs.split(":");
		for (int i = 0; i < s.length; i++)
		{
			TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
			if (ta == null)
				ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
			if (ta == null)
				throw new GameParseException("Triggers: Technology not found :" + s[i]);
			m_tech.add(ta);
		}
	}
	
	public List<TechAdvance> getTech()
	{
		return m_tech;
	}
	
	public void clearTech()
	{
		m_tech.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param techs
	 * @throws GameParseException
	 */
	public void setAvailableTech(String techs) throws GameParseException
	{
		String[] s = techs.split(":");
		if (s.length < 2)
			throw new GameParseException("Triggers: Invalid tech availability: " + techs + " should be category:techs");
		String cat = s[0];
		Map<TechAdvance, Boolean> tlist = new LinkedHashMap<TechAdvance, Boolean>();
		for (int i = 1; i < s.length; i++)
		{
			boolean add = true;
			if (s[i].startsWith("-"))
			{
				add = false;
				s[i] = s[i].substring(1);
			}
			TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
			if (ta == null)
				ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
			if (ta == null)
				throw new GameParseException("Triggers: Technology not found :" + s[i]);
			tlist.put(ta, add);
		}
		if (m_availableTech == null)
			m_availableTech = new HashMap<String, Map<TechAdvance, Boolean>>();
		if (m_availableTech.containsKey(cat))
			tlist.putAll(m_availableTech.get(cat));
		m_availableTech.put(cat, tlist);
	}
	
	public Map<String, Map<TechAdvance, Boolean>> getAvailableTech()
	{
		return m_availableTech;
	}
	
	public void clearAvailableTech()
	{
		m_availableTech.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param sup
	 * @throws GameParseException
	 */
	public void setSupport(String sup) throws GameParseException
	{
		String[] s = sup.split(":");
		for (int i = 0; i < s.length; i++)
		{
			boolean add = true;
			if (s[i].startsWith("-"))
			{
				add = false;
				s[i] = s[i].substring(1);
			}
			boolean found = false;
			for (UnitSupportAttachment support : UnitSupportAttachment.get(getData()))
			{
				if (support.getName().equals(s[i]))
				{
					found = true;
					if (m_support == null)
						m_support = new LinkedHashMap<UnitSupportAttachment, Boolean>();
					m_support.put(support, add);
					break;
				}
			}
			if (!found)
				throw new GameParseException("Triggers: Could not find unitSupportAttachment. name:" + s[i]);
		}
		
	}
	
	public Map<UnitSupportAttachment, Boolean> getSupport()
	{
		return m_support;
	}
	
	public void clearSupport()
	{
		m_support.clear();
	}
	
	public String getResource()
	{
		return m_resource;
	}
	
	public void setResource(String s) throws GameParseException
	{
		Resource r = getData().getResourceList().getResource(s);
		if (r == null)
			throw new GameParseException("Triggers: Invalid resource: " + s);
		else
			m_resource = s;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param relChange
	 * @throws GameParseException
	 */
	public void setRelationshipChange(String relChange) throws GameParseException
	{
		String[] s = relChange.split(":");
		if (s.length != 4)
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: " + relChange + " \n Use: player1:player2:oldRelation:newRelation\n");
		if (getData().getPlayerList().getPlayerID(s[0]) == null)
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: " + relChange + " \n player: " + s[0] + " unknown in: " + getName());
		if (getData().getPlayerList().getPlayerID(s[1]) == null)
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: " + relChange + " \n player: " + s[1] + " unknown in: " + getName());
		
		if (!(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) ||
					s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY) ||
					s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) ||
					s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) || Matches.isValidRelationshipName(getData()).match(s[2])))
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: " + relChange + " \n relationshipType: " + s[2] + " unknown in: " + getName());
		
		if (Matches.isValidRelationshipName(getData()).invert().match(s[3]))
			throw new GameParseException("Triggers: Invalid relationshipChange declaration: " + relChange + " \n relationshipType: " + s[3] + " unknown in: " + getName());
		
		m_relationshipChange.add(relChange);
	}
	
	public List<String> getRelationshipChange()
	{
		return m_relationshipChange;
	}
	
	public void clearRelationshipChange()
	{
		m_relationshipChange.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param names
	 * @throws GameParseException
	 */
	public void setUnitType(String names) throws GameParseException
	{
		String[] s = names.split(":");
		for (int i = 0; i < s.length; i++)
		{
			UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
			if (type == null)
				throw new GameParseException("Triggers: Could not find unitType. name:" + s[i]);
			m_unitType.add(type);
		}
	}
	
	public List<UnitType> getUnitType()
	{
		return m_unitType;
	}
	
	public void clearUnitType()
	{
		m_unitType.clear();
	}
	
	public void setUnitAttachmentName(String name) throws GameParseException
	{
		String[] s = name.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: unitAttachmentName must have 2 entries, the type of attachment and the name of the attachment.");
		// covers UnitAttachment, UnitSupportAttachment
		if (!(s[1].equals("UnitAttachment") || s[1].equals("UnitSupportAttachment")))
			throw new GameParseException("Triggers: unitAttachmentName value must be UnitAttachment or UnitSupportAttachment");
		// TODO validate attachment exists?
		if (s[0].length() < 1)
			throw new GameParseException("Triggers: unitAttachmentName count must be a valid attachment name");
		if (s[1].equals("UnitAttachment") && !s[0].startsWith(Constants.UNIT_ATTACHMENT_NAME))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		if (s[1].equals("UnitSupportAttachment") && !s[0].startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		
		m_unitAttachmentName = new Tuple<String, String>(s[1], s[0]);
	}
	
	public Tuple<String, String> getUnitAttachmentName()
	{
		if (m_unitAttachmentName == null)
			return new Tuple<String, String>("UnitAttachment", Constants.UNIT_ATTACHMENT_NAME);
		return m_unitAttachmentName;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param prop
	 * @throws GameParseException
	 */
	public void setUnitProperty(String prop) throws GameParseException
	{
		String[] s = prop.split(":");
		
		if (m_unitProperty == null)
			m_unitProperty = new ArrayList<Tuple<String, String>>();
		
		String property = s[s.length - 1]; // the last one is the property we are changing, while the rest is the string we are changing it to
		String value = "";
		
		for (int i = 0; i < s.length - 1; i++)
			value += ":" + s[i];
		
		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");
		
		m_unitProperty.add(new Tuple<String, String>(property, value));
	}
	
	public List<Tuple<String, String>> getUnitProperty()
	{
		return m_unitProperty;
	}
	
	public void clearUnitProperty()
	{
		m_unitProperty.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param names
	 * @throws GameParseException
	 */
	public void setTerritories(String names) throws GameParseException
	{
		String[] s = names.split(":");
		for (int i = 0; i < s.length; i++)
		{
			Territory terr = getData().getMap().getTerritory(s[i]);
			if (terr == null)
				throw new GameParseException("Triggers: Could not find territory. name:" + s[i]);
			m_territories.add(terr);
		}
	}
	
	public List<Territory> getTerritories()
	{
		return m_territories;
	}
	
	public void clearTerritories()
	{
		m_territories.clear();
	}
	
	public void setTerritoryAttachmentName(String name) throws GameParseException
	{
		String[] s = name.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: territoryAttachmentName must have 2 entries, the type of attachment and the name of the attachment.");
		// covers TerritoryAttachment, CanalAttachment
		if (!(s[1].equals("TerritoryAttachment") || s[1].equals("CanalAttachment")))
			throw new GameParseException("Triggers: territoryAttachmentName value must be TerritoryAttachment or CanalAttachment");
		// TODO validate attachment exists?
		if (s[0].length() < 1)
			throw new GameParseException("Triggers: territoryAttachmentName count must be a valid attachment name");
		if (s[1].equals("TerritoryAttachment") && !s[0].startsWith(Constants.TERRITORY_ATTACHMENT_NAME))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		if (s[1].equals("CanalAttachment") && !s[0].startsWith(Constants.CANAL_ATTACHMENT_PREFIX))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		m_territoryAttachmentName = new Tuple<String, String>(s[1], s[0]);
	}
	
	public Tuple<String, String> getTerritoryAttachmentName()
	{
		if (m_territoryAttachmentName == null)
			return new Tuple<String, String>("TerritoryAttachment", Constants.TERRITORY_ATTACHMENT_NAME);
		return m_territoryAttachmentName;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param prop
	 * @throws GameParseException
	 */
	public void setTerritoryProperty(String prop) throws GameParseException
	{
		String[] s = prop.split(":");
		
		if (m_territoryProperty == null)
			m_territoryProperty = new ArrayList<Tuple<String, String>>();
		
		String property = s[s.length - 1]; // the last one is the property we are changing, while the rest is the string we are changing it to
		String value = "";
		
		for (int i = 0; i < s.length - 1; i++)
			value += ":" + s[i];
		
		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");
		
		m_territoryProperty.add(new Tuple<String, String>(property, value));
	}
	
	public List<Tuple<String, String>> getTerritoryProperty()
	{
		return m_territoryProperty;
	}
	
	public void clearTerritoryProperty()
	{
		m_territoryProperty.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param names
	 * @throws GameParseException
	 */
	public void setPlayers(String names) throws GameParseException
	{
		String[] s = names.split(":");
		for (int i = 0; i < s.length; i++)
		{
			PlayerID player = getData().getPlayerList().getPlayerID(s[i]);
			if (player == null)
				throw new GameParseException("Triggers: Could not find player. name:" + s[i]);
			m_players.add(player);
		}
	}
	
	public List<PlayerID> getPlayers()
	{
		if (m_players.isEmpty())
			return Collections.singletonList((PlayerID) getAttatchedTo());
		else
			return m_players;
	}
	
	public void clearPlayers()
	{
		m_players.clear();
	}
	
	public void setPlayerAttachmentName(String name) throws GameParseException
	{
		String[] s = name.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: playerAttachmentName must have 2 entries, the type of attachment and the name of the attachment.");
		// covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment
		if (!(s[1].equals("PlayerAttachment") || s[1].equals("RulesAttachment") || s[1].equals("TriggerAttachment") || s[1].equals("TechAttachment") || s[1].equals("PoliticalActionAttachment")))
			throw new GameParseException("Triggers: playerAttachmentName value must be PlayerAttachment or RulesAttachment or TriggerAttachment or TechAttachment or PoliticalActionAttachment");
		// TODO validate attachment exists?
		if (s[0].length() < 1)
			throw new GameParseException("Triggers: playerAttachmentName count must be a valid attachment name");
		if (s[1].equals("PlayerAttachment") && !s[0].startsWith(Constants.PLAYER_ATTACHMENT_NAME))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		if (s[1].equals("RulesAttachment") && !(s[0].startsWith(Constants.RULES_ATTACHMENT_NAME) ||
					s[0].startsWith(Constants.RULES_OBJECTIVE_PREFIX) || s[0].startsWith(Constants.RULES_CONDITION_PREFIX)))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		if (s[1].equals("TriggerAttachment") && !s[0].startsWith(Constants.TRIGGER_ATTACHMENT_PREFIX))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		if (s[1].equals("TechAttachment") && !s[0].startsWith(Constants.TECH_ATTACHMENT_NAME))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		if (s[1].equals("PoliticalActionAttachment") && !s[0].startsWith(Constants.POLITICALACTION_ATTACHMENT_PREFIX))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		m_playerAttachmentName = new Tuple<String, String>(s[1], s[0]);
	}
	
	public Tuple<String, String> getPlayerAttachmentName()
	{
		if (m_playerAttachmentName == null)
			return new Tuple<String, String>("PlayerAttachment", Constants.PLAYER_ATTACHMENT_NAME);
		return m_playerAttachmentName;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param prop
	 * @throws GameParseException
	 */
	public void setPlayerProperty(String prop) throws GameParseException
	{
		String[] s = prop.split(":");
		
		if (m_playerProperty == null)
			m_playerProperty = new ArrayList<Tuple<String, String>>();
		
		String property = s[s.length - 1]; // the last one is the property we are changing, while the rest is the string we are changing it to
		String value = "";
		
		for (int i = 0; i < s.length - 1; i++)
			value += ":" + s[i];
		
		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");
		
		m_playerProperty.add(new Tuple<String, String>(property, value));
	}
	
	public List<Tuple<String, String>> getPlayerProperty()
	{
		return m_playerProperty;
	}
	
	public void clearPlayerProperty()
	{
		m_playerProperty.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param names
	 * @throws GameParseException
	 */
	public void setRelationshipTypes(String names) throws GameParseException
	{
		String[] s = names.split(":");
		for (int i = 0; i < s.length; i++)
		{
			RelationshipType relation = getData().getRelationshipTypeList().getRelationshipType(s[i]);
			if (relation == null)
				throw new GameParseException("Triggers: Could not find relationshipType. name:" + s[i]);
			m_relationshipTypes.add(relation);
		}
	}
	
	public List<RelationshipType> getRelationshipTypes()
	{
		return m_relationshipTypes;
	}
	
	public void clearRelationshipTypes()
	{
		m_relationshipTypes.clear();
	}
	
	public void setRelationshipTypeAttachmentName(String name) throws GameParseException
	{
		String[] s = name.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: relationshipTypeAttachmentName must have 2 entries, the type of attachment and the name of the attachment.");
		// covers RelationshipTypeAttachment
		if (!(s[1].equals("RelationshipTypeAttachment")))
			throw new GameParseException("Triggers: relationshipTypeAttachmentName value must be RelationshipTypeAttachment");
		// TODO validate attachment exists?
		if (s[0].length() < 1)
			throw new GameParseException("Triggers: relationshipTypeAttachmentName count must be a valid attachment name");
		if (s[1].equals("RelationshipTypeAttachment") && !s[0].startsWith(Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		m_relationshipTypeAttachmentName = new Tuple<String, String>(s[1], s[0]);
	}
	
	public Tuple<String, String> getRelationshipTypeAttachmentName()
	{
		if (m_relationshipTypeAttachmentName == null)
			return new Tuple<String, String>("RelationshipTypeAttachment", Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME);
		return m_relationshipTypeAttachmentName;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param prop
	 * @throws GameParseException
	 */
	public void setRelationshipTypeProperty(String prop) throws GameParseException
	{
		String[] s = prop.split(":");
		
		if (m_relationshipTypeProperty == null)
			m_relationshipTypeProperty = new ArrayList<Tuple<String, String>>();
		
		String property = s[s.length - 1]; // the last one is the property we are changing, while the rest is the string we are changing it to
		String value = "";
		
		for (int i = 0; i < s.length - 1; i++)
			value += ":" + s[i];
		
		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");
		
		m_relationshipTypeProperty.add(new Tuple<String, String>(property, value));
	}
	
	public List<Tuple<String, String>> getRelationshipTypeProperty()
	{
		return m_relationshipTypeProperty;
	}
	
	public void clearRelationshipTypeProperty()
	{
		m_relationshipTypeProperty.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param names
	 * @throws GameParseException
	 */
	public void setTerritoryEffects(String names) throws GameParseException
	{
		String[] s = names.split(":");
		for (int i = 0; i < s.length; i++)
		{
			TerritoryEffect effect = getData().getTerritoryEffectList().get(s[i]);
			if (effect == null)
				throw new GameParseException("Triggers: Could not find territoryEffect. name:" + s[i]);
			m_territoryEffects.add(effect);
		}
	}
	
	public List<TerritoryEffect> getTerritoryEffects()
	{
		return m_territoryEffects;
	}
	
	public void clearTerritoryEffects()
	{
		m_territoryEffects.clear();
	}
	
	public void setTerritoryEffectAttachmentName(String name) throws GameParseException
	{
		String[] s = name.split(":");
		if (s.length != 2)
			throw new GameParseException("Triggers: territoryEffectAttachmentName must have 2 entries, the type of attachment and the name of the attachment.");
		// covers TerritoryEffectAttachment
		if (!(s[1].equals("TerritoryEffectAttachment")))
			throw new GameParseException("Triggers: territoryEffectAttachmentName value must be TerritoryEffectAttachment");
		// TODO validate attachment exists?
		if (s[0].length() < 1)
			throw new GameParseException("Triggers: territoryEffectAttachmentName count must be a valid attachment name");
		if (s[1].equals("TerritoryEffectAttachment") && !s[0].startsWith(Constants.TERRITORYEFFECT_ATTACHMENT_NAME))
			throw new GameParseException("Triggers: attachment incorrectly named:" + s[0]);
		m_territoryEffectAttachmentName = new Tuple<String, String>(s[1], s[0]);
	}
	
	public Tuple<String, String> getTerritoryEffectAttachmentName()
	{
		if (m_territoryEffectAttachmentName == null)
			return new Tuple<String, String>("TerritoryEffectAttachment", Constants.TERRITORYEFFECT_ATTACHMENT_NAME);
		return m_territoryEffectAttachmentName;
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param prop
	 * @throws GameParseException
	 */
	public void setTerritoryEffectProperty(String prop) throws GameParseException
	{
		String[] s = prop.split(":");
		
		if (m_territoryEffectProperty == null)
			m_territoryEffectProperty = new ArrayList<Tuple<String, String>>();
		
		String property = s[s.length - 1]; // the last one is the property we are changing, while the rest is the string we are changing it to
		String value = "";
		
		for (int i = 0; i < s.length - 1; i++)
			value += ":" + s[i];
		
		// Remove the leading colon
		if (value.length() > 0 && value.startsWith(":"))
			value = value.replaceFirst(":", "");
		
		m_territoryEffectProperty.add(new Tuple<String, String>(property, value));
	}
	
	public List<Tuple<String, String>> getTerritoryEffectProperty()
	{
		return m_territoryEffectProperty;
	}
	
	public void clearTerritoryEffectProperty()
	{
		m_territoryEffectProperty.clear();
	}
	
	/**
	 * Fudging this, it really represents adding placements.
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param place
	 * @throws GameParseException
	 */
	public void setPlacement(String place) throws GameParseException
	{
		String[] s = place.split(":");
		int count = -1, i = 0;
		if (s.length < 1)
			throw new GameParseException("Triggers: Empty placement list");
		try
		{
			count = getInt(s[0]);
			i++;
		} catch (Exception e)
		{
			count = 1;
		}
		if (s.length < 1 || s.length == 1 && count != -1)
			throw new GameParseException("Triggers: Empty placement list");
		Territory territory = getData().getMap().getTerritory(s[i]);
		if (territory == null)
			throw new GameParseException("Triggers: Territory does not exist " + s[i]);
		else
		{
			i++;
			IntegerMap<UnitType> map = new IntegerMap<UnitType>();
			for (; i < s.length; i++)
			{
				UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
				if (type == null)
					throw new GameParseException("Triggers: UnitType does not exist " + s[i]);
				else
					map.add(type, count);
			}
			if (m_placement == null)
				m_placement = new HashMap<Territory, IntegerMap<UnitType>>();
			if (m_placement.containsKey(territory))
				map.add(m_placement.get(territory));
			m_placement.put(territory, map);
		}
	}
	
	public Map<Territory, IntegerMap<UnitType>> getPlacement()
	{
		return m_placement;
	}
	
	public void clearPlacement()
	{
		m_placement.clear();
	}
	
	/**
	 * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
	 * 
	 * @param place
	 * @throws GameParseException
	 */
	public void setPurchase(String place) throws GameParseException
	{
		String[] s = place.split(":");
		int count = -1, i = 0;
		if (s.length < 1)
			throw new GameParseException("Triggers: Empty purchase list");
		try
		{
			count = getInt(s[0]);
			i++;
		} catch (Exception e)
		{
			count = 1;
		}
		if (s.length < 1 || s.length == 1 && count != -1)
			throw new GameParseException("Triggers: Empty purchase list");
		else
		{
			if (m_purchase == null)
				m_purchase = new IntegerMap<UnitType>();
			for (; i < s.length; i++)
			{
				UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
				if (type == null)
					throw new GameParseException("Triggers: UnitType does not exist " + s[i]);
				else
					m_purchase.add(type, count);
			}
		}
	}
	
	public IntegerMap<UnitType> getPurchase()
	{
		return m_purchase;
	}
	
	public void clearPurchase()
	{
		m_purchase.clear();
	}
	
	private static void placeUnits(TriggerAttachment t, Territory terr, IntegerMap<UnitType> uMap, PlayerID player, GameData data, IDelegateBridge aBridge)
	{
		// createUnits
		List<Unit> units = new ArrayList<Unit>();
		;
		for (UnitType u : uMap.keySet())
		{
			units.addAll(u.create(uMap.getInt(u), player));
		}
		CompositeChange change = new CompositeChange();
		// mark no movement
		for (Unit unit : units)
		{
			UnitAttachment ua = UnitAttachment.get(unit.getType());
			change.add(ChangeFactory.unitPropertyChange(unit, ua.getMovement(unit.getOwner()), TripleAUnit.ALREADY_MOVED));
		}
		// place units
		Collection<Unit> factoryAndAA = Match.getMatches(units,
					Matches.UnitIsAAOrIsFactoryOrIsInfrastructure);
		change.add(DelegateFinder.battleDelegate(data).getOriginalOwnerTracker()
					.addOriginalOwnerChange(factoryAndAA, player));
		
		String transcriptText = MyFormatter.attachmentNameToText(t.getName()) + ": " + player.getName() + " has " + MyFormatter.unitsToTextNoOwner(units) + " placed in " + terr.getName();
		aBridge.getHistoryWriter().startEvent(transcriptText);
		aBridge.getHistoryWriter().setRenderingData(units);
		
		Change place = ChangeFactory.addUnits(terr, units);
		change.add(place);
		
		/* No longer needed, as territory unitProduction is now set by default to equal the territory value. Therefore any time it is different from the default, the map maker set it, so we shouldn't screw with it.
		if(Match.someMatch(units, Matches.UnitIsFactoryOrCanProduceUnits) && !Match.someMatch(terr.getUnits().getUnits(), Matches.UnitIsFactoryOrCanProduceUnits) && games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data))
		{
			// if no factories are there, make sure the territory has no damage (that unitProduction = production)
			TerritoryAttachment ta = TerritoryAttachment.get(terr);
			int prod = 0;
			if(ta != null)
				prod = ta.getProduction();
			
		    Change unitProd = ChangeFactory.changeUnitProduction(terr, prod);
		    change.add(unitProd);
		}*/

		aBridge.addChange(change);
		// handle adding to enemy territories
		if (Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data).match(terr))
			getBattleTracker(data).addBattle(new CRoute(terr), units, false, player, aBridge, null);
	}
	
	private void use(IDelegateBridge aBridge)
	{
		// instead of using up a "use" with every action, we will instead use up a "use" if the trigger is fired during this round
		// this is in order to let a trigger that contains multiple actions, fire all of them in a single use
		if (!m_usedThisRound && m_uses > 0)
		{
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, true, "usedThisRound"));
		}
		/*if (m_uses > 0)
		{
			aBridge.addChange(ChangeFactory.attachmentPropertyChange(this, new Integer(m_uses - 1).toString(), "uses"));
		}*/
	}
	
	public static CompositeChange triggerSetUsedForThisRound(PlayerID player, IDelegateBridge aBridge)
	{
		CompositeChange change = new CompositeChange();
		Iterator<TriggerAttachment> iter = getTriggers(player, aBridge.getData(), null).iterator();
		while (iter.hasNext())
		{
			TriggerAttachment ta = iter.next();
			int currentUses = ta.getUses();
			if (ta.m_usedThisRound && currentUses > 0)
			{
				change.add(ChangeFactory.attachmentPropertyChange(ta, new Integer(currentUses - 1).toString(), "uses"));
				change.add(ChangeFactory.attachmentPropertyChange(ta, false, "usedThisRound"));
			}
		}
		return change;
	}
	
	public static void triggerMustFightBattle(PlayerID player1, PlayerID player2, IDelegateBridge aBridge)
	{
		GameData data = aBridge.getData();
		for (Territory terr : Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsOwnedBy(player1)))
		{
			if (Matches.territoryHasEnemyUnits(player1, data).match(terr))
				DelegateFinder.battleDelegate(data).getBattleTracker().addBattle(new CRoute(terr), terr.getUnits().getMatches(Matches.unitIsOwnedBy(player1)), false, player1, aBridge, null);
		}
	}
	
	private static BattleTracker getBattleTracker(GameData data)
	{
		return DelegateFinder.battleDelegate(data).getBattleTracker();
	}
	
	/**
	 * This will account for Invert and conditionType
	 */
	private static boolean isMet(TriggerAttachment t, GameData data)
	{
		return RulesAttachment.areConditionsMet(t.getTrigger(), t.getConditionType(), t.getInvert(), data);
	}
	
	//
	// And now for the actual triggers, as called throughout the engine.
	// Each trigger should be called exactly twice, once in BaseDelegate (for use with 'when'), and a second time as the default location for when 'when' is not used.
	// Should be void unless you have a really good reason otherwise.
	//
	
	public static void triggerProductionChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, prodMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					change.add(ChangeFactory.changeProductionFrontier(aPlayer, t.getFrontier()));
					aBridge.getHistoryWriter().startEvent(
								MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " has their production frontier changed to: " + t.getFrontier().toString());
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerProductionFrontierEditChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, prodFrontierEditMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				Iterator<String> iter = t.getProductionRule().iterator();
				while (iter.hasNext())
				{
					boolean add = true;
					String[] s = iter.next().split(":");
					ProductionFrontier front = data.getProductionFrontierList().getProductionFrontier(s[0]);
					String rule = s[1];
					if (rule.startsWith("-"))
					{
						rule = rule.replaceFirst("-", "");
						add = false;
					}
					ProductionRule pRule = data.getProductionRuleList().getProductionRule(rule);
					
					if (add)
					{
						if (!front.getRules().contains(pRule))
						{
							change.add(ChangeFactory.addProductionRule(pRule, front));
							aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + pRule.getName() + " added to " + front.getName());
						}
					}
					else
					{
						if (front.getRules().contains(pRule))
						{
							change.add(ChangeFactory.removeProductionRule(pRule, front));
							aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + pRule.getName() + " removed from " + front.getName());
						}
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change); // TODO: we should sort the frontier list if we make changes to it...
	}
	
	public static void triggerTechChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, techMatch(beforeOrAfter, stepName));
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					for (TechAdvance ta : t.getTech())
					{
						if (ta.hasTech(TechAttachment.get(aPlayer))
									|| !TechAdvance.getTechAdvances(data, aPlayer).contains(ta))
							continue;
						aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " activates " + ta);
						TechTracker.addAdvance(aPlayer, aBridge, ta);
					}
				}
			}
		}
	}
	
	public static void triggerAvailableTechChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, techAMatch(beforeOrAfter, stepName));
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					for (String cat : t.getAvailableTech().keySet())
					{
						TechnologyFrontier tf = aPlayer.getTechnologyFrontierList().getTechnologyFrontier(cat);
						if (tf == null)
							throw new IllegalStateException("Triggers: tech category doesn't exist:" + cat + " for player:" + aPlayer);
						for (TechAdvance ta : t.getAvailableTech().get(cat).keySet())
						{
							if (t.getAvailableTech().get(cat).get(ta))
							{
								aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " gains access to " + ta);
								Change change = ChangeFactory.addAvailableTech(tf, ta, aPlayer);
								aBridge.addChange(change);
							}
							else
							{
								aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " loses access to " + ta);
								Change change = ChangeFactory.removeAvailableTech(tf, ta, aPlayer);
								aBridge.addChange(change);
							}
						}
					}
				}
			}
		}
	}
	
	public static void triggerUnitPlacement(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, placeMatch(beforeOrAfter, stepName));
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					for (Territory ter : t.getPlacement().keySet())
					{
						// aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " places " + t.getPlacement().get(ter).toString() + " in territory " + ter.getName());
						placeUnits(t, ter, t.getPlacement().get(ter), aPlayer, data, aBridge);
					}
				}
			}
		}
	}
	
	public static void triggerPurchase(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, purchaseMatch(beforeOrAfter, stepName));
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					List<Unit> units = new ArrayList<Unit>();
					for (UnitType u : t.getPurchase().keySet())
					{
						units.addAll(u.create(t.getPurchase().getInt(u), aPlayer));
					}
					if (!units.isEmpty())
					{
						String transcriptText = MyFormatter.attachmentNameToText(t.getName()) + ": " + MyFormatter.unitsToTextNoOwner(units) + " gained by " + aPlayer;
						aBridge.getHistoryWriter().startEvent(transcriptText);
						aBridge.getHistoryWriter().setRenderingData(units);
						Change place = ChangeFactory.addUnits(aPlayer, units);
						aBridge.addChange(place);
					}
				}
			}
		}
	}
	
	public static void triggerResourceChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, resourceMatch(beforeOrAfter, stepName));
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					int toAdd = t.getResourceCount();
					if (t.getResource().equals(Constants.PUS))
						;
					toAdd *= Properties.getPU_Multiplier(data);
					int total = aPlayer.getResources().getQuantity(t.getResource()) + toAdd;
					if (total < 0)
					{
						toAdd -= total;
						total = 0;
					}
					aBridge.addChange(ChangeFactory.changeResourcesChange(aPlayer, data.getResourceList().getResource(t.getResource()), toAdd));
					String PUMessage = MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " met a national objective for an additional " + t.getResourceCount() + " "
								+ t.getResource() +
								"; end with " + total + " " + t.getResource();
					aBridge.getHistoryWriter().startEvent(PUMessage);
				}
			}
		}
	}
	
	// note this change is silent
	public static void triggerSupportChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, supportMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				for (PlayerID aPlayer : t.getPlayers())
				{
					for (UnitSupportAttachment usa : t.getSupport().keySet())
					{
						List<PlayerID> p = new ArrayList<PlayerID>(usa.getPlayers());
						if (p.contains(aPlayer))
						{
							if (!t.getSupport().get(usa))
							{
								p.remove(aPlayer);
								change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
								aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " is removed from " + usa.toString());
							}
						}
						else
						{
							if (t.getSupport().get(usa))
							{
								p.add(aPlayer);
								change.add(ChangeFactory.attachmentPropertyChange(usa, p, "players"));
								aBridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(t.getName()) + ": " + aPlayer.getName() + " is added to " + usa.toString());
							}
						}
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerRelationshipChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, relationshipChangeMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				for (String relationshipChange : t.getRelationshipChange())
				{
					String[] s = relationshipChange.split(":");
					PlayerID player1 = data.getPlayerList().getPlayerID(s[0]);
					PlayerID player2 = data.getPlayerList().getPlayerID(s[1]);
					RelationshipType currentRelation = data.getRelationshipTracker().getRelationshipType(player1, player2);
					
					if (s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY) ||
								(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_NEUTRAL) && Matches.RelationshipIsNeutral.match(currentRelation)) ||
								(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_ALLIED) && Matches.RelationshipIsAllied.match(currentRelation)) ||
								(s[2].equals(Constants.RELATIONSHIP_CONDITION_ANY_WAR) && Matches.RelationshipIsAtWar.match(currentRelation)) ||
								currentRelation.equals(data.getRelationshipTypeList().getRelationshipType(s[2])))
					{
						
						RelationshipType triggerNewRelation = data.getRelationshipTypeList().getRelationshipType(s[3]);
						change.add(ChangeFactory.relationshipChange(player1, player2, currentRelation, triggerNewRelation));
						aBridge.getHistoryWriter().startEvent(
									MyFormatter.attachmentNameToText(t.getName()) + ": Changing Relationship for " + player1.getName() + " and " + player2.getName() + " from "
												+ currentRelation.getName() + " to " + triggerNewRelation.getName());
						if (Matches.RelationshipIsAtWar.match(triggerNewRelation))
							triggerMustFightBattle(player1, player2, aBridge);
					}
				}
			}
			
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerUnitPropertyChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, unitPropertyMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				for (Tuple<String, String> property : t.getUnitProperty())
				{
					for (UnitType aUnitType : t.getUnitType())
					{
						String newValue = property.getSecond();
						boolean clearFirst = false;
						// test if we are clearing the variable first, and if so, remove the leading "-clear-"
						if (newValue.length() > 0 && newValue.startsWith("-clear-"))
						{
							newValue = newValue.replaceFirst("-clear-", "");
							clearFirst = true;
						}
						// covers UnitAttachment, UnitSupportAttachment
						if (t.getUnitAttachmentName().getFirst().equals("UnitAttachment"))
						{
							UnitAttachment attachment = UnitAttachment.get(aUnitType, t.getUnitAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getUnitAttachmentName().getSecond() + " attached to " + aUnitType.getName());
						}
						else if (t.getUnitAttachmentName().getFirst().equals("UnitSupportAttachment"))
						{
							UnitSupportAttachment attachment = UnitSupportAttachment.get(aUnitType, t.getUnitAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getUnitAttachmentName().getSecond() + " attached to " + aUnitType.getName());
						}
						// TODO add other attachment changes here if they attach to a unitType
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerTerritoryPropertyChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, territoryPropertyMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		HashSet<Territory> territoriesNeedingReDraw = new HashSet<Territory>();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				for (Tuple<String, String> property : t.getTerritoryProperty())
				{
					for (Territory aTerritory : t.getTerritories())
					{
						territoriesNeedingReDraw.add(aTerritory);
						String newValue = property.getSecond();
						boolean clearFirst = false;
						// test if we are clearing the variable first, and if so, remove the leading "-clear-"
						if (newValue.length() > 0 && newValue.startsWith("-clear-"))
						{
							newValue = newValue.replaceFirst("-clear-", "");
							clearFirst = true;
						}
						// covers TerritoryAttachment, CanalAttachment
						if (t.getTerritoryAttachmentName().getFirst().equals("TerritoryAttachment"))
						{
							TerritoryAttachment attachment = TerritoryAttachment.get(aTerritory, t.getTerritoryAttachmentName().getSecond());
							if (attachment == null)
								throw new IllegalStateException("Triggers: No territory attachment for:" + aTerritory.getName()); // water territories may not have an attachment, so this could be null
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getTerritoryAttachmentName().getSecond() + " attached to " + aTerritory.getName());
						}
						else if (t.getTerritoryAttachmentName().getFirst().equals("CanalAttachment"))
						{
							CanalAttachment attachment = CanalAttachment.get(aTerritory, t.getTerritoryAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getTerritoryAttachmentName().getSecond() + " attached to " + aTerritory.getName());
						}
						// TODO add other attachment changes here if they attach to a territory
					}
				}
			}
		}
		if (!change.isEmpty())
		{
			aBridge.addChange(change);
			for (Territory aTerritory : territoriesNeedingReDraw)
			{
				aTerritory.notifyAttachmentChanged();
			}
		}
	}
	
	public static void triggerPlayerPropertyChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, playerPropertyMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				for (Tuple<String, String> property : t.getPlayerProperty())
				{
					for (PlayerID aPlayer : t.getPlayers())
					{
						String newValue = property.getSecond();
						boolean clearFirst = false;
						// test if we are clearing the variable first, and if so, remove the leading "-clear-"
						if (newValue.length() > 0 && newValue.startsWith("-clear-"))
						{
							newValue = newValue.replaceFirst("-clear-", "");
							clearFirst = true;
						}
						// covers PlayerAttachment, TriggerAttachment, RulesAttachment, TechAttachment
						if (t.getPlayerAttachmentName().getFirst().equals("PlayerAttachment"))
						{
							PlayerAttachment attachment = PlayerAttachment.get(aPlayer, t.getPlayerAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getPlayerAttachmentName().getSecond() + " attached to " + aPlayer.getName());
						}
						else if (t.getPlayerAttachmentName().getFirst().equals("RulesAttachment"))
						{
							RulesAttachment attachment = RulesAttachment.get(aPlayer, t.getPlayerAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getPlayerAttachmentName().getSecond() + " attached to " + aPlayer.getName());
						}
						else if (t.getPlayerAttachmentName().getFirst().equals("TriggerAttachment"))
						{
							TriggerAttachment attachment = TriggerAttachment.get(aPlayer, t.getPlayerAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getPlayerAttachmentName().getSecond() + " attached to " + aPlayer.getName());
						}
						else if (t.getPlayerAttachmentName().getFirst().equals("TechAttachment"))
						{
							TechAttachment attachment = TechAttachment.get(aPlayer, t.getPlayerAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getPlayerAttachmentName().getSecond() + " attached to " + aPlayer.getName());
						}
						else if (t.getPlayerAttachmentName().getFirst().equals("PoliticalActionAttachment"))
						{
							PoliticalActionAttachment attachment = PoliticalActionAttachment.get(aPlayer, t.getPlayerAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getPlayerAttachmentName().getSecond() + " attached to " + aPlayer.getName());
						}
						// TODO add other attachment changes here if they attach to a player
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerRelationshipTypePropertyChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, relationshipTypePropertyMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				for (Tuple<String, String> property : t.getRelationshipTypeProperty())
				{
					for (RelationshipType aRelationshipType : t.getRelationshipTypes())
					{
						String newValue = property.getSecond();
						boolean clearFirst = false;
						// test if we are clearing the variable first, and if so, remove the leading "-clear-"
						if (newValue.length() > 0 && newValue.startsWith("-clear-"))
						{
							newValue = newValue.replaceFirst("-clear-", "");
							clearFirst = true;
						}
						// covers RelationshipTypeAttachment
						if (t.getTerritoryAttachmentName().getFirst().equals("RelationshipTypeAttachment"))
						{
							RelationshipTypeAttachment attachment = RelationshipTypeAttachment.get(aRelationshipType, t.getRelationshipTypeAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getRelationshipTypeAttachmentName().getSecond() + " attached to " + aRelationshipType.getName());
						}
						// TODO add other attachment changes here if they attach to a territory
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static void triggerTerritoryEffectPropertyChange(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, territoryEffectPropertyMatch(beforeOrAfter, stepName));
		CompositeChange change = new CompositeChange();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				for (Tuple<String, String> property : t.getTerritoryEffectProperty())
				{
					for (TerritoryEffect aTerritoryEffect : t.getTerritoryEffects())
					{
						String newValue = property.getSecond();
						boolean clearFirst = false;
						// test if we are clearing the variable first, and if so, remove the leading "-clear-"
						if (newValue.length() > 0 && newValue.startsWith("-clear-"))
						{
							newValue = newValue.replaceFirst("-clear-", "");
							clearFirst = true;
						}
						// covers TerritoryEffectAttachment
						if (t.getTerritoryEffectAttachmentName().getFirst().equals("TerritoryEffectAttachment"))
						{
							TerritoryEffectAttachment attachment = TerritoryEffectAttachment.get(aTerritoryEffect, t.getTerritoryEffectAttachmentName().getSecond());
							if (attachment.getRawProperty(property.getFirst()).equals(newValue))
								continue;
							if (clearFirst && newValue.length() < 1)
								change.add(ChangeFactory.attachmentPropertyClear(attachment, property.getFirst(), true));
							else
								change.add(ChangeFactory.attachmentPropertyChange(attachment, newValue, property.getFirst(), true, clearFirst));
							aBridge.getHistoryWriter().startEvent(
										MyFormatter.attachmentNameToText(t.getName()) + ": Setting " + property.getFirst() + (newValue.length() > 0 ? " to " + newValue : " cleared ") + " for "
													+ t.getTerritoryEffectAttachmentName().getSecond() + " attached to " + aTerritoryEffect.getName());
						}
						// TODO add other attachment changes here if they attach to a territory
					}
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	public static Tuple<String, Collection<PlayerID>> triggerVictory(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		Set<TriggerAttachment> trigs = getTriggers(player, data, victoryMatch(beforeOrAfter, stepName));
		for (TriggerAttachment t : trigs)
		{
			boolean met = isMet(t, data);
			if (met)
			{
				t.use(aBridge);
				// no need for history writing as the method calling this has its own history writer
				return new Tuple<String, Collection<PlayerID>>(t.getVictory(), t.getPlayers());
			}
		}
		return null;
	}
	
	public static Set<String> triggerNotifications(PlayerID player, IDelegateBridge aBridge, final String beforeOrAfter, final String stepName)
	{
		GameData data = aBridge.getData();
		// try
		// {
		// data.acquireReadLock();
		Set<TriggerAttachment> trigs = getTriggers(player, data, notificationMatch(beforeOrAfter, stepName));
		Set<String> notifications = new HashSet<String>();
		for (TriggerAttachment t : trigs)
		{
			if (isMet(t, data))
			{
				t.use(aBridge);
				notifications.add(t.getNotification());
			}
		}
		return notifications;
		
		// } finally
		// {
		// data.releaseReadLock();
		// }
	}
	
	/**
	 * If t.getWhen(), beforeOrAfter, and stepName, are all null, then this returns true.
	 * Otherwise, all must be not null, and when's values must match the arguments.
	 * 
	 * @param beforeOrAfter
	 *            can be null, or must be "before" or "after"
	 * @param stepName
	 *            can be null, or must be exact name of a specific stepName
	 * @return true if when and both args are null, and true if all are not null and when matches the args, otherwise false
	 */
	private static Match<TriggerAttachment> whenOrDefaultMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				if (beforeOrAfter == null && stepName == null && t.getWhen() == null)
					return true;
				else if (beforeOrAfter != null && stepName != null && t.getWhen() != null && beforeOrAfter.equals(t.getWhen().getFirst()) && stepName.equals(t.getWhen().getSecond()))
					return true;
				return false;
			}
		};
	}
	
	//
	// All matches need to check for: t.getUses()!=0
	//
	// In addition, all triggers can be activated in only 1 of 2 places: default or when
	//
	// default = t.getWhen == null (this means when was not set, and so the trigger should activate in its default place, like before purchase phase for production frontier trigger changes
	//
	// when = t.getWhen != null (this means when was set, and so the trigger should not activate in its default place, and instead should activate before or after a specific stepName
	//
	// therefore all matches also need to check for: whenStepOrDefaultTriggerMatch(beforeOrAfter, stepName).match(t)
	//
	
	private static Match<TriggerAttachment> prodMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getFrontier() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> prodFrontierEditMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getProductionRule() != null && t.getProductionRule().size() > 0;
			}
		};
	}
	
	private static Match<TriggerAttachment> techMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && !t.getTech().isEmpty();
			}
		};
	}
	
	private static Match<TriggerAttachment> techAMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getAvailableTech() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> placeMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getPlacement() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> purchaseMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getPurchase() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> resourceMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getResource() != null && t.getResourceCount() != 0;
			}
		};
	}
	
	private static Match<TriggerAttachment> supportMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getSupport() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> unitPropertyMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && !t.getUnitType().isEmpty() && t.getUnitProperty() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> territoryPropertyMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && !t.getTerritories().isEmpty() && t.getTerritoryProperty() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> playerPropertyMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getPlayerProperty() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> relationshipTypePropertyMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && !t.getRelationshipTypes().isEmpty() && t.getRelationshipTypeProperty() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> territoryEffectPropertyMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && !t.getTerritoryEffects().isEmpty() && t.getTerritoryEffectProperty() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> relationshipChangeMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && !t.getRelationshipChange().isEmpty();
			}
		};
	}
	
	private static Match<TriggerAttachment> notificationMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getNotification() != null;
			}
		};
	}
	
	private static Match<TriggerAttachment> victoryMatch(final String beforeOrAfter, final String stepName)
	{
		return new Match<TriggerAttachment>()
		{
			
			@Override
			public boolean match(TriggerAttachment t)
			{
				return t.getUses() != 0 && whenOrDefaultMatch(beforeOrAfter, stepName).match(t) && t.getVictory() != null && t.getVictory().length() > 0;
			}
		};
	}
	
	@Override
	public void validate(GameData data) throws GameParseException
	{
		if (m_trigger == null)
			throw new GameParseException("Triggers: Invalid Unit attatchment" + this);
	}
	
	
	// shameless cheating. making a fake route, so as to handle battles
	// properly without breaking battleTracker protected status or duplicating
	// a zillion lines of code.
	private static class CRoute extends Route
	{
		private static final long serialVersionUID = -4571007882522107666L;
		
		public CRoute(Territory terr)
		{
			super(terr);
		}
		
		@Override
		public Territory getEnd()
		{
			return getStart();
		}
		
		@Override
		public int getLength()
		{
			return 1;
		}
		
		@Override
		public Territory at(int i)
		{
			return getStart();
		}
	}
	
}
