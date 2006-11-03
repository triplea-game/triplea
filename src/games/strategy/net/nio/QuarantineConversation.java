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

package games.strategy.net.nio;


/**
 * When a connection is first made, it is quarantined until it logs in. <p>
 * 
 * When quaratined, all messages sent by the node are sent to this quarntine conversation.<p>
 *   
 * The quarantine conversation can only write to the node across the socket from it.<p>
 * 
 * All messages sent to a conversation must be done in the Decode thread.<p>
 * 
 * 
 * 
 * @author sgb
 */
public abstract class QuarantineConversation
{

    public static enum ACTION  {NONE, TERMINATE, UNQUARANTINE}; 

    /**
     * A message has been read.  What should we do? 
     */
    public abstract ACTION message(Object o);
    
    
    /**
     * called if this conversation has been removed, either after a TERMINATE was
     * returned from a message, or the channel has been closed.
     */
    public abstract void close();
    
}
