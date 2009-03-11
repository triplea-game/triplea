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

package games.strategy.engine.chat;

import java.util.HashMap;
import java.util.Map;

/** 
 * Simple flood control, only allow so many events per window of time
 */
public class ChatFloodControl
{
    private static final int ONE_MINUTE = 60 * 1000;
    
    static final int EVENTS_PER_WINDOW = 20;
    static final int WINDOW = ONE_MINUTE;
    
    private final Object lock = new Object();
    private long clearTime = System.currentTimeMillis();
    private Map<String,Integer> messageCount = new HashMap<String, Integer>();
    
    public boolean allow(String from, long now)
    {
        synchronized(lock) 
        {
            //reset the window
            if(now > clearTime) 
            {
                messageCount.clear();
                clearTime = now + WINDOW;
            }
            
            if(!messageCount.containsKey(from)) 
            {
                messageCount.put(from, 0);
            }
            
            messageCount.put(from, messageCount.get(from) + 1);
            return messageCount.get(from) <= EVENTS_PER_WINDOW;
        }
    }
    

}
