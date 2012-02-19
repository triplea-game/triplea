package games.strategy.engine.pbem;

import games.strategy.net.BrowserControl;
import games.strategy.triplea.help.HelpSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * A poster for www.tripleawarclub.org forum
 * We log in and out every time we post, so we don't need to keep state.
 * 
 * @author Klaus Groenbaek
 */
public class TripleAWarClubForumPoster extends AbstractForumPoster
{
	// -----------------------------------------------------------------------
	// constants
	// -----------------------------------------------------------------------
	private static final long serialVersionUID = -4017550807078258152L;
	private static String m_host = "www.tripleawarclub.org";
	private static String s_forumId = "20";
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	private static Pattern s_XOOPS_TOKEN_REQUEST = Pattern.compile(".*XOOPS_TOKEN_REQUEST[^>]*value=\"([^\"]*)\".*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	
	private transient HttpState m_httpState;
	private transient HostConfiguration m_hostConfiguration;
	private transient HttpClient m_client;
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	/**
	 * Logs into the website
	 * 
	 * @throws Exception
	 *             if login fails
	 */
	private void login() throws Exception
	{
		m_client = new HttpClient();
		m_client.getParams().setParameter("http.protocol.single-cookie-header", true);
		m_client.getParams().setParameter("http.useragent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0)");
		m_httpState = new HttpState();
		m_hostConfiguration = new HostConfiguration();
		m_hostConfiguration.setProxy("localhost", 8888); // enable to debug http through Fiddler2
		m_hostConfiguration.setHost("www.tripleawarclub.org");
		
		final PostMethod post = new PostMethod("http://www.tripleawarclub.org/user.php");
		final List<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(new NameValuePair("uname", getUsername()));
		parameters.add(new NameValuePair("pass", getPassword()));
		parameters.add(new NameValuePair("submit", "Login"));
		parameters.add(new NameValuePair("rememberme", "On"));
		parameters.add(new NameValuePair("xoops_redirect", "/"));
		parameters.add(new NameValuePair("op", "login"));
		post.setRequestBody(parameters.toArray(new NameValuePair[parameters.size()]));
		
		final int status = m_client.executeMethod(m_hostConfiguration, post, m_httpState);
		if (status != 200)
		{
			throw new Exception("Login failed, server returned status: " + status);
		}
		final String body = post.getResponseBodyAsString();
		final String lowerBody = body.toLowerCase();
		if (lowerBody.contains("incorrect login!"))
		{
			throw new Exception("Incorrect login credentials");
		}
		
		if (!lowerBody.contains("thank you for logging in"))
		{
			System.out.println("Unknown login error, site response " + body);
			throw new Exception("Unknown login error");
		}
		
	}
	
	/**
	 * Post the turn summary and save game to the forum
	 * After login we must load the post page to get the XOOPS_TOKEN_REQUEST (which I think is CSRF nounce)
	 * then we can post the reply
	 * 
	 * @param summary
	 *            the forum summary
	 * @param subject
	 *            the forum subject
	 * @return true if the post was successful
	 */
	public boolean postTurnSummary(final String summary, final String subject)
	{
		try
		{
			login();
			
			// load the reply page
			
			final String url = "http://www.tripleawarclub.org/modules/newbb/reply.php?forum=" + s_forumId + "&topic_id=" + m_topicId;
			GetMethod get = new GetMethod(url);
			String XOOPS_TOKEN_REQUEST;
			try
			{
				final int status = m_client.executeMethod(m_hostConfiguration, get, m_httpState);
				if (status != 200)
				{
					throw new Exception("Could not load reply page: " + url + ". Site returned " + status);
				}
				
				final String body = get.getResponseBodyAsString();
				final Matcher m = s_XOOPS_TOKEN_REQUEST.matcher(body);
				if (!m.matches())
				{
					throw new Exception("Unable to find 'XOOPS_TOKEN_REQUEST' form field on reply page");
				}
				XOOPS_TOKEN_REQUEST = m.group(1);
			} finally
			{
				get.releaseConnection();
			}
			
			final List<Part> parts = new ArrayList<Part>();
			parts.add(createStringPart("subject", subject));
			parts.add(createStringPart("message", summary));
			parts.add(createStringPart("forum", s_forumId));
			parts.add(createStringPart("topic_id", m_topicId));
			parts.add(createStringPart("XOOPS_TOKEN_REQUEST", XOOPS_TOKEN_REQUEST));
			parts.add(createStringPart("xoops_upload_file[]", "userfile"));
			parts.add(createStringPart("contents_submit", "Submit"));
			
			parts.add(createStringPart("doxcode", "1"));
			parts.add(createStringPart("dosmiley", "1"));
			parts.add(createStringPart("dohtml", "1"));
			parts.add(createStringPart("dobr", "1"));
			parts.add(createStringPart("editor", "dhtmltextarea"));
			
			if (m_includeSaveGame && m_saveGameFile != null)
			{
				final FilePart part = new FilePart("userfile", m_saveGameFileName, m_saveGameFile);
				part.setContentType("application/octet-stream");
				part.setTransferEncoding(null);
				part.setCharSet(null);
				parts.add(part);
			}
			
			final MultipartRequestEntity entity = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), new HttpMethodParams());
			final PostMethod post = new PostMethod("http://www.tripleawarclub.org/modules/newbb/post.php");
			post.setRequestEntity(entity);
			
			try
			{
				final int status = m_client.executeMethod(m_hostConfiguration, post, m_httpState);
				if (status != 200)
				{
					throw new Exception("Posting summary failed, the server returned status: " + status);
				}
				final String body = post.getResponseBodyAsString();
				if (!body.toLowerCase().contains("thanks for your submission!"))
				{
					throw new Exception("Posting summary failed, the server didn't respond with thank you message");
				}
				m_turnSummaryRef = "www.tripleawarclub.org/modules/newbb/viewtopic.php?topic_id=" + m_topicId + "&forum=" + s_forumId;
				
				// now logout, this is just to be nice, so we don't care if this fails
				get = new GetMethod("http://www.tripleawarclub.org/user.php?op=logout");
				try
				{
					m_client.executeMethod(m_hostConfiguration, get, m_httpState);
				} finally
				{
					get.releaseConnection();
				}
				
			} finally
			{
				post.releaseConnection();
			}
		} catch (final Exception e)
		{
			m_turnSummaryRef = e.getMessage();
			e.printStackTrace();
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
	
	public String getTestMessage()
	{
		return "Testing, this will take a couple of seconds...";
	}
	
	public String getHelpText()
	{
		return HelpSupport.loadHelp("tripleAWarClubForum.html");
	}
	
	public IForumPoster doClone()
	{
		final TripleAWarClubForumPoster clone = new TripleAWarClubForumPoster();
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
	
	public String getDisplayName()
	{
		return "TripleWarClub.org";
	}
	
	public void viewPosted()
	{
		final String url = "http://" + m_host + "/modules/newbb/viewtopic.php?topic_id=" + m_topicId;
		BrowserControl.displayURL(url);
	}
	
}
