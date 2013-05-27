package games.strategy.util;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class RotatingLogFileHandlerForHeadlessGameServer extends FileHandler
{
	private static final String LOG_FILE_SIZE_PROP = "triplea.log.file.size";
	private static final String DEFAULT_SIZE = 2 * 1000 * 1000 + "";
	private static final String logFile;
	static
	{
		final File rootDir = new File(System.getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY, "."));
		if (!rootDir.exists())
		{
			throw new IllegalStateException("no dir called:" + rootDir.getAbsolutePath());
		}
		final File logDir = new File(rootDir, "logs");
		if (!logDir.exists())
			logDir.mkdir();
		final String serverInstanceName = System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY, System.getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY, ""));
		logFile = new File(logDir, "headless-game-server-" + serverInstanceName + "-log%g.txt").getAbsolutePath();
		System.out.print("logging to :" + logFile);
	}
	
	public RotatingLogFileHandlerForHeadlessGameServer() throws IOException, SecurityException
	{
		super(logFile, Integer.parseInt(System.getProperty(LOG_FILE_SIZE_PROP, DEFAULT_SIZE)), 10, true);
		final TALogFormatter logFormatter = new TALogFormatter();
		logFormatter.setShowDates(false);
		setFormatter(logFormatter);
	}
}
