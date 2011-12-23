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
package games.strategy.engine.random;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.editors.DiceServerEditor;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * A pbem dice roller that reads its configuration from a properties file
 * 
 * 
 * @author sgb
 */
public class PropertiesDiceRoller implements IRemoteDiceServer
{
	//-----------------------------------------------------------------------
	// class fields
	//-----------------------------------------------------------------------
	private static final long serialVersionUID = 6481409417543119539L;


	//-----------------------------------------------------------------------
	// class methods
	//-----------------------------------------------------------------------

	/**
	 * Loads the property dice rollers from the properties file
	 * @return the collection of available dice rollers
	 */
	public static Collection<PropertiesDiceRoller> loadFromFile() {
	    List<PropertiesDiceRoller> rollers = new ArrayList<PropertiesDiceRoller>();
		final File f = new File(GameRunner.getRootFolder(), "dice_servers");
		if (!f.exists())
		{
			throw new IllegalStateException("No dice server folder:" + f);
		}
		final java.util.List<Properties> propFiles = new ArrayList<Properties>();
		final File[] files = f.listFiles();
		for (final File file : files)
		{
			if (!file.isDirectory() && file.getName().endsWith(".properties"))
			{
				try
				{
					final Properties props = new Properties();
					final FileInputStream fin = new FileInputStream(file);
					try
					{
						props.load(fin);
						propFiles.add(props);
					} finally
					{
						fin.close();
					}
				} catch (final IOException e)
				{
					System.out.println("error reading file:" + file);
					e.printStackTrace();
				}
			}
		}
		Collections.sort(propFiles, new Comparator<Properties>()
		{
			public int compare(final Properties o1, final Properties o2)
			{
				final int n1 = Integer.parseInt(o1.getProperty("order"));
				final int n2 = Integer.parseInt(o2.getProperty("order"));
				return n1 - n2;
			}
		});

		for (final Properties prop : propFiles)
		{
			rollers.add(new PropertiesDiceRoller(prop));
		}

		return rollers;
	}

	//-----------------------------------------------------------------------
	// instance fields
	//-----------------------------------------------------------------------
	private final Properties m_props;
	private String m_toAddress;
	private String m_ccAddress;
	private String m_gameId;

	//-----------------------------------------------------------------------
	// constructors
	//-----------------------------------------------------------------------
	public PropertiesDiceRoller(final Properties props)
	{
		m_props = props;
	}

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------


	public String getDisplayName()
	{
		return m_props.getProperty("name");
	}

	public EditorPanel getEditor()
	{
		return new DiceServerEditor(this);
	}

	public boolean sameType(IBean other)
	{
		return other instanceof PropertiesDiceRoller && getDisplayName().equals(other.getDisplayName());
	}


	public boolean sendsEmail()
	{
		final String property = m_props.getProperty("send.email");
		if (property == null)
		{
			return true;
		}
		return Boolean.valueOf(property);
	}
	
	public String postRequest(final int max, final int numDice, final String subjectMessage, String gameID, final String gameUUID) throws IOException
	{
		if (gameID.trim().length() == 0)
			gameID = "TripleA";
		String message = gameID + ":" + subjectMessage;
		final int maxLength = Integer.valueOf(m_props.getProperty("message.maxlength"));
		if (message.length() > maxLength)
			message = message.substring(0, maxLength - 1);
		final PostMethod post = new PostMethod(m_props.getProperty("path"));
		final NameValuePair[] data = { new NameValuePair("numdice", "" + numDice), new NameValuePair("numsides", "" + max), new NameValuePair("modroll", "No"), new NameValuePair("numroll", "" + 1),
					new NameValuePair("subject", message), new NameValuePair("roller", getToAddress()), new NameValuePair("gm", getCcAddress()), new NameValuePair("send", "true"), };
		post.setRequestHeader("User-Agent", "triplea/" + EngineVersion.VERSION);
		// this is to allow a dice server to allow the user to request the emails for the game
		// rather than sending out email for each roll
		post.setRequestHeader("X-Triplea-Game-UUID", gameUUID);
		post.setRequestBody(data);
		final HttpClient client = new HttpClient();
		try
		{
			final String host = m_props.getProperty("host");
			int port = 80;
			if (m_props.getProperty("port") != null)
			{
				port = Integer.parseInt(m_props.getProperty("port"));
			}
			client.getHostConfiguration().setHost(host, port);
			client.executeMethod(post);
			final String result = post.getResponseBodyAsString();
			return result;
		} finally
		{
			post.releaseConnection();
		}
	}
	
	public String getInfoText()
	{
		return m_props.getProperty("infotext");
	}
	
	/**
	 * 
	 * @throws IOException
	 *             if there was an error parsing the string
	 */
	public int[] getDice(final String string, final int count) throws IOException, InvocationTargetException
	{
		final String errorStartString = m_props.getProperty("error.start");
		final String errorEndString = m_props.getProperty("error.end");
		// if the error strings are defined
		if (errorStartString != null && errorStartString.length() > 0 && errorEndString != null && errorEndString.length() > 0)
		{
			final int startIndex = string.indexOf(errorStartString);
			if (startIndex >= 0)
			{
				final int endIndex = string.indexOf(errorEndString, (startIndex + errorStartString.length()));
				if (endIndex > 0)
				{
					final String error = string.substring(startIndex + errorStartString.length(), endIndex);
					throw new InvocationTargetException(null, error);
				}
			}
		}
		String rollStartString;
		String rollEndString;
		if (count == 1)
		{
			rollStartString = m_props.getProperty("roll.single.start");
			rollEndString = m_props.getProperty("roll.single.end");
		}
		else
		{
			rollStartString = m_props.getProperty("roll.multiple.start");
			rollEndString = m_props.getProperty("roll.multiple.end");
		}
		int startIndex = string.indexOf(rollStartString);
		if (startIndex == -1)
		{
			throw new IOException("Cound not find start index, text returned is:" + string);
		}
		startIndex += rollStartString.length();
		final int endIndex = string.indexOf(rollEndString, startIndex);
		if (endIndex == -1)
		{
			throw new IOException("Cound not find end index");
		}
		final StringTokenizer tokenizer = new StringTokenizer(string.substring(startIndex, endIndex), " ,", false);
		final int[] rVal = new int[count];
		for (int i = 0; i < count; i++)
		{
			try
			{
				// -1 since we are 0 based
				rVal[i] = Integer.parseInt(tokenizer.nextToken()) - 1;
			} catch (final NumberFormatException ex)
			{
				ex.printStackTrace();
				throw new IOException(ex.getMessage());
			}
		}
		return rVal;
	}

	public String getToAddress()
	{
		return m_toAddress;
	}

	public void setToAddress(String toAddress)
	{
		m_toAddress = toAddress;
	}

	public String getCcAddress()
	{
		return m_ccAddress;
	}

	public void setCcAddress(String ccAddress)
	{
		m_ccAddress = ccAddress;
	}

	public boolean supportsGameId()
	{
		String gameid = m_props.getProperty("gameid");
		return "true".equals(gameid);
	}

	public void setGameId(String gameId)
	{
		 m_gameId = gameId;
	}

	public String getGameId()
	{
		return m_gameId;
	}
}
