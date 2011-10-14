/*
 * IObjectStreamFactory.java
 * 
 * Created on December 14, 2001, 8:53 AM
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
public interface IObjectStreamFactory
{
	public ObjectInputStream create(InputStream stream) throws IOException;
	
	public ObjectOutputStream create(OutputStream stream) throws IOException;
}
