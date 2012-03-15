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
/*
 * NotificationMessages.java
 * 
 * Created on August 3, 2011
 */
package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

public class NotificationMessages
{
	// Filename
	private static final String PROPERTY_FILE = "notifications.properties";
	private static NotificationMessages s_nm = null;
	private static long s_timestamp = 0;
	private final Properties m_properties = new Properties();
	
	protected NotificationMessages()
	{
		final ResourceLoader loader = UIContext.getResourceLoader();
		final URL url = loader.getResource(PROPERTY_FILE);
		if (url == null)
		{
			// no propertyfile found
		}
		else
		{
			try
			{
				m_properties.load(url.openStream());
			} catch (final IOException e)
			{
				System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
			}
		}
	}
	
	public static NotificationMessages getInstance()
	{
		if (s_nm == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 10000)
		{ // cache properties for 10 seconds
			s_nm = new NotificationMessages();
			s_timestamp = Calendar.getInstance().getTimeInMillis();
		}
		return s_nm;
	}
	
	public String getMessage(final String notificationMessageKey)
	{
		return m_properties.getProperty(notificationMessageKey, notificationMessageKey);
	}
}
