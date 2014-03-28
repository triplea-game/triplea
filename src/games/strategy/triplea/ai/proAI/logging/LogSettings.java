/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ai.proAI.logging;

import games.strategy.triplea.ai.proAI.ProAI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Class to manage log settings.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class LogSettings implements Serializable
{
	private static final long serialVersionUID = 2696071717784800413L;
	
	public boolean LimitLogHistory = true;
	public int LimitLogHistoryTo = 5;
	public boolean EnableAILogging = true;
	public Level AILoggingDepth = Level.FINEST;
	private static LogSettings s_lastSettings = null;
	private static String PROGRAM_SETTINGS = "Program Settings";
	
	public static LogSettings loadSettings()
	{
		if (s_lastSettings == null)
		{
			LogSettings result = new LogSettings();
			try
			{
				final byte[] pool = Preferences.userNodeForPackage(ProAI.class).getByteArray(PROGRAM_SETTINGS, null);
				if (pool != null)
				{
					result = (LogSettings) new ObjectInputStream(new ByteArrayInputStream(pool)).readObject();
				}
			} catch (final Exception ex)
			{
			}
			if (result == null)
			{
				result = new LogSettings();
			}
			s_lastSettings = result;
			return result;
		}
		else
			return s_lastSettings;
	}
	
	public static void saveSettings(final LogSettings settings)
	{
		s_lastSettings = settings;
		ObjectOutputStream outputStream = null;
		try
		{
			final ByteArrayOutputStream pool = new ByteArrayOutputStream(10000);
			outputStream = new ObjectOutputStream(pool);
			outputStream.writeObject(settings);
			final Preferences prefs = Preferences.userNodeForPackage(ProAI.class);
			prefs.putByteArray(PROGRAM_SETTINGS, pool.toByteArray());
			try
			{
				prefs.flush();
			} catch (final BackingStoreException ex)
			{
				ex.printStackTrace();
			}
		} catch (final Exception ex)
		{
			ex.printStackTrace();
		} finally
		{
			try
			{
				if (outputStream != null)
					outputStream.close();
			} catch (final Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
