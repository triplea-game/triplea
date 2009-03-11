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
* UnitChooser.java
*
* Created on December 3, 2001, 7:32 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.ui.Util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 *
 * @author  Tony Clayton
 * @version 1.0
 */
public class PlayerChooser extends JPanel
{

  private JList m_list;
  private PlayerList m_players;
  private PlayerID m_defaultPlayer;
  private final UIContext m_uiContext;
  private boolean m_allowNeutral;
  
  /** Creates new PlayerChooser */ 
  public PlayerChooser(PlayerList players, UIContext uiContext, boolean allowNeutral)
  {
    this(players, null, uiContext, allowNeutral);
  }

  /** Creates new PlayerChooser */ 
  public PlayerChooser(PlayerList players, PlayerID defaultPlayer, UIContext uiContext, boolean allowNeutral)
  {
    m_players = players;
    m_defaultPlayer = defaultPlayer;
    m_uiContext = uiContext;
    m_allowNeutral = allowNeutral;
    createComponents();
    layoutComponents();
  }


  private void createComponents()
  {
      Collection<PlayerID> players = new ArrayList<PlayerID>(m_players.getPlayers());
      if (m_allowNeutral)
          players.add(PlayerID.NULL_PLAYERID);
      m_list = new JList(players.toArray());
      m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      m_list.setSelectedValue(m_defaultPlayer, true);
      m_list.setFocusable(false);
      m_list.setCellRenderer(new PlayerChooserRenderer(m_players, m_uiContext));
  }

  private void layoutComponents()
  {
      setLayout(new BorderLayout());
      add(new JScrollPane(m_list), BorderLayout.CENTER);
  }

 

  public PlayerID getSelected()
  {
     return (PlayerID) m_list.getSelectedValue();
  }

}


class PlayerChooserRenderer extends DefaultListCellRenderer
{
    
    private final UIContext m_uiContext;
    
    PlayerChooserRenderer(PlayerList players, UIContext uiContext)
    {
        
        m_uiContext  = uiContext;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
    {
       super.getListCellRendererComponent(list, ((PlayerID) value).getName(), index, isSelected, cellHasFocus);
       if ((PlayerID)value == PlayerID.NULL_PLAYERID)
           setIcon(new ImageIcon(Util.createImage(32, 32, true)));
       else
           setIcon(new ImageIcon(m_uiContext.getFlagImageFactory().getFlag((PlayerID) value)));
       return this;
    }
    
}
