package games.strategy.net;

import java.io.*;
import java.util.*;

interface IConnectionListener
{
	/**
	 * A message has been received.
	 */
	public void messageReceived(Serializable msg, Connection connection);
	/**
	 * An error has occured and the connection is no longer valid.
	 * the given messages were not sent completely.
	 */
	public void fatalError(Exception error, Connection connection, List unsent);
}
