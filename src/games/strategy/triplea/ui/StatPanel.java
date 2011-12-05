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
 * StatPanel.java
 * 
 * Created on January 6, 2002, 4:07 PM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.AbstractStat;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

/**
 * 
 * @author Sean Bridges
 */
@SuppressWarnings("serial")
public class StatPanel extends JPanel
{
	private final StatTableModel m_dataModel;
	private final TechTableModel m_techModel;
	private final ResourceTableModel m_resourceModel;
	private IStat[] m_stats;
	private IStat[] m_statsExtended = new IStat[] {};
	private IStat[] m_statsResource;
	protected GameData m_data;
	private final JTable m_statsTable;
	private Image m_statsImage = null;
	
	/** Creates a new instance of InfoPanel */
	
	public StatPanel(final GameData data)
	{
		m_data = data;
		setLayout(new GridLayout(3, 1));
		m_dataModel = new StatTableModel();
		m_techModel = new TechTableModel();
		m_resourceModel = new ResourceTableModel();
		m_statsTable = new JTable(m_dataModel)
		{
			@Override
			public void print(final Graphics g)
			{
				if (m_statsImage != null)
					g.drawImage(m_statsImage, 0, 0, null, null);
				super.print(g);
			}
		};
		
		JTable table = m_statsTable;
		table.getTableHeader().setReorderingAllowed(false);
		// Set width of country column
		TableColumn column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(175);
		JScrollPane scroll = new JScrollPane(table);
		add(scroll);
		
		table = new JTable(m_resourceModel);
		table.getTableHeader().setReorderingAllowed(true);
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(175);
		scroll = new JScrollPane(table);
		add(scroll);
		
		table = new JTable(m_techModel);
		// Strangely, this is enabled by default
		table.getTableHeader().setReorderingAllowed(false);
		// Make the technology column big. Value chosen by trial and error
		// The right way to do this is probably to get a FontMetrics object
		// and measure the pixel width of the longest technology name in the
		// current font.
		column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(500);
		scroll = new JScrollPane(table);
		// add(scroll, BorderLayout.SOUTH);
		add(scroll);
	}
	
	private void fillExtendedStats(final GameData data)
	{
		// add other resources, other than PUs and tech tokens
		final List<Resource> resources = data.getResourceList().getResources();
		for (final Resource r : resources)
		{
			if (r.getName().equals(Constants.PUS) || r.getName().equals(Constants.TECH_TOKENS))
				continue;
			else
			{
				final GenericResourceStat resourceStat = new GenericResourceStat();
				resourceStat.init(r.getName());
				final List<IStat> statsExtended = new ArrayList<IStat>(Arrays.asList(m_statsExtended));
				statsExtended.add(resourceStat);
				m_statsExtended = statsExtended.toArray(new IStat[statsExtended.size()]);
			}
		}
		// add tech related stuff
		if (games.strategy.triplea.Properties.getTechDevelopment(data))
		{
			// add tech tokens
			if (data.getResourceList().getResource(Constants.TECH_TOKENS) != null)
			{
				final List<IStat> statsExtended = new ArrayList<IStat>(Arrays.asList(m_statsExtended));
				statsExtended.add(new TechTokenStat());
				m_statsExtended = statsExtended.toArray(new IStat[statsExtended.size()]);
			}
			// add number of techs
			if (true)
			{
				final List<IStat> statsExtended = new ArrayList<IStat>(Arrays.asList(m_statsExtended));
				statsExtended.add(new TechCountStat());
				m_statsExtended = statsExtended.toArray(new IStat[statsExtended.size()]);
			}
			// add individual techs
			final Iterator<TechAdvance> allTechsIter = TechAdvance.getTechAdvances(m_data, null).iterator();
			while (allTechsIter.hasNext())
			{
				final TechAdvance ta = allTechsIter.next();
				final GenericTechNameStat techNameStat = new GenericTechNameStat();
				techNameStat.init(ta);
				final List<IStat> statsExtended = new ArrayList<IStat>(Arrays.asList(m_statsExtended));
				statsExtended.add(techNameStat);
				m_statsExtended = statsExtended.toArray(new IStat[statsExtended.size()]);
			}
		}
		// now add actual number of each unit type (holy gumdrops batman, this is going to be long!)
		final Iterator<UnitType> allUnitTypes = data.getUnitTypeList().iterator();
		while (allUnitTypes.hasNext())
		{
			final UnitType ut = allUnitTypes.next();
			final GenericUnitNameStat unitNameStat = new GenericUnitNameStat();
			unitNameStat.init(ut);
			final List<IStat> statsExtended = new ArrayList<IStat>(Arrays.asList(m_statsExtended));
			statsExtended.add(unitNameStat);
			m_statsExtended = statsExtended.toArray(new IStat[statsExtended.size()]);
		}
	}
	
	public void setGameData(final GameData data)
	{
		m_data = data;
		m_dataModel.setGameData(data);
		m_techModel.setGameData(data);
		m_resourceModel.setGameData(data);
		m_dataModel.gameDataChanged(null);
		m_techModel.gameDataChanged(null);
		m_resourceModel.gameDataChanged(null);
	}
	
	public void setStatsBgImage(final Image image)
	{
		m_statsImage = image;
	}
	
	public JTable getStatsTable()
	{
		return m_statsTable;
	}
	
	/**
	 * 
	 * @return all the alliances with more than one player.
	 */
	public Collection<String> getAlliances()
	{
		final Iterator<String> allAlliances = m_data.getAllianceTracker().getAlliances().iterator();
		// order the alliances use a Tree Set
		final Collection<String> rVal = new TreeSet<String>();
		
		while (allAlliances.hasNext())
		{
			final String alliance = allAlliances.next();
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
	
	public IStat[] getStats()
	{
		return m_stats;
	}
	
	public IStat[] getStatsExtended(final GameData data)
	{
		if (m_statsExtended.length == 0)
			fillExtendedStats(data);
		return m_statsExtended;
	}
	
	
	class ResourceTableModel extends AbstractTableModel implements GameDataChangeListener
	{
		private boolean m_isDirty = true;
		private String[][] m_collectedData;
		
		public ResourceTableModel()
		{
			setResourceCollums();
			m_data.addDataChangeListener(this);
			m_isDirty = true;
		}
		
		private void setResourceCollums()
		{
			final List<IStat> statList = new ArrayList<IStat>();
			for (final Resource resource : m_data.getResourceList().getResources())
			{
				if (resource.getName().equals(Constants.TECH_TOKENS) || resource.getName().equals(Constants.PUS))
					continue;
				statList.add(new ResourceStat(resource));
			}
			m_statsResource = statList.toArray(new IStat[statList.size()]);
		}
		
		public synchronized Object getValueAt(final int row, final int col)
		{
			if (m_isDirty)
			{
				loadData();
				m_isDirty = false;
			}
			return m_collectedData[row][col];
			
		}
		
		private synchronized void loadData()
		{
			m_data.acquireReadLock();
			try
			{
				final List<PlayerID> players = getPlayers();
				final Collection<String> alliances = getAlliances();
				m_collectedData = new String[players.size() + alliances.size()][m_statsResource.length + 1];
				int row = 0;
				for (final PlayerID player : players)
				{
					m_collectedData[row][0] = player.getName();
					for (int i = 0; i < m_statsResource.length; i++)
					{
						m_collectedData[row][i + 1] = m_statsResource[i].getFormatter().format(m_statsResource[i].getValue(player, m_data));
					}
					row++;
				}
				final Iterator<String> allianceIterator = alliances.iterator();
				while (allianceIterator.hasNext())
				{
					final String alliance = allianceIterator.next();
					m_collectedData[row][0] = alliance;
					for (int i = 0; i < m_statsResource.length; i++)
					{
						m_collectedData[row][i + 1] = m_statsResource[i].getFormatter().format(m_statsResource[i].getValue(alliance, m_data));
					}
					row++;
				}
			} finally
			{
				m_data.releaseReadLock();
			}
		}
		
		public void gameDataChanged(final Change aChange)
		{
			synchronized (this)
			{
				m_isDirty = true;
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
				}
			});
		}
		
		@Override
		public String getColumnName(final int col)
		{
			if (col == 0)
				return "Player";
			return m_statsResource[col - 1].getName();
		}
		
		public int getColumnCount()
		{
			return m_statsResource.length + 1;
		}
		
		public synchronized int getRowCount()
		{
			if (!m_isDirty)
				return m_collectedData.length;
			else
			{
				m_data.acquireReadLock();
				try
				{
					return m_data.getPlayerList().size() + getAlliances().size();
				} finally
				{
					m_data.releaseReadLock();
				}
			}
		}
		
		public synchronized void setGameData(final GameData data)
		{
			synchronized (this)
			{
				m_data.removeDataChangeListener(this);
				m_data = data;
				m_data.addDataChangeListener(this);
				m_isDirty = true;
			}
			repaint();
		}
		
	}
	

	/*
	 * Custom table model.
	 * 
	 * This model is thread safe.
	 */
	class StatTableModel extends AbstractTableModel implements GameDataChangeListener
	{
		/* Flag to indicate whether data needs to be recalculated */
		private boolean m_isDirty = true;
		/* Column Header Names */
		/* Underlying data for the table */
		private String[][] m_collectedData;
		
		public StatTableModel()
		{
			setStatCollums();
			m_data.addDataChangeListener(this);
			m_isDirty = true;
		}
		
		public void setStatCollums()
		{
			m_stats = new IStat[] { new PUStat(), new ProductionStat(), new UnitsStat(), new TUVStat() };
			if (Match.someMatch(m_data.getMap().getTerritories(), Matches.TerritoryIsVictoryCity))
			{
				final List<IStat> stats = new ArrayList<IStat>(Arrays.asList(m_stats));
				stats.add(new VictoryCityStat());
				m_stats = stats.toArray(new IStat[stats.size()]);
			}
			// only add the vps in pacific
			if (m_data.getProperties().get(Constants.PACIFIC_THEATER, false))
			{
				final List<IStat> stats = new ArrayList<IStat>(Arrays.asList(m_stats));
				stats.add(new VPStat());
				m_stats = stats.toArray(new IStat[stats.size()]);
			}
		}
		
		private synchronized void loadData()
		{
			m_data.acquireReadLock();
			try
			{
				final List<PlayerID> players = getPlayers();
				final Collection<String> alliances = getAlliances();
				m_collectedData = new String[players.size() + alliances.size()][m_stats.length + 1];
				int row = 0;
				final Iterator<PlayerID> playerIter = players.iterator();
				while (playerIter.hasNext())
				{
					final PlayerID player = playerIter.next();
					m_collectedData[row][0] = player.getName();
					for (int i = 0; i < m_stats.length; i++)
					{
						m_collectedData[row][i + 1] = m_stats[i].getFormatter().format(m_stats[i].getValue(player, m_data));
					}
					row++;
				}
				final Iterator<String> allianceIterator = alliances.iterator();
				while (allianceIterator.hasNext())
				{
					final String alliance = allianceIterator.next();
					m_collectedData[row][0] = alliance;
					for (int i = 0; i < m_stats.length; i++)
					{
						m_collectedData[row][i + 1] = m_stats[i].getFormatter().format(m_stats[i].getValue(alliance, m_data));
					}
					row++;
				}
			} finally
			{
				m_data.releaseReadLock();
			}
		}
		
		public void gameDataChanged(final Change aChange)
		{
			synchronized (this)
			{
				m_isDirty = true;
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
				}
			});
		}
		
		/*
		 * Recalcs the underlying data in a lazy manner Limitation: This is not
		 * a threadsafe implementation
		 */
		public synchronized Object getValueAt(final int row, final int col)
		{
			if (m_isDirty)
			{
				loadData();
				m_isDirty = false;
			}
			return m_collectedData[row][col];
		}
		
		// Trivial implementations of required methods
		@Override
		public String getColumnName(final int col)
		{
			if (col == 0)
				return "Player";
			return m_stats[col - 1].getName();
		}
		
		public int getColumnCount()
		{
			return m_stats.length + 1;
		}
		
		public synchronized int getRowCount()
		{
			if (!m_isDirty)
				return m_collectedData.length;
			else
			{
				// no need to recalculate all the stats just to get the row count
				// getting the row count is a fairly frequent operation, and will
				// happen even if we are not displayed!
				m_data.acquireReadLock();
				try
				{
					return m_data.getPlayerList().size() + getAlliances().size();
				} finally
				{
					m_data.releaseReadLock();
				}
			}
		}
		
		public synchronized void setGameData(final GameData data)
		{
			synchronized (this)
			{
				m_data.removeDataChangeListener(this);
				m_data = data;
				m_data.addDataChangeListener(this);
				m_isDirty = true;
			}
			repaint();
		}
	}
	

	class TechTableModel extends AbstractTableModel implements GameDataChangeListener
	{
		/* Flag to indicate whether data needs to be recalculated */
		private boolean isDirty = true;
		/* Column Header Names */
		/* Row Header Names */
		private String[] colList;
		/* Underlying data for the table */
		private String[][] data;
		/* Convenience mapping of country names -> col */
		private Map<String, Integer> colMap = null;
		/* Convenience mapping of technology names -> row */
		private Map<String, Integer> rowMap = null;
		
		public TechTableModel()
		{
			m_data.addDataChangeListener(this);
			initColList();
			/* Load the country -> col mapping */
			colMap = new HashMap<String, Integer>();
			for (int i = 0; i < colList.length; i++)
			{
				colMap.put(colList[i], new Integer(i + 1));
			}
			/*
			 * .size()+1 added to stop index out of bounds errors when using an
			 * Italian player.
			 */
			boolean useTech = false;
			if (m_data.getResourceList().getResource(Constants.TECH_TOKENS) != null)
			{
				useTech = true;
				data = new String[TechAdvance.getTechAdvances(m_data, null).size() + 1][colList.length + 2];
			}
			else
			{
				data = new String[TechAdvance.getTechAdvances(m_data, null).size()][colList.length + 1];
			}
			/* Load the technology -> row mapping */
			rowMap = new HashMap<String, Integer>();
			final Iterator<TechAdvance> iter = TechAdvance.getTechAdvances(m_data, null).iterator();
			int row = 0;
			if (useTech)
			{
				rowMap.put("Tokens", new Integer(row));
				data[row][0] = "Tokens";
				row++;
			}
			while (iter.hasNext())
			{
				final TechAdvance tech = iter.next();
				rowMap.put((tech).getName(), new Integer(row));
				data[row][0] = tech.getName();
				row++;
			}
			clearAdvances();
		}
		
		private void clearAdvances()
		{
			/* Initialize the table with the tech names */
			for (int i = 0; i < data.length; i++)
			{
				for (int j = 1; j <= colList.length; j++)
				{
					data[i][j] = "";
				}
			}
		}
		
		private void initColList()
		{
			final java.util.List<PlayerID> players = new ArrayList<PlayerID>(m_data.getPlayerList().getPlayers());
			colList = new String[players.size()];
			for (int i = 0; i < players.size(); i++)
			{
				colList[i] = players.get(i).getName();
			}
			Arrays.sort(colList, 0, players.size());
		}
		
		public void update()
		{
			clearAdvances();
			// copy so aquire/release read lock are on the same object!
			final GameData gameData = m_data;
			gameData.acquireReadLock();
			try
			{
				final Iterator<PlayerID> playerIter = gameData.getPlayerList().getPlayers().iterator();
				while (playerIter.hasNext())
				{
					final PlayerID pid = playerIter.next();
					if (colMap.get(pid.getName()) == null)
						throw new IllegalStateException("Unexpected player in GameData.getPlayerList()" + pid.getName());
					final int col = colMap.get(pid.getName()).intValue();
					int row = 0;
					boolean useTokens = false;
					if (m_data.getResourceList().getResource(Constants.TECH_TOKENS) != null)
					{
						useTokens = true;
						final Integer tokens = pid.getResources().getQuantity(Constants.TECH_TOKENS);
						data[row][col] = tokens.toString();
					}
					Iterator<TechAdvance> advances = TechTracker.getTechAdvances(pid, m_data).iterator();
					while (advances.hasNext())
					{
						final TechAdvance advance = advances.next();
						row = rowMap.get(advance.getName()).intValue();
						// System.err.println("(" + row + ", " + col + ")");
						data[row][col] = "X";
						// data[row][col] = colList[col].substring(0, 1);
					}
					advances = TechAdvance.getTechAdvances(m_data, null).iterator();
					final List<TechAdvance> has = TechAdvance.getTechAdvances(m_data, pid);
					while (advances.hasNext())
					{
						final TechAdvance advance = advances.next();
						// if(!pid.getTechnologyFrontierList().getAdvances().contains(advance)){
						if (!has.contains(advance))
						{
							row = rowMap.get(advance.getName()).intValue();
							data[row][col] = "-";
						}
					}
				}
			} finally
			{
				gameData.releaseReadLock();
			}
		}
		
		@Override
		public String getColumnName(final int col)
		{
			if (col == 0)
				return "Technology";
			return colList[col - 1].substring(0, 1);
		}
		
		/*
		 * Recalcs the underlying data in a lazy manner Limitation: This is not
		 * a threadsafe implementation
		 */
		public Object getValueAt(final int row, final int col)
		{
			if (isDirty)
			{
				update();
				isDirty = false;
			}
			return data[row][col];
		}
		
		// Trivial implementations of required methods
		public int getColumnCount()
		{
			return colList.length + 1;
		}
		
		public int getRowCount()
		{
			return data.length;
		}
		
		public void gameDataChanged(final Change aChange)
		{
			isDirty = true;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
				}
			});
		}
		
		public void setGameData(final GameData data)
		{
			m_data.removeDataChangeListener(this);
			m_data = data;
			m_data.addDataChangeListener(this);
			isDirty = true;
		}
	}
	

	class ProductionStat extends AbstractStat
	{
		public String getName()
		{
			return "Production";
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			int rVal = 0;
			final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
			while (iter.hasNext())
			{
				final Territory place = iter.next();
				final TerritoryAttachment ta = TerritoryAttachment.get(place);
				/* Check if terr is a Land Convoy Route and check ownership of neighboring Sea Zone*/
				if (place.getOwner().equals(player) && Matches.territoryCanCollectIncomeFrom(player, data).match(place))
				{
					rVal += ta.getProduction();
				}
			}
			rVal *= Properties.getPU_Multiplier(data);
			return rVal;
		}
	}
	

	class PUStat extends ResourceStat
	{
		public PUStat()
		{
			super(m_data.getResourceList().getResource(Constants.PUS));
		}
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
	

	class UnitsStat extends AbstractStat
	{
		public String getName()
		{
			return "Units";
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			int rVal = 0;
			final Match<Unit> ownedBy = Matches.unitIsOwnedBy(player);
			final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
			while (iter.hasNext())
			{
				final Territory place = iter.next();
				rVal += place.getUnits().countMatches(ownedBy);
			}
			return rVal;
		}
	}
	

	class TUVStat extends AbstractStat
	{
		public String getName()
		{
			return "TUV";
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(player, data);
			final Match<Unit> unitIsOwnedBy = Matches.unitIsOwnedBy(player);
			int rVal = 0;
			final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
			while (iter.hasNext())
			{
				final Territory place = iter.next();
				final Collection<Unit> owned = place.getUnits().getMatches(unitIsOwnedBy);
				rVal += BattleCalculator.getTUV(owned, costs);
			}
			return rVal;
		}
	}
	

	class VictoryCityStat extends AbstractStat
	{
		public String getName()
		{
			return "VC";
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			int rVal = 0;
			final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
			while (iter.hasNext())
			{
				final Territory place = iter.next();
				if (!place.getOwner().equals(player))
					continue;
				final TerritoryAttachment ta = TerritoryAttachment.get(place);
				if (ta == null)
					continue;
				if (ta.isVictoryCity())
					rVal++;
			}
			return rVal;
		}
	}
	

	class VPStat extends AbstractStat
	{
		public String getName()
		{
			return "VPs";
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			final PlayerAttachment pa = PlayerAttachment.get(player);
			if (pa != null)
				return Double.parseDouble(pa.getVps());
			return 0;
		}
	}
	

	class TechCountStat extends AbstractStat
	{
		public String getName()
		{
			return "Techs";
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			int count = 0;
			final TechAttachment ta = TechAttachment.get(player);
			if (getBool(ta.getHeavyBomber()))
				count++;
			if (getBool(ta.getLongRangeAir()))
				count++;
			if (getBool(ta.getJetPower()))
				count++;
			if (getBool(ta.getRocket()))
				count++;
			if (getBool(ta.getIndustrialTechnology()))
				count++;
			if (getBool(ta.getSuperSub()))
				count++;
			if (getBool(ta.getDestroyerBombard()))
				count++;
			if (getBool(ta.getImprovedArtillerySupport()))
				count++;
			if (getBool(ta.getParatroopers()))
				count++;
			if (getBool(ta.getIncreasedFactoryProduction()))
				count++;
			if (getBool(ta.getWarBonds()))
				count++;
			if (getBool(ta.getMechanizedInfantry()))
				count++;
			if (getBool(ta.getAARadar()))
				count++;
			if (getBool(ta.getShipyards()))
				count++;
			for (final boolean value : ta.getGenericTech().values())
			{
				if (value)
					count++;
			}
			return count;
		}
		
		private boolean getBool(final String aString)
		{
			if (aString.equalsIgnoreCase("true"))
				return true;
			else if (aString.equalsIgnoreCase("false"))
				return false;
			else
				throw new IllegalArgumentException(aString + " is not a valid boolean");
		}
	}
	

	class TechTokenStat extends ResourceStat
	{
		public TechTokenStat()
		{
			super(m_data.getResourceList().getResource(Constants.TECH_TOKENS));
		}
	}
	

	class GenericResourceStat extends AbstractStat
	{
		private String m_name = null;
		
		public void init(final String name)
		{
			m_name = name;
		}
		
		public String getName()
		{
			return "Resource: " + m_name;
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			return player.getResources().getQuantity(m_name);
		}
	}
	

	class GenericTechNameStat extends AbstractStat
	{
		private TechAdvance m_ta = null;
		
		public void init(final TechAdvance ta)
		{
			m_ta = ta;
		}
		
		public String getName()
		{
			return "TechAdvance: " + m_ta.getName();
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			if (m_ta.hasTech(TechAttachment.get(player)))
				return 1;
			return 0;
		}
	}
	

	class GenericUnitNameStat extends AbstractStat
	{
		private UnitType m_ut = null;
		
		public void init(final UnitType ut)
		{
			m_ut = ut;
		}
		
		public String getName()
		{
			return "UnitType: " + m_ut.getName();
		}
		
		public double getValue(final PlayerID player, final GameData data)
		{
			int rVal = 0;
			final Match<Unit> ownedBy = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(m_ut));
			final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
			while (iter.hasNext())
			{
				final Territory place = iter.next();
				rVal += place.getUnits().countMatches(ownedBy);
			}
			return rVal;
		}
	}
}


class PlayerOrderComparator implements Comparator<PlayerID>
{
	private final GameData m_data;
	
	public PlayerOrderComparator(final GameData data)
	{
		m_data = data;
	}
	
	/**
	 * sort based on first step that isn't a bid related step.
	 */
	public int compare(final PlayerID p1, final PlayerID p2)
	{
		final Iterator<GameStep> iter = m_data.getSequence().iterator();
		while (iter.hasNext())
		{
			final GameStep s = iter.next();
			if (s.getPlayerID() == null)
				continue;
			if (s.getDelegate() != null && s.getDelegate().getClass() != null)
			{
				final String delegateClassName = s.getDelegate().getClass().getName();
				if (delegateClassName.equals("games.strategy.triplea.delegate.InitializationDelegate") || delegateClassName.equals("games.strategy.triplea.delegate.BidPurchaseDelegate")
							|| delegateClassName.equals("games.strategy.triplea.delegate.BidPlaceDelegate") || delegateClassName.equals("games.strategy.triplea.delegate.EndRoundDelegate"))
					continue;
			}
			else if (s.getName() != null && (s.getName().endsWith("Bid") || s.getName().endsWith("BidPlace")))
				continue;
			if (s.getPlayerID().equals(p1))
				return -1;
			else if (s.getPlayerID().equals(p2))
				return 1;
		}
		return 0;
	}
}
