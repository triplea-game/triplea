package games.strategy.grid.chess;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.grid.GridGame;
import games.strategy.grid.chess.ui.ChessMapPanel;
import games.strategy.grid.chess.ui.ChessMenu;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.player.RandomAI;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapPanel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Main class responsible for Chess game.
 * 
 * @author Mark Christopher Duncan (veqryn)
 * 
 */
public class Chess extends GridGame implements IGameLoader
{
	private static final long serialVersionUID = 6963459871530489560L;
	private static final String HUMAN_PLAYER_TYPE = "Human";
	private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
	
	@Override
	public String[] getServerPlayerTypes()
	{
		return new String[] { HUMAN_PLAYER_TYPE, RANDOM_COMPUTER_PLAYER_TYPE };
	}
	
	@Override
	public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames)
	{
		final Set<IGamePlayer> iplayers = new HashSet<IGamePlayer>();
		for (final String name : playerNames.keySet())
		{
			final String type = playerNames.get(name);
			if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
			{
				final GridGamePlayer player = new GridGamePlayer(name, type);
				iplayers.add(player);
			}
			else if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE))
			{
				final RandomAI ai = new RandomAI(name, type);
				iplayers.add(ai);
			}
			else
			{
				throw new IllegalStateException("Player type not recognized:" + type);
			}
		}
		return iplayers;
	}
	
	@Override
	protected Class<? extends GridMapPanel> getGridMapPanelClass()
	{
		return ChessMapPanel.class;
	}
	
	@Override
	protected Class<? extends BasicGameMenuBar<GridGameFrame>> getGridTableMenuClass()
	{
		return ChessMenu.class;
	}
	
	@Override
	public IUnitFactory getUnitFactory()
	{
		return new ChessUnitFactory();
	}
}
