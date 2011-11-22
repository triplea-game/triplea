package games.strategy.engine.framework.mapDownload;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadRunnable implements Runnable
{
	private final String urlString;
	private volatile byte[] contents;
	private volatile String error;
	
	public DownloadRunnable(final String urlString)
	{
		super();
		this.urlString = urlString;
	}
	
	public byte[] getContents()
	{
		return contents;
	}
	
	public void setContents(final byte[] contents)
	{
		this.contents = contents;
	}
	
	public String getError()
	{
		return error;
	}
	
	public void setError(final String error)
	{
		this.error = error;
	}
	
	public void run()
	{
		URL url;
		try
		{
			url = new URL(urlString.trim());
		} catch (final MalformedURLException e1)
		{
			error = "invalid url";
			return;
		}
		InputStream stream;
		try
		{
			stream = url.openStream();
			try
			{
				final ByteArrayOutputStream sink = new ByteArrayOutputStream();
				InstallMapDialog.copy(sink, stream);
				contents = sink.toByteArray();
			} finally
			{
				stream.close();
			}
		} catch (final Exception e)
		{
			error = e.getMessage();
		}
	}
}
