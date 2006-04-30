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
import games.strategy.triplea.util.*;

import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class TerritoryDetailPanel extends JPanel
{

    private GameData m_data;
    private final UIContext m_uiContext;

    public TerritoryDetailPanel(MapPanel mapPanel, GameData data, UIContext uiContext)
    {
        m_data = data;
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
    }

    public void setGameData(GameData data)
    {
        m_data = data;
        territoryChanged(null);
    }

    
    
    private void territoryChanged(Territory territory)
    {
        removeAll();
        refresh();

        if (territory == null)
        {
            return;
        }

        add(new JLabel(territory.getName()));
        
        Collection<Unit> unitsInTerritory;
        m_data.aquireReadLock();
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

            ImageIcon icon = m_uiContext.getUnitImageFactory().getIcon(
                item.getType(), item.getOwner(), m_data, item.getDamaged());
            JLabel label = new JLabel("x" + item.getUnits().size(),
                                      icon,
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
