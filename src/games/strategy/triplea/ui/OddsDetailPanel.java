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

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.triplea.oddsCalculator.zengland.OCBattle;

import java.awt.GridLayout;
import java.math.BigDecimal;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class OddsDetailPanel extends JPanel
{

    //fight the battle this many times to get a good average
    private static final int FIGHT_COUNT = 1000;

    private GameData m_data;
    //private final UIContext m_uiContext;
    
	private JLabel controlPercentLabel;
	private JLabel airWinPercentLabel;
	private JLabel clearedPercentLabel;
	private JLabel indecisivePercentLabel;
	private JLabel lossPercentLabel;
	private JLabel battlesLabel;
    private Territory m_currentTerritory;

    private TerritoryListener m_territoryListener;

    public OddsDetailPanel(MapPanel mapPanel, GameData data, UIContext uiContext)
    {
        m_data = data;
        //m_uiContext = uiContext;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(5, 5, 0, 0));

        mapPanel.addMapSelectionListener(
            new DefaultMapSelectionListener()
        {

            public void mouseEntered(Territory territory)
            {
                m_currentTerritory = territory;
                updateOdds();
            }

        }
        );
        
        m_territoryListener = new TerritoryListener(){
                
                            public void ownerChanged(Territory territory)
                            {
                                
                            }
                
                            public void unitsChanged(Territory territory)
                            {
                                if(m_currentTerritory != null && m_currentTerritory.equals(territory))
                                {
                                    SwingUtilities.invokeLater(new Runnable() {
                
                                        public void run()
                                        {
                                            updateOdds();
                                            
                                        }});
                                    
                                }
                                
                            }};
        data.addTerritoryListener(m_territoryListener);
        
    }

    public void setGameData(GameData data)
    {
        m_data.removeTerritoryListener(m_territoryListener);
        m_data = data;
        m_currentTerritory = null;
        updateOdds();
    }



    private void updateOdds()
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
        removeAll();
        refresh();

        if (m_currentTerritory == null)
        {
            return;
        }
        
        add(new JLabel(m_currentTerritory.getName()));
        
        if(m_currentTerritory.getUnits().getPlayersWithUnits().size() < 2)
        {
            add(new JLabel("No pending battle"));
            return;
        }
                
        
        
        OCBattle terrBattle = null;
        m_data.acquireReadLock();
        try
        {
        	terrBattle = new OCBattle(m_currentTerritory, m_data);
        }
        finally
        {
        	m_data.releaseReadLock();
        }
        
        

        if(terrBattle.getAttackers().size()>0&&terrBattle.getDefenders().size()>0)
        	terrBattle.rollBattles(FIGHT_COUNT);
        
        JPanel resultsPanel = new JPanel();
		resultsPanel.setLayout(new GridLayout(6,2));
        
        JLabel controlLabel = new JLabel("Control");
        resultsPanel.add(controlLabel);
		controlPercentLabel = new JLabel(formattedPercent(terrBattle.getControlPercent()));
		resultsPanel.add(controlPercentLabel);
		JLabel airLabel = new JLabel("Air win");
		resultsPanel.add(airLabel);
		airWinPercentLabel = new JLabel(formattedPercent(terrBattle.getAirWinPercent()));
		resultsPanel.add(airWinPercentLabel);
		JLabel clearedLabel = new JLabel("Cleared");
		resultsPanel.add(clearedLabel);
		clearedPercentLabel = new JLabel(formattedPercent(terrBattle.getClearedPercent()));
		resultsPanel.add(clearedPercentLabel);
		JLabel indecisiveLabel = new JLabel("Indecisive");
		resultsPanel.add(indecisiveLabel);
		indecisivePercentLabel = new JLabel(formattedPercent(terrBattle.getIndecisivePercent()));
		resultsPanel.add(indecisivePercentLabel);
		JLabel lossLabel = new JLabel("Loss");
		resultsPanel.add(lossLabel);
		lossPercentLabel = new JLabel(formattedPercent(terrBattle.getLossPercent()));
		resultsPanel.add(lossPercentLabel);
		JLabel battlesTitleLabel = new JLabel("Battles rolled");
		resultsPanel.add(battlesTitleLabel);
		battlesLabel = new JLabel(String.valueOf(terrBattle.getBattles()));
		resultsPanel.add(battlesLabel);
        add(resultsPanel);
        
        add(Box.createVerticalGlue());
        
        
//        Set units = UnitSeperator.categorize(territory.getUnits().getUnits());
//        Iterator iter = units.iterator();
//        PlayerID currentPlayer = null;
//        while (iter.hasNext())
//        {
//            //seperate players with a seperator
//            UnitCategory item = (UnitCategory) iter.next();
//            if (item.getOwner() != currentPlayer)
//            {
//                currentPlayer = item.getOwner();
//                add(Box.createVerticalStrut(15));
//            }
//
//            ImageIcon icon = m_uiContext.getUnitImageFactory().getIcon(
//                item.getType(), item.getOwner(), m_data, item.getDamaged());
//            List<games.strategy.engine.data.Unit> unitList = item.getUnits();
//            JLabel label = new JLabel("x" + item.getUnits().size(),
//                                      icon,
//                                      SwingConstants.LEFT
//                                      );
//
//            add(label);
//
//            refresh();
//
//        }
        
        refresh();
    }

	public String formattedPercent(float per) {
		BigDecimal bd = new BigDecimal(per);
		String res = bd.toString();
		int endSpace = 0;
		if(res.indexOf(".")+3>=res.length()||res.indexOf(".") == -1)
		{
			endSpace = res.length();
		}
		else
			endSpace = res.indexOf(".")+3;
		res = res.substring(0, endSpace);
		if(res.indexOf(".")==-1)
			res += ".00";
		res+="%";
		return res;
	}

	private void refresh()
    {
        validate();
        repaint();
    }

}
