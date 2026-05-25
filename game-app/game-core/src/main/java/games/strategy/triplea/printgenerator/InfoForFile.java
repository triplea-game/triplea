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
  private final Path chosenOutFile;

  /**
   * Escapes a string for inclusion in a CSV cell per RFC 4180. Wraps the value in double quotes
   * (and doubles any inner quotes) when it contains characters that could be misinterpreted by a
   * spreadsheet reader — including a space, since common importers (LibreOffice, Excel) auto-detect
   * space as a delimiter and would otherwise split a name like "Cestra Regina" across columns.
   */
  protected static String csvField(final String value) {
    if (value == null) {
      return "";
    }
    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);
      if (c == ',' || c == '"' || c == '\r' || c == '\n' || c == ' ') {
        return "\"" + value.replace("\"", "\"\"") + "\"";
      }
    }
    return value;
  }

  InfoForFile() {
    this.chosenOutFile = null;
  }

  InfoForFile(Path chosenOutFile) {
    this.chosenOutFile = chosenOutFile;
  }

  /**
   * Saves the data to be printed by collecting it in {@link
   * #gatherDataBeforeWriting(PrintGenerationData)} before the actual writing with {@link
   * #writeIntoFile(Writer)}. It also gathers the output file path from {@code printData}.
   *
   * @param printData base object from which the data to be printed can be inferred.
   */
  void saveToFile(final PrintGenerationData printData) {
    gatherDataBeforeWriting(printData);
    final Path outFile =
        chosenOutFile != null
            ? chosenOutFile
            : printData.getOutDir().resolve(FILE_NAME_GENERAL_INFORMATION_CSV);
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

  protected abstract void writeIntoFile(final Writer writer) throws IOException;
}
