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
package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class LocalLauncher extends AbstractLauncher
{
	private static final Logger s_logger = Logger.getLogger(ILauncher.class.getName());
	private final IRandomSource m_randomSource;
	private final Map<String, String> m_playerTypes;
	
	public LocalLauncher(final GameSelectorModel gameSelectorModel, final IRandomSource randomSource, final Map<String, String> playerTypes)
	{
		super(gameSelectorModel);
		m_randomSource = randomSource;
		m_playerTypes = playerTypes;
	}
	
	@Override
	protected void launchInNewThread(final Component parent)
	{
		// final Runnable runner = new Runnable()
		// {
		// public void run()
		// {
		Exception exceptionLoadingGame = null;
		ServerGame game = null;
		try
		{
			final IServerMessenger messenger = new DummyMessenger();
			final Messengers messengers = new Messengers(messenger);
			final Set<IGamePlayer> gamePlayers = m_gameData.getGameLoader().createPlayers(m_playerTypes);
			game = new ServerGame(m_gameData, gamePlayers, new HashMap<String, INode>(), messengers);
			game.setRandomSource(m_randomSource);
			// for debugging, we can use a scripted random source
			if (ScriptedRandomSource.useScriptedRandom())
			{
				game.setRandomSource(new ScriptedRandomSource());
			}
			m_gameData.getGameLoader().startGame(game, gamePlayers, m_headless);
		} catch (final IllegalStateException e)
		{
			exceptionLoadingGame = e;
			Throwable error = e;
			while (error.getMessage() == null)
				error = error.getCause();
			final String message = error.getMessage();
			m_gameLoadingWindow.doneWait();
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
				}
			});
			
		} catch (final Exception ex)
		{
			ex.printStackTrace();
			exceptionLoadingGame = ex;
		} finally
		{
			m_gameLoadingWindow.doneWait();
		}
		try
		{
			if (exceptionLoadingGame == null)
			{
				s_logger.fine("Game starting");
				game.startGame();
				s_logger.fine("Game over");
			}
		} finally
		{
			// todo(kg), this does not occur on the swing thread, and this notifies setupPanel observers
			m_gameSelectorModel.loadDefaultGame(parent);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					JOptionPane.getFrameForComponent(parent).setVisible(true);
				}
			});
		}
		// }
		// };
		/*final Thread thread = new Thread(runner, "Triplea start local thread");
		thread.start();
		if (SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		try
		{
			thread.join();
		} catch (final InterruptedException e)
		{
		}
		s_logger.fine("Thread done!");*/
	}
}
