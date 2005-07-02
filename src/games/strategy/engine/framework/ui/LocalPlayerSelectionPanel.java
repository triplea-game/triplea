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


package games.strategy.engine.framework.ui;

import javax.swing.*;
import games.strategy.engine.data.*;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Used to select the type of the local players, eg Human or Computer.
* The types of player comes from IGameLoader.getServerPlayerTypes90
 */

public class LocalPlayerSelectionPanel extends JPanel
{
  private GameData m_data;
  private List<LocalPlayerComboBoxSelector> m_playerTypes = new ArrayList<LocalPlayerComboBoxSelector>();



  public void setGameData(GameData data)
  {
    m_data = data;
    layoutComponents();
  }

  public String getPlayerType(String playerName)
  {
    Iterator<LocalPlayerComboBoxSelector> iter = m_playerTypes.iterator();
    while (iter.hasNext())
    {
      LocalPlayerComboBoxSelector item = iter.next();
      if(item.getPlayerName().equals(playerName))
        return item.getPlayerType();
    }
    throw new IllegalStateException("No player found:" + playerName);
  }

  private void layoutComponents()
  {
    removeAll();
    m_playerTypes.clear();
    setLayout(new GridBagLayout());

    if(m_data == null)
      return;


    String[] playerTypes =  m_data.getGameLoader().getServerPlayerTypes();

    String[] playerNames = m_data.getPlayerList().getNames();

    for(int i = 0; i < playerNames.length; i++)
    {
      LocalPlayerComboBoxSelector selector = new LocalPlayerComboBoxSelector(playerNames[i], playerTypes);
      m_playerTypes.add(selector);
      selector.layout(i, this);
    }


    validate();
  }


}

class LocalPlayerComboBoxSelector
{
  private final String m_playerName;
  private final JComboBox m_playerTypes;


  LocalPlayerComboBoxSelector(String playerName, String[] types)
  {
    m_playerName = playerName;
    m_playerTypes = new JComboBox(types);

  }

  public void layout(int row, Container container)
  {
    container.add(new JLabel(m_playerName + ":"), new GridBagConstraints(0,row, 1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,5),0,0) );
    container.add(m_playerTypes, new GridBagConstraints(1, row, 1,1,0,0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,5,5),0,0) );

  }

  public String getPlayerName()
  {
    return m_playerName;
  }

  public String getPlayerType()
  {
    return (String) m_playerTypes.getSelectedItem();
  }

}
