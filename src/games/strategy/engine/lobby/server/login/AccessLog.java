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

package games.strategy.engine.lobby.server.login;

import java.net.InetAddress;
import java.util.Date;
import java.util.logging.Logger;

/**
 * 
 * This class is used to allow an access log to be easily created.  The log settings should
 * be set up to log messages from this class to a seperate access log file.
 * 
 * @author sgg
 */
public class AccessLog
{
    
    private static final Logger s_logger = Logger.getLogger(AccessLog.class.getName());

    public static void successfulLogin(String userName, InetAddress from) 
    {
        s_logger.info("LOGIN name:" + userName + " ip:" + from.getHostAddress() + " time_ms:" + System.currentTimeMillis() + " time:" + new Date());
    }
    
    public static void failedLogin(String userName, InetAddress from, String error) 
    {
        s_logger.info("FAILED name:" + userName + " ip:" + from.getHostAddress() + " time_ms:" + System.currentTimeMillis() + " error:" + error + " time:" + new Date());
    }
    
}
