package org.triplea.maps.upload;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
class UploadTestUtil {

  /**
   * Zips a target directory.
   *
   * @param directory The directory to zip
   * @return Path to zipped file. Zip file name will match the directory name suffixed with '.zip'.
   */
  public static Path zipDirectory(final Path directory) throws IOException {
    Preconditions.checkArgument(directory.toFile().isDirectory(), directory.toAbsolutePath());

    try (FileOutputStream fileOutputStream =
            new FileOutputStream(directory.getFileName() + ".zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

      for (final File childFile : directory.toFile().listFiles()) {
        zipSingleFile(childFile, zipOutputStream);
      }
      return Path.of("dirCompessed.zip");
    }
  }

  private static void zipSingleFile(final File file, final ZipOutputStream zipOutputStream)
      throws IOException {
    zipOutputStream.putNextEntry(new ZipEntry(file.getName()));

    // read the file and write to ZipOutputStream
    try (FileInputStream fis = new FileInputStream(file)) {
      final byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) > 0) {
        zipOutputStream.write(buffer, 0, len);
      }
      zipOutputStream.closeEntry();
    }
  }
}
