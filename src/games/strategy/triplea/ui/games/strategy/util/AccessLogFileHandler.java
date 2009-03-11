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

import games.strategy.engine.framework.startup.launcher.ServerLauncher;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class AccessLogFileHandler extends FileHandler
{
    private static final String logFile;
    static
    {
        File rootDir = new File(System.getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY, "."));
        if(!rootDir.exists()) {
            throw new IllegalStateException("no dir called:" + rootDir.getAbsolutePath());
        }
        
        File logDir = new File(rootDir, "access_logs");
        if(!logDir.exists())
            logDir.mkdir();
        
        logFile = new File(logDir,"access-log%g.txt").getAbsolutePath();
        System.out.print("logging to :" + logFile);
        
    }
    

    public AccessLogFileHandler() throws IOException, SecurityException
    {
        super(logFile, 20 * 1000 * 1000, 10, true);
        setFormatter(new TALogFormatter());
    }


}

class AccessLogFormat extends Formatter 
{

    @Override
    public String format(LogRecord record)
    {
        return record.getMessage() + "\n";
    }
    
}
