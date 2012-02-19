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

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.ForumPosterEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.pbem.AbstractForumPoster;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.net.BrowserControl;
import games.strategy.triplea.help.HelpSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;

/**
 * Post turn summary to www.axisandallies.org to the thread identified by the forumId
 * URL format: http://www.axisandallies.org/forums/index.php?topic=[forumId],
 * like http://www.axisandallies.org/forums/index.php?topic=25878
 * 
 * The poster logs in, and out every time it posts, this way we don't nee to manage any state between posts
 * 
 * @author Klaus Groenbaek
 */
public class AxisAndAlliesForumPoster extends AbstractForumPoster
{
	// -----------------------------------------------------------------------
	// constants
	// -----------------------------------------------------------------------
	private static final long serialVersionUID = 8896923978584346664L;
	
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	// the patterns used to extract values from hidden form fields posted to the server
	public static final Pattern NUM_REPLIES_PATTERN = Pattern.compile(".*name=\"num_replies\" value=\"(\\d+)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	public static final Pattern SEQ_NUM_PATTERN = Pattern.compile(".*name=\"seqnum\"\\svalue=\"(\\d+)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	public static final Pattern SC_PATTERN = Pattern.compile(".*name=\"sc\"\\svalue=\"(\\w+)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	
	// 3 patterns used for error handling
	public static final Pattern AN_ERROR_OCCURRED_PATTERN = Pattern.compile(".*An Error Has Occurred.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	public static final Pattern ERROR_TEXT_PATTERN = Pattern.compile(".*<tr\\s+class=\"windowbg\">\\s*<td[^>]*>([^<]*)</td>.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	public static final Pattern ERROR_LIST_PATTERN = Pattern.compile(".*id=\"error_list[^>]*>\\s+([^<]*)\\s+<.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private transient HttpState m_httpState;
	private transient HostConfiguration m_hostConfiguration;
	private transient HttpClient m_client;
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	/**
	 * Logs into axisandallies.org
	 * nb: Username and password are posted in clear text
	 * 
	 * @throws Exception
	 *             if login fails
	 */
	private void login() throws Exception
	{
		// creates and configures a new http client
		m_client = new HttpClient();
		m_client.getParams().setParameter("http.protocol.single-cookie-header", true);
		m_client.getParams().setParameter("http.useragent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)");
		m_httpState = new HttpState();
		m_hostConfiguration = new HostConfiguration();
		// m_hostConfiguration.setProxy("localhost", 8888); // enable to debug http through Fiddler2
		m_hostConfiguration.setHost("www.axisandallies.org");
		
		final PostMethod post = new PostMethod("http://www.axisandallies.org/forums/index.php?action=login2");
		try
		{
			post.addRequestHeader("Accept", "*/*");
			post.addRequestHeader("Accept-Language", "en-us");
			post.addRequestHeader("Cache-Control", "no-cache");
			post.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
			
			final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			parameters.add(new NameValuePair("user", getUsername()));
			parameters.add(new NameValuePair("passwrd", getPassword()));
			post.setRequestBody(parameters.toArray(new NameValuePair[parameters.size()]));
			
			int status = m_client.executeMethod(m_hostConfiguration, post, m_httpState);
			if (status == 200)
			{
				final String body = post.getResponseBodyAsString();
				if (body.toLowerCase().contains("password incorrect"))
				{
					throw new Exception("Incorrect Password");
				}
				// site responds with 200, and a refresh header
				final Header refreshHeader = post.getResponseHeader("Refresh");
				if (refreshHeader == null)
				{
					throw new Exception("Missing refresh header after login");
				}
				
				final String value = refreshHeader.getValue(); // refresh: 0; URL=http://...
				final Pattern p = Pattern.compile("[^;]*;\\s*url=(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
				final Matcher m = p.matcher(value);
				if (m.matches())
				{
					final String url = m.group(1);
					final GetMethod getRefreshPage = new GetMethod(url);
					
					try
					{
						status = m_client.executeMethod(m_hostConfiguration, getRefreshPage, m_httpState);
						if (status != 200)
						{
							// something is probably wrong, but there is not much we can do about it, we handle errors when we post
						}
					} finally
					{
						getRefreshPage.releaseConnection();
					}
				}
				else
				{
					throw new Exception("The refresh header didn't contain a URL");
				}
			}
			else
			{
				throw new Exception("Failed to login to forum, server responded with status code: " + status);
			}
		} finally
		{
			post.releaseConnection();
		}
	}
	
	public boolean postTurnSummary(final String message, final String subject)
	{
		try
		{
			login();
			
			// Now we load the post page, and find the hidden fields needed to post
			final GetMethod get = new GetMethod("http://www.axisandallies.org/forums/index.php?action=post;topic=" + m_topicId + ".0");
			int status = m_client.executeMethod(m_hostConfiguration, get, m_httpState);
			String body = get.getResponseBodyAsString();
			if (status == 200)
			{
				String numReplies;
				String seqNum;
				String sc;
				Matcher m = NUM_REPLIES_PATTERN.matcher(body);
				if (m.matches())
				{
					numReplies = m.group(1);
				}
				else
				{
					throw new Exception("Hidden field 'num_replies' not found on page");
				}
				
				m = SEQ_NUM_PATTERN.matcher(body);
				if (m.matches())
				{
					seqNum = m.group(1);
				}
				else
				{
					throw new Exception("Hidden field 'seqnum' not found on page");
				}
				
				m = SC_PATTERN.matcher(body);
				if (m.matches())
				{
					sc = m.group(1);
				}
				else
				{
					throw new Exception("Hidden field 'sc' not found on page");
				}
				
				// now we have the required hidden fields to reply to
				final PostMethod post = new PostMethod("http://www.axisandallies.org/forums/index.php?action=post2;start=0;board=40");
				
				try
				{
					// Construct the multi part post
					final List<Part> parts = new ArrayList<Part>();
					
					parts.add(createStringPart("topic", m_topicId));
					parts.add(createStringPart("subject", subject));
					parts.add(createStringPart("icon", "xx"));
					parts.add(createStringPart("message", message));
					parts.add(createStringPart("notify", "0"));
					
					if (m_includeSaveGame && m_saveGameFile != null)
					{
						final FilePart part = new FilePart("attachment[]", m_saveGameFileName, m_saveGameFile);
						part.setContentType("application/octet-stream");
						part.setTransferEncoding(null);
						part.setCharSet(null);
						parts.add(part);
					}
					
					parts.add(createStringPart("post", "Post"));
					parts.add(createStringPart("num_replies", numReplies));
					parts.add(createStringPart("additional_options", "1"));
					parts.add(createStringPart("sc", sc));
					parts.add(createStringPart("seqnum", seqNum));
					
					final MultipartRequestEntity entity = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), new HttpMethodParams());
					post.setRequestEntity(entity);
					
					// add headers
					post.addRequestHeader("Referer", "http://www.axisandallies.org/forums/index.php?action=post;topic=" + m_topicId + ".0;num_replies=" + numReplies);
					post.addRequestHeader("Accept", "*/*");
					
					try
					{
						// the site has spam prevention which means you can't post until 15 seconds after login
						Thread.sleep(15 * 1000);
					} catch (final InterruptedException ie)
					{
						ie.printStackTrace(); // this should never happen
					}
					
					post.setFollowRedirects(false);
					status = m_client.executeMethod(m_hostConfiguration, post, m_httpState);
					body = post.getResponseBodyAsString();
					if (status == 302)
					{
						// site responds with a 302 redirect back to the forum index (board=40)
						
						// The syntax for post is ".....topic=xx.yy" where xx is the thread id, and yy is the post number in the given thread
						// since the site is lenient we can just give a high post_number to go to the last post in the thread
						m_turnSummaryRef = "http://www.axisandallies.org/forums/index.php?topic=" + m_topicId + ".10000";
					}
					else
					{
						// these two patterns find general errors, where the first pattern checks if the error text appears,
						// the second pattern extracts the error message. This could be the "The last posting from your IP was less than 15 seconds ago.Please try again later"
						
						// this patter finds errors that are marked in red (for instance "You are not allowed to post URLs", or
						// "Some one else has posted while you vere reading"
						
						Matcher matcher = ERROR_LIST_PATTERN.matcher(body);
						if (matcher.matches())
						{
							throw new Exception("The site gave an error: '" + matcher.group(1) + "'");
						}
						
						matcher = AN_ERROR_OCCURRED_PATTERN.matcher(body);
						if (matcher.matches())
						{
							matcher = ERROR_TEXT_PATTERN.matcher(body);
							if (matcher.matches())
							{
								throw new Exception("The site gave an error: '" + matcher.group(1) + "'");
							}
						}
						
						final Header refreshHeader = post.getResponseHeader("Refresh");
						if (refreshHeader != null)
						{
							// sometimes the message will be flagged as spam, and a refresh url is given
							final String value = refreshHeader.getValue(); // refresh: 0; URL=http://...topic=26114.new%3bspam=true#new
							final Pattern p = Pattern.compile("[^;]*;\\s*url=.*spam=true.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
							m = p.matcher(value);
							if (m.matches())
							{
								throw new Exception("The summary was posted but was flagged as spam");
							}
						}
						
						throw new Exception("Unknown error, post a bug to the tripleA development team");
					}
				} finally
				{
					post.releaseConnection();
					final GetMethod logout = new GetMethod("http://www.axisandallies.org/forums/index.php?action=logout;sesc=" + sc);
					try
					{
						status = m_client.executeMethod(m_hostConfiguration, logout, m_httpState);
						// site responds with a 200 + Refresh header to redirect to index.php
						if (status != 200)
						{
							// nothing we can do if this fails
						}
					} finally
					{
						logout.releaseConnection();
					}
				}
			}
			else
			{
				throw new Exception("Unable to load forum post " + m_topicId);
			}
			
		} catch (final Exception e)
		{
			m_turnSummaryRef = e.getMessage();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Utility method for creating string parts, since we need to remove transferEncoding and content type to behave like a browser
	 * 
	 * @param name
	 *            the form field name
	 * @param value
	 *            the for field value
	 * @return return the created StringPart
	 */
	private StringPart createStringPart(final String name, final String value)
	{
		final StringPart stringPart = new StringPart(name, value);
		stringPart.setTransferEncoding(null);
		stringPart.setContentType(null);
		return stringPart;
	}
	
	public String getDisplayName()
	{
		return "AxisAndAllies.org";
	}
	
	/**
	 * Create a clone of the poster
	 * 
	 * @return a copy
	 */
	public IForumPoster doClone()
	{
		final AxisAndAlliesForumPoster clone = new AxisAndAlliesForumPoster();
		clone.setTopicId(getTopicId());
		clone.setIncludeSaveGame(getIncludeSaveGame());
		clone.setPassword(getPassword());
		clone.setUsername(getUsername());
		return clone;
	}
	
	public boolean supportsSaveGame()
	{
		return true;
	}
	
	public void viewPosted()
	{
		final String url = "http://www.axisandallies.org/forums/index.php?topic=" + m_topicId + ".10000";
		BrowserControl.displayURL(url);
	}
	
	public String getTestMessage()
	{
		return "Testing, this will take about 20 seconds...";
	}
	
	public String getHelpText()
	{
		return HelpSupport.loadHelp("axisAndAlliesForum.html");
	}

}
