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
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.AbstractStat;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * 
 * @author Sean Bridges
 */
public class StatPanel extends AbstractStatPanel
{
	private static final long serialVersionUID = 4340684166664492498L;
	final private StatTableModel m_dataModel;
	final private TechTableModel m_techModel;
	protected IStat[] m_stats;
	private JTable m_statsTable;
	private JTable m_techTable;
	private Image m_statsImage = null;
	protected final Map<PlayerID, ImageIcon> m_mapPlayerImage = new HashMap<PlayerID, ImageIcon>();
	protected IUIContext m_uiContext;
	
	/** Creates a new instance of InfoPanel */
	public StatPanel(final GameData data, final IUIContext uiContext2)
	{
		super(data);
		m_uiContext = uiContext2;
		m_dataModel = new StatTableModel();
		m_techModel = new TechTableModel();
		fillPlayerIcons();
		initLayout();
	}
	
	@Override
	protected void initLayout()
	{
		final boolean hasTech = !TechAdvance.getTechAdvances(m_data, null).isEmpty();
		// do no include a grid box for tech if there is no tech
		setLayout(new GridLayout((hasTech ? 2 : 1), 1));
		
		m_statsTable = new JTable(m_dataModel)
		{
			private static final long serialVersionUID = -5516554955307630864L;
			
			@Override
			public void print(final Graphics g)
			{
				if (m_statsImage != null)
					g.drawImage(m_statsImage, 0, 0, null, null);
				super.print(g);
			}
		};
		
		m_statsTable.getTableHeader().setReorderingAllowed(false);
		m_statsTable.getColumnModel().getColumn(0).setPreferredWidth(175);
		JScrollPane scroll = new JScrollPane(m_statsTable);
		add(scroll);
		
		// if no technologies, do not show the tech table
		if (!hasTech)
			return;
		
		m_techTable = new JTable(m_techModel);
		m_techTable.getTableHeader().setReorderingAllowed(false);
		m_techTable.getColumnModel().getColumn(0).setPreferredWidth(500);
		// setupIconHeaders(m_techTable);
		// show icons for players:
		final TableCellRenderer componentRenderer = new JComponentTableCellRenderer();
		for (int i = 1; i < m_techTable.getColumnCount(); i++)
		{
			final TableColumn column = m_techTable.getColumnModel().getColumn(i);
			column.setHeaderRenderer(componentRenderer);
			final String player = m_techTable.getColumnName(i);
			final JLabel value = new JLabel("", getIcon(player), JLabel.CENTER);
			value.setToolTipText(player);
			column.setHeaderValue(value);
		}
		
		scroll = new JScrollPane(m_techTable);
		add(scroll);
	}
	
	@Override
	public void setGameData(final GameData data)
	{
		m_data = data;
		m_dataModel.setGameData(data);
		m_techModel.setGameData(data);
		m_dataModel.gameDataChanged(null);
		m_techModel.gameDataChanged(null);
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
	 * Gets the small flag for a given PlayerID
	 * 
	 * @param player
	 *            the player to get the flag for
	 * @return ImageIcon small flag
	 */
	protected ImageIcon getIcon(final PlayerID player)
	{
		ImageIcon icon = m_mapPlayerImage.get(player);
		if (icon == null && m_uiContext != null)
		{
			final Image img = m_uiContext.getFlagImageFactory().getSmallFlag(player);
			icon = new ImageIcon(img);
			icon.setDescription(player.getName());
			m_mapPlayerImage.put(player, icon);
		}
		return icon;
	}
	
	protected ImageIcon getIcon(final String playerName)
	{
		final PlayerID player = this.m_data.getPlayerList().getPlayerID(playerName);
		if (player == null)
		{
			return null;
		}
		return getIcon(player);
	}
	
	protected void fillPlayerIcons()
	{
		for (final PlayerID p : m_data.getPlayerList().getPlayers())
		{
			getIcon(p);
		}
	}
	
	
	/*
	 * helper that populates the icons for the column headers
	 * assuming that the column names are matched with icons
	 * 
	 * @param table
	 *            the table to set icons in the headers
	 *
	protected final void setupIconHeaders(final JTable table)
	{
		final TableModel model = table.getModel();
		final JTableHeader header = table.getTableHeader();
		final TableCellRenderer headerRenderer = header.getDefaultRenderer();
		
		final TableColumnModel cmodel = table.getColumnModel();
		TableColumn column = cmodel.getColumn(0);
		column.setHeaderRenderer(headerRenderer);
		
		final ImageIconRenderer imageRenderer = new ImageIconRenderer();// headerRenderer, true
		header.setDefaultRenderer(imageRenderer);
		
		final int ccnt = model.getColumnCount();
		
		for (int c = 1; c < ccnt; c++)
		{
			final String name = model.getColumnName(c);
			final ImageIcon icon = getIcon(name);
			// System.out.println("icon for name " + name + " = " + icon);
			column = cmodel.getColumn(c);
			column.setHeaderValue(icon);
		}
	}

	class ImageIconRenderer extends DefaultTableCellRenderer
	{
		private static final long serialVersionUID = -8596751565205016905L;
		
		@Override
		protected void setValue(final Object value)
		{
			setIcon((ImageIcon) value);
		}
	}*/
	
	class JComponentTableCellRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column)
		{
			return (JComponent) value;
		}
	}
	
	
	/*
	 * Custom table model.
	 * 
	 * This model is thread safe.
	 */
	class StatTableModel extends AbstractTableModel implements GameDataChangeListener
	{
		private static final long serialVersionUID = -6156153062049822444L;
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
				for (final PlayerID player : players)
				{
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
		private static final long serialVersionUID = -4612476336419396081L;
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
				colMap.put(colList[i], Integer.valueOf(i + 1));
			}
			/*
			 * .size()+1 added to stop index out of bounds errors when using an
			 * Italian player.
			 */
			boolean useTech = false;
			try
			{
				m_data.acquireReadLock();
				if (m_data.getResourceList().getResource(Constants.TECH_TOKENS) != null)
				{
					useTech = true;
					data = new String[TechAdvance.getTechAdvances(m_data).size() + 1][colList.length + 2];
				}
				else
				{
					data = new String[TechAdvance.getTechAdvances(m_data).size()][colList.length + 1];
				}
			} finally
			{
				m_data.releaseReadLock();
			}
			/* Load the technology -> row mapping */
			rowMap = new HashMap<String, Integer>();
			final Iterator<TechAdvance> iter = TechAdvance.getTechAdvances(m_data, null).iterator();
			int row = 0;
			if (useTech)
			{
				rowMap.put("Tokens", Integer.valueOf(row));
				data[row][0] = "Tokens";
				row++;
			}
			while (iter.hasNext())
			{
				final TechAdvance tech = iter.next();
				rowMap.put((tech).getName(), Integer.valueOf(row));
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
				for (final PlayerID pid : gameData.getPlayerList().getPlayers())
				{
					if (colMap.get(pid.getName()) == null)
						throw new IllegalStateException("Unexpected player in GameData.getPlayerList()" + pid.getName());
					final int col = colMap.get(pid.getName()).intValue();
					int row = 0;
					// boolean useTokens = false;
					if (m_data.getResourceList().getResource(Constants.TECH_TOKENS) != null)
					{
						// useTokens = true;
						final Integer tokens = pid.getResources().getQuantity(Constants.TECH_TOKENS);
						data[row][col] = tokens.toString();
					}
					final Iterator<TechAdvance> advancesAll = TechAdvance.getTechAdvances(m_data).iterator();
					final List<TechAdvance> has = TechAdvance.getTechAdvances(m_data, pid);
					while (advancesAll.hasNext())
					{
						final TechAdvance advance = advancesAll.next();
						// if(!pid.getTechnologyFrontierList().getAdvances().contains(advance)){
						if (!has.contains(advance))
						{
							row = rowMap.get(advance.getName()).intValue();
							data[row][col] = "-";
						}
					}
					final Iterator<TechAdvance> advances = TechTracker.getCurrentTechAdvances(pid, m_data).iterator();
					while (advances.hasNext())
					{
						final TechAdvance advance = advances.next();
						row = rowMap.get(advance.getName()).intValue();
						// System.err.println("(" + row + ", " + col + ")");
						data[row][col] = "X";
						// data[row][col] = colList[col].substring(0, 1);
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
			// return colList[col - 1].substring(0, 1);
			return colList[col - 1];
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
			for (final Territory place : data.getMap().getTerritories())
			{
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
			super(getResourcePUs(m_data));
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
			for (final Territory place : data.getMap().getTerritories())
			{
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
			for (final Territory place : data.getMap().getTerritories())
			{
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
			for (final Territory place : data.getMap().getTerritories())
			{
				if (!place.getOwner().equals(player))
					continue;
				final TerritoryAttachment ta = TerritoryAttachment.get(place);
				if (ta == null)
					continue;
				if (ta.getVictoryCity() != 0)
				{
					rVal = rVal + ta.getVictoryCity();
				}
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
				return pa.getVps();
			return 0;
		}
	}
	
}
