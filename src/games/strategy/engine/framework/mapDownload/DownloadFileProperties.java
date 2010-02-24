package games.strategy.engine.framework.mapDownload;

import games.strategy.engine.EngineVersion;
import games.strategy.util.Version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;


class DownloadFileProperties {

	private static final String VERSION = "map.version";
	private Properties props = new Properties();
	
	public static DownloadFileProperties loadForZip(File zipFile) {
		
		if(!fromZip(zipFile).exists()) {
			return new DownloadFileProperties();
		}
		
		DownloadFileProperties rVal = new DownloadFileProperties();
		try
		{
			FileInputStream fis = new FileInputStream(fromZip(zipFile));
			try
			{
				rVal.props.load(fis);
			} finally {
				fis.close();
			}
		} catch(IOException e) {
			e.printStackTrace(System.out);
		}
		return rVal;
		
	}
	
	public static void saveForZip(File zipFile, DownloadFileProperties props) {
		try
		{
			FileOutputStream fos = new FileOutputStream(fromZip(zipFile));
			try
			{
				props.props.store(fos, null);
			} finally {
				fos.close();
			}
		} catch(IOException e) {
			e.printStackTrace(System.out);
		}
	}
	
	public DownloadFileProperties() {
		
	}
	
	private static File fromZip(File zipFile) { 
		return new File(zipFile.getParent(), zipFile.getName() + ".properties");
	}
	

	public Version getVersion() {
		if(!props.containsKey(VERSION)) {
			return null;
		}
		return new Version(props.getProperty(VERSION));
	}
	
	private void setVersion(Version v) {
		if(v != null) {
			props.put(VERSION, v.toString());
		}
	}

	public void setFrom(DownloadFileDescription selected) {	
		setVersion(selected.getVersion());
		props.setProperty("map.url",  selected.getUrl());
		props.setProperty("download.time", new Date().toString());
		props.setProperty("download.hostedBy", selected.getHostedUrl());
		props.setProperty("engine.version", EngineVersion.VERSION.toString());
		
		
	}	
}
