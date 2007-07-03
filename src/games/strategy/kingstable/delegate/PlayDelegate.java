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

package games.strategy.kingstable.delegate;

import java.util.Collection;
import java.util.HashSet;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.kingstable.attachments.TerritoryAttachment;
import games.strategy.kingstable.delegate.remote.IPlayDelegate;
import games.strategy.kingstable.ui.display.IKingsTableDisplay;

/**
 * Responsible for performing a move in a game of King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
@AutoSave(beforeStepStart=false,afterStepEnd=true)
public class PlayDelegate extends BaseDelegate implements IPlayDelegate
{
    
    private Matches matches = null;

    
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {
        super.start(bridge, gameData);

        if (matches==null)
            matches = new Matches(gameData);
        
        IKingsTableDisplay display = (IKingsTableDisplay) bridge.getDisplayChannelBroadcaster();
        display.setStatus(m_player.getName() + "'s turn");
    }

    /**
     * Attempt to move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
     * 
     * @param start <code>Territory</code> where the move should start
     * @param end <code>Territory</code> where the move should end
     */
    public String play(Territory start, Territory end)
    {   
        String error = isValidPlay(start,end);
        if (error != null)
            return error;
        
        Collection<Territory> captured = checkForCaptures(end);
        performPlay(start,end,captured,m_player);
        
        return null;
    }

    
    /**
     * After a move completes, look to see if any captures occur.
     * 
     * @param end <code>Territory</code> where the move ended. All potential captures must involve this <code>Territory</code>.
     * @return
     */
    private Collection<Territory> checkForCaptures(Territory end)
    {   
        // At most, four pieces will be captured
        Collection<Territory> captured = new HashSet<Territory>(4);
        
        // Failsafe - end should never be null, so only check for captures if it isn't null
        if (end!=null)
        {   
            // Get the coordinates where the move ended
            int endX = end.getX();
            int endY = end.getY();

            // Look above end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
            { 
                Territory possibleCapture = m_data.getMap().getTerritoryFromCoordinates(endX,endY-1);
                if (matches.eligibleForCapture(possibleCapture, m_player))
                {   
                    // Get the territory to the left of the possible capture
                    Territory above = m_data.getMap().getTerritoryFromCoordinates(endX,endY-2);
                    
                    // Can the king be captured?
                    if (matches.kingInSquare(possibleCapture))
                    {   
                        Territory left = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY-1);
                        Territory right = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY-1);

                        if (matches.eligibleParticipantsInKingCapture(m_player, above, left, right))
                            captured.add(possibleCapture);
                        else if (matches.kingCanBeCapturedLikeAPawn && matches.eligibleParticipantInPawnCapture(m_player, above))
                            captured.add(possibleCapture);
                        
                    }                     
                    // Can a pawn be captured?
                    else if (matches.eligibleParticipantInPawnCapture(m_player, above))
                    {
                        captured.add(possibleCapture);
                    }
       
                }                    
            }   


            // Look below end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
            { 
                Territory possibleCapture = m_data.getMap().getTerritoryFromCoordinates(endX,endY+1);
                if (matches.eligibleForCapture(possibleCapture, m_player))
                {   
                    // Get the territory to the left of the possible capture
                    Territory below = m_data.getMap().getTerritoryFromCoordinates(endX,endY+2);
                    
                    // Can the king be captured?
                    if (matches.kingInSquare(possibleCapture))
                    {   
                        Territory left = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY+1);
                        Territory right = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY+1);

                        if (matches.eligibleParticipantsInKingCapture(m_player, below, left, right))
                            captured.add(possibleCapture);
                        else if (matches.kingCanBeCapturedLikeAPawn && matches.eligibleParticipantInPawnCapture(m_player, below))
                            captured.add(possibleCapture);
                        
                    }                     
                    // Can a pawn be captured?
                    else if (matches.eligibleParticipantInPawnCapture(m_player, below))
                    {
                        captured.add(possibleCapture);
                    }
       
                }                    
            }
            
            
            // Look left end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces            
            { 
                Territory possibleCapture = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY);
                if (matches.eligibleForCapture(possibleCapture, m_player))
                {   
                    // Get the territory to the left of the possible capture
                    Territory left = m_data.getMap().getTerritoryFromCoordinates(endX-2,endY);
                    
                    // Can the king be captured?
                    if (matches.kingInSquare(possibleCapture))
                    {   
                        Territory above = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY-1);
                        Territory below = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY+1);

                        if (matches.eligibleParticipantsInKingCapture(m_player, left, above, below))
                            captured.add(possibleCapture);
                        else if (matches.kingCanBeCapturedLikeAPawn && matches.eligibleParticipantInPawnCapture(m_player, left))
                            captured.add(possibleCapture);
                        
                    }                     
                    // Can a pawn be captured?
                    else if (matches.eligibleParticipantInPawnCapture(m_player, left))
                    {   
                        captured.add(possibleCapture);
                    }
       
                }                    
            }
            
            
            // Look right end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of possibleCapture stays within the braces
            { 
                Territory possibleCapture = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY);
                if (matches.eligibleForCapture(possibleCapture, m_player))
                {   
                    // Get the territory to the left of the possible capture
                    Territory right = m_data.getMap().getTerritoryFromCoordinates(endX+2,endY);
                    
                    // Can the king be captured?
                    if (matches.kingInSquare(possibleCapture))
                    {   
                        Territory above = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY-1);
                        Territory below = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY+1);

                        if (matches.eligibleParticipantsInKingCapture(m_player, right, above, below))
                            captured.add(possibleCapture);
                        else if (matches.kingCanBeCapturedLikeAPawn && matches.eligibleParticipantInPawnCapture(m_player, right))
                            captured.add(possibleCapture);
                        
                    }                     
                    // Can a pawn be captured?
                    else if (matches.eligibleParticipantInPawnCapture(m_player, right))
                    {
                        captured.add(possibleCapture);
                    }
                }                    
            }
        }
        
        return captured;
    }
    
    
    /**
     * Check to see if moving a piece from the start <code>Territory</code> to the end <code>Territory</code> is a valid play.
     * 
     * @param start <code>Territory</code> where the move should start
     * @param end <code>Territory</code> where the move should end
     */
    private String isValidPlay(Territory start, Territory end)
    {
        int unitCount = start.getUnits().getUnitCount(m_player);
        
        // The current player must have exactly one unit in the starting territory
        if ( unitCount < 1)
            return m_player.getName() + " doesn't have a piece in the selected starting square.";
        else if (unitCount > 1)
            return "The selected starting square contains more than one piece - that shouldn't be possible.";
        
        // The destination territory must be empty
        if (! end.getUnits().isEmpty())
            return "The selected destination square is not empty";
        
        int startX = start.getX();
        int endX = end.getX();
        int startY = start.getY();
        int endY = end.getY();
        
        // Pieces can only move in a straight line
        //   and the intervening spaces must be empty
        if (startX == endX)
        {
            int y1,y2;
            if (startY < endY)
            {
                y1 = startY + 1;
                y2 = endY - 1;
            } else
            {
                y1 = endY + 1;
                y2 = startY - 1;
            }

            for (int y=y1; y<=y2; y++) 
            {
                Territory at = m_data.getMap().getTerritoryFromCoordinates(startX,y);
                if (at.getUnits().size() > 0)
                    return "Pieces can only move through empty spaces.";
            }
            
        }
        else if (startY == endY)
        {
            int x1,x2;
            if (startX < endX)
            {
                x1 = startX + 1;
                x2 = endX - 1;
            } else
            {
                x1 = endX + 1;
                x2 = startX - 1;
            }
            
            for (int x=x1; x<=x2; x++) {
                Territory at = m_data.getMap().getTerritoryFromCoordinates(x,startY);
                if (at.getUnits().size() > 0)
                    return "Intervening square (" + x + "," + startY + ") is not empty.";
            }
        }
        else
            return "Pieces can only move in a straight line.";
        
        
        // Only the king can move to king's squares  
        if (! matches.kingInSquare(start) && matches.isKingsSquare(end))
        {
            return "Only the king can go there";
        }
        
        return null;
    }
   
    
    /**
     * Move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
     * 
     * @param start <code>Territory</code> where the move should start
     * @param end <code>Territory</code> where the move should end
     */
    private void performPlay(Territory start, Territory end, Collection<Territory> captured, PlayerID player)
    {  
        Collection<Unit> units = start.getUnits().getUnits();
        
        String transcriptText = player.getName() + " moved from " + start.getName() + " to " + end.getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        m_bridge.getHistoryWriter().setRenderingData(units);
        
        Change removeUnit = ChangeFactory.removeUnits(start, units);
        Change removeStartOwner = ChangeFactory.changeOwner(start, PlayerID.NULL_PLAYERID);
        Change addUnit = ChangeFactory.addUnits(end, units);
        Change addEndOwner = ChangeFactory.changeOwner(end, player);
        
        CompositeChange change = new CompositeChange();
        change.add(removeUnit);
        change.add(removeStartOwner);
        change.add(addUnit);
        change.add(addEndOwner);
        
        for (Territory at : captured) {
            if (at != null)
            {   
                Collection<Unit> capturedUnits = at.getUnits().getUnits();
                Change capture = ChangeFactory.removeUnits(at, capturedUnits);
                change.add(capture);
                
                Change removeOwner = ChangeFactory.changeOwner(at, PlayerID.NULL_PLAYERID);
                change.add(removeOwner);
            }
        }
        m_bridge.addChange(change);
     
        
        IKingsTableDisplay display = (IKingsTableDisplay) m_bridge.getDisplayChannelBroadcaster();
        display.performPlay(start,end,captured);
    }
    
    
    /**
     * If this class implements an interface which inherits from IRemote, returns the class of that interface.
     * Otherwise, returns null.
     */
    public Class<? extends IRemote> getRemoteType()
    {
        // This class implements IPlayDelegate, which inherits from IRemote.
        return IPlayDelegate.class;
    }
    
    
    /**
     * Utility class providing matching methods for use in King's Table.
     * 
     * @author Lane Schwartz
     */
    private class Matches
    {
        private final boolean kingCanParticipateInCaptures;
        private final boolean cornerSquaresCanBeUsedToCapturePawns;
        private final boolean centerSquareCanBeUsedToCapturePawns;
        private final boolean cornerSquaresCanBeUsedToCaptureTheKing;
        private final boolean centerSquareCanBeUsedToCaptureTheKing;
        private final boolean edgeOfBoardCanBeUsedToCaptureTheKing;
        private final boolean kingCanBeCapturedLikeAPawn;   
        
        Matches(GameData gameData)
        {
            GameProperties properties = gameData.getProperties();
            kingCanParticipateInCaptures = properties.get("King can participate in captures", true);
            cornerSquaresCanBeUsedToCapturePawns = properties.get("Corner squares can be used to capture pawns", true);
            centerSquareCanBeUsedToCapturePawns = properties.get("Center square can be used to capture pawns", false);
            cornerSquaresCanBeUsedToCaptureTheKing = properties.get("Corner squares can be used to capture the king", false);
            centerSquareCanBeUsedToCaptureTheKing= properties.get("Center square can be used to capture the king", true);
            edgeOfBoardCanBeUsedToCaptureTheKing = properties.get("Edge of board can be used to capture the king", false);
            kingCanBeCapturedLikeAPawn = properties.get("King can be captured like a pawn", false);
            
        }
        
        public boolean kingInSquare(Territory t) 
        {   
            if (t==null)
            {
                return false;
            }
            else 
            {   Collection<Unit> units = t.getUnits().getUnits();
                if (units.isEmpty())
                    return false;
                else
                {
                    Unit unit = (Unit) units.toArray()[0];
                    if (unit.getType().getName().equals("king"))
                        return true;
                    else 
                        return false;    
                }
            }                
        }

        public boolean isKingsExit(Territory t)
        {
            TerritoryAttachment ta = ((TerritoryAttachment) t.getAttachment("territoryAttachment"));
            if (ta==null)
                return false;
            else if (ta.isKingsExit())
                return true;
            else
                return false;
        }

        public boolean isKingsSquare(Territory t)
        {
            TerritoryAttachment ta = ((TerritoryAttachment) t.getAttachment("territoryAttachment"));
            if (ta==null)
                return false;
            else if (ta.isKingsSquare())
                return true;
            else
                return false;
        }

        public boolean eligibleParticipantInPawnCapture(PlayerID currentPlayer, Territory territory) 
        {
            //System.out.println("eligibleParticipantInPawnCapture" + currentPlayer.getName() + " " + territory.getName());
            if (territory==null)
            {
                return false;
            }
            else 
            {
                if (territory.getOwner().equals(currentPlayer))
                {
                    if (territory.getUnits().isEmpty())
                        return false;
                    else if (! kingCanParticipateInCaptures && kingInSquare(territory))
                        return false;
                    else
                        return true;
                }
                else
                {
                    if (cornerSquaresCanBeUsedToCapturePawns && isKingsExit(territory))
                        return true;
                    else if (centerSquareCanBeUsedToCapturePawns && isKingsSquare(territory))
                        return true;
                    else
                        return false;
                }
            }
        }


        public boolean eligibleParticipantInKingCapture(PlayerID currentPlayer, Territory territory) 
        {
            if (territory==null)
            {
                if (edgeOfBoardCanBeUsedToCaptureTheKing)
                    return true;
                else
                    return false;
            }
            else 
            {
                if (territory.getOwner().equals(currentPlayer))
                {
                    if (territory.getUnits().size() > 0)
                        return true;
                    else
                        return false;
                }
                else
                {
                    if (cornerSquaresCanBeUsedToCaptureTheKing && isKingsExit(territory))
                        return true;
                    else if (centerSquareCanBeUsedToCaptureTheKing && isKingsSquare(territory))
                        return true;
                    else
                        return false;
                }
            }
        }

        public boolean eligibleParticipantsInKingCapture(PlayerID currentPlayer, Territory... territories)
        {
            if (territories==null || territories.length==0)
                return false;

            for (Territory territory : territories)
            {
                if (! eligibleParticipantInKingCapture(currentPlayer, territory))
                    return false;
            }

            return true;
        }

        public boolean eligibleForCapture(Territory territory, PlayerID currentPlayer)
        {
            if (territory==null || territory.getUnits().isEmpty() || territory.getOwner().equals(currentPlayer))
                return false;
            else
                return true;
        }

    }
}
