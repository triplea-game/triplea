package games.strategy.debug;

import java.io.PrintStream;

public class ClientLogger
{
	private static final PrintStream developerOutputStream = System.out;

	public static void logQuietly(String msg)
	{
		logQuietly(msg, null);
	}
	
	public static void logQuietly(String msg , Exception e)
	{
		developerOutputStream.println(msg);
		if( e != null )
		{
			for (final StackTraceElement stackTraceElement : e.getStackTrace())
			{
				developerOutputStream.println(stackTraceElement.toString());
			}
		}
		
	}

	public static void logQuietly(Exception e)
	{
		String msg = "Exception: " + e.getMessage();
		logQuietly(msg,e);
	}

}
