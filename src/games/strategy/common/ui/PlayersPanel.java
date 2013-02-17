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
package games.strategy.common.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerManager;
import games.strategy.engine.framework.IGame;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;

import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * 
 * Panel to show who is playing which players
 * 
 * @author Lane Schwartz, based on code by sgb
 */
public class PlayersPanel extends JPanel
{
	private static final long serialVersionUID = -4283654829822141065L;
	private final PlayerManager m_players;
	
	public PlayersPanel(final PlayerManager players, final GameData data)
	{
		m_players = players;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		for (final String player : m_players.getPlayers())
		{
			final PlayerID playerID = data.getPlayerList().getPlayerID(player);
			if (playerID.isAI())
				add(new JLabel(playerID.getWhoAmI().split(":")[1] + " is " + playerID.getName(), JLabel.RIGHT));
			else
				add(new JLabel(m_players.getNode(player).getName() + " is " + playerID.getName(), JLabel.RIGHT));
		}
	}
	
	public static void showPlayers(final IGame game, final Component parent)
	{
		final PlayersPanel panel = new PlayersPanel(game.getPlayerManager(), game.getData());
		EventThreadJOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent), panel, "Players", JOptionPane.PLAIN_MESSAGE, new CountDownLatchHandler(true));
	}
}
