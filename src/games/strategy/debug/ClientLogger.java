package games.strategy.debug;

import java.io.PrintStream;

public class ClientLogger
{
	private static final PrintStream developerOutputStream = System.out;
	
	public static void logQuietly(final Exception e)
	{
		developerOutputStream.println("Exception: " + e.getMessage());
		for (final StackTraceElement stackTraceElement : e.getStackTrace())
		{
			developerOutputStream.println(stackTraceElement.toString());
		}
	}
	
}
