package org.triplea.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ZipFileUtil {

  /**
   * Finds all game XMLs in a zip file. More specifically, given a zip file, finds all '*.xml' files
   */
  public List<URI> findXmlFilesInZip(final File zip) {
    try (ZipFile zipFile = new ZipFile(zip);
        FileSystem fileSystem = FileSystems.newFileSystem(zip.toPath(), null)) {

      return zipFile.stream()
          .map(ZipEntry::getName)
          .filter(name -> name.toLowerCase().endsWith(".xml"))
          .map(fileSystem::getPath)
          .map(Path::toUri)
          .collect(Collectors.toList());
    } catch (final IOException e) {
      log.error("Error reading zip file in: " + zip.getAbsolutePath(), e);
    }
    return new ArrayList<>();
  }
}
