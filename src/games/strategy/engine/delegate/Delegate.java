/*
 * Delegate.java
 *
 * Created on October 13, 2001, 4:27 PM
 */

package games.strategy.engine.delegate;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.message.*;
import games.strategy.engine.message.IDestination;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A section of code that implements game logic.
 * The delegate should be deterministic.  All random events should be 
 * obtained through calls to the delegateBridge.
 *
 * Delegates make changes to gameData by calling the addChange method in DelegateBridge.
 *
 * All delegates must have a zero argument constructor, due to reflection constraints.
 * The delegate will be initialized with a call of initialize(..) before used.
 *
 * Delegates start executing with the start method, and stop with the end message.
 * 
 */
public interface Delegate extends IDestination
{

	//called after creation
	public void initialize(String name);
	
	/**
	 * Called before the delegate will run.
	 */
	public void start(DelegateBridge aBridge, GameData gameData);  
	/**
	 * Called before the delegate will stop running.
	 */
	public void end();
	
	/**
	 * A message has been received.
	 */
	public Message sendMessage(Message aMessage);
	
	public String getName();
}
