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
import java.util.logging.Formatter;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
public class TALogFormatter extends Formatter
{
    
    /**
     * 
     */
    public TALogFormatter()
    {

     
    }

    /* 
     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
     */
    public String format(LogRecord record)
    {
       return record.getLevel() + ":" + record.getLoggerName() + ":->" + record.getMessage() + "\n";
    }

}
