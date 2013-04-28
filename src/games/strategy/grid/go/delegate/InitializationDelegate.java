package games.strategy.grid.go.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;
import java.util.HashSet;

/**
 * 
 * @author veqryn
 * 
 */
public class InitializationDelegate extends AbstractDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final GameData data = getData();
		final GameMap map = data.getMap();
		final int newWidth = data.getProperties().get("Width", map.getXDimension());
		final int newHeight = data.getProperties().get("Height", map.getYDimension());
		if (newWidth != map.getXDimension() || newHeight != map.getYDimension())
		{
			m_bridge.getHistoryWriter().startEvent("Changing Map Dimensions");
			final Territory t1 = map.getTerritories().iterator().next();
			final String name = t1.getName().substring(0, t1.getName().indexOf("_"));
			m_bridge.addChange(ChangeFactory.addGridGameMapChange(map, "square", name, newWidth, newHeight, new HashSet<String>(), "implicit", "implicit", "explicit"));
		}
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		return null;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
	}
	
	public boolean delegateCurrentlyRequiresUserInput()
	{
		return false;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class does not implement the IRemote interface, so return null.
		return null;
	}
}
