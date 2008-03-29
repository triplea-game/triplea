package games.strategy.util;

import games.strategy.engine.framework.startup.launcher.ServerLauncher;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class RotatingLogFileHandler extends FileHandler
{
    static
    {
        File rootDir = new File(System.getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY, "."));
        if(!rootDir.exists()) {
            throw new IllegalStateException("no dir called:" + rootDir.getAbsolutePath());
        }
        
        File logDir = new File(rootDir, "logs");
        if(!logDir.exists())
            logDir.mkdir();
    }
    

    public RotatingLogFileHandler() throws IOException, SecurityException
    {
        super("logs/server-log%g.txt", 20 * 1000 * 1000, 10, true);
        setFormatter(new TALogFormatter());
    }

}
