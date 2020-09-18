package games.strategy.engine.framework.map.file.system.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

@UtilityClass
@Log
class MapZipReaderUtil {

  /**
   * Finds all game XMLs in a zip file. More specifically, given a zip file, finds all '*.xml' files
   * that have a 'games/' folder on the zip file path.
   */
  List<URI> findGameXmlFilesInZip(final File zip) {
    final List<URI> zipFiles = new ArrayList<>();

    try (ZipFile zipFile = new ZipFile(zip);
         URLClassLoader loader = new URLClassLoader(new URL[] {zip.toURI().toURL()})) {

      final Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();

        if (entry.getName().toLowerCase().endsWith(".xml")) {
          Optional.ofNullable(loader.getResource(entry.getName()))
              .map(url -> URI.create(url.toString().replace(" ", "%20")))
              .ifPresent(zipFiles::add);
        }
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error reading zip file in: " + zip.getAbsolutePath(), e);
    }
    return zipFiles;
  }
}
