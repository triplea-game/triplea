package games.strategy.chess;

import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

import java.util.Map;
import java.util.Set;

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
	
	public String[] getServerPlayerTypes()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public Set<IGamePlayer> createPlayers(final Map<String, String> players)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public void startGame(final IGame game, final Set<IGamePlayer> players) throws Exception
	{
		// TODO Auto-generated method stub
		
	}
	
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public Class<? extends IRemote> getRemotePlayerType()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public void shutDown()
	{
		// TODO Auto-generated method stub
		
	}
	
	public IUnitFactory getUnitFactory()
	{
		return new DefaultUnitFactory();
	}
	
}
