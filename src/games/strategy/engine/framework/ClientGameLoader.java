/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.framework;

import javax.swing.*;

import games.strategy.net.*;
import games.strategy.engine.data.*;
import games.strategy.engine.framework.message.*;
import java.io.*;

/**
 * <p>Title: TripleA</p>
 * <p> </p>
 * <p> Copyright (c) 2002</p>
 * <p> </p>
 * @author Sean Bridges
 *
 */



public class ClientGameLoader implements IGameDataLoader
{

	private ClientMessenger m_messenger;
	private Object m_lock = new Object();
	private GameData m_data;

    public ClientGameLoader(ClientMessenger messenger)
    {
	    m_messenger = messenger;
    }

	public GameData loadData()
	{

		System.out.println("Loading game data from server");
		long now = System.currentTimeMillis();

		m_messenger.addMessageListener(m_messageListener);
		synchronized(m_lock)
		{
		    m_messenger.send(new GameDataRequest(), m_messenger.getServerNode());
			try
			{
				m_lock.wait();
			} catch( InterruptedException ie)
			{
				ie.printStackTrace();
			}
			finally
			{
				m_messenger.removeMessageListener(m_messageListener);
			}
		}
		System.out.println("Done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");


		return m_data;
	}


	private IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			if(msg instanceof GameDataResponse)
			{
				GameDataResponse response = (GameDataResponse) msg;
				System.out.println("data revieved: dataSize:" + response.getDataSize() + " bytes, compressed size:" + response.getTransmittedSize());
				try
				{
					m_data = new GameDataManager().loadGame(response.getGameData());
					synchronized(m_lock)
					{
						m_lock.notifyAll();
					}
				} catch(IOException ioe)
				{
					ioe.printStackTrace();
					System.exit(0);
				}
			}
		}
	};

}