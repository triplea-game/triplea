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

package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

/**
 * Returns a bunch of messages from politicstext.properties
 * 
 * @author Edwin van der Wal
 * 
 */
public class PoliticsText
{
	// Filename
	private static final String PROPERTY_FILE = "politicstext.properties";
	
	private static PoliticsText s_pt = null;
	private static long s_timestamp = 0;
	private final Properties m_properties = new Properties();
	
	private final static String BUTTON = "BUTTON";
	private final static String DESCRIPTION = "DESCRIPTION";
	private final static String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
	private final static String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
	private final static String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
	private final static String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
	private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";
	
	protected PoliticsText()
	{
		ResourceLoader loader = ResourceLoader.getMapresourceLoader(UIContext.getMapDir());
		URL url = loader.getResource(PROPERTY_FILE);
		if (url == null)
		{
			// no propertyfile found
		}
		else
		{
			try
			{
				m_properties.load(url.openStream());
			} catch (IOException e)
			{
				System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
			}
		}
	}
	
	public static PoliticsText getInstance()
	{
		if (s_pt == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 10000)
		{ // cache properties for 10 seconds
			s_pt = new PoliticsText();
			s_timestamp = Calendar.getInstance().getTimeInMillis();
		}
		return s_pt;
	}
	
	private String getString(String value)
	{
		return m_properties.getProperty(value, "NO: " + value + " set.");
	}
	
	private String getMessage(String politicsKey, String messageKey)
	{
		return getString(politicsKey + "." + messageKey);
	}
	
	public String getButtonText(String politicsKey)
	{
		return getMessage(politicsKey, BUTTON);
	}
	
	public String getDescription(String politicsKey)
	{
		return getMessage(politicsKey, PoliticsText.DESCRIPTION);
	}
	
	public String getNotificationSucccess(String politicsKey)
	{
		return getMessage(politicsKey, PoliticsText.NOTIFICATION_SUCCESS);
	}
	
	public String getNotificationSuccessOthers(String politicsKey)
	{
		return getMessage(politicsKey, PoliticsText.OTHER_NOTIFICATION_SUCCESS);
	}
	
	public String getNotificationFailure(String politicsKey)
	{
		return getMessage(politicsKey, PoliticsText.NOTIFICATION_FAILURE);
	}
	
	public String getNotificationFailureOthers(String politicsKey)
	{
		return getMessage(politicsKey, PoliticsText.OTHER_NOTIFICATION_FAILURE);
	}
	
	public String getAcceptanceQuestion(String politicsKey)
	{
		return getMessage(politicsKey, PoliticsText.ACCEPT_QUESTION);
	}
}
