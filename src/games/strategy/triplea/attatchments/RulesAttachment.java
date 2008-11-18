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
import games.strategy.triplea.Constants;

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
     * Convenience method.
     */
    public static RulesAttachment get(Rule r)
    {
    	RulesAttachment rVal =  (RulesAttachment) r.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        if(rVal == null)
            throw new IllegalStateException("No rule attachment for:" + r.getName());
        return rVal;
    }


    private PlayerID m_ruleOwner = null;
    private int m_objectiveValue = 0;
    private String m_alliedExclusion = null;
    private String m_enemyExclusion = null;
    private int m_alliedOwnershipTerritoryCount = -1;
    private String[] m_alliedOwnershipTerritories;
    private String m_territories = null;
    private int m_perOwnedTerritories = -1;
    private String m_allowedUnitType = null;
    
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
  
  //exclusion types = controlled, original, all, or list
  public void setAlliedExclusion(String value)
  {
      m_alliedExclusion = value;
  }

  public String getAlliedExclusion()
  {
      return m_alliedExclusion;
  }
  
//exclusion types = controlled, original, all, or list
  public void setAlliedOwnershipTerritories(String value)
  {
	  m_alliedOwnershipTerritories = value.split(":");
  }

  public String[] getAlliedOwnershipTerritories()
  {
      return m_alliedOwnershipTerritories;
  }
  
  //exclusion types = controlled, original, all, or list
  public void setEnemyExclusion(String value)
  {
      m_enemyExclusion = value;
  }

  public String getEnemyExclusion()
  {
      return m_enemyExclusion;
  }
  
  public void setAlliedOwnershipTerritoryCount(String value)
  {
	  m_alliedOwnershipTerritoryCount = getInt(value);
  }

  public int getAlliedOwnershipTerritoryCount()
  {
      return m_alliedOwnershipTerritoryCount;
  }
  
/*  public void setTerritories(String value)
  {
	  m_territories = value;
  }

  public String getTerritories()
  {
      return m_territories;
  }*/

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

  

  /**
   * Called after the attatchment is created.
   */
  public void validate() throws GameParseException
  {
      if(m_objectiveValue == 0 || ((m_alliedOwnershipTerritories == null || m_alliedOwnershipTerritories.length == 0) && m_alliedExclusion == null && m_enemyExclusion == null))
          throw new IllegalStateException("ObjectiveAttachment error for:" + m_ruleOwner + " not all variables set");
      getLandTerritories();
  }
  
  //Validate that all listed territories actually exist
  public Collection<Territory> getLandTerritories()    
  {
      List<Territory> rVal = new ArrayList<Territory>();
      
      for(String name : m_alliedOwnershipTerritories)
      {
    	  try
    	  {
    		  int temp = getInt(name);
    		  setAlliedOwnershipTerritoryCount(name);
    		  continue;
    	  }
    	  catch(Exception e)
    	  {    		  
    	  }
          Territory territory = getData().getMap().getTerritory(name);
          if(territory == null)
              throw new IllegalStateException("No territory called:" + territory); 
          rVal.add(territory);
      }        
      return rVal;
  }
  
  /*
  //The input to this contains a count and a list of matched pairs of strings
  public void setTerritoryRequirements(String value)
  {	  
	  String[] itemValues = stringToArray(value);
	  m_territoryRequirementsCount = getInt(itemValues[0]);
	  
	  for(int i =1; i < itemValues.length; i++)
      {
          String current = new String(itemValues[i]);
          i++;
          String next = new String(itemValues[i]);
          
          m_territoryRequirementsName = current;
          m_territoryRequirementsValue = next;
      }
	 
  }*/
  
  /*public static Set<Territory> getAllListedTerritories(String territoryRequirements, GameData data)
  {
      Set<Territory> rVal = new HashSet<Territory>();       
      for(Territory t : data.getMap())
      {
          Set<RulesAttachment> ruleAttachments = get(t);
          if(ruleAttachments.isEmpty())
              continue;
          
          Iterator<RulesAttachment> iter = ruleAttachments.iterator();
          while(iter.hasNext() )
          {
        	  RulesAttachment ruleAttachment = iter.next();
              if (ruleAttachment.getRuleName().equals(canalName))
              {
                  rVal.add(t);
              }
          }
      }
	return rVal;
  }
  
  public static Set<RulesAttachment> get(Territory t)
  {
      Set<RulesAttachment> rVal = new HashSet<RulesAttachment>();
      Map<String, IAttachment> map = t.getAttachments();
      Iterator<String> iter = map.keySet().iterator();
      while(iter.hasNext() )
      {
          IAttachment attachment = map.get(iter.next());
          String name = attachment.getName();
          if (name.startsWith(Constants.RULES_OBJECTIVE_PREFIX))
          {
              rVal.add((RulesAttachment)attachment);
          }
      }
      return rVal;
      
  }
  
  
  public static String [] stringToArray(String str) {
	    StringTokenizer t = new StringTokenizer(str, ",");
	    String [] array = new String[t.countTokens()];

	    for(int i=0; t.hasMoreTokens(); ++i) {
	      array[i] = t.nextToken();
	    }
	    return(array);
	  }
*/
}
