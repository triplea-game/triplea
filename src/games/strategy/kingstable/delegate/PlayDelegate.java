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
public class PlayDelegate extends BaseDelegate implements IPlayDelegate
{
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {
        super.start(bridge, gameData);
        
        
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
            // This extra set of braces is for bug prevention - it makes sure that the scope of above stays within the braces
            { 
                Territory above = m_data.getMap().getTerritoryFromCoordinates(endX,endY-1);
                if (above != null  &&  !above.getUnits().isEmpty()  && above.getOwner()!=m_player)
                {   
                    Territory other = m_data.getMap().getTerritoryFromCoordinates(endX,endY-2);
                    Unit unit = (Unit) above.getUnits().getUnits().toArray()[0];
                    
                    // Can the king be captured?
                    if (unit.getType().getName().equals("king"))
                    {

                        Territory other_left = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY-1);
                        Territory other_right = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY-1);
                        TerritoryAttachment ta_other = ((TerritoryAttachment)other.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_left = ((TerritoryAttachment)other_left.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_right = ((TerritoryAttachment)other_right.getAttachment("territoryAttachment"));

                        if (other != null  &&  other_left != null  &&  other_right != null  &&  
                                ((!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)  ||  (ta_other!=null && ta_other.isKingsSquare()))  && 
                                ((!other_left.getUnits().isEmpty()  &&  other_left.getOwner()==m_player)  ||  (ta_other_left!=null && ta_other_left.isKingsSquare()))  &&  
                                ((!other_right.getUnits().isEmpty()  &&  other_right.getOwner()==m_player)  ||  (ta_other_right!=null && ta_other_right.isKingsSquare())))                           
                        {    
                            captured.add(above);
                        }
                    }
                    // Can a pawn be captured?
                    else 
                    {
                        if (other != null)
                        {
                            if (!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)
                            {
                                captured.add(above); 
                            }
                            else 
                            {
                                TerritoryAttachment ta = (TerritoryAttachment) other.getAttachment("territoryAttachment");
                                if (ta != null && ta.isKingsExit())
                                    captured.add(above);
                            }
                        }
                    }
                }                    
            }   


            // Look below end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of below stays within the braces
            {
                Territory below = m_data.getMap().getTerritoryFromCoordinates(endX,endY+1);
                if (below != null && below.getUnits().getUnitCount() > 0 && below.getOwner()!=m_player)
                {
                    Territory other = m_data.getMap().getTerritoryFromCoordinates(endX,endY+2);
                    Unit unit = (Unit) below.getUnits().getUnits().toArray()[0];
                    
                    // Can the king be captured?
                    if (unit.getType().getName().equals("king"))
                    {
                        Territory other_left = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY+1);
                        Territory other_right = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY+1);
                        TerritoryAttachment ta_other = ((TerritoryAttachment)other.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_left = ((TerritoryAttachment)other_left.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_right = ((TerritoryAttachment)other_right.getAttachment("territoryAttachment"));

                        if (other != null  &&  other_left != null  &&  other_right != null  &&  
                                ((!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)  ||  (ta_other!=null && ta_other.isKingsSquare()))  && 
                                ((!other_left.getUnits().isEmpty()  &&  other_left.getOwner()==m_player)  ||  (ta_other_left!=null && ta_other_left.isKingsSquare()))  &&  
                                ((!other_right.getUnits().isEmpty()  &&  other_right.getOwner()==m_player)  ||  (ta_other_right!=null && ta_other_right.isKingsSquare())))                            
                        {
                            captured.add(below);
                        }                        
                    } 
                    // Can a pawn be captured?
                    else 
                    {
                        if (other != null)
                        {
                            if (!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)
                            {
                                captured.add(below); 
                            }
                            else 
                            {
                                TerritoryAttachment ta = (TerritoryAttachment) other.getAttachment("territoryAttachment");
                                if (ta != null && ta.isKingsExit())
                                    captured.add(below);
                            }
                        }
                    }
                }
            }

            
            // Look left end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of left stays within the braces            
            {
                Territory left = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY);
                if (left != null && left.getUnits().getUnitCount() > 0 && left.getOwner()!=m_player)
                {
                    Territory other = m_data.getMap().getTerritoryFromCoordinates(endX-2,endY);
                    Unit unit = (Unit) left.getUnits().getUnits().toArray()[0];
                    
                    // Can the king be captured?
                    if (unit.getType().getName().equals("king"))
                    {
                        Territory other_above = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY-1);
                        Territory other_below = m_data.getMap().getTerritoryFromCoordinates(endX-1,endY+1);
                        TerritoryAttachment ta_other = ((TerritoryAttachment)other.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_above = ((TerritoryAttachment)other_above.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_below = ((TerritoryAttachment)other_below.getAttachment("territoryAttachment"));

                        if (other != null  &&  other_above != null  &&  other_below != null  &&  
                                ((!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)  ||  (ta_other!=null && ta_other.isKingsSquare()))  && 
                                ((!other_above.getUnits().isEmpty()  &&  other_above.getOwner()==m_player)  ||  (ta_other_above!=null && ta_other_above.isKingsSquare()))  &&  
                                ((!other_below.getUnits().isEmpty()  &&  other_below.getOwner()==m_player)  ||  (ta_other_below!=null && ta_other_below.isKingsSquare())))
                        {    
                            captured.add(left);
                        }
                    } 
                    // Can a pawn be captured?
                    else 
                    {
                        if (other != null)
                        {
                            if (!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)
                            {
                                captured.add(left); 
                            }
                            else 
                            {
                                TerritoryAttachment ta = (TerritoryAttachment) other.getAttachment("territoryAttachment");
                                if (ta != null && ta.isKingsExit())
                                    captured.add(left);
                            }
                        }
                    }

                }            
            }

            
            // Look right end for a potential capture
            // This extra set of braces is for bug prevention - it makes sure that the scope of right stays within the braces
            {
                Territory right = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY);
                if (right != null && right.getUnits().getUnitCount() > 0 && right.getOwner()!=m_player)
                {
                    Territory other = m_data.getMap().getTerritoryFromCoordinates(endX+2,endY);
                    Unit unit = (Unit) right.getUnits().getUnits().toArray()[0];
                    
                    // Can the king be captured?
                    if (unit.getType().getName().equals("king"))
                    {
                        Territory other_above = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY-1);
                        Territory other_below = m_data.getMap().getTerritoryFromCoordinates(endX+1,endY+1);
                        TerritoryAttachment ta_other = ((TerritoryAttachment)other.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_above = ((TerritoryAttachment)other_above.getAttachment("territoryAttachment"));
                        TerritoryAttachment ta_other_below = ((TerritoryAttachment)other_below.getAttachment("territoryAttachment"));
                        System.out.println(ta_other_below);
                        if (ta_other_below != null)
                            System.out.println(ta_other_below.isKingsSquare());

                        if (other != null  &&  other_above != null  &&  other_below != null  &&  
                                ((!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)  ||  (ta_other!=null && ta_other.isKingsSquare()))  && 
                                ((!other_above.getUnits().isEmpty()  &&  other_above.getOwner()==m_player)  ||  (ta_other_above!=null && ta_other_above.isKingsSquare()))  &&  
                                ((!other_below.getUnits().isEmpty()  &&  other_below.getOwner()==m_player)  ||  (ta_other_below!=null && ta_other_below.isKingsSquare())))                            
                        {    
                            captured.add(right);
                        }
                    } 
                    // Can a pawn be captured?
                    else 
                    {
                        if (other != null)
                        {
                            if (!other.getUnits().isEmpty()  &&  other.getOwner()==m_player)
                            {
                                captured.add(right); 
                            } 
                            else 
                            {
                                TerritoryAttachment ta = (TerritoryAttachment) other.getAttachment("territoryAttachment");
                                if (ta != null && ta.isKingsExit())
                                    captured.add(right);
                            }
                        }
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
            //System.out.println("startY==" + startY + "\n" + "endY=="+endY);
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
            //System.out.println("y1==" + y1 + "\n" + "y2=="+y2);
            for (int y=y1; y<=y2; y++) {
                Territory at = m_data.getMap().getTerritoryFromCoordinates(startX,y);
                if (at.getUnits().size() > 0)
                    return "Pieces can only move through empty spaces.";
                //else
                  //  System.out.println(at.getName() + " is empty");
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
            //System.out.println("x1==" + x1 + "\n" + "x2=="+x2);
            for (int x=x1; x<=x2; x++) {
                Territory at = m_data.getMap().getTerritoryFromCoordinates(x,startY);
                if (at.getUnits().size() > 0)
                    return "Intervening square (" + x + "," + startY + ") is not empty.";
            }
        }
        else
            return "Pieces can only move in a straight line.";
        
        
        
        // Only the king can be on king's squares        
        Unit unit = (Unit) start.getUnits().getUnits().toArray()[0];
        if (! unit.getType().getName().equals("king"))
        {   //System.out.println(unit.getType().getName());
            TerritoryAttachment ta = (TerritoryAttachment) end.getAttachment("territoryAttachment");
            //System.out.println(ta.getName());
            if (ta != null && ta.isKingsSquare())
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
}
