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

package games.strategy.common.ui;

import java.awt.Component;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.util.EventThreadJOptionPane;

import javax.swing.*;

/**
 *
 * Panel to show who is playing which players
 * 
 * @author Lane Schwartz, based on code by sgb
 */
public class PlayersPanel extends JPanel
{
    private final PlayerManager m_players;

    public PlayersPanel(PlayerManager players, GameData data)
    {
        m_players = players;
    
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS ));
        
             
        for(String player : m_players.getPlayers())
        {
            PlayerID playerID = data.getPlayerList().getPlayerID(player); 
            
            add(new JLabel( m_players.getNode(player).getName() + " is " + playerID.getName(), JLabel.RIGHT));
            
        }
        
    }
    
    public static void showPlayers(IGame game, Component parent)
    {
        PlayersPanel panel = new PlayersPanel(game.getPlayerManager(), game.getData());
        EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent),panel, "Players", JOptionPane.PLAIN_MESSAGE);
    }

        
}
