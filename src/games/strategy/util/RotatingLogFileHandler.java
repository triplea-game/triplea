package games.strategy.util;

import games.strategy.engine.framework.startup.launcher.ServerLauncher;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class RotatingLogFileHandler extends FileHandler
{
    
    private static final String logFile;
    static
    {
        File rootDir = new File(System.getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY, "."));
        if(!rootDir.exists()) {
            throw new IllegalStateException("no dir called:" + rootDir.getAbsolutePath());
        }
        
        File logDir = new File(rootDir, "logs");
        if(!logDir.exists())
            logDir.mkdir();
        
        logFile = new File(logDir,"server-log%g.txt").getAbsolutePath();
        System.out.print("logging to :" + logFile);
        
    }
    

    public RotatingLogFileHandler() throws IOException, SecurityException
    {
        super(logFile, 20 * 1000 * 1000, 10, true);
        setFormatter(new TALogFormatter());
    }

}
