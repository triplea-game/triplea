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

import javax.swing.*;
import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.data.Territory;
import java.awt.event.MouseEvent;
import games.strategy.triplea.util.*;
import games.strategy.triplea.image.*;
import java.awt.*;
import java.lang.reflect.*;
import javax.swing.border.*;

public class TerritoryDetailPanel extends JPanel
{

  private GameData m_data;

  public TerritoryDetailPanel(MapPanel mapPanel, GameData data)
  {
    m_data = data;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5,5,0,0));

    mapPanel.addMapSelectionListener(
      new MapSelectionListener()
    {

      public void territorySelected(Territory territory, MouseEvent me)
      {}

      public void mouseEntered(Territory territory)
      {
        removeAll();
        refresh();

        if(territory == null)
        {
          return;
        }

        add(new JLabel(territory.getName()));
        Set units = UnitSeperator.categorize(territory.getUnits().getUnits());
        Iterator iter = (new TreeSet(units)).iterator();
        PlayerID currentPlayer = null;
        while (iter.hasNext())
        {
          //seperate players
          UnitCategory item = (UnitCategory) iter.next();
          if(item.getOwner() != currentPlayer)
          {
            currentPlayer = item.getOwner();
            add(Box.createVerticalStrut(15));
          }


          ImageIcon icon = UnitIconImageFactory.instance().getIcon(
            item.getType(), item.getOwner(), m_data);
          JLabel label = new JLabel("x" + item.getUnits().size(),
                                    icon,
                                    JLabel.LEFT
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
    );
  }

}