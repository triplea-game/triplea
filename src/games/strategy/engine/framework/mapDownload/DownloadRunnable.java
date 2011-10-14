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
	
	public DownloadRunnable(String urlString)
	{
		super();
		this.urlString = urlString;
	}
	
	public byte[] getContents()
	{
		return contents;
	}
	
	public void setContents(byte[] contents)
	{
		this.contents = contents;
	}
	
	public String getError()
	{
		return error;
	}
	
	public void setError(String error)
	{
		this.error = error;
	}
	
	@Override
	public void run()
	{
		
		URL url;
		try
		{
			url = new URL(urlString.trim());
			
		} catch (MalformedURLException e1)
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
				ByteArrayOutputStream sink = new ByteArrayOutputStream();
				InstallMapDialog.copy(sink, stream);
				contents = sink.toByteArray();
			} finally
			{
				stream.close();
			}
		} catch (Exception e)
		{
			error = e.getMessage();
		}
	}
	
}
