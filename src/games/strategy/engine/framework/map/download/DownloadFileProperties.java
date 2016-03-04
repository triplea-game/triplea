package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.util.Version;

/** Properties file used to know which map versions have been installed */
public class DownloadFileProperties {
  protected static final String VERSION_PROPERTY = "map.version";
  private final Properties props = new Properties();

  public static DownloadFileProperties loadForZip(final File zipFile) {
    if (!fromZip(zipFile).exists()) {
      return new DownloadFileProperties();
    }
    final DownloadFileProperties rVal = new DownloadFileProperties();
    try (final FileInputStream fis = new FileInputStream(fromZip(zipFile))) {
      rVal.props.load(fis);
    } catch (final IOException e) {
      ClientLogger.logError("Failed to read property file: " + fromZip(zipFile).getAbsolutePath(), e);
    }
    return rVal;
  }

  public static void saveForZip(final File zipFile, final DownloadFileProperties props) {
    try (final FileOutputStream fos = new FileOutputStream(fromZip(zipFile))) {
      props.props.store(fos, null);
    } catch (final IOException e) {
      ClientLogger.logError("Failed to write property file to: " + fromZip(zipFile).getAbsolutePath(), e);
    }
  }

  public DownloadFileProperties() {}

  private static File fromZip(final File zipFile) {
    return new File(zipFile.getAbsolutePath() + ".properties");
  }

  public Version getVersion() {
    if (!props.containsKey(VERSION_PROPERTY)) {
      return null;
    }
    return new Version(props.getProperty(VERSION_PROPERTY));
  }

  private void setVersion(final Version v) {
    if (v != null) {
      props.put(VERSION_PROPERTY, v.toString());
    }
  }

  public void setFrom(final DownloadFileDescription selected) {
    setVersion(selected.getVersion());
    props.setProperty("map.url", selected.getUrl());
    props.setProperty("download.time", new Date().toString());
    props.setProperty("engine.version", ClientContext.engineVersion().toString());
  }
}
