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

/*
* TripleAPlayer.java
*
* Created on November 2, 2001, 8:45 PM
 */

package games.strategy.triplea;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.message.*;
import games.strategy.engine.data.events.*;
import games.strategy.triplea.ui.TripleAFrame;

import games.strategy.triplea.delegate.message.*;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TripleAPlayer implements GamePlayer
{
  private final String m_name;
  private PlayerID m_id;
  private TripleAFrame m_ui;
  private PlayerBridge m_bridge;

  /** Creates new TripleAPlayer */
  public TripleAPlayer(String name)
  {
    m_name = name;
  }

  public void setFrame(TripleAFrame frame)
  {
    m_ui = frame;
  }

  public String getName()
  {
    return m_name;
  }

  public Message sendMessage(Message message)
  {
    if(message instanceof MultiDestinationMessage)
    {
      if(MultiDestinationMessage.shouldIgnore((MultiDestinationMessage) message))
        return null;
    }


    if(message instanceof SelectCasualtyQueryMessage)
      return m_ui.getCasualties( m_id, (SelectCasualtyQueryMessage) message);
    else if(message instanceof StringMessage)
    {
      StringMessage smsg = (StringMessage) message;
      if(!m_ui.playing(smsg.getIgnore()))
      {
        if(smsg.isError())
          m_ui.notifyError(smsg.getMessage());
        else
          m_ui.notifyMessage(smsg.getMessage());
      }
    }
    else if (message instanceof BattleStepMessage)
    {
      return m_ui.listBattle((BattleStepMessage) message);
    }
    else if (message instanceof BattleInfoMessage)
    {
      return m_ui.battleInfo((BattleInfoMessage) message);
    }
    else if (message instanceof BattleStringMessage)
    {
      return m_ui.battleStringMessage((BattleStringMessage) message);
    }
    if(message instanceof BattleStartMessage)
    {
      m_ui.battleStartMessage((BattleStartMessage) message);
    }
    else if(message instanceof RetreatQueryMessage)
    {
      return m_ui.getRetreat( (RetreatQueryMessage) message);
    }
    else if(message instanceof StrategicBombQuery)
    {
      return new BooleanMessage( m_ui.getStrategicBombingRaid((StrategicBombQuery) message));
    }
    else if (message instanceof RocketAttackQuery)
    {
      return m_ui.getRocketAttack( ((RocketAttackQuery) message).getTerritories());
    }
    return null;
  }

  public PlayerID getID()
  {
    return m_id;
  }

  public void initialize(PlayerBridge bridge, PlayerID id)
  {
    m_bridge = bridge;
    m_id = id;
  }

  public void start(String name)
  {
    if(name.endsWith("Bid"))
      purchase(true);
    else if(name.endsWith("Tech"))
      tech();
    else if(name.endsWith("Purchase"))
      purchase(false);
    else if(name.endsWith("Move"))
      move(name.endsWith("NonCombatMove"));
    else if(name.endsWith("Battle"))
      battle();
    else if(name.endsWith("Place"))
      place(name.indexOf("Bid") != -1);
    else if(name.endsWith("EndTurn"))
      ;//intentionally blank
    else
      throw new IllegalArgumentException("Unrecognized step name:" + name);

  }

  private void tech()
  {
    IntegerMessage message = m_ui.getTechRolls(m_id);
    if(message != null)
    {
      StringMessage msg = (StringMessage) m_bridge.sendMessage(message);
      if(msg == null)
        return;
      if(msg.isError())
      {
        m_ui.notifyError(msg.getMessage());
        tech();
      }
      else
      {
        m_ui.notifyMessage(msg.getMessage());
      }
    }
  }

  private void move(boolean nonCombat)
  {
    MoveMessage message = m_ui.getMove(m_id, m_bridge);
    if(message == null)
    {
      if(nonCombat)
        ensureAirCanLand();
      return;
    }


    StringMessage response = (StringMessage) m_bridge.sendMessage(message);
    if(response!= null && response.isError())
      m_ui.notifyError(response.getMessage());
    move(nonCombat);
  }

  private void ensureAirCanLand()
  {
    TerritoryCollectionMessage response = (TerritoryCollectionMessage) m_bridge.sendMessage(new MustMoveAirQueryMessage());
    if(response.getTerritories().size() == 0)
      return;
    else
    {
      StringBuffer buf = new StringBuffer("Air in following territories cant land:");
      Iterator iter = response.getTerritories().iterator();
      while(iter.hasNext())
      {
        buf.append( ((Territory) iter.next()).getName());
        buf.append(" ");
      }
      if(! m_ui.getOKToLetAirDie(buf.toString()))
        move(true);
    }
  }

  private void purchase(boolean bid)
  {
    if(bid)
    {
      String propertyName = m_id.getName() + " bid";
      if(Integer.parseInt(m_bridge.getGameData().getProperties().get(propertyName).toString()) == 0)
        return;
    }


    IntegerMap prod = m_ui.getProduction(m_id, bid);
    if(prod == null)
      return;
    BuyMessage message = new BuyMessage(prod);
    Message response = m_bridge.sendMessage(message);
    if(response != null && response instanceof StringMessage)
    {
      StringMessage error = (StringMessage) response;
      if(error.isError())
      {
        m_ui.notifyError(error.getMessage());
        purchase(bid);
      }
    }
    return;
  }

  private void battle()
  {
    while(true)
    {

      Message response = m_bridge.sendMessage(new GetBattles());
      if(response instanceof BattleListingMessage)
      {
        BattleListingMessage battles = (BattleListingMessage) response;
        if(battles.isEmpty())
        {
          return;
        }
        response = m_bridge.sendMessage(m_ui.getBattle(m_id, battles.getBattles(), battles.getStrategicRaids()));
        if(response instanceof StringMessage)
        {
          StringMessage msg = (StringMessage) response;
          if(msg.isError())
            m_ui.notifyError(((StringMessage) response).getMessage());
        }

      }
      else
        throw new IllegalArgumentException("Received response of wrong type:" + response);
    }
  }

  private void place(boolean bid)
  {
    while(true)
    {
      if(m_id.getUnits().size() == 0)
        return;

      PlaceMessage message = m_ui.getPlace(m_id, bid);
      if(message == null)
        return;
      else
      {
        StringMessage response = (StringMessage) m_bridge.sendMessage(message);
        if(response.isError())
          m_ui.notifyError(response.getMessage() );
      }
    }
  }
}