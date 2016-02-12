package games.strategy.engine.framework.mapDownload;

import java.io.File;
import java.util.Optional;

import games.strategy.util.Version;

public class FileSystemStrategy {


  private final File rootFolder;

  public FileSystemStrategy(final File rootFolder) {
    this.rootFolder = rootFolder;
  }

  public Optional<Version> getMapVersion(final String mapName) {
    final String mapFileName = convertToFileName(mapName);
    final File potentialFile = new File(rootFolder, mapFileName);

    if (!potentialFile.exists()) {
      return Optional.empty();
    } else {
      final DownloadFileProperties props = DownloadFileProperties.loadForZip(potentialFile);
      if( props.getVersion() == null ) {
        return Optional.empty();
      } else{
        return Optional.of(props.getVersion());
      }
    }
  }

  public static String convertToFileName(String mapName) {
    return mapName + ".zip";
  }
}
