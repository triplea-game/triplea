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
package games.strategy.triplea.util;

import java.util.logging.*;
import java.util.logging.Logger;

/**
 * Utility class for timing.
 * 
 * Use,
 * 
 * Stopwatch someTask = new StopWatch(someLogger, someLevel, taskDescriptiopn);
 * 
 * ...do stuff
 * 
 * someTask.done();
 *
 *
 * @author Sean Bridges
 */
public class Stopwatch
{
    private final long m_startTime = System.currentTimeMillis();
    private final String m_taskDescription;
    private final Logger m_logger;
    private final Level m_level;
   
    public Stopwatch(final Logger logger, final Level level, final String taskDescription)
    {
        m_taskDescription = taskDescription;
        m_logger = logger;
        m_level = level;
    }
    
    public void done()
    {
        if(m_logger.isLoggable(m_level))
        {
            m_logger.log(m_level, m_taskDescription + " took " + (System.currentTimeMillis() - m_startTime) + " ms");
        }
    }
    
}
