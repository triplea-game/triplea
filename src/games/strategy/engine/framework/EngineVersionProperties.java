package games.strategy.engine.framework;

import games.strategy.engine.EngineVersion;
import games.strategy.util.Version;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

public class EngineVersionProperties
{
	private final Version m_latestVersionOut;
	private final Map<Version, String> m_releaseNotes;
	private final String m_link;
	private volatile boolean m_done = false;
	private static final String s_linkToTripleA = "http://triplea.sourceforge.net/latest/latest_version.properties";
	
	private EngineVersionProperties(final URL url)
	{
		this(getProperties(url));
	}
	
	private EngineVersionProperties(final Properties props)
	{
		m_latestVersionOut = new Version(props.getProperty("LATEST", EngineVersion.VERSION.toStringFull(".")));
		m_link = props.getProperty("LINK", "http://triplea.sourceforge.net/");
		m_releaseNotes = new HashMap<Version, String>();
		for (final Entry<Object, Object> entry : props.entrySet())
		{
			final String key = (String) entry.getKey();
			if (key != null && key.length() > 6 && key.startsWith("NOTES_"))
			{
				final Version version = new Version(key.substring(6));
				if (EngineVersion.VERSION.isLessThan(version, false))
				{
					final String value = (String) entry.getValue();
					if (value != null && value.trim().length() > 0)
					{
						m_releaseNotes.put(version, value);
					}
				}
			}
		}
		m_done = true;
	}
	
	public static EngineVersionProperties contactServerForEngineVersionProperties()
	{
		final URL engineversionPropsURL;
		try
		{
			engineversionPropsURL = new URL(s_linkToTripleA);
		} catch (final MalformedURLException e)
		{
			e.printStackTrace();
			return new EngineVersionProperties(new Properties());
		}
		return contactServerForEngineVersionProperties(engineversionPropsURL);
	}
	
	private static EngineVersionProperties contactServerForEngineVersionProperties(final URL engineversionPropsURL)
	{
		// sourceforge sometimes takes a long while to return results
		// so run a couple requests in parallel, starting with delays to try and get a response quickly
		final AtomicReference<EngineVersionProperties> ref = new AtomicReference<EngineVersionProperties>();
		final CountDownLatch latch = new CountDownLatch(1);
		final Runnable r = new Runnable()
		{
			public void run()
			{
				for (int i = 0; i < 5; i++)
				{
					spawnRequest(engineversionPropsURL, ref, latch);
					try
					{
						latch.await(2, TimeUnit.SECONDS);
					} catch (final InterruptedException e)
					{
						e.printStackTrace();
					}
					if (ref.get() != null)
						break;
				}
				// we have spawned a bunch of requests
				try
				{
					latch.await(15, TimeUnit.SECONDS);
				} catch (final InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
			private void spawnRequest(final URL engineversionPropsURL, final AtomicReference<EngineVersionProperties> ref, final CountDownLatch latch)
			{
				final Thread t1 = new Thread(new Runnable()
				{
					public void run()
					{
						ref.set(new EngineVersionProperties(engineversionPropsURL));
						latch.countDown();
					}
				});
				t1.start();
			}
		};
		runInBackground(null, "Checking for Latest Version", r);
		final EngineVersionProperties props = ref.get();
		return props;
	}
	
	private static void runInBackground(final Component parent, final String waitMessage, final Runnable r)
	{
		// we do not need to alert the user to this, or have a waiting window or progress window. just check in the background.
		// BackgroundTaskRunner.runInBackground(parent, waitMessage, r);
		r.run();
	}
	
	private static Properties getProperties(final URL url)
	{
		final Properties props = new Properties();
		final HttpClient client = new HttpClient();
		final HostConfiguration config = client.getHostConfiguration();
		config.setHost(url.getHost());
		// add the proxy
		GameRunner2.addProxy(config);
		
		final GetMethod method = new GetMethod(url.getPath());
		// pretend to be ie
		method.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
		try
		{
			client.executeMethod(method);
			final String propsString = method.getResponseBodyAsString();
			props.load(new ByteArrayInputStream(propsString.getBytes()));
		} catch (final Exception ioe)
		{
			// do nothing, we return an empty properties
		} finally
		{
			method.releaseConnection();
		}
		return props;
	}
	
	public boolean isDone()
	{
		return m_done;
	}
	
	public Version getLatestVersionOut()
	{
		return m_latestVersionOut;
	}
	
	public String getLinkToDownloadLatestVersion()
	{
		return m_link;
	}
	
	public Map<Version, String> getReleaseNotes()
	{
		return m_releaseNotes;
	}
	
	public String getOutOfDateMessage()
	{
		final StringBuilder text = new StringBuilder("<html>");
		text.append("<b>A new version of TripleA is out.  Please Update TripleA!</b>");
		text.append("<br />Your current version: " + EngineVersion.VERSION);
		text.append("<br />Latest version available for download: " + getLatestVersionOut());
		text.append("<br /><br /><a class=\"external\" href=\"" + getLinkToDownloadLatestVersion() + "\">" + getLinkToDownloadLatestVersion() + "</a>");
		text.append("<br /><br />");
		text.append("</html>");
		return text.toString();
	}
	
	public static void main(final String[] args) throws Exception
	{
		final URL url = new URL(s_linkToTripleA);
		final EngineVersionProperties props = new EngineVersionProperties(url);
		System.out.println(props.getLatestVersionOut());
		System.out.println(props.getLinkToDownloadLatestVersion());
		System.out.println(props.getReleaseNotes());
	}
}
