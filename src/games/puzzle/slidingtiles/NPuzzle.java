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
package games.puzzle.slidingtiles;

import games.puzzle.slidingtiles.player.BetterAI;
import games.puzzle.slidingtiles.player.BetterAI.Algorithm;
import games.puzzle.slidingtiles.player.BetterAI.Heuristic;
import games.puzzle.slidingtiles.player.INPuzzlePlayer;
import games.puzzle.slidingtiles.player.NPuzzlePlayer;
import games.puzzle.slidingtiles.player.RandomAI;
import games.puzzle.slidingtiles.ui.NPuzzleFrame;
import games.puzzle.slidingtiles.ui.display.INPuzzleDisplay;
import games.puzzle.slidingtiles.ui.display.NPuzzleDisplay;
import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Main class responsible for an n-puzzle game.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class NPuzzle implements IGameLoader
{
	private static final long serialVersionUID = 6359686118939451301L;
	// When serializing, do not save transient member variables
	private transient NPuzzleDisplay m_display;
	private transient IGame m_game;
	private static final String HUMAN_PLAYER_TYPE = "Human";
	private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
	private static final String DFS_COMPUTER_PLAYER_TYPE = "Depth First Search AI";
	
	public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames)
	{
		final Set<IGamePlayer> players = new HashSet<IGamePlayer>();
		final Iterator<String> iter = playerNames.keySet().iterator();
		while (iter.hasNext())
		{
			final String name = iter.next();
			final String type = playerNames.get(name);
			if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
			{
				final NPuzzlePlayer player = new NPuzzlePlayer(name, type);
				players.add(player);
			}
			else if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE))
			{
				final RandomAI ai = new RandomAI(name, type);
				players.add(ai);
			}
			else if (type.equals(DFS_COMPUTER_PLAYER_TYPE))
			{
				final BetterAI ai = new BetterAI(name, type, Algorithm.DFS, Heuristic.NUMBER_OF_MISPLACED_TILES);
				players.add(ai);
			}
			else
			{
				throw new IllegalStateException("Player type not recognized:" + type);
			}
		}
		return players;
	}
	
	/**
	 * Return an array of player types that can play on the server.
	 */
	public String[] getServerPlayerTypes()
	{
		return new String[] { HUMAN_PLAYER_TYPE, DFS_COMPUTER_PLAYER_TYPE, RANDOM_COMPUTER_PLAYER_TYPE };
	}
	
	public void shutDown()
	{
		if (m_display != null)
		{
			m_game.removeDisplay(m_display);
			m_display.shutDown();
		}
	}
	
	public void startGame(final IGame game, final Set<IGamePlayer> players) throws Exception
	{
		try
		{
			m_game = game;
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					final NPuzzleFrame frame = new NPuzzleFrame(game, players);
					m_display = new NPuzzleDisplay(frame);
					m_game.addDisplay(m_display);
					frame.setVisible(true);
					connectPlayers(players, frame);
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							// frame.setExtendedState(Frame.MAXIMIZED_BOTH);
							frame.toFront();
						}
					});
				}
			});
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		} catch (final InvocationTargetException e)
		{
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			else
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getCause().getMessage());
			}
		}
	}
	
	private void connectPlayers(final Set<IGamePlayer> players, final NPuzzleFrame frame)
	{
		final Iterator<IGamePlayer> iter = players.iterator();
		while (iter.hasNext())
		{
			final IGamePlayer player = iter.next();
			if (player instanceof NPuzzlePlayer)
				((NPuzzlePlayer) player).setFrame(frame);
		}
	}
	
	/**
	 * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
	 */
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		return INPuzzleDisplay.class;
	}
	
	public Class<? extends IRemote> getRemotePlayerType()
	{
		return INPuzzlePlayer.class;
	}
	
	public IUnitFactory getUnitFactory()
	{
		return new DefaultUnitFactory();
	}
}
