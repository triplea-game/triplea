/*
 * Change.java
 *
 * Created on October 25, 2001, 1:27 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Not an interface because we want the perform() method to be protected.
 */
public abstract class Change implements Serializable
{	
	
	static final long serialVersionUID = -5563487769423328606L;

	protected abstract void perform(GameData data);
	public abstract Change invert();

}
