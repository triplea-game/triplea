package games.strategy.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class RotatingLogFileHandler extends FileHandler
{
    static
    {
        File logDir = new File("logs");
        if(!logDir.exists())
            logDir.mkdir();
    }
    

    public RotatingLogFileHandler() throws IOException, SecurityException
    {
        super("logs/server-log%g.txt", 20 * 1000 * 1000, 10, true);
        setFormatter(new TALogFormatter());
    }

}
