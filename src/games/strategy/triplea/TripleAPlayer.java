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
import games.strategy.triplea.ui.*;
import games.strategy.triplea.ui.TripleAFrame;

import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.delegate.remote.*;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.delegate.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleAPlayer implements GamePlayer
{
    private final String m_name;

    private PlayerID m_id;

    private TripleAFrame m_ui;

    private IPlayerBridge m_bridge;

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
        if (message instanceof MultiDestinationMessage)
        {
            if (MultiDestinationMessage
                    .shouldIgnore((MultiDestinationMessage) message))
                return null;
        }

        if (message instanceof SelectCasualtyQueryMessage)
        {
            return m_ui.getCasualties((SelectCasualtyQueryMessage) message);
        } else if (message instanceof BombardmentQueryMessage)
        {
            return m_ui.getBombardment((BombardmentQueryMessage) message);
        } else if (message instanceof StringMessage)
        {
            StringMessage smsg = (StringMessage) message;
            if (!m_ui.playing(smsg.getIgnore()))
            {
                if (smsg.isError())
                    m_ui.notifyError(smsg.getMessage());
                else
                    m_ui.notifyMessage(smsg.getMessage());
            }
        } else if (message instanceof BattleStepMessage)
        {
            return m_ui.listBattle((BattleStepMessage) message);
        } else if (message instanceof CasualtyNotificationMessage)
        {
            m_ui
                    .casualtyNotificationMessage((CasualtyNotificationMessage) message);
            return null;
        } else if (message instanceof BattleInfoMessage)
        {
            return m_ui.battleInfo((BattleInfoMessage) message);
        } else if (message instanceof BattleStringMessage)
        {
            return m_ui.battleStringMessage((BattleStringMessage) message);
        } else if (message instanceof MoveFightersToNewCarrierMessage)
        {
            return m_ui
                    .moveFightersToCarrier((MoveFightersToNewCarrierMessage) message);
        }
        if (message instanceof BattleStartMessage)
        {
            m_ui.battleStartMessage((BattleStartMessage) message);
        } else if (message instanceof RetreatQueryMessage)
        {
            return m_ui.getRetreat((RetreatQueryMessage) message);
        } else if (message instanceof RetreatNotificationMessage)
        {
            m_ui.notifyRetreat((RetreatNotificationMessage) message);
            return null;
        } else if (message instanceof LandAirQueryMessage)
        {
            return m_ui.getLandAir((LandAirQueryMessage) message);
        } else if (message instanceof StrategicBombQuery)
        {
            return new BooleanMessage(m_ui
                    .getStrategicBombingRaid((StrategicBombQuery) message));
        } else if (message instanceof RocketAttackQuery)
        {
            return m_ui.getRocketAttack((RocketAttackQuery) message);
        } else if (message instanceof BattleEndMessage)
        {
            m_ui.battleEndMessage((BattleEndMessage) message);
            return null;
        } else if (message instanceof BombingResults)
        {
            m_ui.bombingResults((BombingResults) message);
            return null;
        }

        return null;
    }

    public PlayerID getID()
    {
        return m_id;
    }

    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
        m_bridge = bridge;
        m_id = id;
    }

    public void start(String name)
    {
        if (name.endsWith("Bid"))
            purchase(true);
        else if (name.endsWith("Tech"))
            tech();
        else if (name.endsWith("TechActivation"))
            ; // the delegate handles everything
        else if (name.endsWith("Purchase"))
            purchase(false);
        else if (name.endsWith("Move"))
            move(name.endsWith("NonCombatMove"));
        else if (name.endsWith("Battle"))
            battle();
        else if (name.endsWith("Place"))
            place(name.indexOf("Bid") != -1);
        else if (name.endsWith("EndTurn"))
            ;//intentionally blank
        else
            throw new IllegalArgumentException("Unrecognized step name:" + name);

    }

    private void tech()
    {
        //can we tech?
        if (m_id.getResources().getQuantity(Constants.IPCS) == 0)
            return;

        TechRoll techRoll = m_ui.getTechRolls(m_id);
        if (techRoll != null)
        {
            ITechDelegate techDelegate = (ITechDelegate) m_bridge.getRemote();
            TechResults techResults = techDelegate.rollTech(
                    techRoll.getRolls(), techRoll.getTech());

            if (techResults.isError())
            {
                m_ui.notifyError(techResults.getErrorString());
                tech();
            } else
                m_ui.notifyTechResults(techResults);
        }
    }

    private void move(boolean nonCombat)
    {
        if (!hasUnitsThatCanMove(nonCombat))
            return;

        MoveDescription moveDescription = m_ui.getMove(m_id, m_bridge, nonCombat);
        if (moveDescription == null)
        {
            if (nonCombat)
                ensureAirCanLand();
            return;
        }

        IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
        String error = moveDel.move(moveDescription.getUnits(), moveDescription.getRoute(), moveDescription.getTransportsThatCanBeLoaded());
        
        if (error != null )
            m_ui.notifyError(error);
        move(nonCombat);	
    }

    private void ensureAirCanLand()
    {
        Collection airCantLand = ((IMoveDelegate) m_bridge.getRemote()).getTerritoriesWhereAirCantLand();
        
        if (airCantLand.isEmpty())
            return;
        else
        {
            StringBuffer buf = new StringBuffer(
                    "Air in following territories cant land:");
            Iterator iter = airCantLand.iterator();
            while (iter.hasNext())
            {
                buf.append(((Territory) iter.next()).getName());
                buf.append(" ");
            }
            if (!m_ui.getOKToLetAirDie(buf.toString()))
                move(true);
        }
    }

    private boolean hasUnitsThatCanMove(boolean nonCom)
    {
        CompositeMatchAnd moveableUnitOwnedByMe = new CompositeMatchAnd();
        moveableUnitOwnedByMe.add(Matches.unitIsOwnedBy(m_id));
        //non com, can move aa units
        if(nonCom)
            moveableUnitOwnedByMe.add(new InverseMatch(Matches.UnitIsFactory));
        else //combat move, cant move aa units
            moveableUnitOwnedByMe.add(new InverseMatch(Matches.UnitIsAAOrFactory));
        
        Iterator territoryIter = m_bridge.getGameData().getMap()
                .getTerritories().iterator();
        while (territoryIter.hasNext())
        {
            Territory item = (Territory) territoryIter.next();
            if (item.getUnits().someMatch(moveableUnitOwnedByMe))
            {
                return true;
            }
        }
        return false;

    }

    private void purchase(boolean bid)
    {
        if (bid)
        {
            String propertyName = m_id.getName() + " bid";
            if (Integer.parseInt(m_bridge.getGameData().getProperties().get(
                    propertyName).toString()) == 0)
                return;
        } else
        {
            //can we buy anything
            if (m_id.getResources().getQuantity(Constants.IPCS) == 0)
                return;
        }

        IntegerMap prod = m_ui.getProduction(m_id, bid);
        if (prod == null)
            return;
        IPurchaseDelegate purchaseDel = (IPurchaseDelegate) m_bridge.getRemote();
        String error = purchaseDel.purchase(prod);
        
        if (error != null)
        {
            m_ui.notifyError(error);
            //dont give up, keep going
            purchase(bid);

        }
        return;
    }

    private void battle()
    {
        while (true)
        {
            IBattleDelegate battleDel = (IBattleDelegate) m_bridge.getRemote();
            BattleListing battles = battleDel.getBattles();
           
                
                if (battles.isEmpty())
                {
                    return;
                }
                
                FightBattleDetails details = m_ui.getBattle(m_id, battles
                        .getBattles(), battles.getStrategicRaids());
                String error = battleDel.fightBattle(details.getWhere(), details.isBombingRaid());

                if(error != null)
                        m_ui.notifyError(error);
        }
    }

    private void place(boolean bid)
    {
        //nothing to place
        if (m_id.getUnits().size() == 0)
            return;
        
        while(true)
        {
            PlaceData data = m_ui.waitForPlace(m_id ,bid, m_bridge);
            if(data == null)
                return;
            IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) m_bridge.getRemote();
            String error = placeDel.placeUnits(data.getUnits(), data.getAt());
            if(error != null)
                m_ui.notifyError(error);
        }
    }
}








