/*
 * DefaultObjectInputStreamFactory.java
 *
 * Created on December 14, 2001, 8:54 AM
 */

package games.strategy.net;

import java.io.*;

/**
 * 
 * @author  Sean Bridges
 */
public class DefaultObjectStreamFactory implements IObjectStreamFactory
{
	public ObjectInputStream create(InputStream stream) throws IOException
	{
		return new ObjectInputStream(stream);
	}	
	
	public ObjectOutputStream create(OutputStream stream) throws IOException
	{
		return new ObjectOutputStream(stream);
	}
}
