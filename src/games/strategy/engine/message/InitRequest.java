/*
 * InitRequest.java
 *
 * Created on December 27, 2001, 12:02 PM
 */

package games.strategy.engine.message;

/**
 *
 * @author  Sean Bridges
 *
 * When a MessageManager is created it broadcasts this message
 * to find which destinations other managers will accept messages to.
 */
class InitRequest implements java.io.Serializable
{

	private static final long serialVersionUID = 2211791977019945104L;
	
	/** Creates a new instance of InitRequest */
    InitRequest() 
	{
    }

	
}
