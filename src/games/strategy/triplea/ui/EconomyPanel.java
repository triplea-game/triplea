package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.IStat;
import games.strategy.triplea.Constants;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

public class EconomyPanel extends StatPanel
{
	
	private IStat[] m_statsResource;
	private ResourceTableModel m_resourceModel;
	
	public EconomyPanel(final GameData data)
	{
		super(data);
		m_resourceModel = new ResourceTableModel();
		final JTable table = new JTable(m_resourceModel);
	}
	
	@Override
	protected void initLayout()
	{
		setLayout(new GridLayout(1, 1));
		m_resourceModel = new ResourceTableModel();
		final JTable table = new JTable(m_resourceModel);
		table.getTableHeader().setReorderingAllowed(false);
		final TableColumn column = table.getColumnModel().getColumn(0);
		column.setPreferredWidth(175);
		final JScrollPane scroll = new JScrollPane(table);
		add(scroll);
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
				if (resource.getName().equals(Constants.TECH_TOKENS) || resource.getName().equals(Constants.VPS))
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
	
	@Override
	public void setGameData(final GameData data)
	{
		m_data = data;
		m_resourceModel.setGameData(data);
		m_resourceModel.gameDataChanged(null);
	}
	
}
