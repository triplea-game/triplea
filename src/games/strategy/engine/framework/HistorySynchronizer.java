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

import java.io.*;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;
import games.strategy.engine.history.RemoteHistoryMessage;
import games.strategy.net.IMessageListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;


/**
 * This class synchronizes a Game history by listening to the messages sent between the client
 * and the server.
 * Its a bit of a hack.
 */
public class HistorySynchronizer
{
  private final GameData m_data;
  private int m_currentRound;
  private final IMessenger m_messenger;

  public HistorySynchronizer(GameData data, IMessenger messenger)
  {
    m_data = data;
    m_currentRound = data.getSequence().getRound();
    m_messenger = messenger;
    m_messenger.addMessageListener(m_messageListener);
  }

  public void deactivate()
  {
    m_messenger.removeMessageListener(m_messageListener);
  }

  private IMessageListener m_messageListener = new IMessageListener()
  {
    public void messageReceived(final Serializable msg, INode from)
    {
      SwingUtilities.invokeLater(
        new Runnable()
      {
        public void run()
        {
          processMessage(translateIntoMyData(msg));
        }
      }
      );

    }



    private void processMessage(Serializable msg)
    {
      if (msg instanceof StepChangedMessage)
      {
        StepChangedMessage stepChange = (StepChangedMessage) msg;

        if (m_currentRound != stepChange.getRound())
        {
          m_currentRound = stepChange.getRound();
          m_data.getHistory().getHistoryWriter().startNextRound(m_currentRound);
        }
        m_data.getHistory().getHistoryWriter().startNextStep(stepChange.getStepName(), stepChange.getDelegateName(), stepChange.getPlayer(), stepChange.getDisplayName());
      }
      else if (msg instanceof ChangeMessage)
      {
        ChangeMessage changeMessage = (ChangeMessage) msg;
        m_data.getHistory().getHistoryWriter().addChange(changeMessage.getChange());
      }
      else if (msg instanceof RemoteHistoryMessage)
      {
        ( (RemoteHistoryMessage) msg).perform(m_data.getHistory().getHistoryWriter());
      }
    }

  };

  /**
   * Serializes the object and then deserializes it, resolving object
   * references into m_data.
   * Note the the history we are synching may refer to a different game data than
   * the GaneData held by the IGame.  A clone is made so that we can walk up and down
   * the history without changing the game.
   */
  private Serializable translateIntoMyData(Serializable msg)
  {
    try
    {
      ByteArrayOutputStream sink = new ByteArrayOutputStream();
      GameObjectOutputStream out = new GameObjectOutputStream(sink);
      out.writeObject(msg);
      out.flush();
      out.close();

      ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
      sink = null;

      GameObjectStreamFactory factory = new GameObjectStreamFactory(m_data);
      ObjectInputStream in = factory.create(source);
      try
      {
        Serializable newMessage = (Serializable) in.readObject();
        return newMessage;
      }
      catch (ClassNotFoundException ex)
      {
        //should never happen
        throw new RuntimeException(ex);
      }
    }
    catch(IOException ioe)
    {
      throw new RuntimeException(ioe);
    }
  }

}
