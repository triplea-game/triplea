/*
 * DefaultObjectInputStreamFactory.java
 * 
 * Created on December 14, 2001, 8:54 AM
 */

package games.strategy.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * 
 * @author Sean Bridges
 */
public class DefaultObjectStreamFactory implements IObjectStreamFactory
{
	@Override
	public ObjectInputStream create(InputStream stream) throws IOException
	{
		return new ObjectInputStream(stream);
	}
	
	@Override
	public ObjectOutputStream create(OutputStream stream) throws IOException
	{
		return new ObjectOutputStream(stream);
	}
}
