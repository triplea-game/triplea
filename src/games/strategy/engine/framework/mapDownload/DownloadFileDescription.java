package games.strategy.engine.framework.mapDownload;

public class DownloadFileDescription {

	
	private final String url;
	private final String description;
	private final String mapName;
	
	public DownloadFileDescription(String url, String description,
			String mapName) {
		super();
		this.url = url;
		this.description = description;
		this.mapName = mapName;
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
	
	
	
}
