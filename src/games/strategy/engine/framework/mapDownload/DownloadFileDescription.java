package games.strategy.engine.framework.mapDownload;

import games.strategy.util.Version;

public class DownloadFileDescription {

	
	private final String url;
	private final String description;
	private final String mapName;
	private final Version version;
	private final String hostedUrl;
	
	public String getHostedUrl() {
		return hostedUrl;
	}

	public DownloadFileDescription(String url, String description,
			String mapName, Version version, String hostedUrl) {
		super();
		this.url = url;
		this.description = description;
		this.mapName = mapName;
		this.version = version;
		this.hostedUrl = hostedUrl;
	}

	public String getUrl() {
		return url;
	}

	public String getDescription() {
		return description;
	}

	public String getMapName() {
		return mapName;
	}
	
	public boolean isDummyUrl() {
		return url.startsWith("!");
	}

	public Version getVersion() {
		return version;
	}
	
	
	
}
