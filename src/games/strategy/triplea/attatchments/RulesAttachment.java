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
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import games.strategy.engine.data.*;

/**
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class RulesAttachment extends DefaultAttachment
{

/*    public static Territory getCapital(PlayerID player, GameData data)
    {
        Iterator iter = data.getMap().getRules().iterator();
        while(iter.hasNext())
        {
        	Rule current = (Rule) iter.next();
            RulesAttachment ta = RulesAttachment.get(current);
            if(ta != null && ta.getCapital() != null)
            {
                PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
                if(whoseCapital == null)
                    throw new IllegalStateException("Invalid capital for player name:" + ta.getCapital());

                if(player.equals(whoseCapital))
                    return current;
            }
        }
        //Added check for optional players- no error thrown for them
        if(player.getOptional())
        	return null;
        
        throw new IllegalStateException("Capital not found for:" + player);
    }*/


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
    private int m_territoryOwner = -1;
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
  public void setEnemyExclusion(String value)
  {
      m_enemyExclusion = value;
  }

  public String getEnemyExclusion()
  {
      return m_enemyExclusion;
  }

  public void setTerritoryOwner(String value)
  {
      m_territoryOwner = getInt(value);
  }

  public int getTerritoryOwner()
  {
      return m_territoryOwner;
  }
  
  public void setTerritories(String value)
  {
	  m_territories = value;
  }

  public String getTerritories()
  {
      return m_territories;
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

}
