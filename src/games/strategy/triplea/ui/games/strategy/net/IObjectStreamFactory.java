/*
 * IObjectStreamFactory.java
 *
 * Created on December 14, 2001, 8:53 AM
 */

package games.strategy.net;

import java.io.*;

/**
 *
 * @author  Sean Bridges
 */
public interface IObjectStreamFactory 
{
	public ObjectInputStream create(InputStream stream) throws IOException;
	public ObjectOutputStream create(OutputStream stream) throws IOException;
}
