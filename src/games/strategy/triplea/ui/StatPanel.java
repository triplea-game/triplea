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
 * StatPanel.java
 *
 * Created on January 6, 2002, 4:07 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.stats.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.delegate.*;
import games.strategy.util.*;

import java.awt.GridLayout;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

/**
 * 
 * @author Sean Bridges
 */
public class StatPanel extends JPanel
{
    private StatTableModel m_dataModel;
    private TechTableModel m_techModel;
    private IStat[] m_stats = new IStat[] {new IPCStat(), new ProductionStat(), new UnitsStat(), new TUVStat()};
    private GameData m_data;
    
    //sort based on first step
    private final Comparator<PlayerID> m_playerOrderComparator = new Comparator<PlayerID>()
    {
        public int compare(PlayerID p1, PlayerID p2)
        {
            
            Iterator iter = m_data.getSequence().iterator();
            
            while(iter.hasNext())
            {
                GameStep s = (GameStep) iter.next();
                
                if(s.getPlayerID() == null)
                    continue;
                
                if(s.getPlayerID().equals(p1))
                    return -1;
                else if(s.getPlayerID().equals(p2))
                    return 1;
            }
            return 0;
        }
        
    };

    /** Creates a new instance of InfoPanel */
    public StatPanel(GameData data)
    {
        m_data = data;
        //only add the vc stat if we have some victory cities
        if(Match.someMatch(data.getMap().getTerritories(), Matches.TerritoryIsVictoryCity))
        {
            List<IStat> stats = new ArrayList<IStat>(Arrays.asList(m_stats));
            stats.add(new VictoryCityStat());
            m_stats = (IStat[]) stats.toArray(new IStat[stats.size()]);
        }
        //only add the vps in pacific
        if(data.getProperties().get(Constants.PACIFIC_EDITION, false))
        {
            List<IStat> stats = new ArrayList<IStat>(Arrays.asList(m_stats));
            stats.add(new VPStat());
            m_stats = (IStat[]) stats.toArray(new IStat[stats.size()]);
        }
        
        setLayout(new GridLayout(2, 1));

        m_dataModel = new StatTableModel();
        m_techModel = new TechTableModel();

        JTable table = new JTable(m_dataModel);
        // Strangely, this is enabled by default
        table.getTableHeader().setReorderingAllowed(false);

        // Set width of country column
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setPreferredWidth(175);

        JScrollPane scroll = new JScrollPane(table);
        // add(scroll, BorderLayout.NORTH);
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
    
    
    public void setGameData(GameData data)
    {
        m_data = data;
        m_dataModel.setGameData(data);
        m_techModel.setGameData(data);
        m_dataModel.gameDataChanged(null);
        m_techModel.gameDataChanged(null);

    }
    
    /**
     * 
     * @return all the alliances with more than one player.
     */
    public Collection<String> getAlliances()
    {
        Iterator allAlliances = m_data.getAllianceTracker().getAliances().iterator();
        //order the alliances use a Tree Set
        Collection<String> rVal = new TreeSet<String>();

        while (allAlliances.hasNext())
        {
            String alliance = (String) allAlliances.next();
            if (m_data.getAllianceTracker().getPlayersInAlliance(alliance).size() > 1)
            {
                rVal.add(alliance);
            }
        }
        return rVal;
    }

    
    public List<PlayerID> getPlayers()
    {
        List<PlayerID> players = new ArrayList<PlayerID>( m_data.getPlayerList().getPlayers());
        Collections.sort(players,m_playerOrderComparator);
        return players;
        
    }

    public IStat[] getStats()
    {
        return m_stats;
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
            m_data.addDataChangeListener(this);
            m_isDirty = true;
            
        }

        private synchronized void loadData()
        {
            m_data.aquireReadLock();
            try
            {
                List players = getPlayers();
                Collection<String> alliances = getAlliances();
	            
	            m_collectedData = new String[players.size() + alliances.size()][m_stats.length + 1];
	            
	            int row = 0;
	            Iterator playerIter = players.iterator();
	            while (playerIter.hasNext())
	            {
	                PlayerID player = (PlayerID) playerIter.next();
	                
	                m_collectedData[row][0] = player.getName();
	                for(int i = 0; i < m_stats.length; i++)
	                {
	                    m_collectedData[row][i+1] = m_stats[i].getFormatter().format(m_stats[i].getValue(player, m_data));
	                }
	                row++;
	            }
	            Iterator<String> allianceIterator = alliances.iterator();
	            while (allianceIterator.hasNext())
	            {
	                String alliance = allianceIterator.next();
	                
	                m_collectedData[row][0] = alliance;
	                for(int i = 0; i < m_stats.length; i++)
	                {
	                    m_collectedData[row][i+1] = m_stats[i].getFormatter().format(m_stats[i].getValue(alliance, m_data));
	                }
	                row++;
	            }
            }
            finally
            {
                m_data.releaseReadLock();
            }
            
        }



        public void gameDataChanged(Change aChange)
        {
            synchronized(this)
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
        public synchronized Object getValueAt(int row, int col)
        {
            if (m_isDirty)
            {
                loadData();
                m_isDirty = false;
            }

            return m_collectedData[row][col];
        }

        // Trivial implementations of required methods
        public String getColumnName(int col)
        {
            if(col == 0)
                return "Player";
            return 
             	m_stats[col -1].getName();
        }

        public int getColumnCount()
        {
            return m_stats.length + 1;
        }

        public synchronized int getRowCount()
        {
            if(!m_isDirty)
                return m_collectedData.length;
            else
            {
                //no need to recalculate all the stats just to get the row count
                //getting the row count is a fairly frequent operation, and will
                //happen even if we are not displayed!
                m_data.aquireReadLock();
                try
                {
                    return m_data.getPlayerList().size() + getAlliances().size();

                }
                finally
                {
                  m_data.releaseReadLock();  
                }
            }
        }

        public synchronized void setGameData(GameData data)
        {
            synchronized(this)
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

            data = new String[TechAdvance.getTechAdvances(m_data).size()][colList.length + 1];

            /* Load the technology -> row mapping */
            rowMap = new HashMap<String, Integer>();
            Iterator iter = TechAdvance.getTechAdvances(m_data).iterator();
            int row = 0;

            while (iter.hasNext())
            {
                TechAdvance tech = (TechAdvance) iter.next();
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
            java.util.List<PlayerID> players = new ArrayList<PlayerID>(m_data.getPlayerList().getPlayers());

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
            //copy so aquire/release read lock are on the same object!
            final GameData gameData = m_data;
            
            Iterator playerIter = gameData.getPlayerList().getPlayers().iterator();

            gameData.aquireReadLock();
            try
            {
                while (playerIter.hasNext())
                {
                    PlayerID pid = (PlayerID) playerIter.next();
                    if (colMap.get(pid.getName()) == null)
                        throw new IllegalStateException("Unexpected player in GameData.getPlayerList()" + pid.getName());
    
                    int col = colMap.get(pid.getName()).intValue();
    
                    Iterator advances = TechTracker.getTechAdvances(pid).iterator();
    
                    while (advances.hasNext())
                    {
                        int row = rowMap.get(((TechAdvance) advances.next()).getName()).intValue();
                        // System.err.println("(" + row + ", " + col + ")");
                        data[row][col] = "X";
                        // data[row][col] = colList[col].substring(0, 1);
                    }
                }
            }
            finally
            {
                gameData.releaseReadLock();
            }
        }

        public String getColumnName(int col)
        {
            if (col == 0)
                return "Technology";
            return colList[col - 1].substring(0, 1);
        }

        /*
         * Recalcs the underlying data in a lazy manner Limitation: This is not
         * a threadsafe implementation
         */
        public Object getValueAt(int row, int col)
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

        public void gameDataChanged(Change aChange)
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

        public void setGameData(GameData data)
        {
            m_data.removeDataChangeListener(this);
            m_data = data;
            m_data.addDataChangeListener(this);
            isDirty = true;
        }
    }
}



class ProductionStat extends AbstractStat
{

    public String getName()
    {
        return "Production";
    }

    public double getValue(PlayerID player, GameData data)
    {
        int rVal = 0; 
        Iterator iter = data.getMap().getTerritories().iterator();
        while (iter.hasNext())
        {
            boolean isConvoyOrLand = false; 
            Territory place = (Territory) iter.next();
            TerritoryAttachment ta = TerritoryAttachment.get(place);

            if(!place.isWater())
            {
                isConvoyOrLand = true;
            } 
            else if(place.isWater() &&
                        ta != null &&
                        data.getPlayerList().getPlayerID(ta.getOriginalOwner()) != PlayerID.NULL_PLAYERID &&
                        data.getPlayerList().getPlayerID(ta.getOriginalOwner()) == player &&
                        place.getOwner().equals(player))
            {
                isConvoyOrLand = true; 
            }
            
            if(place.getOwner().equals(player) && isConvoyOrLand)
            {
                if(ta != null)
                    rVal += ta.getProduction(); 
            }
            
        }
        return rVal;
    }
    
}

class IPCStat extends AbstractStat
{

    public String getName()
    {
        return "IPCs";
    }

    public double getValue(PlayerID player, GameData data)
    {
        return player.getResources().getQuantity(Constants.IPCS);
    }
}


class UnitsStat extends AbstractStat
{

    public String getName()
    {
        return "Units";
    }

    public double getValue(PlayerID player, GameData data)
    {
        int rVal = 0; 
        Match<Unit> ownedBy = Matches.unitIsOwnedBy(player);
        Iterator iter = data.getMap().getTerritories().iterator();
        while (iter.hasNext())
        {
            Territory place = (Territory) iter.next();
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

    public double getValue(PlayerID player, GameData data)
    {
        IntegerMap<UnitType> costs = BattleCalculator.getCosts(player, data);
        
        Match<Unit> unitIsOwnedBy = Matches.unitIsOwnedBy(player);
        
        int rVal = 0; 
        Iterator iter = data.getMap().getTerritories().iterator();
        while (iter.hasNext())
        {
            Territory place = (Territory) iter.next();
            Collection owned = place.getUnits().getMatches(unitIsOwnedBy);
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

    public double getValue(PlayerID player, GameData data)
    {
        int rVal = 0; 
        Iterator iter = data.getMap().getTerritories().iterator();
        while (iter.hasNext())
        {
            Territory place = (Territory) iter.next();
            if(!place.getOwner().equals(player))
                continue;
            
            TerritoryAttachment ta =  TerritoryAttachment.get(place);
            if(ta == null)
                continue;
            
            if(ta.isVictoryCity())
                rVal ++;
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

    public double getValue(PlayerID player, GameData data)
    {
        PlayerAttachment pa = PlayerAttachment.get(player);
        if(pa != null)
            return Double.parseDouble(pa.getVps());
        return 0; 
    }
}
