package games.strategy.net;

import java.io.*;
import java.util.*;

public interface IMessageListener
{
	public void messageReceived(Serializable msg, INode from);
}
