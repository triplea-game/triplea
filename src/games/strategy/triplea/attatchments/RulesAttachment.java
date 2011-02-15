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
 * TerritoryAttachment.java
 *
 * Created on November 8, 2001, 3:08 PM
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.File;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


import games.strategy.engine.data.*;
import games.strategy.engine.framework.GameRunner;

/**
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class RulesAttachment extends DefaultAttachment
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7301965634079412516L;

	/**
     * Convenience method.
     */
    public static RulesAttachment get(Rule r)
    {
    	RulesAttachment rVal =  (RulesAttachment) r.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        if(rVal == null)
            throw new IllegalStateException("Rules & Conditions: No rule attachment for:" + r.getName());
        return rVal;
    }
    //Players
    private PlayerID m_ruleOwner = null;
    
    //Strings
    private String m_alliedExclusion = null;
    private String m_enemyExclusion = null;
    private String m_allowedUnitType = null;
    private String m_movementRestrictionType = null;
    
    //Territory lists
    private String[] m_alliedOwnershipTerritories;
    private String[] m_alliedExcludedTerritories;
    private String[] m_enemyExcludedTerritories;
    private String[] m_enemySurfaceExcludedTerritories;
	//Territory Lists for direct ownership, astabada mod
    private String[] m_directOwnershipTerritories;
    
    private String[] m_movementRestrictionTerritories;
    
    //booleans
    private boolean m_placementAnyTerritory = false;
    private boolean m_placementCapturedTerritory = false;
    private boolean m_unlimitedProduction = false;
    private boolean m_placementInCapitalRestricted = false;
    private boolean m_dominatingFirstRoundAttack = false;
    private boolean m_negateDominatingFirstRoundAttack = false;
    private boolean m_invert = false;
    
    //Integers
    private int m_territoryCount = -1;
    private int m_objectiveValue = 0;
    private int m_perOwnedTerritories = -1;
    private int m_productionPerTerritory = -1;
    private int m_placementPerTerritory = -1;
    private int m_maxPlacePerTerritory = -1;
    private int m_atWarCount = -1;
    private int m_uses = -1; 
    private int m_techCount = -1;
    
    private Set<PlayerID> m_atWarPlayers = null;
 // using map as tuple set, describes ranges from Integer-Integer
    private Map<Integer,Integer> m_turns = null;  
    private List<TechAdvance> m_techs = null;
    
  /** Creates new RulesAttachment */
  public RulesAttachment()
  {
  }

  public void setRuleOwner(PlayerID player)
  {
	  m_ruleOwner = player;
  }
  
  public PlayerID getRuleOwner()
  {
	  return m_ruleOwner;
  }

  public void setObjectiveValue(String value)
  {
      m_objectiveValue = getInt(value);
  }

  public int getObjectiveValue()
  {
      return m_objectiveValue;
  }
  
  public void setAlliedOwnershipTerritories(String value)
  {
	  m_alliedOwnershipTerritories = value.split(":");
  }

  public String[] getAlliedOwnershipTerritories()
  {
      return m_alliedOwnershipTerritories;
  }

  //exclusion types = controlled, original, all, or list
  public void setAlliedExclusionTerritories(String value)
  {	
	  m_alliedExcludedTerritories = value.split(":");
  }

  public String[]  getAlliedExclusionTerritories()
  {
      return m_alliedExcludedTerritories;
  }
  
  //exclusion types = original or list
  public void setEnemyExclusionTerritories(String value)
  {	
	  m_enemyExcludedTerritories = value.split(":");
  }

  public String[]  getEnemyExclusionTerritories()
  {
      return m_enemyExcludedTerritories;
  }
  
  //exclusion types = original or list
  public void setEnemySurfaceExclusionTerritories(String value)
  {	
	  m_enemySurfaceExcludedTerritories = value.split(":");
  }

  public String[]  getEnemySurfaceExclusionTerritories()
  {
      return m_enemySurfaceExcludedTerritories;
  }


  //Territory Lists for direct ownership
  //exclusion types = controlled, original, all, or list
  public void setDirectOwnershipTerritories(String value)
  {	
	  m_directOwnershipTerritories = value.split(":");
  }

  public String[]  getDirectOwnershipTerritories()
  {
      return m_directOwnershipTerritories;
  }
  public void setTerritoryCount(String value)
  {
	  m_territoryCount = getInt(value);
  }

  public int getTerritoryCount()
  {
      return m_territoryCount;
  }
  
  public void setPerOwnedTerritories(String value)
  {
      m_perOwnedTerritories = getInt(value);
  }

  public int getPerOwnedTerritories()
  {
      return m_perOwnedTerritories;
  }

  public void setAllowedUnitType(String value)
  {
	  m_allowedUnitType = value;
  }

  public String getAllowedUnitType()
  {
      return m_allowedUnitType;
  }

  public void setMovementRestrictionTerritories(String value)
  {
	  m_movementRestrictionTerritories = value.split(":");
  }
  
  public String[]  getMovementRestrictionTerritories()
  {
      return m_movementRestrictionTerritories;
  }
  
  public void setMovementRestrictionType(String value)
  {
	  m_movementRestrictionType = value;
  }

  public String getMovementRestrictionType()
  {
      return m_movementRestrictionType;
  }

  public void setProductionPerXTerritories(String value)
  {
	  m_productionPerTerritory = getInt(value);
  }

  public int getProductionPerXTerritories()
  {
      return m_productionPerTerritory;
  }

  public void setPlacementPerTerritory(String value)
  {
	  m_placementPerTerritory = getInt(value);
  }

  public int getPlacementPerTerritory()
  {
      return m_placementPerTerritory;
  }

  public void setMaxPlacePerTerritory(String value)
  {
	  m_maxPlacePerTerritory = getInt(value);
  }

  public int getMaxPlacePerTerritory()
  {
      return m_maxPlacePerTerritory;
  }

  public void setPlacementAnyTerritory(String value)
  {
	  m_placementAnyTerritory = getBool(value);
  }

  public boolean getPlacementAnyTerritory()
  {
      return m_placementAnyTerritory;
  }

  public void setPlacementCapturedTerritory(String value)
  {
      m_placementCapturedTerritory = getBool(value);
  }

  public boolean getPlacementCapturedTerritory()
  {
      return m_placementCapturedTerritory;
  }

  public void setPlacementInCapitalRestricted(String value)
  {
      m_placementInCapitalRestricted = getBool(value);
  }

  public boolean getPlacementInCapitalRestricted()
  {
      return m_placementInCapitalRestricted;
  }
  
  public void setUnlimitedProduction(String value)
  {
      m_unlimitedProduction = getBool(value);
  }

  public boolean getUnlimitedProduction()
  {
      return m_unlimitedProduction;
  }
  
  public void setDominatingFirstRoundAttack(String value)
  {
      m_dominatingFirstRoundAttack = getBool(value);
  }

  public boolean getDominatingFirstRoundAttack()
  {
      return m_dominatingFirstRoundAttack;
  }
  
  public void setNegateDominatingFirstRoundAttack(String value)
  {
      m_negateDominatingFirstRoundAttack = getBool(value);
  }

  public boolean getNegateDominatingFirstRoundAttack()
  {
      return m_negateDominatingFirstRoundAttack;
  }
  
  public int getAtWarCount() {
	  return m_atWarCount;
  }
  
  public void setAtWarCount(String s) {
	  m_atWarCount = getInt(s);
  }
  public int getUses() {
	  return m_uses;
  }
  
  public void setUses(String s) {
	  m_uses = getInt(s);
  }
  public void setUses(int u) {
	  m_uses = u;
  }
  
  public boolean getInvert() {
	  return m_invert;
  }

  public void setInvert(String s) {
	  m_invert = getBool(s);
  }

  public Set<PlayerID> getAtWarPlayers() {
	  return m_atWarPlayers;
  }
  public void setAtWarPlayers( String players) throws GameParseException{
	  {
	    	String[] s = players.split(":");
	    	int count = -1;
	    	if(s.length<1)
	    		throw new GameParseException( "Rules & Conditions: Empty enemy list");
	    	try {
	    		count = getInt(s[0]);
	    		m_atWarCount = count;
	    	} catch(Exception e) {
	    		m_atWarCount = 0;
	    	}
	    	if(s.length<1 || s.length ==1 && count != -1)
	    		throw new GameParseException( "Rules & Conditions: Empty enemy list");
	    	m_atWarPlayers = new HashSet<PlayerID>();
	    	for( int i=count==-1?0:1; i < s.length; i++){
	    		PlayerID player = getData().getPlayerList().getPlayerID(s[i]);
	            if(player == null)
	                throw new GameParseException("Rules & Conditions: Could not find player. name:" + s[i]);
	            else
	            	m_atWarPlayers.add(player);
	    	}
	    }
  }
  
  public void setTechs( String techs) throws GameParseException{
	  {
	    	String[] s = techs.split(":");
	    	int count = -1;
	    	if(s.length<1)
	    		throw new GameParseException( "Rules & Conditions: Empty tech list");
	    	try {
	    		count = getInt(s[0]);
	    		m_techCount = count;
	    	} catch(Exception e) {
	    		m_techCount = 0;
	    	}
	    	if(s.length<1 || s.length ==1 && count != -1)
	    		throw new GameParseException( "Rules & Conditions: Empty tech list");
	    	m_techs = new ArrayList<TechAdvance>();
	    	for( int i=count==-1?0:1; i < s.length; i++){
	    		TechAdvance ta = getData().getTechnologyFrontier().getAdvanceByProperty(s[i]);
	            if(ta==null)
	            	ta = getData().getTechnologyFrontier().getAdvanceByName(s[i]);
	            if(ta==null)
	            	throw new GameParseException("Rules & Conditions: Technology not found :"+s);
	    		m_techs.add(ta);
	    	}
	    }
  }
  
  public void setTurns(String turns) throws GameParseException{	  
	  m_turns = new HashMap<Integer,Integer>();
	  String[] s = turns.split(":");
	  if(s.length<1)
		  throw new GameParseException( "Rules & Conditions: Empty turn list");
	  for( int i=0; i < s.length; i++){
		  int start,end;
		  try {
			  start = getInt(s[i]);
			  end = start;
		  } catch(Exception e) {
			  String[] s2 = s[i].split("-");
			  if(s2.length!=2)
				  throw new GameParseException( "Rules & Conditions: Invalid syntax for range, must be 'int-int'");
			  start = getInt(s2[0]);
			  if( s2[1].equals("+")) {
				  end = Integer.MAX_VALUE;
			  }
			  else
				  end = getInt(s2[1]);
		  }
		  Integer t = new Integer(start);
		  Integer u = new Integer(end);
		  m_turns.put(t,u);
	  }
  }
  
  private boolean checkTurns(GameData data) {
	  int turn =data.getSequence().getRound();
	  for(Integer t:m_turns.keySet()){
		  if(turn>=t.intValue()&& turn <=m_turns.get(t).intValue())
			  return true;
	  }
	  return false;
  }
  
  public boolean isSatisfied(GameData data) {
	  boolean objectiveMet = true;
	  PlayerID player = (PlayerID) getAttatchedTo();
	  //
	  //check turn limits
	  //
	  if(m_turns!=null)
		  objectiveMet = checkTurns(data);
	  //
	  //Check for allied unit exclusions
	  //
	  if(getAlliedExclusionTerritories() != null && objectiveMet)
		{
			//Get the listed territories
			String[] terrs = getAlliedExclusionTerritories();
			String value = new String();    			
			
  		//If there's only 1, it might be a 'group' (original, controlled, all)
			if(terrs.length == 1)
			{    				
  			for(String name : terrs)
  			{
  				if(name.equals("original"))
  				{	//get all originally owned territories
  					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
      				Collection<Territory> originalTerrs = origOwnerTracker.getOriginallyOwned(data, player);
      				setTerritoryCount(String.valueOf(originalTerrs.size()));
      				//Colon delimit the collection as it would exist in the XML
      				for (Territory item : originalTerrs)
    					  	value = value + ":" + item;
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else if (name.equals("controlled"))
      			{
      				Collection<Territory> ownedTerrs = data.getMap().getTerritoriesOwnedBy(player);
      				setTerritoryCount(String.valueOf(ownedTerrs.size()));
      				//Colon delimit the collection as it would exist in the XML	        				
      				  for (Territory item : ownedTerrs)
      					  value = value + ":" + item;
      				  //Remove the leading colon
      				  value = value.replaceFirst(":", "");
      			}
  				else if (name.equals("all"))
      			{
      				Collection<Territory> allTerrs = data.getMap().getTerritoriesOwnedBy(player);
      				OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
      				allTerrs.addAll(origOwnerTracker.getOriginallyOwned(data, player));
      				setTerritoryCount(String.valueOf(allTerrs.size()));
      				//Colon delimit the collection as it would exist in the XML	        				
      				for (Territory item : allTerrs)
    					  value = value + ":" + item;
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
      			}
  				else
  				{	//The list just contained 1 territory
  					value = name;
  				}
  			}
			}
			else
			{
				//Get the list of territories	
				Collection<Territory> listedTerrs = getListedTerritories(terrs);
				//Colon delimit the collection as it exists in the XML
				for (Territory item : listedTerrs)
				  value = value + ":" + item;
				//Remove the leading colon
				value = value.replaceFirst(":", "");
			}

			//create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "allied", getTerritoryCount(), player,data);    			
		}

		//
		//Check for enemy unit exclusions (ANY UNITS)
		//TODO Transports and Subs don't count-- perhaps list types to exclude  
		//   		
		if(getEnemyExclusionTerritories() != null && objectiveMet == true)
		{
			//Get the listed territories
			String[] terrs = getEnemyExclusionTerritories();
			String value = new String();    			
			
  		//If there's only 1, it might be a 'group'  (original)
			if(terrs.length == 1)
			{    				
  			for(String name : terrs)
  			{
  				if(name.equals("original"))
  				{	//get all originally owned territories
  					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
      				Collection<Territory> enemyTerrs = origOwnerTracker.getOriginallyOwned(data, player);
      				setTerritoryCount(String.valueOf(enemyTerrs.size()));
      				//Colon delimit the collection as it would exist in the XML
      				for (Territory item : enemyTerrs)
    					  	value = value + ":" + item;
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else
  				{	//The list just contained 1 territory
  					value = name;
  				}
  			}
			}
			else
			{
				//Get the list of territories	
				Collection<Territory> listedTerrs = getListedTerritories(terrs);
				//Colon delimit the collection as it exists in the XML
				for (Territory item : listedTerrs)
				  value = value + ":" + item;
				//Remove the leading colon
				value = value.replaceFirst(":", "");
			}

			//create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "enemy", getTerritoryCount(), player,data);
		}

		//Check for enemy unit exclusions (SURFACE UNITS with ATTACK POWER)
		if(getEnemySurfaceExclusionTerritories() != null && objectiveMet == true)
		{
			//Get the listed territories
			String[] terrs = getEnemySurfaceExclusionTerritories();
			String value = new String();    			
			
  		//If there's only 1, it might be a 'group'  (original)
			if(terrs.length == 1)
			{    				
  			for(String name : terrs)
  			{
  				if(name.equals("original"))
  				{	//get all originally owned territories
  					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
      				Collection<Territory> enemyTerrs = origOwnerTracker.getOriginallyOwned(data, player);
      				setTerritoryCount(String.valueOf(enemyTerrs.size()));
      				//Colon delimit the collection as it would exist in the XML
      				for (Territory item : enemyTerrs)
    					  	value = value + ":" + item;
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else
  				{	//The list just contained 1 territory
  					value = name;
  				}
  			}
			}
			else
			{
				//Get the list of territories	
				Collection<Territory> listedTerrs = getListedTerritories(terrs);
				//Colon delimit the collection as it exists in the XML
				for (Territory item : listedTerrs)
				  value = value + ":" + item;
				//Remove the leading colon
				value = value.replaceFirst(":", "");
			}

			//create the String list from the XML/gathered territories
			terrs = value.split(":");
			objectiveMet = checkUnitExclusions(objectiveMet, getListedTerritories(terrs), "enemy_surface", getTerritoryCount(), player,data);
		}
		
		//
		//Check for Territory Ownership rules
		//
		if(getAlliedOwnershipTerritories() != null && objectiveMet == true)
		{
			//Get the listed territories
			String[] terrs = getAlliedOwnershipTerritories();
			String value = new String();    			
			
  		//If there's only 1, it might be a 'group' (original)
			if(terrs.length == 1)
			{    				
  			for(String name : terrs)
  			{
  				if(name.equals("original"))
  				{	
  					Collection<PlayerID> players = data.getPlayerList().getPlayers();
  					Iterator<PlayerID> playersIter = players.iterator();
  					while(playersIter.hasNext())
  					{
  						PlayerID currPlayer = playersIter.next();
  						if (data.getAllianceTracker().isAllied(currPlayer, player))
  						{
  							//get all originally owned territories
  	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
  	        				Collection<Territory> originalAlliedTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
  	        				setTerritoryCount(String.valueOf(originalAlliedTerrs.size()));
  	        				//Colon delimit the collection as it would exist in the XML
  	        				for (Territory item : originalAlliedTerrs)
  	      					  	value = value + ":" + item;
  						}
  					}
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else if(name.equals("enemy"))
  				{	//TODO Perhaps add a count to signify how many territories must be controlled- currently, it's ALL
  					Collection<PlayerID> players = data.getPlayerList().getPlayers();
  					Iterator<PlayerID> playersIter = players.iterator();
  					while(playersIter.hasNext())
  					{
  						PlayerID currPlayer = playersIter.next();
  						if (!data.getAllianceTracker().isAllied(currPlayer, player))
  						{
  							//get all originally owned territories
  	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
  	        				Collection<Territory> originalEnemyTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
  	        				setTerritoryCount(String.valueOf(originalEnemyTerrs.size()));
  	        				//Colon delimit the collection as it would exist in the XML
  	        				for (Territory item : originalEnemyTerrs)
  	      					  	value = value + ":" + item;
  						}
  					}
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else
  				{	//The list just contained 1 territory
  					value = name;
  				}
  			}	    			
			}
			else
			{
				//Get the list of territories	
				Collection<Territory> listedTerrs = getListedTerritories(terrs);
				//Colon delimit the collection as it exists in the XML
				for (Territory item : listedTerrs)
				  value = value + ":" + item;
				//Remove the leading colon
				value = value.replaceFirst(":", "");
			}

			//create the String list from the XML/gathered territories
			terrs = value.split(":");
			
			objectiveMet = checkAlliedOwnership(objectiveMet, getListedTerritories(terrs), getTerritoryCount(), player,data);			
		}

		//Direct Ownership mod by astabada

		//
		//Check for Direct Territory Ownership rules
		//
		if(getDirectOwnershipTerritories() != null && objectiveMet == true)
		{
			//Get the listed territories
			String[] terrs = getDirectOwnershipTerritories();
			String value = new String();    			
			
  		//If there's only 1, it might be a 'group' (original)
			if(terrs.length == 1)
			{    				
  			for(String name : terrs)
  			{
  				if(name.equals("original"))
  				{
					//get all originally owned territories
					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
    				Collection<Territory> originalAlliedTerrs = origOwnerTracker.getOriginallyOwned(data, player);
    				setTerritoryCount(String.valueOf(originalAlliedTerrs.size()));
    				//Colon delimit the collection as it would exist in the XML
    				for (Territory item : originalAlliedTerrs)
  					  	value = value + ":" + item;
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else if(name.equals("enemy"))
  				{	//TODO Perhaps add a count to signify how many territories must be controlled- currently, it's ALL
  					Collection<PlayerID> players = data.getPlayerList().getPlayers();
  					Iterator<PlayerID> playersIter = players.iterator();
  					while(playersIter.hasNext())
  					{
  						PlayerID currPlayer = playersIter.next();
  						if (!data.getAllianceTracker().isAllied(currPlayer, player))
  						{
  							//get all originally owned territories
  	    					OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
  	        				Collection<Territory> originalEnemyTerrs = origOwnerTracker.getOriginallyOwned(data, currPlayer);
  	        				setTerritoryCount(String.valueOf(originalEnemyTerrs.size()));
  	        				//Colon delimit the collection as it would exist in the XML
  	        				for (Territory item : originalEnemyTerrs)
  	      					  	value = value + ":" + item;
  						}
  					}
      				//Remove the leading colon
      				value = value.replaceFirst(":", "");
  				}
  				else
  				{	//The list just contained 1 territory
  					value = name;
  				}
  			}	    			
			}
			else
			{
				//Get the list of territories	
				Collection<Territory> listedTerrs = getListedTerritories(terrs);
				//Colon delimit the collection as it exists in the XML
				for (Territory item : listedTerrs)
				  value = value + ":" + item;
				//Remove the leading colon
				value = value.replaceFirst(":", "");
			}

			//create the String list from the XML/gathered territories
			terrs = value.split(":");

			objectiveMet = checkDirectOwnership(objectiveMet, getListedTerritories(terrs), getTerritoryCount(), player);			
		}

		if( getAtWarPlayers()!=null && objectiveMet == true) {
			objectiveMet = checkAtWar(player, getAtWarPlayers(), getAtWarCount(), data);
		}
		if( m_techs!=null && objectiveMet == true) {
			objectiveMet = checkTechs(player, data);
		}
		return objectiveMet!=m_invert;
  }
  /**
   * Called after the attatchment is created.
   */
  public void validate() throws GameParseException
  {
      if(m_alliedOwnershipTerritories != null && (!m_alliedOwnershipTerritories.equals("controlled") && !m_alliedOwnershipTerritories.equals("original") && !m_alliedOwnershipTerritories.equals("all")))
    	  getListedTerritories(m_alliedOwnershipTerritories);

      if(m_enemyExcludedTerritories != null && (!m_enemyExcludedTerritories.equals("controlled") && !m_enemyExcludedTerritories.equals("original") && !m_enemyExcludedTerritories.equals("all")))
    	  getListedTerritories(m_enemyExcludedTerritories);

      if(m_enemySurfaceExcludedTerritories != null && (!m_enemySurfaceExcludedTerritories.equals("controlled") && !m_enemySurfaceExcludedTerritories.equals("original") && !m_enemySurfaceExcludedTerritories.equals("all")))
    	  getListedTerritories(m_enemySurfaceExcludedTerritories);
      
      if(m_alliedExcludedTerritories != null && (!m_alliedExcludedTerritories.equals("controlled") && !m_alliedExcludedTerritories.equals("original") && !m_alliedExcludedTerritories.equals("all")))
    	  getListedTerritories(m_alliedExcludedTerritories);
      
      //Territory Lists for direct ownership
      if(m_directOwnershipTerritories != null && (!m_directOwnershipTerritories.equals("controlled") && !m_directOwnershipTerritories.equals("original") && !m_directOwnershipTerritories.equals("all")))
    	  getListedTerritories(m_directOwnershipTerritories);
      if(m_movementRestrictionTerritories != null && (!m_movementRestrictionTerritories.equals("controlled") && !m_movementRestrictionTerritories.equals("original") && !m_movementRestrictionTerritories.equals("all")))
    	  getListedTerritories(m_movementRestrictionTerritories);
  }
  
  //Validate that all listed territories actually exist
  public Collection<Territory> getListedTerritories(String[] list)    
  {
      List<Territory> rVal = new ArrayList<Territory>();
      
      for(String name : list)
      {
    	  //See if the first entry contains the number of territories needed to meet the criteria
    	  try
    	  {
    		  //Leave the temp field- it checks if the list just starts with a territory by failing the TRY
    		  int temp = getInt(name);
    		  setTerritoryCount(name);
    		  continue;    		  
    	  }
    	  catch(Exception e)
    	  {    		  
    	  }
    	  
    	  //Skip looking for the territory if the original list contains one of the 'group' commands
    	  if(name.equals("controlled") || name.equals("original") || name.equals("all"))
    		  break;
    	  
    	  //Validate all territories exist
          Territory territory = getData().getMap().getTerritory(name);
          if(territory == null)
              throw new IllegalStateException("Rules & Conditions: No territory called:" + name); 
          rVal.add(territory);
      }        
      return rVal;
  }
  
  /**
   * Checks for the collection of territories to see if they have units owned by the exclType alliance.  
   * It doesn't yet threshold the data
   */
  private boolean checkUnitExclusions(boolean satisfied, Collection<Territory> Territories, String exclType, int numberNeeded, PlayerID player, GameData data) 
	{
		int numberMet = 0;
		satisfied = false;
		
		Iterator<Territory> ownedTerrIter = Territories.iterator();
		//Go through the owned territories and see if there are any units owned by allied/enemy based on exclType
		while (ownedTerrIter.hasNext())
		{
			//get all the units in the territory
			Territory terr = ownedTerrIter.next();
			Collection<Unit> allUnits =  terr.getUnits().getUnits();
			
			if (exclType == "allied")
			{	//any allied units in the territory
				allUnits.removeAll(Match.getMatches(allUnits, Matches.unitIsOwnedBy(player)));
				Collection<Unit> playerUnits = Match.getMatches(allUnits, Matches.alliedUnit(player, data));
				if (playerUnits.size() < 1)
				{
					numberMet += 1;
					if(numberMet >= numberNeeded)
					{
						satisfied = true;
						break;
					}
				}
			}
			else if (exclType == "enemy")
			{	//any enemy units in the territory
				Collection<Unit> enemyUnits = Match.getMatches(allUnits, Matches.enemyUnit(player, data));
				if (enemyUnits.size() < 1)
				{
					numberMet += 1;
					if(numberMet >= numberNeeded)
					{
						satisfied = true;
						break;
					}
				}
			}
			else //if (exclType == "enemy_surface")
			{//any enemy units (not trn/sub) in the territory
				Collection<Unit> enemyUnits = Match.getMatches(allUnits, new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsNotSub, Matches.UnitIsNotTransport));
				if (enemyUnits.size() < 1)
				{
					numberMet += 1;
					if(numberMet >= numberNeeded)
					{
						satisfied = true;
						break;
					}
				}				
			}
		}		
		return satisfied;
	}
  
  /**
   * Checks for allied ownership of the collection of territories.  Once the needed number threshold is reached, the satisfied flag is set
   * to true and returned
   */
	private boolean checkAlliedOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player,GameData data) 
	{		
		int numberMet = 0;
		satisfied = false;
		
		Iterator<Territory> listedTerrIter = listedTerrs.iterator();
		    			
		while(listedTerrIter.hasNext())
		{
			Territory listedTerr = listedTerrIter.next();
			//if the territory owner is an ally
			if (data.getAllianceTracker().isAllied(listedTerr.getOwner(), player))
			{
				numberMet += 1;
				if(numberMet >= numberNeeded)
				{
					satisfied = true;
					break;
				}
			}
		}
		return satisfied;
	}
	
	/** astabada
     * Checks for direct ownership of the collection of territories.  Once the needed number threshold is reached, the satisfied flag is set
     * to true and returned
     */
	private boolean checkDirectOwnership(boolean satisfied, Collection<Territory> listedTerrs, int numberNeeded, PlayerID player) 
	{		
		int numberMet = 0;
		satisfied = false;
		
		Iterator<Territory> listedTerrIter = listedTerrs.iterator();
		    			
		while(listedTerrIter.hasNext())
		{
			Territory listedTerr = listedTerrIter.next();
			//if the territory owner is an ally
			if (listedTerr.getOwner() == player)
			{
				numberMet += 1;
				if(numberMet >= numberNeeded)
				{
					satisfied = true;
					break;
				}
			}
		}
		return satisfied;
	}
  
	private boolean checkAtWar(PlayerID player, Set<PlayerID> enemies, int count, GameData data) {
		int found =0;
		for( PlayerID e:enemies) 	
			if(data.getAllianceTracker().isAtWar(player,e)) 
				found++;
		if( count == 0)
			return count == found;
		else
			return  found >= count;
	}
	
	private boolean checkTechs(PlayerID player, GameData data) {
		int found =0;
		for( TechAdvance a: TechTracker.getTechAdvances(player,data)) 	
			if(m_techs.contains(a)) 
				found++;
		if( m_techCount == 0)
			return m_techCount == found;
		else
			return  found >= m_techCount;
	}
}
