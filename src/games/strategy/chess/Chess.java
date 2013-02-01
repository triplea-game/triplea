package games.strategy.chess;

import games.strategy.chess.player.ChessPlayer;
import games.strategy.chess.player.IChessPlayer;
import games.strategy.chess.ui.ChessFrame;
import games.strategy.chess.ui.display.ChessDisplay;
import games.strategy.chess.ui.display.IChessDisplay;
import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * Main class responsible for Chess game.
 * 
 * @author Mark Christopher Duncan (veqryn)
 * 
 */
public class Chess implements IGameLoader
{
	private static final long serialVersionUID = 6963459871530489560L;
	// When serializing, do not save transient member variables
	private transient ChessDisplay m_display;
	private transient IGame m_game;
	private static final String HUMAN_PLAYER_TYPE = "Human";
	
	public String[] getServerPlayerTypes()
	{
		return new String[] { HUMAN_PLAYER_TYPE };
	}
	
	public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames)
	{
		final Set<IGamePlayer> iplayers = new HashSet<IGamePlayer>();
		for (final String name : playerNames.keySet())
		{
			final String type = playerNames.get(name);
			if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
			{
				final ChessPlayer player = new ChessPlayer(name, type);
				iplayers.add(player);
			}
			else
			{
				throw new IllegalStateException("Player type not recognized:" + type);
			}
		}
		return iplayers;
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
					final ChessFrame frame = new ChessFrame(game, players);
					m_display = new ChessDisplay(frame);
					m_game.addDisplay(m_display);
					frame.setVisible(true);
					connectPlayers(players, frame);
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
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
	
	private void connectPlayers(final Set<IGamePlayer> players, final ChessFrame frame)
	{
		for (final IGamePlayer player : players)
		{
			if (player instanceof ChessPlayer)
				((ChessPlayer) player).setFrame(frame);
		}
	}
	
	/**
	 * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
	 */
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		return IChessDisplay.class;
	}
	
	public Class<? extends IRemote> getRemotePlayerType()
	{
		return IChessPlayer.class;
	}
	
	public void shutDown()
	{
		if (m_display != null)
		{
			m_game.removeDisplay(m_display);
			m_display.shutDown();
		}
	}
	
	public IUnitFactory getUnitFactory()
	{
		return new DefaultUnitFactory();
	}
	
}
