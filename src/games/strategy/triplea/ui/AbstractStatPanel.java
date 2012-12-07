package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.stats.AbstractStat;
import games.strategy.triplea.util.PlayerOrderComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JPanel;

/**
 * Hold some common things like GameData for stats panels and other tab panels.
 * 
 * @author veqryn
 * 
 */
public abstract class AbstractStatPanel extends JPanel
{
	private static final long serialVersionUID = 1906611524937548809L;
	
	protected GameData m_data;
	
	/**
	 * Does not call initLayout() because initLayout may depend on some private tables being created with GameData first.
	 * So make sure you call initLayout() yourself.
	 * 
	 * @param data
	 */
	public AbstractStatPanel(final GameData data)
	{
		m_data = data;
	}
	
	/**
	 * You will need to call this yourself.
	 */
	abstract protected void initLayout();
	
	abstract public void setGameData(final GameData data);
	
	/**
	 * 
	 * @return all the alliances with more than one player.
	 */
	public Collection<String> getAlliances()
	{
		
		final Collection<String> rVal = new TreeSet<String>();
		for (final String alliance : m_data.getAllianceTracker().getAlliances())
		{
			if (m_data.getAllianceTracker().getPlayersInAlliance(alliance).size() > 1)
			{
				rVal.add(alliance);
			}
		}
		return rVal;
	}
	
	public List<PlayerID> getPlayers()
	{
		final List<PlayerID> players = new ArrayList<PlayerID>(m_data.getPlayerList().getPlayers());
		Collections.sort(players, new PlayerOrderComparator(m_data));
		return players;
	}
	
	
	class ResourceStat extends AbstractStat
	{
		final Resource m_resource;
		
		public ResourceStat(final Resource resource)
		{
			super();
			m_resource = resource;
		}
		
		public String getName()
		{
			return m_resource.getName();
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			return player.getResources().getQuantity(m_resource);
		}
	}
}
