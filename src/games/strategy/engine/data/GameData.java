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
 * GameData.java
 *
 * Created on October 14, 2001, 7:11 AM
 */

package games.strategy.engine.data;

import java.util.*;

import games.strategy.engine.data.events.*;
import games.strategy.engine.framework.*;
import games.strategy.util.*;
import games.strategy.engine.history.*;

/**
 *
 * Central place to find all the information for a running game.
 * Using this object you can find the territores, connections, production rules,
 * unit types...
 * 
 * Everything in the xml game file.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameData implements java.io.Serializable
{
	private String m_gameName;
	private Version m_gameVersion;

	private transient ListenerList m_territoryListeners = new ListenerList();
	private transient ListenerList m_dataChangeListeners = new ListenerList();

	private final AllianceTracker m_alliances = new AllianceTracker(this);
	private final  DelegateList m_delegateList;
	private final GameMap m_map = new GameMap(this);
	private final PlayerList m_playerList = new PlayerList(this);
	private final ProductionFrontierList m_productionFrontierList = new ProductionFrontierList(this);
	private final ProductionRuleList m_productionRuleList = new ProductionRuleList(this);
	private final ResourceList m_resourceList = new ResourceList(this);
	private final GameSequence m_sequence = new GameSequence(this);
	private final UnitTypeList m_unitTypeList = new UnitTypeList(this);
	private final GameProperties m_properties = new GameProperties(this);
    private final UnitsList m_unitsList = new UnitsList();

	private IGameLoader m_loader;
    private final History m_gameHistory = new History(this);

	/** Creates new GameData */
	public GameData()
	{
		m_delegateList = new DelegateList(this);
	}

	public GameMap getMap()
	{
		return m_map;
	}

    public UnitsList getUnits()
    {
        return m_unitsList;
    }

	public PlayerList getPlayerList()
	{
		return m_playerList;
	}

	public ResourceList getResourceList()
	{
		return m_resourceList;
	}

	public ProductionFrontierList getProductionFrontierList()
	{
		return m_productionFrontierList;
	}

	public ProductionRuleList getProductionRuleList()
	{
		return m_productionRuleList;
	}

	public AllianceTracker getAllianceTracker()
	{
		return m_alliances;
	}

	public GameSequence getSequence()
	{
		return m_sequence;
	}

	public UnitTypeList getUnitTypeList()
	{
		return m_unitTypeList;
	}

	public DelegateList getDelegateList()
	{
		return m_delegateList;
	}

	public UnitHolder getUnitHolder(String name)
	{
		//two choices
		if(m_map.getTerritory(name) != null)
			return m_map.getTerritory(name);
		return m_playerList.getPlayerID(name);
	}

	public GameProperties getProperties()
	{
		return m_properties;
	}

	public void addTerritoryListener(TerritoryListener listener)
	{
		m_territoryListeners.add(listener);
	}

	public void removeTerritoryListener(TerritoryListener listener)
	{
		m_territoryListeners.remove(listener);
	}

	public void addDataChangeListener(GameDataChangeListener listener)
	{
		m_dataChangeListeners.add(listener);
	}

	public void removeDataChangeListener(GameDataChangeListener listener)
	{
		m_dataChangeListeners.remove(listener);
	}


	protected void notifyTerritoryUnitsChanged(Territory t)
	{
		Iterator iter = m_territoryListeners.iterator();
		while(iter.hasNext())
		{
			TerritoryListener listener = (TerritoryListener) iter.next();
			listener.unitsChanged(t);
		}
	}

	protected void notifyTerritoryOwnerChanged(Territory t)
	{
		Iterator iter = m_territoryListeners.iterator();
		while(iter.hasNext())
		{
			TerritoryListener listener = (TerritoryListener) iter.next();
			listener.ownerChanged(t);
		}
	}

	protected void notifyGameDataChanged(Change aChange)
	{
		Iterator iter = m_dataChangeListeners.iterator();
		while(iter.hasNext())
		{
			GameDataChangeListener listener = (GameDataChangeListener) iter.next();
			listener.gameDataChanged(aChange);
		}
	}

	public IGameLoader getGameLoader()
	{
		return m_loader;
	}

	protected void setGameLoader(IGameLoader loader)
	{
		m_loader = loader;
	}

	void setGameVersion(Version version)
	{
		m_gameVersion = version;
	}

	public Version getGameVersion()
	{
		return m_gameVersion;
	}

	void setGameName(String gameName)
	{
		m_gameName = gameName;
	}

	public String getGameName()
	{
		return m_gameName;
	}

    public History getHistory()
    {
        return m_gameHistory;
    }

	public void postSerialize()
	{
		m_territoryListeners = new ListenerList();;
		m_dataChangeListeners = new ListenerList();;
	}
}
