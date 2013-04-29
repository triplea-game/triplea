package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Properties;

/**
 * Same as PoliticsText but for user actions.
 * 
 * @author veqryn
 * 
 */
public class UserActionText
{
	// Filename
	private static final String PROPERTY_FILE = "actionstext.properties";
	private static UserActionText s_text = null;
	private static long s_timestamp = 0;
	private final Properties m_properties = new Properties();
	private final static String BUTTON = "BUTTON";
	private final static String DESCRIPTION = "DESCRIPTION";
	private final static String NOTIFICATION_SUCCESS = "NOTIFICATION_SUCCESS";
	private final static String OTHER_NOTIFICATION_SUCCESS = "OTHER_NOTIFICATION_SUCCESS";
	private final static String NOTIFICATION_FAILURE = "NOTIFICATION_FAILURE";
	private final static String OTHER_NOTIFICATION_FAILURE = "OTHER_NOTIFICATION_FAILURE";
	private static final String ACCEPT_QUESTION = "ACCEPT_QUESTION";
	
	protected UserActionText()
	{
		final ResourceLoader loader = UIContext.getResourceLoader();
		final URL url = loader.getResource(PROPERTY_FILE);
		if (url == null)
		{
			// no property file found
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
	
	public static UserActionText getInstance()
	{
		if (s_text == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 10000)
		{ // cache properties for 10 seconds
			s_text = new UserActionText();
			s_timestamp = Calendar.getInstance().getTimeInMillis();
		}
		return s_text;
	}
	
	private String getString(final String value)
	{
		return m_properties.getProperty(value, "NO: " + value + " set.");
	}
	
	private String getMessage(final String actionKey, final String messageKey)
	{
		return getString(actionKey + "." + messageKey);
	}
	
	public String getButtonText(final String actionKey)
	{
		return getMessage(actionKey, BUTTON);
	}
	
	public String getDescription(final String actionKey)
	{
		return getMessage(actionKey, DESCRIPTION);
	}
	
	public String getNotificationSucccess(final String actionKey)
	{
		return getMessage(actionKey, NOTIFICATION_SUCCESS);
	}
	
	public String getNotificationSuccessOthers(final String actionKey)
	{
		return getMessage(actionKey, OTHER_NOTIFICATION_SUCCESS);
	}
	
	public String getNotificationFailure(final String actionKey)
	{
		return getMessage(actionKey, NOTIFICATION_FAILURE);
	}
	
	public String getNotificationFailureOthers(final String actionKey)
	{
		return getMessage(actionKey, OTHER_NOTIFICATION_FAILURE);
	}
	
	public String getAcceptanceQuestion(final String actionKey)
	{
		return getMessage(actionKey, ACCEPT_QUESTION);
	}
}
