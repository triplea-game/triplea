package games.strategy.triplea.printgenerator;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.extern.slf4j.Slf4j;

/** Helper superclass to consistently write the files. */
@Slf4j
abstract class InfoForFile {
  public static final String FILE_NAME_GENERAL_INFORMATION_CSV = "General Information.csv";
  public static final String DELIMITER = ",";
  public static final String LINE_SEPARATOR = "\r\n";

  /**
   * Saves the data to be printed by collecting it in {@link
   * #gatherDataBeforeWriting(PrintGenerationData)} before the actual writing with {@link
   * #writeIntoFile(Writer)}. It also gathers the output file path from {@code printData}.
   *
   * @param printData base object from which the data to be printed can be inferred.
   */
  void saveToFile(final PrintGenerationData printData) {
    gatherDataBeforeWriting(printData);
    final Path outFile = printData.getOutDir().resolve(FILE_NAME_GENERAL_INFORMATION_CSV);
    try {
      try (Writer writer =
          Files.newBufferedWriter(
              outFile,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.APPEND)) {
        writeIntoFile(writer);
      }
    } catch (final IOException e) {
      log.error("Failed to save print generation data general information to file {}", outFile, e);
    }
  }

  /**
   * Collects the data later used for writing to the output file.
   *
   * @param printData base object from which the data to be printed can be inferred.
   */
  protected abstract void gatherDataBeforeWriting(PrintGenerationData printData);

  protected abstract void writeIntoFile(Writer writer) throws IOException;
}
