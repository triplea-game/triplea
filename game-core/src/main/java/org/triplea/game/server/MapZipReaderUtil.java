package org.triplea.game.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

@UtilityClass
@Log
class MapZipReaderUtil {

  /**
   * Finds all game XMLs in a zip file. More specifically, given a zip file, finds all '*.xml' files
   * that have a 'games/' folder on the zip file path.
   */
  List<URI> findGameXmlFilesInZip(final File zipFile) {
    final List<URI> zipFiles = new ArrayList<>();

    try (InputStream fis = new FileInputStream(zipFile);
        ZipInputStream zis = new ZipInputStream(fis);
        URLClassLoader loader = new URLClassLoader(new URL[] {zipFile.toURI().toURL()})) {
      ZipEntry entry = zis.getNextEntry();
      while (entry != null) {
        if (entry.getName().contains("games/") && entry.getName().toLowerCase().endsWith(".xml")) {
          Optional.ofNullable(loader.getResource(entry.getName()))
              .map(url -> URI.create(url.toString().replace(" ", "%20")))
              .ifPresent(zipFiles::add);
        }
        // we have to close the loader to allow files to be deleted on windows
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error reading zip file in: " + zipFile.getAbsolutePath(), e);
    }
    return zipFiles;
  }
}
