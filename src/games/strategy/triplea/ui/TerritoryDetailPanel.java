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
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.util.*;
import games.strategy.ui.OverlayIcon;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TerritoryDetailPanel extends JPanel
{

    private GameData m_data;
    private final UIContext m_uiContext;
    private JButton m_showOdds;
    private Territory m_currentTerritory;
    private final TripleAFrame m_frame;

    public TerritoryDetailPanel(MapPanel mapPanel, GameData data, UIContext uiContext, TripleAFrame frame)
    {
        m_data = data;
        m_frame = frame;
        m_showOdds = new JButton("Battle Calculator (Ctrl-B)");
        m_uiContext = uiContext;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(5, 5, 0, 0));

        mapPanel.addMapSelectionListener(
            new DefaultMapSelectionListener()
        {

            public void mouseEntered(Territory territory)
            {
                territoryChanged(territory);
            }

        }
        );
        
        String show_battle_calc = "show_battle_calc";
        final Action showBattleCalc = new AbstractAction(show_battle_calc)
        {
        
            public void actionPerformed(ActionEvent e)
            {
                OddsCalculatorDialog.show(m_frame, m_currentTerritory);
            }
        
        };
        
        m_showOdds.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                showBattleCalc.actionPerformed(e);
            }
        });
        
        
        ((JComponent)m_frame.getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('B', java.awt.event.InputEvent.META_MASK), show_battle_calc );
        ((JComponent)m_frame.getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('B', java.awt.event.InputEvent.CTRL_MASK), show_battle_calc );
        ((JComponent)m_frame.getContentPane()).getActionMap().put(show_battle_calc, showBattleCalc);

    }

    public void setGameData(GameData data)
    {
        m_data = data;
        territoryChanged(null);
    }

    
    
    private void territoryChanged(Territory territory)
    {
        m_currentTerritory = territory;
        removeAll();
        refresh();

        if (territory == null)
        {
            return;
        }

        add(new JLabel(territory.getName()));
        
        add(m_showOdds);
        
        Collection<Unit> unitsInTerritory;
        m_data.acquireReadLock();
        try
        {
            unitsInTerritory = territory.getUnits().getUnits();
        }
        finally
        {
            m_data.releaseReadLock();
        }
        
        Set units = UnitSeperator.categorize(unitsInTerritory);
        Iterator iter = units.iterator();
        PlayerID currentPlayer = null;
        while (iter.hasNext())
        {
            //seperate players with a seperator
            UnitCategory item = (UnitCategory) iter.next();
            if (item.getOwner() != currentPlayer)
            {
                currentPlayer = item.getOwner();
                add(Box.createVerticalStrut(15));
            }

            ImageIcon unitIcon = m_uiContext.getUnitImageFactory().getIcon(
                item.getType(), item.getOwner(), m_data, item.getDamaged());
            ImageIcon flagIcon = new ImageIcon(
                m_uiContext.getFlagImageFactory().getSmallFlag(item.getOwner()));
            
            // overlay flag onto upper-right of icon
            Icon flaggedUnitIcon = new OverlayIcon(unitIcon, flagIcon,
                    unitIcon.getIconWidth() - flagIcon.getIconWidth() - 3, 3);
            
            JLabel label = new JLabel("x" + item.getUnits().size(),
                    flaggedUnitIcon,
                    SwingConstants.LEFT
                    );

            add(label);

            refresh();

        }
    }

    private void refresh()
    {
        validate();
        repaint();
    }

}
