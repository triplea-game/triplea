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
package games.strategy.triplea.pbem;

import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.ForumPosterEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.net.BrowserControl;
import games.strategy.net.MultiPartFormOutputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class is serialized and stored the users local cache. Be careful when adding
 * 
 * @author Tony Clayton
 * @version 1.0
 */
public class AxisAndAlliesForumPoster implements IForumPoster
{
	// -----------------------------------------------------------------------
	// Class fields
	// -----------------------------------------------------------------------
	private static final Logger s_logger = Logger.getLogger(ILauncher.class.getName());
	private static final int m_loginDelayInSeconds = 16;
	private static final String m_host = "www.axisandallies.org";
	private static final long serialVersionUID = 7957926213311732949L;
	private static final String USE_TRANSITIVE_PASSWORD = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private String m_username = null;
	private String m_password = null;
	private transient String m_transPassword;
	private String m_gameId = null;
	private boolean m_includeSaveGame = true;
	
	// -----------------------------------------------------------------------
	// Transient fields
	// -----------------------------------------------------------------------
	private transient String m_cookies = null;
	private transient String m_referer = null;
	private transient String m_sesc = null;
	private transient String m_numReplies = null;
	private transient String m_msgNum = null;
	private transient String m_attachId = null;
	private transient String m_seqNum = null;
	private transient String m_screenshotRef = null;
	private transient String m_turnSummaryRef = null;
	private transient File m_saveGameFile = null;
	private transient String m_saveGameRef = null;
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	public AxisAndAlliesForumPoster()
	{
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public EditorPanel getEditor()
	{
		return new ForumPosterEditor(this);
	}
	
	public boolean sameType(final IBean other)
	{
		return other.getClass() == AxisAndAlliesForumPoster.class;
	}
	
	public String getDisplayName()
	{
		return "www.AxisAndAllies.org";
	}
	
	public boolean getCanViewPosted()
	{
		return true;
	}
	
	public String getScreenshotRef()
	{
		return m_screenshotRef;
	}
	
	public void viewPosted()
	{
		final String url = "http://" + m_host + "/forums/index.php?topic=" + m_gameId + ".new#new";
		BrowserControl.displayURL(url);
	}
	
	public void clearSensitiveInfo()
	{
		m_password = USE_TRANSITIVE_PASSWORD;
	}
	
	public void setForumId(final String forumId)
	{
		m_gameId = forumId;
	}
	
	public String getForumId()
	{
		return m_gameId;
	}
	
	public void setUsername(final String username)
	{
		m_username = username;
	}
	
	public String getUsername()
	{
		return m_username;
	}
	
	public void setPassword(final String password)
	{
		m_password = password;
		m_transPassword = password;
	}
	
	public String getPassword()
	{
		if (USE_TRANSITIVE_PASSWORD.equals(m_password))
		{
			return m_transPassword;
		}
		return m_password;
	}
	
	private void readCookies(final URLConnection urlConn)
	{
		// read cookies from server
		int n = 1; // n=0 has no key, and the HTTP return status in the value field
		for (boolean done = false; !done; n++)
		{
			final String headerKey = urlConn.getHeaderFieldKey(n);
			final String headerVal = urlConn.getHeaderField(n);
			if (headerKey != null || headerVal != null)
			{
				if (!headerKey.equals("Set-Cookie"))
					continue;
				final String cookie = headerVal.substring(0, headerVal.indexOf(";"));
				if (m_cookies == null)
				{
					m_cookies = cookie;
					continue;
				}
				if (m_cookies.indexOf(cookie) == -1)
					m_cookies = m_cookies + "; " + cookie;
			}
			else
			{
				done = true;
			}
		}
	}
	
	private boolean postLogin()
	{
		URL url = null;
		URLConnection urlConn = null;
		OutputStream out = null;
		int code = 0;
		boolean gotPhpsessid = false;
		// set up connection
		try
		{
			url = new URL("http://" + m_host + "/forums/index.php?action=login2");
		} catch (final MalformedURLException e)
		{
			e.printStackTrace();
			return false;
		}
		try
		{
			urlConn = url.openConnection();
			((HttpURLConnection) urlConn).setRequestMethod("POST");
			((HttpURLConnection) urlConn).setInstanceFollowRedirects(false);
			urlConn.setDoOutput(true);
			urlConn.setDoInput(true);
			urlConn.setRequestProperty("Host", m_host);
			urlConn.setRequestProperty("Accept", "*/*");
			urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			urlConn.setRequestProperty("Connection", "Keep-Alive");
			urlConn.setRequestProperty("Cache-Control", "no-cache");
			// send request to server
			out = urlConn.getOutputStream();
			out.write(("user=" + m_username + "&passwrd=" + m_password + "&cookielength=60").getBytes());
			out.flush();
			out.close();
			do
			{
				readCookies(urlConn);
				m_referer = url.toString();
				code = ((HttpURLConnection) urlConn).getResponseCode();
				if (code == 301 || code == 302)
				{
					// redirect
					url = new URL(urlConn.getHeaderField("Location"));
					urlConn = url.openConnection();
					((HttpURLConnection) urlConn).setRequestMethod("GET");
					((HttpURLConnection) urlConn).setInstanceFollowRedirects(false);
					urlConn.setDoOutput(true);
					urlConn.setDoInput(true);
					urlConn.setRequestProperty("Host", m_host);
					urlConn.setRequestProperty("Accept", "*/*");
					urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					urlConn.setRequestProperty("Connection", "Keep-Alive");
					urlConn.setRequestProperty("Cache-Control", "no-cache");
					if (m_referer != null)
						urlConn.setRequestProperty("Referer", m_referer);
					if (m_cookies != null)
						urlConn.setRequestProperty("Cookie", m_cookies);
					continue;
				}
				if (code == 200)
				{
					// fetch PHPSESSID
					String header = urlConn.getHeaderField("Refresh");
					if (header == null)
						header = urlConn.getHeaderField("Set-Cookie");
					final Pattern p_phpsessid = Pattern.compile(".*?\\?PHPSESSID=(\\w+).*"); // TODO: fix this regex
					final Matcher match_phpsessid = p_phpsessid.matcher(header);
					if (match_phpsessid.matches())
					{
						// m_phpsessid = match_phpsessid.group(1);
						gotPhpsessid = true;
					}
					urlConn = null;
					continue;
				}
				// http error
				final String msg = ((HttpURLConnection) urlConn).getResponseMessage();
				m_turnSummaryRef = String.valueOf(code) + ": " + msg;
				return false;
			} while (urlConn != null);
		} catch (final Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if (!gotPhpsessid)
		{
			m_turnSummaryRef = "Error: PHPSESSID not found after login. ";
			return false;
		}
		if (m_cookies == null)
		{
			m_turnSummaryRef = "Error: cookies not found after login. ";
			return false;
		}
		try
		{
			Thread.sleep(m_loginDelayInSeconds * 1000);
		} catch (final InterruptedException ie)
		{
			ie.printStackTrace();
		}
		return true;
	}
	
	private boolean goToForum()
	{
		URL url = null;
		URLConnection urlConn = null;
		int code = 0;
		boolean gotNumReplies = false;
		boolean gotMsgNum = false;
		// set up connection
		try
		{
			url = new URL("http://" + m_host + "/forums/index.php?topic=" + m_gameId + ".new");
		} catch (final MalformedURLException e)
		{
			e.printStackTrace();
			return false;
		}
		try
		{
			urlConn = url.openConnection();
			((HttpURLConnection) urlConn).setRequestMethod("GET");
			((HttpURLConnection) urlConn).setInstanceFollowRedirects(false);
			urlConn.setDoOutput(false);
			urlConn.setDoInput(true);
			urlConn.setRequestProperty("Host", m_host);
			urlConn.setRequestProperty("Accept", "*/*");
			urlConn.setRequestProperty("Connection", "Keep-Alive");
			urlConn.setRequestProperty("Cache-Control", "no-cache");
			if (m_referer != null)
				urlConn.setRequestProperty("Referer", m_referer);
			if (m_cookies != null)
				urlConn.setRequestProperty("Cookie", m_cookies);
			m_referer = url.toString();
			readCookies(urlConn);
			code = ((HttpURLConnection) urlConn).getResponseCode();
			if (code != 200)
			{
				// http error
				final String msg = ((HttpURLConnection) urlConn).getResponseMessage();
				m_turnSummaryRef = String.valueOf(code) + ": " + msg;
				return false;
			}
			// parse num_replies
			final BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			String line = "";
			final Pattern p_numReplies = Pattern.compile(".*?;num_replies=(\\d+)\".*");
			// parse msg num
			final Pattern p_msgNum = Pattern.compile(".*?<a name=\"msg(\\d+)\"></a><a name=\"new\"></a>.*");
			// parse attachment id
			final Pattern p_attachId = Pattern.compile(".*?action=dlattach;topic=" + m_gameId + ".0;attach=(\\d+)\">.*");
			while ((line = in.readLine()) != null)
			{
				if (!gotNumReplies)
				{
					final Matcher match_numReplies = p_numReplies.matcher(line);
					if (match_numReplies.matches())
					{
						m_numReplies = match_numReplies.group(1);
						gotNumReplies = true;
						continue;
					}
				}
				if (!gotMsgNum)
				{
					final Matcher match_msgNum = p_msgNum.matcher(line);
					if (match_msgNum.matches())
					{
						m_msgNum = match_msgNum.group(1);
						gotMsgNum = true;
						continue;
					}
				}
				// must match every line... always take the last attachId
				final Matcher match_attachId = p_attachId.matcher(line);
				if (match_attachId.matches())
					m_attachId = match_attachId.group(1);
			}
			in.close();
		} catch (final Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if (!gotNumReplies || !gotMsgNum)
		{
			m_turnSummaryRef = "Error: ";
			if (!gotNumReplies)
				m_turnSummaryRef += "No num_replies found in A&A.org forum topic. ";
			if (!gotMsgNum)
				m_turnSummaryRef += "No msgXXXXXX found in A&A.org forum topic. ";
			return false;
		}
		return true;
	}
	
	private boolean preparePost()
	{
		URL url = null;
		URLConnection urlConn = null;
		int code = 0;
		boolean gotNumReplies = false;
		boolean gotSeqNum = false;
		boolean gotSesc = false;
		try
		{
			url = new URL("http://" + m_host + "/forums/index.php?action=post;topic=" + m_gameId + ".0;num_replies=" + m_numReplies);
		} catch (final MalformedURLException e)
		{
			e.printStackTrace();
		}
		try
		{
			urlConn = url.openConnection();
			((HttpURLConnection) urlConn).setRequestMethod("GET");
			((HttpURLConnection) urlConn).setInstanceFollowRedirects(false);
			urlConn.setDoOutput(false);
			urlConn.setDoInput(true);
			urlConn.setRequestProperty("Host", m_host);
			urlConn.setRequestProperty("Accept", "*/*");
			urlConn.setRequestProperty("Connection", "Keep-Alive");
			urlConn.setRequestProperty("Cache-Control", "no-cache");
			if (m_referer != null)
				urlConn.setRequestProperty("Referer", m_referer);
			if (m_cookies != null)
				urlConn.setRequestProperty("Cookie", m_cookies);
			m_referer = url.toString();
			readCookies(urlConn);
			code = ((HttpURLConnection) urlConn).getResponseCode();
			if (code != 200)
			{
				// http error
				final String msg = ((HttpURLConnection) urlConn).getResponseMessage();
				m_turnSummaryRef = String.valueOf(code) + ": " + msg;
				return false;
			}
			// parse num_replies again
			final BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			String line = "";
			final Pattern p_numReplies = Pattern.compile(".*?<input type=\"hidden\" name=\"num_replies\" value=\"(\\d+)\" />.*");
			// parse seqnum
			final Pattern p_seqNum = Pattern.compile(".*?<input type=\"hidden\" name=\"seqnum\" value=\"(\\d+)\" />.*");
			// parse sesc/sc
			final Pattern p_sesc = Pattern.compile(".*?<input type=\"hidden\" name=\"sc\" value=\"(\\w+)\" />.*");
			while ((line = in.readLine()) != null)
			{
				if (!gotNumReplies)
				{
					final Matcher match_numReplies = p_numReplies.matcher(line);
					if (match_numReplies.matches())
					{
						m_numReplies = match_numReplies.group(1);
						gotNumReplies = true;
						continue;
					}
				}
				if (!gotSeqNum)
				{
					final Matcher match_seqNum = p_seqNum.matcher(line);
					if (match_seqNum.matches())
					{
						m_seqNum = match_seqNum.group(1);
						gotSeqNum = true;
						continue;
					}
				}
				if (!gotSesc)
				{
					final Matcher match_sesc = p_sesc.matcher(line);
					if (match_sesc.matches())
					{
						m_sesc = match_sesc.group(1);
						gotSesc = true;
						continue;
					}
				}
			}
			in.close();
		} catch (final Exception e)
		{
			e.printStackTrace();
			return false;
		}
		if (!gotNumReplies || !gotSeqNum || !gotSesc)
		{
			m_turnSummaryRef = "Error: ";
			if (!gotNumReplies)
				m_turnSummaryRef += "No num_replies found in A&A.org post form. ";
			if (!gotSeqNum)
				m_turnSummaryRef += "No seqnum found in A&A.org post form. ";
			if (!gotSesc)
				m_turnSummaryRef += "No sc found in A&A.org post form. ";
			return false;
		}
		return true;
	}
	
	private String getNextNumReplies()
	{
		final int postNum = Integer.parseInt(m_numReplies) + 1;
		return String.valueOf(postNum);
	}
	
	private String getNextReplyUrl()
	{
		return "http://" + m_host + "/forums/index.php?topic=" + m_gameId + "." + getNextNumReplies() + "#msg" + m_msgNum;
	}
	
	private String getNewReplyUrl()
	{
		return "http://" + m_host + "/forums/index.php?topic=" + m_gameId + "." + m_numReplies + "#msg" + m_msgNum;
	}
	
	private String getNewAttachUrl()
	{
		return "http://" + m_host + "/forums/index.php?action=dlattach;topic=" + m_gameId + ".0;attach=" + m_attachId;
	}
	
	public boolean postTurnSummary(final String summary)
	{
		URL url = null;
		URLConnection urlConn = null;
		MultiPartFormOutputStream out = null;
		String forumSummary = "";
		String screenshotSummary = "";
		final String saveGameSummary = "";
		String finalSummary = "";
		m_turnSummaryRef = null;
		
		// first, login if necessary
		if (m_cookies == null && !postLogin())
			return false;
		// now, go to forum
		if (!goToForum())
			return false;
		// now, prepare post
		if (!preparePost())
			return false;
		// set up connection
		try
		{
			url = new URL("http://" + m_host + "/forums/index.php?action=post2;start=0;board=40");
		} catch (final MalformedURLException e)
		{
			e.printStackTrace();
			return false;
		}
		forumSummary = "\n\nA&A Forum : " + getNextReplyUrl();
		if (m_screenshotRef != null)
			screenshotSummary = "\nScreenshot: " + m_screenshotRef;
		
		finalSummary = summary + forumSummary + screenshotSummary + saveGameSummary;
		int code;
		BufferedReader in = null;
		String line;
		final Pattern p1;
		final Pattern p2;
		final Pattern p3;
		// send request to server
		try
		{
			final String boundary = MultiPartFormOutputStream.createBoundary();
			urlConn = MultiPartFormOutputStream.createConnection(url);
			((HttpURLConnection) urlConn).setInstanceFollowRedirects(false);
			urlConn.setRequestProperty("Host", m_host);
			urlConn.setRequestProperty("Accept", "*/*");
			urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
			urlConn.setRequestProperty("Connection", "keep-alive");
			urlConn.setRequestProperty("Cache-Control", "no-cache");
			if (m_referer != null)
				urlConn.setRequestProperty("Referer", m_referer);
			if (m_cookies != null)
				urlConn.setRequestProperty("Cookie", m_cookies);
			m_referer = url.toString();
			out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
			// send request
			out.writeField("topic", m_gameId);
			out.writeField("subject", "TripleA Turn Summary Post " + m_gameId + "." + getNextNumReplies());
			out.writeField("icon", "xx");
			out.writeField("message", finalSummary);
			out.writeField("notify", "0");
			out.writeField("goback", "1");
			out.writeField("ns", "NS");
			// empty attachment
			if (m_saveGameFile != null)
			{
				FileInputStream fin = null;
				
				try
				{
					fin = new FileInputStream(m_saveGameFile);
					out.writeFile("attachment[]", "application/octet-stream", m_saveGameFile.getName(), fin);
					out.writeField("attachmentPreview", "");
				} finally
				{
					if (fin != null)
					{
						try
						{
							fin.close();
						} catch (final IOException e)
						{
							// ignore
						}
					}
				}
			}
			out.writeField("post", "Post");
			out.writeField("num_replies", m_numReplies);
			out.writeField("sc", m_sesc);
			out.writeField("seqnum", m_seqNum);
			out.close();
			code = ((HttpURLConnection) urlConn).getResponseCode();
			// 200 OK for error, redirects 301/302 if successful
			if (code == 200)
			{
				
				in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				final StringBuilder sb = new StringBuilder();
				String responseBody; // read the whole response for easier debugging
				try
				{
					while ((line = in.readLine()) != null)
					{
						sb.append(line);
					}
					responseBody = sb.toString();
					
				} finally
				{
					in.close();
				}
				
				// The forum may also report <div...id="error_list">Sorry, you are not allowed to post links.</div>
				final Pattern p4 = Pattern.compile(".*?id=\"error_list[^>]*>\\s+([^<]*)\\s+<.*");
				final Matcher matcher = p4.matcher(responseBody);
				if (matcher.matches())
				{
					m_turnSummaryRef = "The site gave an error: '" + matcher.group(1) + "'";
					return false;
				}
				
				s_logger.warning("Unknown error html: " + responseBody);
				m_turnSummaryRef = "Unknown error, if this problem persists, post an error to the TripleA dev forum";
				return false;
				
			}
			if (code != 301 && code != 302)
			{
				// http error
				final String msg = ((HttpURLConnection) urlConn).getResponseMessage();
				m_turnSummaryRef = String.valueOf(code) + ": " + msg;
				return false;
			}
			// success!
			// now, go back to forum
			if (!goToForum())
				return false;
			
		} catch (final Exception e)
		{
			
			s_logger.severe(e.getMessage());
			m_turnSummaryRef = "Unknown error, message written to debug log";
			return false;
		} finally
		{
			if (in != null)
			{
				try
				{
					in.close();
				} catch (final IOException e)
				{
					// ignore
				}
			}
		}
		m_turnSummaryRef = getNewReplyUrl();
		if (m_saveGameFile != null)
		{
			if (m_attachId == null)
			{
				m_saveGameRef = "Error: attachment id not found in A&A.org post.";
				return false;
			}
			m_saveGameRef = getNewAttachUrl();
		}
		return true;
	}
	
	public String getTurnSummaryRef()
	{
		return m_turnSummaryRef;
	}
	
	public boolean getIncludeSaveGame()
	{
		return m_includeSaveGame;
	}
	
	public void setIncludeSaveGame(final boolean include)
	{
		m_includeSaveGame = include;
	}
	
	public void addSaveGame(final File saveGameFile, final String fileName)
	{
		m_saveGameFile = saveGameFile;
	}
	
	public IForumPoster doClone()
	{
		final AxisAndAlliesForumPoster clone = new AxisAndAlliesForumPoster();
		clone.setForumId(getForumId());
		clone.setIncludeSaveGame(getIncludeSaveGame());
		clone.setPassword(getPassword());
		clone.setUsername(getUsername());
		return clone;
	}
	
	public boolean supportsSaveGame()
	{
		return true;
	}
	
}
