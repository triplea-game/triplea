package games.strategy.common.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * Base class designed to make writing custom persistent delegates simpler.
 * Code common to all persistent delegates is implemented here.
 * 
 * @author Chris Duncan
 */
public abstract class BasePersistentDelegate implements IDelegate, IPersistentDelegate
{
	protected String m_name;
	protected String m_displayName;
	protected PlayerID m_player;
	protected IDelegateBridge m_bridge;
	protected GameData m_data;
	
	public BasePersistentDelegate()
	{
	}
	
	public void initialize(final String name)
	{
		initialize(name, name);
	}
	
	public void initialize(final String name, final String displayName)
	{
		m_name = name;
		m_displayName = displayName;
	}
	
	/**
	 * Called before the delegate will run.
	 * All classes should call super.start if they override this.
	 */
	public void start(final IDelegateBridge bridge)
	{
		m_bridge = bridge;
		m_player = bridge.getPlayerID();
		m_data = bridge.getData();
	}
	
	/**
	 * Called before the delegate will stop running.
	 * All classes should call super.end if they override this.
	 */
	public void end()
	{
		// nothing to do here
	}
	
	public String getName()
	{
		return m_name;
	}
	
	public String getDisplayName()
	{
		return m_displayName;
	}
	
	/**
	 * Returns the state of the Delegate.
	 * All classes should super.saveState if they override this.
	 */
	public Serializable saveState()
	{
		return null;
	}
	
	/**
	 * Loads the delegates state
	 */
	public void loadState(final Serializable state)
	{
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	public abstract Class<? extends IRemote> getRemoteType();
	
	public IDelegateBridge getBridge()
	{
		return m_bridge;
	}
	
	protected GameData getData()
	{
		return m_data;
	}
}
