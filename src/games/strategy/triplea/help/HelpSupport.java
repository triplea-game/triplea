package games.strategy.triplea.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A class for loading help files from the data folder (merged with src at runtime)
 * 
 * @author Klaus Groenbaek
 */
public class HelpSupport
{
	
	// -----------------------------------------------------------------------
	// class methods
	// -----------------------------------------------------------------------
	public static String loadHelp(final String fileName)
	{
		try
		{
			final InputStream is = HelpSupport.class.getResourceAsStream(fileName);
			final BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			final StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null)
			{
				sb.append(line);
			}
			return sb.toString();
		} catch (final IOException e)
		{
			return "<html><body>Unable to load help file" + fileName + "</body></html>";
		} catch (final Exception e)
		{
			return "<html><body>Unable to load help file" + fileName + " And with error message: " + e.getMessage() + "</body></html>";
		}
		
	}
	
}
