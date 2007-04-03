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

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class AccessLogFileHandler extends FileHandler
{
    static
    {
        File logDir = new File("logs");
        if(!logDir.exists())
            logDir.mkdir();
    }
    

    public AccessLogFileHandler() throws IOException, SecurityException
    {
        super("logs/access-log%g.txt", 20 * 1000 * 1000, 10, true);
        setFormatter(new AccessLogFormat());
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
