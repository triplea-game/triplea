package games.strategy.net;

import java.net.*;
import java.io.Serializable;

public interface INode extends Serializable
{
	public String getName();
	
	public InetAddress getAddress();
	
	public int getPort();
}




