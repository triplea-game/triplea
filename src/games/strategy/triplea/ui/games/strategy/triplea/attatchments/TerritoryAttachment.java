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
 * @author  Sean Bridges
 * @version 1.0
 */
public class TerritoryAttachment extends DefaultAttachment
{

    public static Territory getCapital(PlayerID player, GameData data)
    {
        Iterator iter = data.getMap().getTerritories().iterator();
        while(iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            TerritoryAttachment ta = TerritoryAttachment.get(current);
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
    }

    /**
     * Convenience method.
     */
    public static TerritoryAttachment get(Territory t)
    {
        TerritoryAttachment rVal =  (TerritoryAttachment) t.getAttachment(Constants.TERRITORY_ATTATCHMENT_NAME);
        if(rVal == null && !t.isWater())
            throw new IllegalStateException("No territory attachment for:" + t.getName());
        return rVal;
    }

    private String m_capital = null;
    private boolean m_originalFactory = false;
    private int m_production = 2;  
    private boolean m_isVictoryCity = false;
    private boolean m_isImpassible = false;
    private PlayerID m_originalOwner = null;
    private PlayerID m_occupiedTerrOf = null;    
    private boolean m_isConvoyRoute = false;
    private boolean m_changeUnitOwners = false;
    private String m_convoyAttached = null;
    private boolean m_navalBase = false;
    private boolean m_airBase = false;
    private boolean m_kamikazeZone = false;
    private int m_unitProduction = 0;  
    //private String m_unitProduction = "0";  

  /** Creates new TerritoryAttatchment */
  public TerritoryAttachment()
  {
  }

    public void setIsImpassible(String value)
    {
        m_isImpassible = getBool(value);
    }

    public boolean isImpassible()
    {
        return m_isImpassible;
    }

    public void setCapital(String value)
    {
        m_capital = value;
    }

    public boolean isCapital()
    {
        return m_capital != null;
    }

    public String getCapital()
    {
        return m_capital;
    }

    public void setVictoryCity(String value)
    {
        m_isVictoryCity = getBool(value);
    }

    public boolean isVictoryCity()
    {
        return m_isVictoryCity;
    }
    
    public void setOriginalFactory(String value)
    {
        m_originalFactory = getBool(value);
    }

    public boolean isOriginalFactory()
    {
        return m_originalFactory;
    }

    public void setProduction(String value)
    {
        m_production = getInt(value);
    }

    public int getProduction()
    {
        return m_production;
    }

    public void setUnitProduction(Integer value)
    {
        m_unitProduction = value;       
    }

    public void setUnitProduction(String value)
    {
        m_unitProduction = Integer.parseInt(value);       
    }
    
    public int getUnitProduction()
    {     
        return m_unitProduction;
    }

    public void setOccupiedTerrOf(String value)
    {
    	if (value != null)
    		m_occupiedTerrOf = getData().getPlayerList().getPlayerID(value);
    }

    public PlayerID getOccupiedTerrOf()
    {
        return m_occupiedTerrOf;
    }
    
    public void setOriginalOwner(PlayerID player)
    {
        m_originalOwner = player;
    }

    public PlayerID getOriginalOwner()
    {
        return m_originalOwner;
    }
    
    public void setConvoyRoute(String value)
    {
        m_isConvoyRoute = getBool(value);
    }
    
    public boolean isConvoyRoute()
    {
        return m_isConvoyRoute;
    }
    
    public void setChangeUnitOwners(String value)
    {
        m_changeUnitOwners = getBool(value);
    }
    
    public boolean getChangeUnitOwners()
    {
        return m_changeUnitOwners;
    }
    
    public void setConvoyAttached(String value)
    {
        m_convoyAttached = value;
    }

    public String getConvoyAttached()
    {
        return m_convoyAttached;
    }
       
    public void setNavalBase(String value)
    {
    	m_navalBase = getBool(value);
    }
    
    public boolean isNavalBase()
    {
    	return m_navalBase;
    }
    
    public void setAirBase(String value)
    {
    	m_airBase = getBool(value);
    }
    
    public boolean isAirBase()
    {
    	return m_airBase;
    }
        
    public boolean isKamikazeZone()
    {
    	return m_kamikazeZone;
    }
    
    public void setKamikazeZone(String value)
    {
    	m_kamikazeZone = getBool(value);
    }
}
