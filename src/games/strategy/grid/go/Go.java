package games.strategy.grid.go;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.grid.GridGame;
import games.strategy.grid.go.player.GoPlayer;
import games.strategy.grid.go.player.RandomAI;
import games.strategy.grid.go.ui.GoMapPanel;
import games.strategy.grid.go.ui.GoMenu;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Main class responsible for Go game.
 * 
 * @author Mark Christopher Duncan (veqryn)
 * 
 */
public class Go extends GridGame implements IGameLoader
{
	private static final long serialVersionUID = 2560802787325950593L;
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
				final GoPlayer player = new GoPlayer(name, type);
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
		return GoMapPanel.class;
	}
	
	@Override
	protected Class<? extends GridMapData> getGridMapDataClass()
	{
		return GridMapData.class;
	}
	
	@Override
	protected Class<? extends BasicGameMenuBar<GridGameFrame>> getGridTableMenuClass()
	{
		return GoMenu.class;
	}
	
	@Override
	public int getSquareWidth()
	{
		return 30;
	}
	
	@Override
	public int getSquareHeight()
	{
		return 30;
	}
	
	@Override
	public int getBevelSize()
	{
		return 10;
	}
	
	/**
	 * Only use this method from within a non-static Delegate class method.
	 * (Because only the host has the delegates)
	 */
	public static final games.strategy.grid.go.delegate.PlayDelegate playDelegate(final GameData data)
	{
		return (games.strategy.grid.go.delegate.PlayDelegate) findDelegate(data, "play");
	}
	
	/**
	 * Only use this method from within a non-static Delegate class method.
	 * (Because only the host has the delegates)
	 */
	public static final games.strategy.grid.go.delegate.EndTurnDelegate endTurnDelegate(final GameData data)
	{
		return (games.strategy.grid.go.delegate.EndTurnDelegate) findDelegate(data, "endTurn");
	}
}
