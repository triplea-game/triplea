package games.strategy.common.delegate;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IPersistentDelegate;

/**
 * Base class designed to make writing custom persistent delegates simpler.
 * Code common to all persistent delegates is implemented here.
 * 
 * Do NOT combine this class with "BaseTripleADelegate.java"
 * It is supposed to be separate, as Persistent Delegates do not do many things that normal delegates do, like Triggers, etc.
 * Persistent Delegates are active all the time.
 * 
 * @author Chris Duncan
 */
public abstract class BasePersistentDelegate extends AbstractDelegate implements IDelegate, IPersistentDelegate
{
	public BasePersistentDelegate()
	{
		super();
	}
	
	/**
	 * Called before the delegate will run.
	 * All classes should call super.start if they override this.
	 */
	@Override
	public void start()
	{
		super.start();
	}
	
	/**
	 * Called before the delegate will stop running.
	 * All classes should call super.end if they override this.
	 */
	@Override
	public void end()
	{
		super.end();
	}
}
