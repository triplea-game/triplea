package games.strategy.grid.chess;

import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.grid.GridGame;
import games.strategy.grid.chess.player.HeuristicAI;
import games.strategy.grid.chess.ui.ChessMapPanel;
import games.strategy.grid.chess.ui.ChessMenu;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;
import games.strategy.grid.ui.GridMapData;
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
	// private static final String RANDOM_COMPUTER_PLAYER_TYPE = "Random AI";
	private static final String HEURISTIC_COMPUTER_PLAYER_TYPE = "Mostly Random AI"; // the heuristic ai is basically random, so no need to have two random AIs
	// private static final String ALPHA_BETA_COMPUTER_PLAYER_TYPE = "Alpha-Beta AI"; // this ai was an experiment, and it didn't turn out well.
	public static final String AI_SEARCH_DEPTH_PROPERTY = "AI Search Depth";
	
	@Override
	public String[] getServerPlayerTypes()
	{
		return new String[] { HUMAN_PLAYER_TYPE, HEURISTIC_COMPUTER_PLAYER_TYPE, };
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
			/*
			else if (type.equals(RANDOM_COMPUTER_PLAYER_TYPE))
			{
				final RandomAI ai = new RandomAI(name, type);
				iplayers.add(ai);
			}
			*/
			else if (type.equals(HEURISTIC_COMPUTER_PLAYER_TYPE))
			{
				final HeuristicAI ai = new HeuristicAI(name, type);
				iplayers.add(ai);
			}
			/*
			else if (type.equals(ALPHA_BETA_COMPUTER_PLAYER_TYPE))
			{
				final AlphaBeta ai = new AlphaBeta(name, type);
				iplayers.add(ai);
			}
			*/
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
	protected Class<? extends GridMapData> getGridMapDataClass()
	{
		return GridMapData.class;
	}
	
	@Override
	protected Class<? extends GridGameMenu<GridGameFrame>> getGridTableMenuClass()
	{
		return ChessMenu.class;
	}
	
	@Override
	public IUnitFactory getUnitFactory()
	{
		return new ChessUnitFactory();
	}
}
