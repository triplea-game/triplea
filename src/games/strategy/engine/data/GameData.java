/*
 * GameData.java
 *
 * Created on October 14, 2001, 7:11 AM
 */

package games.strategy.engine.data;

import java.util.*;
import games.strategy.engine.data.events.*;
import games.strategy.util.ListenerList;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameData {

	private ListenerList m_territoryListeners = new ListenerList();
	private ListenerList m_dataChangeListeners = new ListenerList();
	
	private final AllianceTracker m_alliances = new AllianceTracker(this);
	private final DelegateList m_delegateList = new DelegateList(this);
	private final GameMap m_map = new GameMap(this);
	private final PlayerList m_playerList = new PlayerList(this);
	private final ProductionFrontierList m_productionFrontierList = new ProductionFrontierList(this);
	private final ProductionRuleList m_productionRuleList = new ProductionRuleList(this);
	private final ResourceList m_resourceList = new ResourceList(this);
	private final GameSequence m_sequence = new GameSequence(this);
	private final UnitTypeList m_unitTypeList = new UnitTypeList(this);
	private final GameProperties m_properties = new GameProperties(this);
	
	/** Creates new GameData */
    public GameData() 
	{
    }
	
	public GameMap getMap()
	{
		return m_map;
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
	
	protected void notifyGameDataChanged()
	{
		Iterator iter = m_dataChangeListeners.iterator();
		while(iter.hasNext())
		{
			GameDataChangeListener listener = (GameDataChangeListener) iter.next();
			listener.gameDataChanged();
		}
	}
			
}

