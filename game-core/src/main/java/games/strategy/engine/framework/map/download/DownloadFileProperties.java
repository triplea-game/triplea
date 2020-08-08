package games.strategy.engine.framework.map.download;

import games.strategy.engine.ClientContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.java.DateTimeFormatterUtil;
import org.triplea.util.Version;

/** Properties file used to know which map versions have been installed. */
@Log
class DownloadFileProperties {
  static final String VERSION_PROPERTY = "map.version";
  private final Properties props = new Properties();

  static DownloadFileProperties loadForZip(final File zipFile) {
    if (!fromZip(zipFile).exists()) {
      return new DownloadFileProperties();
    }
    final DownloadFileProperties downloadFileProperties = new DownloadFileProperties();
    try (InputStream fis = new FileInputStream(fromZip(zipFile))) {
      downloadFileProperties.props.load(fis);
    } catch (final IOException e) {
      log.log(
          Level.SEVERE, "Failed to read property file: " + fromZip(zipFile).getAbsolutePath(), e);
    }
    return downloadFileProperties;
  }

  static void saveForZip(final File zipFile, final DownloadFileProperties props) {
    try (OutputStream fos = new FileOutputStream(fromZip(zipFile))) {
      props.props.store(fos, null);
    } catch (final IOException e) {
      log.log(
          Level.SEVERE,
          "Failed to write property file to: " + fromZip(zipFile).getAbsolutePath(),
          e);
    }
  }

  private static File fromZip(final File zipFile) {
    return new File(zipFile.getAbsolutePath() + ".properties");
  }

  Version getVersion() {
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

  void setFrom(final DownloadFileDescription selected) {
    setVersion(selected.getVersion());
    props.setProperty("map.url", selected.getUrl());
    props.setProperty(
        "download.time",
        DateTimeFormatterUtil.toDateString(LocalDateTime.now(ZoneId.systemDefault())));
    props.setProperty("engine.version", ClientContext.engineVersion().toString());
  }
}
