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

package games.strategy.triplea.troxAI;

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.delegate.remote.*;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.IntegerMap;

import java.io.*;
import java.util.*;

/**
 * 
 * @author Troy Graber
 * @version 1.0
 */
public class TroxAIPlayer implements IGamePlayer, ITripleaPlayer
{
    private final String m_name;
    private PlayerID m_id;
    private IPlayerBridge m_bridge;
    //added by Troy Graber
    private AI m_ai;

    //end addition

    /** Creates new TripleAPlayer */
    public TroxAIPlayer(String name)
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    public PlayerID getID()
    {
        return m_id;
    }

    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
        m_bridge = bridge;
        m_id = id;
        //GameData tempdata = m_ui.cloneGameData();
        if (m_id.getName().equals("Russians"))
        {
            int[] line = { 5, 5, 5, 5, 5, 0, 0 };
            try
            {
                char temp;
                int n = 0;
                BufferedReader in = new BufferedReader(new FileReader(
                        "personality.txt"));
                while ((temp = (char) in.read()) != -1 && n < 5)
                {
                    //System.out.println("" + temp);
                    if (temp != ',')
                    {
                        line[n] = Character.getNumericValue(temp);
                        n++;
                    }
                }
            } catch (IOException ioe)
            {
                System.err.println(ioe.getMessage());
            }
            m_ai = new AI(new Personality(line[0], line[1], line[2], line[3],
                    line[4]), m_id, m_bridge.getGameData());
            //System.out.println(""+line[0]+line[1]+line[2]+line[3]+line[4]);

        } else
        {
            m_ai = new AI(new Personality(5, 5, 5, 5, 5), m_id, m_bridge
                    .getGameData());
        }
    }

    public void start(String name)
    {
        if (name.endsWith("Bid"))
            purchase(true);
        else if (name.endsWith("Tech"))
            tech();
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

    }

    private void move(boolean nonCombat)
    {
        //The following lines added by Troy Graber

        Collection moves;

        AllianceTracker m_allies = m_bridge.getGameData().getAllianceTracker();

        Territory cc = m_bridge.getGameData().getMap().getTerritory("Caucaus");
        //System.out.println(cc.toString());
        if (cc != null
                && cc.getUnits().getUnits().iterator().hasNext()
                && !m_allies.isAllied(cc.getOwner(), (((Unit) (cc.getUnits()
                        .getUnits().iterator().next())).getOwner())))
            cc.setOwner(((Unit) (cc.getUnits().getUnits().iterator().next()))
                    .getOwner());

        //m_ai.setData(m_ui.cloneGameData());
        if (nonCombat)
        {
            moves = m_ai.selectNonCombatMoves();
        } else
        {
            moves = m_ai.selectCombatMoves();
        }

        Iterator iter2 = moves.iterator();
        while (iter2.hasNext())
        {
            MoveDescription m = (MoveDescription) iter2.next();
            if (m.getRoute() != null && m.getRoute().getLength() >= 1)
            {
                //System.out.println(""+m.toString());
                //System.out.println("Attacking " +
                // m.getRoute().getEnd().toString() + " Owned by " +
                // m.getRoute().getEnd().getOwner().toString());
                IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
                String error = moveDel.move(m.getUnits(), m.getRoute());
                if (error != null)
                    break;

                /*
                 * if(response!= null && response.isError()) {
                 * System.out.println(""+m.getRoute().toString() + " length " +
                 * m.getRoute().getLength() + " Units " + m.getUnits().size());
                 * System.out.println(""+response.getMessage());
                 * 
                 * Route r = m.getRoute();
                 * System.out.println(r.getStart().toString()); for(int q = 0; q <
                 * r.getLength(); q++) { System.out.println(r.at(q).toString()); }
                 * //m_ui.notifyError(response.getMessage());
                 *  }
                 */
            }
        }

    }

    private void purchase(boolean bid)
    {
        //m_ai.setData(m_ui.cloneGameData());
        if (bid)
        {
            String propertyName = m_id.getName() + " bid";
            if (Integer.parseInt(m_bridge.getGameData().getProperties().get(
                    propertyName).toString()) == 0)
                return;
        }

        //Need to make an integer Map of production
        Territory temp = TerritoryAttatchment.getCapital(m_id, m_bridge
                .getGameData());
        if (temp.getOwner() != m_id)
        { //EXIT HERE
            //m_ui.notifyError(temp.getOwner().getName() + " wins!");
            try
            {
                //FileOutputStream fout;

                // Open an output stream
                FileOutputStream fout = new FileOutputStream("results.txt",
                        true);

                //fout = new FileOutputStream ("results.txt");

                // Print a line of text
                new PrintStream(fout).println(temp.getOwner().getName() + ","
                        + m_ai.getTotalValue(temp.getOwner()) + ","
                        + m_bridge.getGameData().getSequence().getRound());

                // Close our output stream
                fout.close();
            } catch (IOException e)
            {
                System.err.println("Unable to write to file");
                System.exit(-1);
            }

            System.exit(0);
            return;
        }
        IntegerMap prod = m_ai.selectPurchases();

        if (prod == null)
            return;

        IPurchaseDelegate purchaseDel = (IPurchaseDelegate) m_bridge
                .getRemote();
        String error = purchaseDel.purchase(prod);

        if (error != null)
        {
            System.err.println(error);
            purchase(bid);
        }
        return;
    }

    private void battle()
    {
        while (true)
        {

            IBattleDelegate battleDel = (IBattleDelegate) m_bridge.getRemote();
            Collection battles = battleDel.getBattles().getBattles();
            if (battles.isEmpty())
                return;

            //code added by Troy Graber
            String error = battleDel.fightBattle((Territory) battles.iterator()
                    .next(), false);

            if (error != null)
                System.err.println(error);

        }
    }

    private void place(boolean bid)
    {
        while (true)
        {
            if (m_id.getUnits().size() == 0)
                return;

            //added by Troy Graber
            GameData g = m_bridge.getGameData();

            TerritoryAttatchment.getCapital(m_id, g);

            //we need to check for 4th edition to see if there is a limit on
            // the
            //number of units that can be produced in the capital
            IAbstractPlaceDelegate del = (IAbstractPlaceDelegate) m_bridge
                    .getRemote();
            del.placeUnits(m_id.getUnits().getUnits(), TerritoryAttatchment
                    .getCapital(m_id, g));
        }
    }

    /*
     * @see games.strategy.engine.framework.IGameLoader#getRemotePlayerType()
     */
    public Class getRemotePlayerType()
    {
        return ITripleaPlayer.class;
    }

    /*
     * 
     * 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String,
     *      java.util.Collection, java.util.Map, int, java.lang.String,
     *      games.strategy.triplea.delegate.DiceRoll,
     *      games.strategy.engine.data.PlayerID, java.util.List)
     */
    public CasualtyDetails selectCasualties(String step, Collection selectFrom,
            Map dependents, int count, String message, DiceRoll dice,
            PlayerID hit, List defaultCasualties)
    {

        List killed = new ArrayList();
        killed.addAll(m_ai.selectCasualities(selectFrom, count));
        List damaged = new ArrayList();
        CasualtyDetails m2 = new CasualtyDetails(killed, damaged, false);
        return m2;

    }

    /*
     * (non-Javadoc)
     * 
     * @see games.strategy.triplea.player.ITripleaPlayer#reportError(java.lang.String)
     */
    public void reportError(String error)
    {
        System.err.println(error);

    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectBombardingTerritory(games.strategy.engine.data.Unit, games.strategy.engine.data.Territory, java.util.Collection, boolean)
     */
    public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {       
        //return the first one
        return (Territory) territories.iterator().next();
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
     */
    public boolean shouldBomberBomb(Territory territory)
    {
        return false;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#whereShouldRocketsAttach(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Territory whereShouldRocketsAttach(Collection candidates, Territory from)
    {   
        //just use the first one
        return (Territory) candidates.iterator().next();
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Collection getNumberOfFightersToMoveToNewCarrier(Collection fightersThatCanBeMoved, Territory from)
    {
        return Collections.EMPTY_LIST;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
     */
    public Territory selectTerritoryForAirToLand(Collection candidates)
    {
       return (Territory) candidates.iterator().next();
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatNotificationMessage(java.util.Collection)
     */
    public void retreatNotificationMessage(Collection units)
    {
        // yeah, whatever
        
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmMoveInFaceOfAA(java.util.Collection)
     */
    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        return true;
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatQuery(games.strategy.net.GUID, boolean, java.util.Collection, java.lang.String, java.lang.String)
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection possibleTerritories, String message, String step)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#reportMessage(java.lang.String)
     */
    public void reportMessage(String message)
    {
       //dont bother
        
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#battleInfoMessage(java.lang.String, games.strategy.triplea.delegate.DiceRoll, java.lang.String)
     */
    public void battleInfoMessage(String shortMessage, DiceRoll dice, String step)
    {
      
        
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmCasualties(games.strategy.net.GUID, java.lang.String, java.lang.String)
     */
    public void confirmCasualties(GUID battleId, String message, String step)
    {
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmEnemyCasualties(games.strategy.net.GUID, java.lang.String, java.lang.String, games.strategy.engine.data.PlayerID)
     */
    public void confirmEnemyCasualties(GUID battleId, String message, String step, PlayerID hitPlayer)
    {
    }

}