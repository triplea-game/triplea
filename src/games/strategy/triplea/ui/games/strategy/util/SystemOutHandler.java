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
package games.strategy.util;

import java.util.logging.*;
import java.util.logging.StreamHandler;

/**
 * A simple logger that prints to System.out.
 * 
 * wtf?  Why do I need to write this.  Why cant ConsoleHandler
 * be set up to write to something other than System.err?  I
 * am so close to switching to log4j
 * 
 * @author Sean Bridges
 */
public class SystemOutHandler extends StreamHandler
{
    public SystemOutHandler()
    {
        super(System.out, new SimpleFormatter());
        setFormatter(new TALogFormatter());
    }
    
    public void publish(LogRecord record)
    {
        super.publish(record);
        flush();
    }
    

}
